package com.music.payment

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import com.music.payment.actors.BankAccount
import com.music.payment.messages.PaymentMessages._
import org.scalatest.wordspec.AnyWordSpecLike

/**
 * Tests unitaires pour l'acteur BankAccount.
 */
class BankAccountSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {

  "BankAccount" should {

    "accepter un dépôt valide" in {
      val probe = createTestProbe[OperationResult]()
      val account = spawn(BankAccount("test-001", "Alice", 100.0))

      account ! DepositCmd(50.0, probe.ref)

      val response = probe.receiveMessage()
      response match {
        case OperationSuccess(id, newBalance, _) =>
          assert(id == "test-001")
          assert(newBalance == 150.0)
        case _ => fail("Le dépôt aurait dû réussir")
      }
    }

    "accepter un retrait valide" in {
      val probe = createTestProbe[OperationResult]()
      val account = spawn(BankAccount("test-002", "Bob", 200.0))

      account ! WithdrawCmd(80.0, probe.ref)

      val response = probe.receiveMessage()
      response match {
        case OperationSuccess(id, newBalance, _) =>
          assert(id == "test-002")
          assert(newBalance == 120.0)
        case _ => fail("Le retrait aurait dû réussir")
      }
    }

    "refuser un retrait dépassant le solde" in {
      val probe = createTestProbe[OperationResult]()
      val account = spawn(BankAccount("test-003", "Charlie", 50.0))

      account ! WithdrawCmd(100.0, probe.ref)

      val response = probe.receiveMessage()
      response match {
        case OperationFailure(id, reason) =>
          assert(id == "test-003")
          assert(reason.contains("insuffisants"))
        case _ => fail("Le retrait aurait dû être refusé")
      }
    }

    "retourner le solde correct" in {
      val probe = createTestProbe[BalanceResponse]()
      val account = spawn(BankAccount("test-004", "Diana", 300.0))

      account ! GetBalanceCmd(probe.ref)

      val response = probe.receiveMessage()
      assert(response.accountId == "test-004")
      assert(response.balance == 300.0)
    }

    "maintenir le solde après un retrait refusé" in {
      val opProbe = createTestProbe[OperationResult]()
      val balProbe = createTestProbe[BalanceResponse]()
      val account = spawn(BankAccount("test-005", "Eve", 100.0))

      // Retrait refusé
      account ! WithdrawCmd(200.0, opProbe.ref)
      opProbe.receiveMessage()

      // Vérifier que le solde n'a pas changé
      account ! GetBalanceCmd(balProbe.ref)
      val balance = balProbe.receiveMessage()
      assert(balance.balance == 100.0)
    }

    "gérer plusieurs opérations séquentielles" in {
      val probe = createTestProbe[OperationResult]()
      val balProbe = createTestProbe[BalanceResponse]()
      val account = spawn(BankAccount("test-006", "Frank", 1000.0))

      // Dépôt
      account ! DepositCmd(500.0, probe.ref)
      probe.receiveMessage()

      // Retrait
      account ! WithdrawCmd(300.0, probe.ref)
      probe.receiveMessage()

      // Solde final : 1000 + 500 - 300 = 1200
      account ! GetBalanceCmd(balProbe.ref)
      val balance = balProbe.receiveMessage()
      assert(balance.balance == 1200.0)
    }
  }
}
