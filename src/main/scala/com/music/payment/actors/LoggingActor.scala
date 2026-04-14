package com.music.payment.actors

import akka.actor.typed.{Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.music.payment.messages.PaymentMessages._
import org.slf4j.LoggerFactory
import scala.collection.mutable

/**
 * Actor recording all transactions and operations of the banking system.
 *
 * Responsibilities :
 * - Record transfers between accounts
 * - Record deposits and withdrawals
 * - Maintain a centralized transaction history
 * - Provide transaction reports
 *
 * Each transaction is timestamped and fully traced for audit.
 */
object LoggingActor {

  private val logger = LoggerFactory.getLogger(getClass)

  def apply(): Behavior[LoggingCommand] = {
    Behaviors.setup { context =>
      logger.info("LoggingActor started - logging system active")
      active(mutable.ListBuffer.empty[String])
    }
  }

  private def active(transactionLog: mutable.ListBuffer[String]): Behavior[LoggingCommand] = {
    Behaviors.receiveMessage {

      case LogTransaction(transactionType, fromAccountId, toAccountId, amount, status, details, timestamp) =>
        val logEntry = formatTransactionLog(
          transactionType = transactionType,
          fromAccountId = fromAccountId,
          toAccountId = toAccountId,
          amount = amount,
          status = status,
          details = details,
          timestamp = timestamp
        )
        transactionLog += logEntry
        logger.info(logEntry)
        Behaviors.same

      case LogAccountOperation(operationType, accountId, amount, newBalance, status, details, timestamp) =>
        val logEntry = formatOperationLog(
          operationType = operationType,
          accountId = accountId,
          amount = amount,
          newBalance = newBalance,
          status = status,
          details = details,
          timestamp = timestamp
        )
        transactionLog += logEntry
        logger.info(logEntry)
        Behaviors.same

      case GetTransactionLog(replyTo) =>
        logger.info(s"Transaction log query request (${transactionLog.length} entries)")
        replyTo ! TransactionLogResponse(transactionLog.toList)
        Behaviors.same
    }
  }

  private def formatTransactionLog(
    transactionType: String,
    fromAccountId: Option[String],
    toAccountId: Option[String],
    amount: Option[Double],
    status: String,
    details: String,
    timestamp: Long
  ): String = {
    val dateTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date(timestamp))
    val from = fromAccountId.getOrElse("N/A")
    val to = toAccountId.getOrElse("N/A")
    val amt = amount.map(_.toString).getOrElse("N/A")
    s"[$dateTime] [$transactionType] $from -> $to | Amount: $amt | Status: $status | Details: $details"
  }

  private def formatOperationLog(
    operationType: String,
    accountId: String,
    amount: Option[Double],
    newBalance: Double,
    status: String,
    details: String,
    timestamp: Long
  ): String = {
    val dateTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new java.util.Date(timestamp))
    val amt = amount.map(_.toString).getOrElse("N/A")
    s"[$dateTime] [$operationType] $accountId | Amount: $amt | New balance: $newBalance | Status: $status | Details: $details"
  }
}
