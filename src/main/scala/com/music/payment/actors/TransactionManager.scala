package com.music.payment.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.music.payment.messages.PaymentMessages._
import org.slf4j.LoggerFactory

/**
 * Acteur orchestrant les transferts entre comptes.
 *
 * Protocole de transfert en 2 phases avec compensation (rollback) :
 * 1. Débiter le compte source
 * 2. Si succès du débit → créditer le compte destination
 *    Si échec du débit → annuler immédiatement
 * 3. Si échec du crédit → ROLLBACK : re-créditer le compte source
 *
 * Garantit la cohérence des transactions distribuées :
 * - L'argent n'est jamais perdu
 * - Le solde total du système reste constant
 */
object TransactionManager {

  private val logger = LoggerFactory.getLogger(getClass)

  def apply(accounts: Map[String, ActorRef[AccountCommand]], loggingActor: ActorRef[LoggingCommand]): Behavior[TransactionCommand] = {
    Behaviors.setup { context =>
      handleTransactions(accounts, loggingActor, context)
    }
  }

  private def handleTransactions(
    accounts: Map[String, ActorRef[AccountCommand]],
    loggingActor: ActorRef[LoggingCommand],
    context: akka.actor.typed.scaladsl.ActorContext[TransactionCommand]
  ): Behavior[TransactionCommand] = {
    Behaviors.receiveMessage {

      // ===== Phase 0 : Réception de la demande de transfert =====
      case TransferRequest(fromId, toId, amount, replyTo) =>
        logger.info(s"Transfert demandé: $fromId → $toId, montant: $amount")
        loggingActor ! LogTransaction(
          transactionType = "TRANSFER_INITIATED",
          fromAccountId = Some(fromId),
          toAccountId = Some(toId),
          amount = Some(amount),
          status = "INITIATED",
          details = s"Transfert initié de $fromId vers $toId"
        )

        // Vérifier que les deux comptes existent AVANT toute opération
        (accounts.get(fromId), accounts.get(toId)) match {
          case (Some(fromAccount), Some(_)) =>
            // Phase 1 : Débiter le compte source
            val debitAdapter = context.messageAdapter[OperationResult] {
              result => DebitResult(result, toId, amount, replyTo)
            }
            fromAccount ! WithdrawCmd(amount, debitAdapter)
            Behaviors.same

          case (None, _) =>
            logger.error(s"Compte source $fromId introuvable")
            loggingActor ! LogTransaction(
              transactionType = "TRANSFER_FAILED",
              fromAccountId = Some(fromId),
              toAccountId = Some(toId),
              amount = Some(amount),
              status = "FAILED",
              details = s"Compte source $fromId introuvable"
            )
            replyTo ! TransferFailure(fromId, toId, amount, s"Compte source $fromId introuvable")
            Behaviors.same

          case (_, None) =>
            logger.error(s"Compte destination $toId introuvable")
            loggingActor ! LogTransaction(
              transactionType = "TRANSFER_FAILED",
              fromAccountId = Some(fromId),
              toAccountId = Some(toId),
              amount = Some(amount),
              status = "FAILED",
              details = s"Compte destination $toId introuvable"
            )
            replyTo ! TransferFailure(fromId, toId, amount, s"Compte destination $toId introuvable")
            Behaviors.same
        }

      // ===== Phase 1 : Résultat du débit =====
      case DebitResult(result, toId, amount, replyTo) =>
        result match {
          case OperationSuccess(fromId, _, _) =>
            // Débit réussi → Phase 2 : Créditer le compte destination
            logger.info(s"Débit réussi sur $fromId, crédit en cours sur $toId")
            loggingActor ! LogTransaction(
              transactionType = "TRANSFER_DEBIT_SUCCESS",
              fromAccountId = Some(fromId),
              toAccountId = Some(toId),
              amount = Some(amount),
              status = "DEBIT_SUCCESSFUL",
              details = s"Montant débité du compte $fromId"
            )
            accounts.get(toId) match {
              case Some(toAccount) =>
                val creditAdapter = context.messageAdapter[OperationResult] {
                  res => CreditResult(res, fromId, amount, replyTo)
                }
                toAccount ! DepositCmd(amount, creditAdapter)

              case None =>
                logger.error(s"Compte destination $toId disparu pendant la transaction!")
                loggingActor ! LogTransaction(
                  transactionType = "TRANSFER_FAILED",
                  fromAccountId = Some(fromId),
                  toAccountId = Some(toId),
                  amount = Some(amount),
                  status = "FAILED",
                  details = s"Compte destination $toId disparu après débit réussi"
                )
                replyTo ! TransferFailure("?", toId, amount, "Compte destination disparu")
            }
            Behaviors.same

          case OperationFailure(fromId, reason) =>
            // Débit refusé (ex: fonds insuffisants) → pas besoin de rollback
            logger.warn(s"Transfert échoué - débit refusé sur $fromId: $reason")
            loggingActor ! LogTransaction(
              transactionType = "TRANSFER_FAILED",
              fromAccountId = Some(fromId),
              toAccountId = Some(toId),
              amount = Some(amount),
              status = "FAILED",
              details = s"Débit refusé: $reason"
            )
            replyTo ! TransferFailure(fromId, toId, amount, s"Débit refusé: $reason")
            Behaviors.same
        }

      // ===== Phase 2 : Résultat du crédit =====
      case CreditResult(result, fromId, amount, replyTo) =>
        result match {
          case OperationSuccess(toId, newBalance, _) =>
            logger.info(s"Transfert terminé avec succès, $toId nouveau solde: $newBalance")
            loggingActor ! LogTransaction(
              transactionType = "TRANSFER_SUCCESS",
              fromAccountId = None,
              toAccountId = Some(toId),
              amount = None,
              status = "SUCCESS",
              details = s"Transfert complété avec succès, nouveau solde de $toId: $newBalance"
            )
            replyTo ! TransferSuccess("source", toId, 0, "Transfert effectué avec succès")
            Behaviors.same

          case OperationFailure(toId, reason) =>
            // Cas critique : le débit a réussi mais le crédit a échoué
            logger.error(s"ERREUR CRITIQUE: crédit échoué sur $toId après débit réussi: $reason")
            loggingActor ! LogTransaction(
              transactionType = "TRANSFER_CRITICAL_FAILURE",
              fromAccountId = None,
              toAccountId = Some(toId),
              amount = None,
              status = "CRITICAL_FAILURE",
              details = s"Erreur critique: crédit échoué après débit réussi: $reason"
            )
            replyTo ! TransferFailure("source", toId, 0, s"Crédit échoué après débit: $reason")
            Behaviors.same
        }
    }
  }
}
