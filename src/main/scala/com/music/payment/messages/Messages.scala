package com.music.payment.messages

/**
 * Protocole de messages pour le système de paiement distribué.
 * Définit tous les messages échangés entre les acteurs.
 */
object PaymentMessages {

  // ===== Messages vers BankAccount =====

  /** Déposer un montant sur le compte */
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

  // ===== Réponses du BankAccount =====

  sealed trait OperationResult
  case class OperationSuccess(accountId: String, newBalance: Double, message: String) extends OperationResult
  case class OperationFailure(accountId: String, reason: String) extends OperationResult

  case class BalanceResponse(accountId: String, balance: Double)

  // ===== Messages vers TransactionManager =====

  sealed trait TransactionCommand
  case class TransferRequest(
    fromAccountId: String,
    toAccountId: String,
    amount: Double,
    replyTo: akka.actor.typed.ActorRef[TransactionResult]
  ) extends TransactionCommand

  /** Messages internes pour le protocole de transfert */
  case class DebitResult(result: OperationResult, toAccountId: String, amount: Double,
                         replyTo: akka.actor.typed.ActorRef[TransactionResult]) extends TransactionCommand
  case class CreditResult(result: OperationResult,
                          replyTo: akka.actor.typed.ActorRef[TransactionResult]) extends TransactionCommand

  // ===== Réponses du TransactionManager =====

  sealed trait TransactionResult
  case class TransferSuccess(fromId: String, toId: String, amount: Double, message: String) extends TransactionResult
  case class TransferFailure(fromId: String, toId: String, amount: Double, reason: String) extends TransactionResult

  // ===== Messages vers BankSupervisor =====

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

  // ===== Messages vers LoggingActor =====

  sealed trait LoggingCommand
  case class LogTransaction(
    transactionType: String,
    fromAccountId: Option[String],
    toAccountId: Option[String],
    amount: Option[Double],
    status: String,
    details: String,
    timestamp: Long = System.currentTimeMillis()
  ) extends LoggingCommand
  case class LogAccountOperation(
    operationType: String,
    accountId: String,
    amount: Option[Double],
    newBalance: Double,
    status: String,
    details: String,
    timestamp: Long = System.currentTimeMillis()
  ) extends LoggingCommand
  case class GetTransactionLog(replyTo: akka.actor.typed.ActorRef[TransactionLogResponse]) extends LoggingCommand

  case class TransactionLogResponse(transactions: List[String])
}
