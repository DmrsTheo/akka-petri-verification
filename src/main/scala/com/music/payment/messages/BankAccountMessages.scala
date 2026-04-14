package com.music.payment.messages
import com.music.payment.messages.CommonMessages.{OperationResult, BalanceResponse}
/**
 * Messages for the BankAccount actor.
 * Manages operations on bank accounts (deposit, withdrawal, balance query).
 */
object BankAccountMessages {

  /** Deposit an amount to the account */
  case class Deposit(amount: Double, replyTo: akka.actor.typed.ActorRef[OperationResult])

  /** Retirer un montant du compte */
  case class Withdraw(amount: Double, replyTo: akka.actor.typed.ActorRef[OperationResult])

  /** Demander le solde actuel */
  case class GetBalance(replyTo: akka.actor.typed.ActorRef[BalanceResponse])

  /** Type union pour les commandes BankAccount */
  sealed trait AccountCommand
  case class DepositCmd(amount: Double, replyTo: akka.actor.typed.ActorRef[OperationResult]) extends AccountCommand
  case class WithdrawCmd(amount: Double, replyTo: akka.actor.typed.ActorRef[OperationResult]) extends AccountCommand
  case class GetBalanceCmd(replyTo: akka.actor.typed.ActorRef[BalanceResponse]) extends AccountCommand
}
