package com.music.payment.messages

/**
 * Messages pour l'acteur LoggingActor.
 * Gère la journalisation de toutes les opérations du système.
 */
object LoggingMessages {

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
