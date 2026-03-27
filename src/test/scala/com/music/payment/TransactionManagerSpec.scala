package com.music.payment

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.music.payment.actors.{BankAccount, LoggingActor, TransactionManager}
import com.music.payment.messages.PaymentMessages._
import org.scalatest.wordspec.AnyWordSpecLike

/**
 * Tests unitaires pour l'acteur TransactionManager.
 * Vérifie que les transactions sont correctement enregistrées dans les logs.
 */
class TransactionManagerSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "TransactionManager" should {

    "effectuer un transfert réussi et enregistrer les logs" in {
      val txProbe = createTestProbe[TransactionResult]()
      val logProbe = createTestProbe[TransactionLogResponse]()

      // Créer les comptes
      val accountA = spawn(BankAccount("acc-a", "Alice", 500.0))
      val accountB = spawn(BankAccount("acc-b", "Bob", 200.0))

      val accounts = Map(
        "acc-a" -> accountA,
        "acc-b" -> accountB
      )

      // Créer le LoggingActor
      val loggingActor = spawn(LoggingActor())

      val behavior: akka.actor.typed.Behavior[TransactionCommand] = TransactionManager(accounts, loggingActor)
      val txManager = spawn(behavior)
      txManager ! TransferRequest("acc-a", "acc-b", 100.0, txProbe.ref)

      val result = txProbe.receiveMessage()
      result match {
        case TransferSuccess(_, _, _, msg) =>
          assert(msg.nonEmpty)
          // Vérifier que les logs ont été enregistrés
          loggingActor ! GetTransactionLog(logProbe.ref)
          val logResponse = logProbe.receiveMessage()
          assert(logResponse.transactions.nonEmpty, "Les logs de transfert devraient être remplis")
          assert(logResponse.transactions.exists(_.contains("TRANSFER")), "Les logs devraient contenir 'TRANSFER'")
        case TransferFailure(_, _, _, reason) =>
          fail(s"Le transfert aurait dû réussir: $reason")
      }
    }

    "refuser un transfert avec fonds insuffisants et enregistrer l'échec" in {
      val txProbe = createTestProbe[TransactionResult]()
      val logProbe = createTestProbe[TransactionLogResponse]()

      val accountA = spawn(BankAccount("acc-c", "Charlie", 50.0))
      val accountB = spawn(BankAccount("acc-d", "Diana", 200.0))

      val accounts = Map(
        "acc-c" -> accountA,
        "acc-d" -> accountB
      )

      val loggingActor = spawn(LoggingActor())
      val behavior: akka.actor.typed.Behavior[TransactionCommand] = TransactionManager(accounts, loggingActor)
      val txManager = spawn(behavior)
      txManager ! TransferRequest("acc-c", "acc-d", 100.0, txProbe.ref)

      val result = txProbe.receiveMessage()
      result match {
        case TransferFailure(_, _, _, reason) =>
          assert(reason.contains("refusé") || reason.contains("insuffisants"))
          // Vérifier que l'échec a été enregistré
          loggingActor ! GetTransactionLog(logProbe.ref)
          val logResponse = logProbe.receiveMessage()
          assert(logResponse.transactions.nonEmpty, "Les logs d'échec devraient être remplis")
          assert(logResponse.transactions.exists(_.contains("FAILED")), "Les logs devraient contenir 'FAILED'")
        case _ =>
          fail("Le transfert aurait dû échouer pour fonds insuffisants")
      }
    }

    "refuser un transfert depuis un compte inexistant et enregistrer l'erreur" in {
      val txProbe = createTestProbe[TransactionResult]()
      val logProbe = createTestProbe[TransactionLogResponse]()

      val accountA = spawn(BankAccount("acc-e", "Eve", 500.0))
      val accounts = Map("acc-e" -> accountA)

      val loggingActor = spawn(LoggingActor())
      val behavior: akka.actor.typed.Behavior[TransactionCommand] = TransactionManager(accounts, loggingActor)
      val txManager = spawn(behavior)
      txManager ! TransferRequest("acc-inexistant", "acc-e", 100.0, txProbe.ref)

      val result = txProbe.receiveMessage()
      result match {
        case TransferFailure(_, _, _, reason) =>
          assert(reason.contains("introuvable"))
          // Vérifier que l'erreur a été enregistrée
          loggingActor ! GetTransactionLog(logProbe.ref)
          val logResponse = logProbe.receiveMessage()
          assert(logResponse.transactions.nonEmpty, "Les logs d'erreur devraient être remplis")
          assert(logResponse.transactions.exists(_.contains("acc-inexistant")), 
            "Les logs devraient contenir le compte inexistant")
        case _ =>
          fail("Le transfert aurait dû échouer pour compte inexistant")
      }
    }

    "enregistrer les détails complets d'une transaction réussie" in {
      val txProbe = createTestProbe[TransactionResult]()
      val logProbe = createTestProbe[TransactionLogResponse]()

      val accountA = spawn(BankAccount("alice-account", "Alice", 1000.0))
      val accountB = spawn(BankAccount("bob-account", "Bob", 500.0))

      val accounts = Map(
        "alice-account" -> accountA,
        "bob-account" -> accountB
      )

      val loggingActor = spawn(LoggingActor())
      val behavior: akka.actor.typed.Behavior[TransactionCommand] = TransactionManager(accounts, loggingActor)
      val txManager = spawn(behavior)
      txManager ! TransferRequest("alice-account", "bob-account", 250.0, txProbe.ref)

      val result = txProbe.receiveMessage()
      result match {
        case TransferSuccess(_, _, _, _) =>
          loggingActor ! GetTransactionLog(logProbe.ref)
          val logResponse = logProbe.receiveMessage()
          val logs = logResponse.transactions
          
          assert(logs.length >= 2, "Devrait avoir au moins 2 entrées de log (initiation + succès)")
          assert(logs.exists(_.contains("250")), "Les logs devraient contenir le montant (250)")
          assert(logs.exists(_.contains("alice-account")), "Les logs devraient contenir le compte source")
          assert(logs.exists(_.contains("bob-account")), "Les logs devraient contenir le compte destination")
          assert(logs.exists(_.contains("SUCCESS")), "Les logs devraient indiquer le succès")
        case _ =>
          fail("Le transfert aurait dû réussir")
      }
    }
  }
}
