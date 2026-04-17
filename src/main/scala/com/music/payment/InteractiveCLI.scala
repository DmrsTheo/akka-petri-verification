package com.music.payment

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import com.music.payment.actors.BankSupervisor
import com.music.payment.messages.PaymentMessages._

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._
import scala.io.StdIn

/**
 * Interface en ligne de commande interactive pour le système bancaire.
 *
 * Permet à l'utilisateur d'exécuter manuellement des opérations bancaires
 * (création de comptes, dépôts, retraits, virements, consultation de solde, etc.)
 * via un terminal REPL.
 */
object InteractiveCLI {

  // Codes ANSI pour la couleur
  private val RESET  = "\u001b[0m"
  private val GREEN  = "\u001b[32m"
  private val RED    = "\u001b[31m"
  private val CYAN   = "\u001b[36m"
  private val YELLOW = "\u001b[33m"
  private val BOLD   = "\u001b[1m"
  private val BLUE   = "\u001b[34m"

  def run(system: ActorSystem[SupervisorCommand]): Unit = {
    implicit val timeout: Timeout = 5.seconds
    implicit val ec: ExecutionContext = system.executionContext
    implicit val scheduler: akka.actor.typed.Scheduler = system.scheduler

    printBanner()
    printHelp()

    var running = true
    while (running) {
      print(s"\n${CYAN}${BOLD}bank>${RESET} ")
      val input = StdIn.readLine()

      if (input == null) {
        running = false
      } else {
        val parts = input.trim.split("\\s+").toList
        parts match {
          case Nil | List("") =>
            // ligne vide, on ignore

          case "creer" :: id :: nom :: soldeStr :: Nil =>
            handleCreateAccount(system, id, nom, soldeStr)

          case "depot" :: id :: montantStr :: Nil =>
            handleDeposit(system, id, montantStr)

          case "retrait" :: id :: montantStr :: Nil =>
            handleWithdraw(system, id, montantStr)

          case "virement" :: de :: vers :: montantStr :: Nil =>
            handleTransfer(system, de, vers, montantStr)

          case "solde" :: id :: Nil =>
            handleBalance(system, id)

          case "comptes" :: Nil =>
            handleListAccounts(system)

          case "historique" :: Nil =>
            handleHistory(system)

          case "aide" :: Nil =>
            printHelp()

          case "quitter" :: Nil =>
            println(s"\n${YELLOW}Arrêt du système...${RESET}")
            running = false

          case cmd :: _ =>
            println(s"${RED}Commande inconnue: '$cmd'. Tapez 'aide' pour la liste des commandes.${RESET}")
        }
      }
    }
  }

  // ===== Handlers =====

  private def handleCreateAccount(system: ActorSystem[SupervisorCommand], id: String, nom: String, soldeStr: String)
                                  (implicit timeout: Timeout, scheduler: akka.actor.typed.Scheduler): Unit = {
    parseDouble(soldeStr, "solde initial") match {
      case Some(solde) =>
        try {
          val result = Await.result(
            system.ask[OperationResult](ref => CreateAccount(id, nom, solde, ref)),
            5.seconds
          )
          result match {
            case OperationSuccess(_, balance, msg) =>
              println(s"${GREEN}✓ $msg (solde: $balance)${RESET}")
            case OperationFailure(_, reason) =>
              println(s"${RED}✗ Échec: $reason${RESET}")
          }
        } catch {
          case e: Exception => println(s"${RED}✗ Erreur: ${e.getMessage}${RESET}")
        }
      case None => // erreur déjà affichée
    }
  }

  private def handleDeposit(system: ActorSystem[SupervisorCommand], id: String, montantStr: String)
                            (implicit timeout: Timeout, scheduler: akka.actor.typed.Scheduler): Unit = {
    parseDouble(montantStr, "montant") match {
      case Some(montant) =>
        try {
          val result = Await.result(
            system.ask[OperationResult](ref => PerformDeposit(id, montant, ref)),
            5.seconds
          )
          result match {
            case OperationSuccess(accId, balance, _) =>
              println(s"${GREEN}✓ Dépôt de $montant sur $accId réussi. Nouveau solde: $balance${RESET}")
            case OperationFailure(accId, reason) =>
              println(s"${RED}✗ Dépôt échoué sur $accId: $reason${RESET}")
          }
        } catch {
          case e: Exception => println(s"${RED}✗ Erreur: ${e.getMessage}${RESET}")
        }
      case None =>
    }
  }

  private def handleWithdraw(system: ActorSystem[SupervisorCommand], id: String, montantStr: String)
                             (implicit timeout: Timeout, scheduler: akka.actor.typed.Scheduler): Unit = {
    parseDouble(montantStr, "montant") match {
      case Some(montant) =>
        try {
          val result = Await.result(
            system.ask[OperationResult](ref => PerformWithdraw(id, montant, ref)),
            5.seconds
          )
          result match {
            case OperationSuccess(accId, balance, _) =>
              println(s"${GREEN}✓ Retrait de $montant sur $accId réussi. Nouveau solde: $balance${RESET}")
            case OperationFailure(accId, reason) =>
              println(s"${RED}✗ Retrait refusé sur $accId: $reason${RESET}")
          }
        } catch {
          case e: Exception => println(s"${RED}✗ Erreur: ${e.getMessage}${RESET}")
        }
      case None =>
    }
  }

