package com.music.payment.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import com.music.payment.messages.PaymentMessages._
import com.music.payment.model.Account
import org.slf4j.LoggerFactory

/**
 * Actor managing an individual bank account.
 *
 * Responsibilities :
 * - Manage account balance
 * - Process deposits and withdrawals
 * - Guarantee business invariant : balance >= 0
 *
 * Each instance represents a unique account in the distributed system.
 */
object BankAccount {

  private val logger = LoggerFactory.getLogger(getClass)

  def apply(accountId: String, ownerName: String, initialBalance: Double): Behavior[AccountCommand] = {
    val account = Account(accountId, ownerName, initialBalance)
    logger.info(s"Account created: $accountId (owner: $ownerName, initial balance: $initialBalance)")
    active(account)
  }

  private def active(account: Account): Behavior[AccountCommand] = {
    Behaviors.receive { (context, message) =>
      message match {

        case DepositCmd(amount, replyTo) =>
          try {
            val updated = account.deposit(amount)
            logger.info(s"[${account.id}] Deposit of $amount successful. New balance: ${updated.balance}")
            replyTo ! OperationSuccess(account.id, updated.balance, s"Deposit of $amount completed")
            active(updated)
          } catch {
            case e: IllegalArgumentException =>
              logger.warn(s"[${account.id}] Deposit failed: ${e.getMessage}")
              replyTo ! OperationFailure(account.id, e.getMessage)
              Behaviors.same
          }

        case WithdrawCmd(amount, replyTo) =>
          account.withdraw(amount) match {
            case Right(updated) =>
              logger.info(s"[${account.id}] Withdrawal of $amount successful. New balance: ${updated.balance}")
              replyTo ! OperationSuccess(account.id, updated.balance, s"Withdrawal of $amount completed")
              active(updated)
            case Left(reason) =>
              logger.warn(s"[${account.id}] Withdrawal of $amount refused: $reason")
              replyTo ! OperationFailure(account.id, reason)
              Behaviors.same
          }

        case GetBalanceCmd(replyTo) =>
          logger.debug(s"[${account.id}] Balance query: ${account.balance}")
          replyTo ! BalanceResponse(account.id, account.balance)
          Behaviors.same
      }
    }
  }
}
