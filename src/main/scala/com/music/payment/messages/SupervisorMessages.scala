package com.music.payment.messages

import com.music.payment.messages.CommonMessages.{OperationResult, BalanceResponse}
import com.music.payment.messages.TransactionMessages.TransactionResult

/**
 * Messages for the BankSupervisor actor.
 * Supervisor of bank accounts and transactions.
 */
object SupervisorMessages {

  sealed trait SupervisorCommand
  case class CreateAccount(id: String, ownerName: String, initialBalance: Double,
                           replyTo: akka.actor.typed.ActorRef[OperationResult]) extends SupervisorCommand
  case class PerformDeposit(accountId: String, amount: Double,
                            replyTo: akka.actor.typed.ActorRef[OperationResult]) extends SupervisorCommand
  case class PerformWithdraw(accountId: String, amount: Double,
                             replyTo: akka.actor.typed.ActorRef[OperationResult]) extends SupervisorCommand
  case class QueryBalance(accountId: String,
                          replyTo: akka.actor.typed.ActorRef[BalanceResponse]) extends SupervisorCommand
  case class PerformTransfer(fromId: String, toId: String, amount: Double,
                             replyTo: akka.actor.typed.ActorRef[TransactionResult]) extends SupervisorCommand
  case class ListAccounts(replyTo: akka.actor.typed.ActorRef[AccountList]) extends SupervisorCommand

  case class AccountList(accounts: List[String])
}
