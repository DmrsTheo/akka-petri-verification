package com.music.payment

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import com.music.payment.actors.BankSupervisor
import com.music.payment.messages.PaymentMessages._
import com.music.simulation.SimulationRunner

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * Point d'entrée principal de l'application.
 *
 * Démontre :
 * 1. Le système de paiement Akka (création de comptes, dépôts, retraits, transferts)
 * 2. L'analyse formelle via réseau de Pétri
 * 3. La comparaison simulation vs modèle formel
 */
object Main extends App {

  println()
  println("=" * 70)
  println("  PAYMENT SYSTEM - AKKA + PETRI NET VERIFICATION")
  println("=" * 70)
  println()

  // === PART 1 : Akka Simulation ===
  println("-" * 50)
  println("  PART 1: AKKA SIMULATION")
  println("-" * 50)
  println()

  implicit val timeout: Timeout = 5.seconds

  val system = ActorSystem(BankSupervisor(), "payment-system")
  implicit val ec: ExecutionContext = system.executionContext
  implicit val scheduler: akka.actor.typed.Scheduler = system.scheduler

  try {
    // Create accounts
    println("  > Creating accounts...")
    val create1 = system.ask[OperationResult](ref => CreateAccount("ACC-001", "Alice", 1000.0, ref))
    val create2 = system.ask[OperationResult](ref => CreateAccount("ACC-002", "Bob", 500.0, ref))
    val create3 = system.ask[OperationResult](ref => CreateAccount("ACC-003", "Charlie", 250.0, ref))

    val results = Await.result(Future.sequence(List(create1, create2, create3)), 10.seconds)
    results.foreach {
      case OperationSuccess(id, balance, msg) => println(s"    + $msg (balance: $balance)")
      case OperationFailure(id, reason) => println(s"    - Failed for $id: $reason")
    }

    // Deposits
    println("\n  > Deposit operations...")
    val dep1 = system.ask[OperationResult](ref => PerformDeposit("ACC-001", 200.0, ref))
    val depResult = Await.result(dep1, 5.seconds)
    depResult match {
      case OperationSuccess(id, balance, _) => println(s"    + Deposit on $id successful. New balance: $balance")
      case OperationFailure(id, reason) => println(s"    - Deposit failed on $id: $reason")
    }

    // Successful withdrawal
    println("\n  > Withdrawal operations...")
    val wd1 = system.ask[OperationResult](ref => PerformWithdraw("ACC-002", 100.0, ref))
    val wdResult = Await.result(wd1, 5.seconds)
    wdResult match {
      case OperationSuccess(id, balance, _) => println(s"    + Withdrawal on $id successful. New balance: $balance")
      case OperationFailure(id, reason) => println(s"    - Withdrawal refused on $id: $reason")
    }

    // Withdrawal refused (insufficient funds)
    val wd2 = system.ask[OperationResult](ref => PerformWithdraw("ACC-003", 500.0, ref))
    val wdResult2 = Await.result(wd2, 5.seconds)
    wdResult2 match {
      case OperationSuccess(id, balance, _) => println(s"    + Withdrawal on $id successful. New balance: $balance")
      case OperationFailure(id, reason) => println(s"    - Withdrawal refused on $id: $reason")
    }

    // Successful transfer
    println("\n  > Transfers between accounts...")
    val tx1 = system.ask[TransactionResult](ref => PerformTransfer("ACC-001", "ACC-002", 300.0, ref))
    val txResult = Await.result(tx1, 5.seconds)
    txResult match {
      case TransferSuccess(from, to, amount, msg) => println(s"    + $msg")
      case TransferFailure(from, to, amount, reason) => println(s"    - Transfer failed: $reason")
    }

    // Check balances
    println("\n  > Balance queries...")
    for (accId <- List("ACC-001", "ACC-002", "ACC-003")) {
      val bal = system.ask[BalanceResponse](ref => QueryBalance(accId, ref))
      val balResult = Await.result(bal, 5.seconds)
      println(s"    Account ${balResult.accountId}: balance = ${balResult.balance}")
    }

    // Failed transfer (insufficient funds)
    println("\n  > Transfer with insufficient funds...")
    val tx2 = system.ask[TransactionResult](ref => PerformTransfer("ACC-003", "ACC-001", 999.0, ref))
    val txResult2 = Await.result(tx2, 5.seconds)
    txResult2 match {
      case TransferSuccess(_, _, _, msg) => println(s"    + $msg")
      case TransferFailure(_, _, _, reason) => println(s"    - Transfer refused: $reason")
    }

    println()
    println("  Akka simulation completed successfully.")
    println()

  } finally {
    system.terminate()
    Await.result(system.whenTerminated, 10.seconds)
  }

  // === PARTIE 2 : Analyse formelle Petri Net ===
  SimulationRunner.runPetriNetAnalysis()
}
