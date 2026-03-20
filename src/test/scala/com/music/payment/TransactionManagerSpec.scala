package com.music.payment

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.music.payment.actors.{BankAccount, TransactionManager}
import com.music.payment.messages.PaymentMessages._
import org.scalatest.wordspec.AnyWordSpecLike

/**
 * Tests unitaires pour l'acteur TransactionManager.
 */
class TransactionManagerSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "TransactionManager" should {

    "effectuer un transfert réussi" in {
      val probe = createTestProbe[TransactionResult]()

      // Créer les comptes
      val accountA = spawn(BankAccount("acc-a", "Alice", 500.0))
      val accountB = spawn(BankAccount("acc-b", "Bob", 200.0))

      val accounts = Map(
        "acc-a" -> accountA,
        "acc-b" -> accountB
      )

      val txManager = spawn(TransactionManager(accounts))
      txManager ! TransferRequest("acc-a", "acc-b", 100.0, probe.ref)

      val result = probe.receiveMessage()
      result match {
        case TransferSuccess(_, _, _, msg) =>
          assert(msg.nonEmpty)
        case TransferFailure(_, _, _, reason) =>
          fail(s"Le transfert aurait dû réussir: $reason")
      }
    }

    "refuser un transfert avec fonds insuffisants" in {
      val probe = createTestProbe[TransactionResult]()

      val accountA = spawn(BankAccount("acc-c", "Charlie", 50.0))
      val accountB = spawn(BankAccount("acc-d", "Diana", 200.0))

      val accounts = Map(
        "acc-c" -> accountA,
        "acc-d" -> accountB
      )

      val txManager = spawn(TransactionManager(accounts))
      txManager ! TransferRequest("acc-c", "acc-d", 100.0, probe.ref)

      val result = probe.receiveMessage()
      result match {
        case TransferFailure(_, _, _, reason) =>
          assert(reason.contains("refusé") || reason.contains("insuffisants"))
        case _ =>
          fail("Le transfert aurait dû échouer pour fonds insuffisants")
      }
    }

    "refuser un transfert depuis un compte inexistant" in {
      val probe = createTestProbe[TransactionResult]()

      val accountA = spawn(BankAccount("acc-e", "Eve", 500.0))
      val accounts = Map("acc-e" -> accountA)

      val txManager = spawn(TransactionManager(accounts))
      txManager ! TransferRequest("acc-inexistant", "acc-e", 100.0, probe.ref)

      val result = probe.receiveMessage()
      result match {
        case TransferFailure(_, _, _, reason) =>
          assert(reason.contains("introuvable"))
        case _ =>
          fail("Le transfert aurait dû échouer pour compte inexistant")
      }
    }
  }
}
