package com.music.payment

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.music.payment.actors.LoggingActor
import com.music.payment.messages.PaymentMessages._
import org.scalatest.freespec.AnyFreeSpecLike

/**
 * Tests unitaires pour l'acteur LoggingActor.
 * Vérifie que toutes les transactions et opérations sont correctement enregistrées.
 */
class LoggingActorSpec extends ScalaTestWithActorTestKit with AnyFreeSpecLike {

  "LoggingActor" - {

    "enregistrer les transferts de transactions" in {
      val logProbe = createTestProbe[TransactionLogResponse]()
      val loggingActor = spawn(LoggingActor())

      // Enregistrer un transfert
      loggingActor ! LogTransaction(
        transactionType = "TRANSFER",
        fromAccountId = Some("acc-1"),
        toAccountId = Some("acc-2"),
        amount = Some(100.0),
        status = "SUCCESS",
        details = "Transfert réussi"
      )

      // Consulter les logs
      loggingActor ! GetTransactionLog(logProbe.ref)
      val response = logProbe.receiveMessage()

      assert(response.transactions.nonEmpty, "Le journal devrait contenir des entrées")
      assert(response.transactions.length >= 1, "Devrait avoir au moins 1 entrée")
      val logEntry = response.transactions.head
      assert(logEntry.contains("TRANSFER"), "Le log devrait contenir 'TRANSFER'")
      assert(logEntry.contains("acc-1"), "Le log devrait contenir le compte source")
      assert(logEntry.contains("acc-2"), "Le log devrait contenir le compte destination")
      assert(logEntry.contains("100"), "Le log devrait contenir le montant")
      assert(logEntry.contains("SUCCESS"), "Le log devrait contenir le statut")
    }

    "enregistrer les opérations de compte (dépôt/retrait)" in {
      val logProbe = createTestProbe[TransactionLogResponse]()
      val loggingActor = spawn(LoggingActor())

      // Enregistrer un dépôt
      loggingActor ! LogAccountOperation(
        operationType = "DEPOSIT",
        accountId = "acc-test",
        amount = Some(500.0),
        newBalance = 1500.0,
        status = "SUCCESS",
        details = "Dépôt effectué"
      )

      // Consulter les logs
      loggingActor ! GetTransactionLog(logProbe.ref)
      val response = logProbe.receiveMessage()

      assert(response.transactions.nonEmpty, "Le journal devrait contenir des entrées")
      val logEntry = response.transactions.head
      assert(logEntry.contains("DEPOSIT"), "Le log devrait contenir 'DEPOSIT'")
      assert(logEntry.contains("acc-test"), "Le log devrait contenir l'ID du compte")
      assert(logEntry.contains("500"), "Le log devrait contenir le montant du dépôt")
      assert(logEntry.contains("1500"), "Le log devrait contenir le nouveau solde")
    }

    "maintenir l'historique de plusieurs transactions" in {
      val logProbe = createTestProbe[TransactionLogResponse]()
      val loggingActor = spawn(LoggingActor())

      // Première transaction
      loggingActor ! LogTransaction(
        transactionType = "TRANSFER",
        fromAccountId = Some("alice"),
        toAccountId = Some("bob"),
        amount = Some(100.0),
        status = "SUCCESS",
        details = "Transfert 1"
      )

      // Deuxième transaction
      loggingActor ! LogTransaction(
        transactionType = "TRANSFER",
        fromAccountId = Some("bob"),
        toAccountId = Some("charlie"),
        amount = Some(50.0),
        status = "SUCCESS",
        details = "Transfert 2"
      )

      // Consulter les logs
      loggingActor ! GetTransactionLog(logProbe.ref)
      val response = logProbe.receiveMessage()

      assert(response.transactions.length == 2, "Devrait avoir 2 entrées de log")
      assert(response.transactions.exists(_.contains("alice")), "Devrait contenir la première transaction")
      assert(response.transactions.exists(_.contains("bob")), "Devrait contenir les deux comptes")
      assert(response.transactions.exists(_.contains("charlie")), "Devrait contenir la deuxième transaction")
    }

    "enregistrer les erreurs et échecs de transactions" in {
      val logProbe = createTestProbe[TransactionLogResponse]()
      val loggingActor = spawn(LoggingActor())

      // Transaction échouée
      loggingActor ! LogTransaction(
        transactionType = "TRANSFER",
        fromAccountId = Some("poor-account"),
        toAccountId = Some("rich-account"),
        amount = Some(1000.0),
        status = "FAILED",
        details = "Fonds insuffisants"
      )

      // Consulter les logs
      loggingActor ! GetTransactionLog(logProbe.ref)
      val response = logProbe.receiveMessage()

      assert(response.transactions.nonEmpty, "Le journal devrait contenir l'erreur")
      val logEntry = response.transactions.head
      assert(logEntry.contains("FAILED"), "Le log devrait indiquer l'échec")
      assert(logEntry.contains("Fonds insuffisants"), "Le log devrait contenir la raison de l'échec")
    }

    "horodater correctement chaque entrée de log" in {
      val logProbe = createTestProbe[TransactionLogResponse]()
      val loggingActor = spawn(LoggingActor())

      val beforeTime = System.currentTimeMillis()
      
      loggingActor ! LogTransaction(
        transactionType = "TRANSFER",
        fromAccountId = Some("acc-1"),
        toAccountId = Some("acc-2"),
        amount = Some(100.0),
        status = "SUCCESS",
        details = "Test"
      )

      val afterTime = System.currentTimeMillis()

      loggingActor ! GetTransactionLog(logProbe.ref)
      val response = logProbe.receiveMessage()

      assert(response.transactions.nonEmpty, "Le journal devrait contenir des entrées")
      val logEntry = response.transactions.head
      // Les logs ont un horodatage au format [YYYY-MM-DD HH:mm:ss.SSS]
      assert(logEntry.matches(""".*\[\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}\].*"""), 
        "Le log devrait contenir un horodatage")
    }

    "gérer les transactions avec montants optionnels" in {
      val logProbe = createTestProbe[TransactionLogResponse]()
      val loggingActor = spawn(LoggingActor())

      // Transaction sans montant
      loggingActor ! LogTransaction(
        transactionType = "TRANSFER_COMPLETED",
        fromAccountId = None,
        toAccountId = Some("acc-2"),
        amount = None,
        status = "SUCCESS",
        details = "Transfert complété"
      )

      loggingActor ! GetTransactionLog(logProbe.ref)
      val response = logProbe.receiveMessage()

      assert(response.transactions.nonEmpty, "Le journal devrait contenir l'entrée")
      val logEntry = response.transactions.head
      assert(logEntry.contains("TRANSFER_COMPLETED"), "Le log devrait contenir le type de transaction")
      assert(logEntry.contains("acc-2"), "Le log devrait contenir le compte destination")
      assert(logEntry.contains("N/A"), "Le log devrait indiquer N/A pour les champs optionnels manquants")
    }
  }
}
