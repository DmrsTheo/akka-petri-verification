package com.music.petri

import com.music.petri.model._
import com.music.petri.engine._
import com.music.petri.models.PaymentPetriNet
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

/**
 * Tests unitaires pour le moteur de réseau de Pétri.
 */
class PetriNetSpec extends AnyWordSpec with Matchers {

  "PetriNet" should {

    "détecter les transitions activées" in {
      val p1 = Place("p1", "Place1")
      val p2 = Place("p2", "Place2")
      val t1 = Transition("t1", "Trans1")

      val net = PetriNet(
        places = Set(p1, p2),
        transitions = Set(t1),
        inputArcs = Set(InputArc(p1, t1)),
        outputArcs = Set(OutputArc(t1, p2))
      )

      val marking = Marking(p1 -> 1, p2 -> 0)
      net.isEnabled(t1, marking) shouldBe true

      val emptyMarking = Marking(p1 -> 0, p2 -> 0)
      net.isEnabled(t1, emptyMarking) shouldBe false
    }

    "franchir correctement une transition" in {
      val p1 = Place("p1", "Source")
      val p2 = Place("p2", "Destination")
      val t1 = Transition("t1", "Transfer")

      val net = PetriNet(
        places = Set(p1, p2),
        transitions = Set(t1),
        inputArcs = Set(InputArc(p1, t1, weight = 2)),
        outputArcs = Set(OutputArc(t1, p2, weight = 3))
      )

      val initial = Marking(p1 -> 5, p2 -> 0)
      val result = net.fire(t1, initial)

      result.getTokens(p1) shouldBe 3
      result.getTokens(p2) shouldBe 3
    }

    "gérer les arcs inhibiteurs" in {
      val p1 = Place("p1", "Guard")
      val p2 = Place("p2", "Source")
      val p3 = Place("p3", "Output")
      val t1 = Transition("t1", "InhibitedTransition")

      val net = PetriNet(
        places = Set(p1, p2, p3),
        transitions = Set(t1),
        inputArcs = Set(
          InputArc(p1, t1, arcType = Inhibitor),
          InputArc(p2, t1)
        ),
        outputArcs = Set(OutputArc(t1, p3))
      )

      // t1 activée quand p1=0 et p2>=1
      val ok = Marking(p1 -> 0, p2 -> 1, p3 -> 0)
      net.isEnabled(t1, ok) shouldBe true

      // t1 désactivée quand p1>0
      val blocked = Marking(p1 -> 1, p2 -> 1, p3 -> 0)
      net.isEnabled(t1, blocked) shouldBe false
    }
  }

  "StateSpaceExplorer" should {

    "explorer l'espace d'états complet" in {
      val p1 = Place("p1", "A")
      val p2 = Place("p2", "B")
      val t1 = Transition("t1", "AtoB")
      val t2 = Transition("t2", "BtoA")

      val net = PetriNet(
        places = Set(p1, p2),
        transitions = Set(t1, t2),
        inputArcs = Set(InputArc(p1, t1), InputArc(p2, t2)),
        outputArcs = Set(OutputArc(t1, p2), OutputArc(t2, p1))
      )

      val initial = Marking(p1 -> 2, p2 -> 0)
      val stateSpace = StateSpaceExplorer.explore(net, initial)

      // Avec 2 jetons et 2 places: 3 états (2,0), (1,1), (0,2)
      stateSpace.stateCount shouldBe 3
      stateSpace.hasDeadlock shouldBe false
    }

    "détecter les deadlocks" in {
      val p1 = Place("p1", "Start")
      val p2 = Place("p2", "End")
      val t1 = Transition("t1", "Forward")

      val net = PetriNet(
        places = Set(p1, p2),
        transitions = Set(t1),
        inputArcs = Set(InputArc(p1, t1)),
        outputArcs = Set(OutputArc(t1, p2))
      )

      val initial = Marking(p1 -> 1, p2 -> 0)
      val stateSpace = StateSpaceExplorer.explore(net, initial)

      stateSpace.hasDeadlock shouldBe true
      stateSpace.deadlockStates should have size 1
    }
  }

  "InvariantChecker" should {

    "vérifier la conservation des jetons" in {
      val p1 = Place("p1", "A")
      val p2 = Place("p2", "B")
      val t1 = Transition("t1", "Transfer")

      val net = PetriNet(
        places = Set(p1, p2),
        transitions = Set(t1),
        inputArcs = Set(InputArc(p1, t1)),
        outputArcs = Set(OutputArc(t1, p2))
      )

      val initial = Marking(p1 -> 3, p2 -> 0)
      val stateSpace = StateSpaceExplorer.explore(net, initial)

      val result = InvariantChecker.checkTokenConservation(stateSpace, Set(p1, p2))
      result.satisfied shouldBe true
    }

    "détecter une violation d'invariant" in {
      val p1 = Place("p1", "Source")
      val p2 = Place("p2", "Sink")
      val t1 = Transition("t1", "Consume")

      // Transition qui consomme sans produire
      val net = PetriNet(
        places = Set(p1, p2),
        transitions = Set(t1),
        inputArcs = Set(InputArc(p1, t1, weight = 2)),
        outputArcs = Set(OutputArc(t1, p2, weight = 1))
      )

      val initial = Marking(p1 -> 4, p2 -> 0)
      val stateSpace = StateSpaceExplorer.explore(net, initial)

      val result = InvariantChecker.checkTokenConservation(stateSpace, Set(p1, p2))
      result.satisfied shouldBe false
    }
  }

  "PaymentPetriNet" should {

    "construire un modèle valide" in {
      val (net, marking) = PaymentPetriNet.build()

      net.places should have size 7
      net.transitions should have size 6
      marking.getTokens(PaymentPetriNet.accountA) shouldBe 5
      marking.getTokens(PaymentPetriNet.accountB) shouldBe 3
      marking.getTokens(PaymentPetriNet.transferReady) shouldBe 1
    }

    "permettre un transfert complet" in {
      val (net, marking) = PaymentPetriNet.build(initialBalanceA = 5, initialBalanceB = 0, transferAmount = 2)

      val stateSpace = StateSpaceExplorer.explore(net, marking)

      // Le transfert devrait pouvoir être complété
      stateSpace.reachableMarkings.exists(_.getTokens(PaymentPetriNet.transferDone) > 0) shouldBe true
    }

    "maintenir la non-négativité des soldes" in {
      val (net, marking) = PaymentPetriNet.build(initialBalanceA = 3, initialBalanceB = 2, transferAmount = 2)

      val stateSpace = StateSpaceExplorer.explore(net, marking)

      val result = InvariantChecker.checkNonNegativity(stateSpace, net.places)
      result.satisfied shouldBe true
    }
  }
}
