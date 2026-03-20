package com.music.payment.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.music.payment.messages.PaymentMessages._
import org.slf4j.LoggerFactory

/**
 * Acteur orchestrant les transferts entre comptes.
 *
 * Protocole de transfert en 2 phases :
 * 1. Débiter le compte source
 * 2. Si succès → créditer le compte destination
 *    Si échec → annuler la transaction
 *
 * Garantit la cohérence des transactions distribuées.
 */
object TransactionManager {

  private val logger = LoggerFactory.getLogger(getClass)

  def apply(accounts: Map[String, ActorRef[AccountCommand]]): Behavior[TransactionCommand] = {
    Behaviors.setup { context =>
      handleTransactions(accounts, context)
    }
  }

  private def handleTransactions(
    accounts: Map[String, ActorRef[AccountCommand]],
    context: akka.actor.typed.scaladsl.ActorContext[TransactionCommand]
  ): Behavior[TransactionCommand] = {
    Behaviors.receiveMessage {

      case TransferRequest(fromId, toId, amount, replyTo) =>
        logger.info(s"Transfert demandé: $fromId → $toId, montant: $amount")

        // Vérifier que les deux comptes existent
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
            replyTo ! TransferFailure(fromId, toId, amount, s"Compte source $fromId introuvable")
            Behaviors.same

          case (_, None) =>
            logger.error(s"Compte destination $toId introuvable")
            replyTo ! TransferFailure(fromId, toId, amount, s"Compte destination $toId introuvable")
            Behaviors.same
        }

      case DebitResult(result, toId, amount, replyTo) =>
        result match {
          case OperationSuccess(fromId, _, _) =>
            // Phase 2 : Créditer le compte destination
            logger.info(s"Débit réussi sur $fromId, crédit en cours sur $toId")
            accounts.get(toId) match {
              case Some(toAccount) =>
                val creditAdapter = context.messageAdapter[OperationResult] {
                  res => CreditResult(res, replyTo)
                }
                toAccount ! DepositCmd(amount, creditAdapter)
              case None =>
                logger.error(s"Compte destination $toId disparu pendant la transaction!")
                replyTo ! TransferFailure("?", toId, amount, "Compte destination disparu")
            }
            Behaviors.same

          case OperationFailure(fromId, reason) =>
            logger.warn(s"Transfert échoué - débit refusé sur $fromId: $reason")
            replyTo ! TransferFailure(fromId, toId, amount, s"Débit refusé: $reason")
            Behaviors.same
        }

      case CreditResult(result, replyTo) =>
        result match {
          case OperationSuccess(toId, newBalance, _) =>
            logger.info(s"Transfert terminé avec succès, $toId nouveau solde: $newBalance")
            replyTo ! TransferSuccess("source", toId, 0, "Transfert effectué avec succès")
            Behaviors.same

          case OperationFailure(toId, reason) =>
            // Cas critique : le débit a réussi mais le crédit a échoué
            logger.error(s"ERREUR CRITIQUE: crédit échoué sur $toId après débit réussi: $reason")
            replyTo ! TransferFailure("source", toId, 0, s"Crédit échoué après débit: $reason")
            Behaviors.same
        }
    }
  }
}
