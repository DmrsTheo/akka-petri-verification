package com.music.payment.actors

import akka.actor.typed.{ActorRef, Behavior, SupervisorStrategy}
import akka.actor.typed.scaladsl.Behaviors
import com.music.payment.messages.PaymentMessages._
import org.slf4j.LoggerFactory

/**
 * Acteur superviseur gérant l'ensemble des comptes bancaires.
 *
 * Responsabilités :
 * - Créer et gérer les acteurs BankAccount
 * - Router les opérations vers le bon compte
 * - Superviser les acteurs enfants (redémarrage en cas d'erreur)
 * - Gérer les transactions via le TransactionManager
 */
object BankSupervisor {

  private val logger = LoggerFactory.getLogger(getClass)

  def apply(): Behavior[SupervisorCommand] = {
    Behaviors.setup { context =>
      logger.info("BankSupervisor démarré")
      supervise(Map.empty, context)
    }
  }

  private def supervise(
    accounts: Map[String, ActorRef[AccountCommand]],
    context: akka.actor.typed.scaladsl.ActorContext[SupervisorCommand]
  ): Behavior[SupervisorCommand] = {

    Behaviors.receiveMessage {

      case CreateAccount(id, ownerName, initialBalance, replyTo) =>
        if (accounts.contains(id)) {
          logger.warn(s"Tentative de création d'un compte existant: $id")
          replyTo ! OperationFailure(id, s"Le compte $id existe déjà")
          Behaviors.same
        } else {
          // Créer l'acteur BankAccount avec supervision
          val accountBehavior = Behaviors.supervise(
            BankAccount(id, ownerName, initialBalance)
          ).onFailure[Exception](SupervisorStrategy.restart)

          val accountRef = context.spawn(accountBehavior, s"account-$id")
          logger.info(s"Compte $id créé pour $ownerName avec solde initial $initialBalance")
          replyTo ! OperationSuccess(id, initialBalance, s"Compte $id créé avec succès")
          supervise(accounts + (id -> accountRef), context)
        }

      case PerformDeposit(accountId, amount, replyTo) =>
        accounts.get(accountId) match {
          case Some(accountRef) =>
            accountRef ! DepositCmd(amount, replyTo)
          case None =>
            logger.warn(s"Dépôt impossible: compte $accountId introuvable")
            replyTo ! OperationFailure(accountId, s"Compte $accountId introuvable")
        }
        Behaviors.same

      case PerformWithdraw(accountId, amount, replyTo) =>
        accounts.get(accountId) match {
          case Some(accountRef) =>
            accountRef ! WithdrawCmd(amount, replyTo)
          case None =>
            logger.warn(s"Retrait impossible: compte $accountId introuvable")
            replyTo ! OperationFailure(accountId, s"Compte $accountId introuvable")
        }
        Behaviors.same

      case QueryBalance(accountId, replyTo) =>
        accounts.get(accountId) match {
          case Some(accountRef) =>
            accountRef ! GetBalanceCmd(replyTo)
          case None =>
            logger.warn(s"Consultation impossible: compte $accountId introuvable")
            replyTo ! BalanceResponse(accountId, -1)
        }
        Behaviors.same

      case PerformTransfer(fromId, toId, amount, replyTo) =>
        // Créer un TransactionManager dédié pour ce transfert
        val txManager = context.spawn(
          TransactionManager(accounts),
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
