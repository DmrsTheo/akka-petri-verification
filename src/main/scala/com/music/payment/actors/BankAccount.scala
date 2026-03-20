package com.music.payment.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import com.music.payment.messages.PaymentMessages._
import com.music.payment.model.Account
import org.slf4j.LoggerFactory

/**
 * Acteur gérant un compte bancaire individuel.
 *
 * Responsabilités :
 * - Gérer le solde du compte
 * - Traiter les dépôts et retraits
 * - Garantir l'invariant métier : solde >= 0
 *
 * Chaque instance représente un compte unique dans le système distribué.
 */
object BankAccount {

  private val logger = LoggerFactory.getLogger(getClass)

  def apply(accountId: String, ownerName: String, initialBalance: Double): Behavior[AccountCommand] = {
    val account = Account(accountId, ownerName, initialBalance)
    logger.info(s"Compte créé: $accountId (propriétaire: $ownerName, solde initial: $initialBalance)")
    active(account)
  }

  private def active(account: Account): Behavior[AccountCommand] = {
    Behaviors.receive { (context, message) =>
      message match {

        case DepositCmd(amount, replyTo) =>
          try {
            val updated = account.deposit(amount)
            logger.info(s"[${account.id}] Dépôt de $amount réussi. Nouveau solde: ${updated.balance}")
            replyTo ! OperationSuccess(account.id, updated.balance, s"Dépôt de $amount effectué")
            active(updated)
          } catch {
            case e: IllegalArgumentException =>
              logger.warn(s"[${account.id}] Dépôt échoué: ${e.getMessage}")
              replyTo ! OperationFailure(account.id, e.getMessage)
              Behaviors.same
          }

        case WithdrawCmd(amount, replyTo) =>
          account.withdraw(amount) match {
            case Right(updated) =>
              logger.info(s"[${account.id}] Retrait de $amount réussi. Nouveau solde: ${updated.balance}")
              replyTo ! OperationSuccess(account.id, updated.balance, s"Retrait de $amount effectué")
              active(updated)
            case Left(reason) =>
              logger.warn(s"[${account.id}] Retrait de $amount refusé: $reason")
              replyTo ! OperationFailure(account.id, reason)
              Behaviors.same
          }

        case GetBalanceCmd(replyTo) =>
          logger.debug(s"[${account.id}] Consultation du solde: ${account.balance}")
          replyTo ! BalanceResponse(account.id, account.balance)
          Behaviors.same
      }
    }
  }
}
