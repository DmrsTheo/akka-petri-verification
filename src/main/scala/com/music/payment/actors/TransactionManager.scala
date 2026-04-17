package com.music.payment.actors

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.music.payment.messages.PaymentMessages._
import org.slf4j.LoggerFactory

/**
 * Actor orchestrating transfers between accounts.
 *
 * 2-phase transfer protocol with compensation (rollback) :
 * 1. Debit source account
 * 2. If debit succeeds -> credit destination account
 *    If debit fails -> cancel immediately
 * 3. If credit fails -> ROLLBACK : re-credit source account
 *
 * Guarantees distributed transaction consistency :
 * - Money is never lost
 * - Total system balance remains constant
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

      // ===== Phase 0 : Receiving transfer request =====
      case TransferRequest(fromId, toId, amount, replyTo) =>
        logger.info(s"Transfer requested: $fromId -> $toId, amount: $amount")
        loggingActor ! LogTransaction(
          transactionType = "TRANSFER_INITIATED",
          fromAccountId = Some(fromId),
          toAccountId = Some(toId),
          amount = Some(amount),
          status = "INITIATED",
          details = s"Transfer initiated from $fromId to $toId"
        )

        // Check that both accounts exist BEFORE any operation
        (accounts.get(fromId), accounts.get(toId)) match {
          case (Some(fromAccount), Some(_)) =>
            // Phase 1 : Debit source account
            val debitAdapter = context.messageAdapter[OperationResult] {
              result => DebitResult(result, toId, amount, replyTo)
            }
            fromAccount ! WithdrawCmd(amount, debitAdapter)
            Behaviors.same

          case (None, _) =>
            logger.error(s"Source account $fromId not found")
            loggingActor ! LogTransaction(
              transactionType = "TRANSFER_FAILED",
              fromAccountId = Some(fromId),
              toAccountId = Some(toId),
              amount = Some(amount),
              status = "FAILED",
              details = s"Source account $fromId not found"
            )
            replyTo ! TransferFailure(fromId, toId, amount, s"Source account $fromId not found")
            Behaviors.same

          case (_, None) =>
            logger.error(s"Destination account $toId not found")
            loggingActor ! LogTransaction(
              transactionType = "TRANSFER_FAILED",
              fromAccountId = Some(fromId),
              toAccountId = Some(toId),
              amount = Some(amount),
              status = "FAILED",
              details = s"Destination account $toId not found"
            )
            replyTo ! TransferFailure(fromId, toId, amount, s"Destination account $toId not found")
            Behaviors.same
        }

      // ===== Phase 1 : Debit result =====
      case DebitResult(result, toId, amount, replyTo) =>
        result match {
          case OperationSuccess(fromId, _, _) =>
            // Debit succeeded -> Phase 2 : Credit destination account
            logger.info(s"Debit succeeded on $fromId, crediting $toId")
            loggingActor ! LogTransaction(
              transactionType = "TRANSFER_DEBIT_SUCCESS",
              fromAccountId = Some(fromId),
              toAccountId = Some(toId),
              amount = Some(amount),
              status = "DEBIT_SUCCESSFUL",
              details = s"Amount debited from account $fromId"
            )
            accounts.get(toId) match {
              case Some(toAccount) =>
                val creditAdapter = context.messageAdapter[OperationResult] {
                  res => CreditResult(res, fromId, amount, replyTo)
                }
                toAccount ! DepositCmd(amount, creditAdapter)

              case None =>
                logger.error(s"Destination account $toId disappeared during transaction!")
                loggingActor ! LogTransaction(
                  transactionType = "TRANSFER_FAILED",
                  fromAccountId = Some(fromId),
                  toAccountId = Some(toId),
                  amount = Some(amount),
                  status = "FAILED",
                  details = s"Destination account $toId disappeared after successful debit"
                )
                replyTo ! TransferFailure("?", toId, amount, "Destination account disappeared")
            }
            Behaviors.same

          case OperationFailure(fromId, reason) =>
            // Debit refused (e.g. insufficient funds) -> no rollback needed
            logger.warn(s"Transfer failed - debit refused on $fromId: $reason")
            loggingActor ! LogTransaction(
              transactionType = "TRANSFER_FAILED",
              fromAccountId = Some(fromId),
              toAccountId = Some(toId),
              amount = Some(amount),
              status = "FAILED",
              details = s"Debit refused: $reason"
            )
            replyTo ! TransferFailure(fromId, toId, amount, s"Debit refused: $reason")
            Behaviors.same
        }

      // ===== Phase 2 : Credit result =====
      case CreditResult(result, fromId, amount, replyTo) =>
        result match {
          case OperationSuccess(toId, newBalance, _) =>
            logger.info(s"Transfer completed successfully, $toId new balance: $newBalance")
            loggingActor ! LogTransaction(
              transactionType = "TRANSFER_SUCCESS",
              fromAccountId = None,
              toAccountId = Some(toId),
              amount = None,
              status = "SUCCESS",
              details = s"Transfer completed successfully, new balance for $toId: $newBalance"
            )
            replyTo ! TransferSuccess("source", toId, 0, "Transfer completed successfully")
            Behaviors.same

          case OperationFailure(toId, reason) =>
            // Critical case: debit succeeded but credit failed
            logger.error(s"CRITICAL ERROR: credit failed on $toId after successful debit: $reason")
            loggingActor ! LogTransaction(
              transactionType = "TRANSFER_CRITICAL_FAILURE",
              fromAccountId = None,
              toAccountId = Some(toId),
              amount = None,
              status = "CRITICAL_FAILURE",
              details = s"Critical error: credit failed after successful debit: $reason"
            )
            replyTo ! TransferFailure("source", toId, 0, s"Credit failed after debit: $reason")
            Behaviors.same
        }
    }
  }
}
