package com.music.payment.actors

import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors
import com.music.payment.messages.PaymentMessages._
import org.slf4j.LoggerFactory

/**
 * Supervisor actor managing all bank accounts.
 *
 * Responsibilities :
 * - Create and manage BankAccount actors
 * - Route operations to the right account
 * - Supervise child actors (restart on error)
 * - Manage transactions via TransactionManager
 */
object BankSupervisor {

  private val logger = LoggerFactory.getLogger(getClass)

  def apply(): Behavior[SupervisorCommand] = {
    Behaviors.setup { context =>
      logger.info("BankSupervisor started")
      // Create the logging actor at startup
      val loggingActor = context.spawn(LoggingActor(), "logging-actor")
      logger.info("LoggingActor created and integrated into system")
      supervise(Map.empty, context, loggingActor)
    }
  }

  private def supervise(
    accounts: Map[String, ActorRef[AccountCommand]],
    context: akka.actor.typed.scaladsl.ActorContext[SupervisorCommand],
    loggingActor: ActorRef[LoggingCommand]
  ): Behavior[SupervisorCommand] = {

    Behaviors.receiveMessage {

      case CreateAccount(id, ownerName, initialBalance, replyTo) =>
        if (accounts.contains(id)) {
          logger.warn(s"Attempt to create existing account: $id")
          replyTo ! OperationFailure(id, s"Account $id already exists")
          Behaviors.same
        } else {
          // Create BankAccount actor with supervision
          val accountBehavior = Behaviors.supervise(
            BankAccount(id, ownerName, initialBalance)
          ).onFailure[Exception](SupervisorStrategy.restart)

          val accountRef = context.spawn(accountBehavior, s"account-$id")
          logger.info(s"Account $id created for $ownerName with initial balance $initialBalance")
          replyTo ! OperationSuccess(id, initialBalance, s"Account $id created successfully")
          
          // Log account creation
          loggingActor ! LogAccountOperation(
            operationType = "ACCOUNT_CREATION",
            accountId = id,
            amount = Some(initialBalance),
            newBalance = initialBalance,
            status = "SUCCESS",
            details = s"Account created for $ownerName"
          )
          
          supervise(accounts + (id -> accountRef), context, loggingActor)
        }

      case PerformDeposit(accountId, amount, replyTo) =>
        accounts.get(accountId) match {
          case Some(accountRef) =>
            accountRef ! DepositCmd(amount, replyTo)
          case None =>
            logger.warn(s"Deposit impossible: account $accountId not found")
            replyTo ! OperationFailure(accountId, s"Account $accountId not found")
        }
        Behaviors.same

      case PerformWithdraw(accountId, amount, replyTo) =>
        accounts.get(accountId) match {
          case Some(accountRef) =>
            accountRef ! WithdrawCmd(amount, replyTo)
          case None =>
            logger.warn(s"Withdrawal impossible: account $accountId not found")
            replyTo ! OperationFailure(accountId, s"Account $accountId not found")
        }
        Behaviors.same

      case QueryBalance(accountId, replyTo) =>
        accounts.get(accountId) match {
          case Some(accountRef) =>
            accountRef ! GetBalanceCmd(replyTo)
          case None =>
            logger.warn(s"Query impossible: account $accountId not found")
            replyTo ! BalanceResponse(accountId, -1)
        }
        Behaviors.same

      case PerformTransfer(fromId, toId, amount, replyTo) =>
        // Create a dedicated TransactionManager for this transfer
        val txManager = context.spawn(
          TransactionManager(accounts, loggingActor),
          s"tx-$fromId-$toId-${System.nanoTime()}"
        )
        txManager ! TransferRequest(fromId, toId, amount, replyTo)
        Behaviors.same

      case ListAccounts(replyTo) =>
        replyTo ! AccountList(accounts.keys.toList.sorted)
        Behaviors.same
    }
  }
}
