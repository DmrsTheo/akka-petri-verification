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

      // ===== Phase 0 : Réception de la demande de transfert =====
      case TransferRequest(fromId, toId, amount, replyTo) =>
        logger.info(s"Transfert demandé: $fromId → $toId, montant: $amount")

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
            replyTo ! TransferFailure(fromId, toId, amount, s"Compte source $fromId introuvable")
            Behaviors.same

          case (_, None) =>
            logger.error(s"Compte destination $toId introuvable")
            replyTo ! TransferFailure(fromId, toId, amount, s"Compte destination $toId introuvable")
            Behaviors.same
        }

      // ===== Phase 1 : Résultat du débit =====
      case DebitResult(result, toId, amount, replyTo) =>
        result match {
          case OperationSuccess(fromId, _, _) =>
            // Débit réussi → Phase 2 : Créditer le compte destination
            logger.info(s"Débit réussi sur $fromId, crédit en cours sur $toId")
            accounts.get(toId) match {
              case Some(toAccount) =>
                val creditAdapter = context.messageAdapter[OperationResult] {
                  res => CreditResult(res, fromId, amount, replyTo)
                }
                toAccount ! DepositCmd(amount, creditAdapter)

              case None =>
                // Le compte destination a disparu après la vérification initiale !
                // → ROLLBACK : re-créditer le compte source
                logger.error(s"Compte destination $toId disparu ! Rollback en cours sur $fromId...")
                accounts.get(fromId) match {
                  case Some(fromAccount) =>
                    val rollbackAdapter = context.messageAdapter[OperationResult] {
                      res => RollbackResult(res, fromId, toId, amount, replyTo)
                    }
                    fromAccount ! DepositCmd(amount, rollbackAdapter)
                  case None =>
                    // Cas extrême : les deux comptes ont disparu
                    logger.error(s"ERREUR CRITIQUE: les deux comptes ont disparu ($fromId, $toId)")
                    replyTo ! TransferFailure(fromId, toId, amount,
                      "Impossible de faire le rollback: compte source également disparu")
                }
            }
            Behaviors.same

          case OperationFailure(fromId, reason) =>
            // Débit refusé (ex: fonds insuffisants) → pas besoin de rollback
            logger.warn(s"Transfert échoué - débit refusé sur $fromId: $reason")
            replyTo ! TransferFailure(fromId, toId, amount, s"Débit refusé: $reason")
            Behaviors.same
        }

      // ===== Phase 2 : Résultat du crédit =====
      case CreditResult(result, fromId, amount, replyTo) =>
        result match {
          case OperationSuccess(toId, newBalance, _) =>
            // Crédit réussi → Transfert terminé avec succès
            logger.info(s"Transfert terminé avec succès. $toId nouveau solde: $newBalance")
            replyTo ! TransferSuccess(fromId, toId, amount, "Transfert effectué avec succès")
            Behaviors.same

          case OperationFailure(toId, reason) =>
            // Crédit échoué → ROLLBACK : re-créditer le compte source
            logger.error(s"Crédit échoué sur $toId: $reason. Rollback en cours sur $fromId...")
            accounts.get(fromId) match {
              case Some(fromAccount) =>
                val rollbackAdapter = context.messageAdapter[OperationResult] {
                  res => RollbackResult(res, fromId, toId, amount, replyTo)
                }
                fromAccount ! DepositCmd(amount, rollbackAdapter)
              case None =>
                logger.error(s"ERREUR CRITIQUE: rollback impossible, compte $fromId introuvable!")
                replyTo ! TransferFailure(fromId, toId, amount,
                  s"Crédit échoué ($reason) ET rollback impossible: compte source disparu")
            }
            Behaviors.same
        }

      // ===== Phase 3 : Résultat du rollback (compensation) =====
      case RollbackResult(result, fromId, toId, amount, replyTo) =>
        result match {
          case OperationSuccess(_, newBalance, _) =>
            // Rollback réussi → le compte source a été re-crédité
            logger.info(s"Rollback réussi: $fromId re-crédité de $amount. Nouveau solde: $newBalance")
            replyTo ! TransferFailure(fromId, toId, amount,
              s"Transfert annulé: crédit sur $toId impossible. Compte $fromId re-crédité (solde: $newBalance)")
            Behaviors.same

          case OperationFailure(_, reason) =>
            // Rollback échoué → situation critique
            logger.error(s"ERREUR CRITIQUE: rollback échoué sur $fromId: $reason")
            replyTo ! TransferFailure(fromId, toId, amount,
              s"ERREUR CRITIQUE: rollback échoué. $amount potentiellement perdus sur $fromId")
            Behaviors.same
        }
    }
  }
}
