package com.music.petri

import com.music.petri.model._
import com.music.petri.engine._
import com.music.petri.engine.LTLChecker._
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

/**
 * Tests unitaires pour le vérificateur LTL.
 */
class LTLCheckerSpec extends AnyWordSpec with Matchers {

  // Réseau de Pétri simple pour les tests
  private def simpleNet: (PetriNet, Marking) = {
    val p1 = Place("p1", "Source")
    val p2 = Place("p2", "Dest")
    val t1 = Transition("t1", "Move")

    val net = PetriNet(
      places = Set(p1, p2),
      transitions = Set(t1),
      inputArcs = Set(InputArc(p1, t1)),
      outputArcs = Set(OutputArc(t1, p2))
    )

    val marking = Marking(p1 -> 2, p2 -> 0)
    (net, marking)
  }

  "LTLChecker" should {

    "vérifier Always (sûreté) — satisfait" in {
      val (net, marking) = simpleNet
      val stateSpace = StateSpaceExplorer.explore(net, marking)
      val p1 = net.places.find(_.id == "p1").get
      val p2 = net.places.find(_.id == "p2").get

      // Always(p1 >= 0) devrait être vrai
      val formula = Always(Atom("p1 >= 0", m => m.getTokens(p1) >= 0))
      val result = LTLChecker.check(formula, stateSpace)
      result.satisfied shouldBe true
    }

    "vérifier Always (sûreté) — violé" in {
      val p1 = Place("p1", "Account")
      val p2 = Place("p2", "Sink")
      val t1 = Transition("t1", "Drain")

      val net = PetriNet(
        places = Set(p1, p2),
        transitions = Set(t1),
        inputArcs = Set(InputArc(p1, t1, weight = 2)),
        outputArcs = Set(OutputArc(t1, p2))
      )

      val stateSpace = StateSpaceExplorer.explore(net, Marking(p1 -> 3, p2 -> 0))

      // Always(p1 > 0) devrait être faux car p1 peut atteindre 1 (< 2 pour fire mais still > 0)
      // Actually: with p1=3, fire(t1) -> p1=1,p2=1. Then p1=1 < 2 so deadlock.
      // p1 is always >= 0 (never negative), but p1 > 1 is not always true
      val formula = Always(Atom("p1 > 1", m => m.getTokens(p1) > 1))
      val result = LTLChecker.check(formula, stateSpace)
      result.satisfied shouldBe false
    }

    "vérifier Eventually (vivacité)" in {
      val (net, marking) = simpleNet
      val stateSpace = StateSpaceExplorer.explore(net, marking)
      val p2 = net.places.find(_.id == "p2").get

      // Eventually(p2 > 0) devrait être vrai
      val formula = Eventually(Atom("p2 > 0", m => m.getTokens(p2) > 0))
      val result = LTLChecker.check(formula, stateSpace)
      result.satisfied shouldBe true
    }

    "vérifier l'absence de deadlock" in {
      val p1 = Place("p1", "A")
      val p2 = Place("p2", "B")
      val t1 = Transition("t1", "Forward")
      val t2 = Transition("t2", "Back")

      val net = PetriNet(
        places = Set(p1, p2),
        transitions = Set(t1, t2),
        inputArcs = Set(InputArc(p1, t1), InputArc(p2, t2)),
        outputArcs = Set(OutputArc(t1, p2), OutputArc(t2, p1))
      )

      val stateSpace = StateSpaceExplorer.explore(net, Marking(p1 -> 1, p2 -> 0))

      val result = LTLChecker.deadlockFreedom(stateSpace)
      result.satisfied shouldBe true
    }

    "détecter un deadlock" in {
      val (net, marking) = simpleNet
      val stateSpace = StateSpaceExplorer.explore(net, marking)

      val result = LTLChecker.deadlockFreedom(stateSpace)
      result.satisfied shouldBe false // Le réseau simple a un deadlock quand p1=0
    }

    "vérifier les propriétés du système de paiement" in {
      val (net, marking) = com.music.petri.models.PaymentPetriNet.build(
        initialBalanceA = 5, initialBalanceB = 3, transferAmount = 2
      )

      val stateSpace = StateSpaceExplorer.explore(net, marking)

      // Sûreté: solde A >= 0
      val safetyA = LTLChecker.safetyBalanceNonNegative(
        com.music.petri.models.PaymentPetriNet.accountA
      )
      LTLChecker.check(safetyA, stateSpace).satisfied shouldBe true

      // Sûreté: solde B >= 0
      val safetyB = LTLChecker.safetyBalanceNonNegative(
        com.music.petri.models.PaymentPetriNet.accountB
      )
      LTLChecker.check(safetyB, stateSpace).satisfied shouldBe true

      // Vivacité: le transfert peut être complété
      val liveness = LTLChecker.livenessTransferCompletes(
        com.music.petri.models.PaymentPetriNet.transferDone
      )
      LTLChecker.check(liveness, stateSpace).satisfied shouldBe true
    }
  }
}