  private def handleTransfer(system: ActorSystem[SupervisorCommand], de: String, vers: String, montantStr: String)
                             (implicit timeout: Timeout, scheduler: akka.actor.typed.Scheduler): Unit = {
    parseDouble(montantStr, "montant") match {
      case Some(montant) =>
        try {
          val result = Await.result(
            system.ask[TransactionResult](ref => PerformTransfer(de, vers, montant, ref)),
            5.seconds
          )
          result match {
            case TransferSuccess(_, _, _, msg) =>
              println(s"${GREEN}✓ $msg${RESET}")
            case TransferFailure(_, _, _, reason) =>
              println(s"${RED}✗ Virement échoué: $reason${RESET}")
          }
        } catch {
          case e: Exception => println(s"${RED}✗ Erreur: ${e.getMessage}${RESET}")
        }
      case None =>
    }
  }

  private def handleBalance(system: ActorSystem[SupervisorCommand], id: String)
                            (implicit timeout: Timeout, scheduler: akka.actor.typed.Scheduler): Unit = {
    try {
      val result = Await.result(
        system.ask[BalanceResponse](ref => QueryBalance(id, ref)),
        5.seconds
      )
      if (result.balance < 0) {
        println(s"${RED}✗ Compte $id introuvable${RESET}")
      } else {
        println(s"${BLUE}  Compte ${result.accountId}: solde = ${result.balance}${RESET}")
      }
    } catch {
      case e: Exception => println(s"${RED}✗ Erreur: ${e.getMessage}${RESET}")
    }
  }

  private def handleListAccounts(system: ActorSystem[SupervisorCommand])
                                 (implicit timeout: Timeout, scheduler: akka.actor.typed.Scheduler): Unit = {
    try {
      val result = Await.result(
        system.ask[AccountList](ref => ListAccounts(ref)),
        5.seconds
      )
      if (result.accounts.isEmpty) {
        println(s"${YELLOW}  Aucun compte enregistré.${RESET}")
      } else {
        println(s"${BLUE}  Comptes enregistrés (${result.accounts.size}):${RESET}")
        result.accounts.foreach(acc => println(s"    • $acc"))
      }
    } catch {
      case e: Exception => println(s"${RED}✗ Erreur: ${e.getMessage}${RESET}")
    }
  }

  private def handleHistory(system: ActorSystem[SupervisorCommand])
                            (implicit timeout: Timeout, scheduler: akka.actor.typed.Scheduler): Unit = {
    // Le LoggingActor est un enfant du BankSupervisor, on doit passer par le système d'acteurs.
    // Malheureusement, le BankSupervisor n'expose pas directement le GetTransactionLog.
    // On va ajouter un message pour cela.
    println(s"${YELLOW}  Fonctionnalité d'historique non disponible dans cette version.${RESET}")
    println(s"${YELLOW}  L'historique est enregistré dans les logs du système (LoggingActor).${RESET}")
  }

  // ===== Utilitaires =====

  private def parseDouble(str: String, label: String): Option[Double] = {
    try {
      Some(str.toDouble)
    } catch {
      case _: NumberFormatException =>
        println(s"${RED}✗ '$str' n'est pas un $label valide.${RESET}")
        None
    }
  }

  private def printBanner(): Unit = {
    println()
    println(s"${BOLD}${CYAN}╔══════════════════════════════════════════════════════╗${RESET}")
    println(s"${BOLD}${CYAN}║       SYSTÈME BANCAIRE — MODE INTERACTIF             ║${RESET}")
    println(s"${BOLD}${CYAN}╚══════════════════════════════════════════════════════╝${RESET}")
    println()
  }

  private def printHelp(): Unit = {
    println(s"${BOLD}Commandes disponibles:${RESET}")
    println(s"  ${GREEN}creer${RESET} <id> <nom> <solde>        Créer un compte")
    println(s"  ${GREEN}depot${RESET} <id> <montant>            Déposer de l'argent")
    println(s"  ${GREEN}retrait${RESET} <id> <montant>          Retirer de l'argent")
    println(s"  ${GREEN}virement${RESET} <de> <vers> <montant>  Transférer entre comptes")
    println(s"  ${GREEN}solde${RESET} <id>                      Consulter le solde")
    println(s"  ${GREEN}comptes${RESET}                         Lister tous les comptes")
    println(s"  ${GREEN}historique${RESET}                      Afficher l'historique")
    println(s"  ${GREEN}aide${RESET}                            Afficher cette aide")
    println(s"  ${GREEN}quitter${RESET}                         Quitter le programme")
  }
}
