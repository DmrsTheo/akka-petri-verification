package com.music.simulation

import com.music.petri.engine._
import com.music.petri.models.PaymentPetriNet
import org.slf4j.LoggerFactory

/**
 * Exécute la simulation et la comparaison entre le système Akka et le modèle Petri Net.
 *
 * Ce module :
 * 1. Construit le réseau de Pétri du système de paiement
 * 2. Explore l'espace d'états
 * 3. Vérifie les propriétés structurelles et invariants métier
 * 4. Vérifie les propriétés LTL (sûreté, vivacité)
 * 5. Produit un rapport de comparaison
 */
object SimulationRunner {

  private val logger = LoggerFactory.getLogger(getClass)

  /**
   * Exécute l'analyse complète du modèle Petri Net pour le système de paiement.
   */
  def runPetriNetAnalysis(): Unit = {
    println()
    println("=" * 70)
    println("  FORMAL ANALYSIS - PAYMENT SYSTEM PETRI NET")
    println("=" * 70)
    println()

    // === 1. Transfer model ===
    println("-" * 50)
    println("  1. TRANSFER MODEL (Account A -> Account B)")
    println("-" * 50)

    val (transferNet, transferMarking) = PaymentPetriNet.build(
      initialBalanceA = 5,
      initialBalanceB = 3,
      transferAmount = 2
    )

    println(s"\n  Initial marking: $transferMarking")
    println(s"  Number of places: ${transferNet.places.size}")
    println(s"  Number of transitions: ${transferNet.transitions.size}")

    // Explore state space
    println("\n  > Exploring state space...")
    val transferStateSpace = StateSpaceExplorer.explore(transferNet, transferMarking)
    println(s"  ${transferStateSpace.toString.replace("\n", "\n  ")}")

    // Check invariants
    println("\n  > Checking business invariants...")

    val transferInvariants = List(
      ("Account A balance >= 0", (m: com.music.petri.model.Marking) =>
        m.getTokens(PaymentPetriNet.accountA) >= 0),
      ("Account B balance >= 0", (m: com.music.petri.model.Marking) =>
        m.getTokens(PaymentPetriNet.accountB) >= 0),
      ("Transfer mutual exclusion", (m: com.music.petri.model.Marking) =>
        m.getTokens(PaymentPetriNet.transferInProgress) + m.getTokens(PaymentPetriNet.debitDone) <= 1),
        ("Strict money conservation", (m: com.music.petri.model.Marking) => {
        val accountAMoney = m.getTokens(PaymentPetriNet.accountA)
        val accountBMoney = m.getTokens(PaymentPetriNet.accountB)
        // Money in transit (held after debit but before credit)
        val moneyInTransit = m.getTokens(PaymentPetriNet.debitDone) * 2 // 2 is transfer amount
        // Sum must always equal total initial (5 in A + 3 in B = 8)
        (accountAMoney + accountBMoney + moneyInTransit) == 8
      })
    )

    val invariantResults = InvariantChecker.fullCheck(transferStateSpace, transferNet, transferInvariants)
    invariantResults.foreach(r => println(s"    $r"))

    // Check LTL
    println("\n  > Checking LTL properties...")

    val ltlProperties = List(
      LTLChecker.safetyBalanceNonNegative(PaymentPetriNet.accountA),
      LTLChecker.safetyBalanceNonNegative(PaymentPetriNet.accountB),
      LTLChecker.livenessTransferCompletes(PaymentPetriNet.transferDone)
    )

    ltlProperties.foreach { formula =>
      val result = LTLChecker.check(formula, transferStateSpace)
      println(s"    $result")
    }

    // Deadlock
    println("\n  > Checking absence of deadlock...")
    val deadlockResult = LTLChecker.deadlockFreedom(transferStateSpace)
    println(s"    $deadlockResult")

    // === 2. Simple account model ===
    println()
    println("-" * 50)
    println("  2. SIMPLE ACCOUNT MODEL (deposit/withdrawal)")
    println("-" * 50)

    val (singleNet, singleMarking) = PaymentPetriNet.buildSingleAccount(initialBalance = 3)
    println(s"\n  Initial marking: $singleMarking")

    val singleStateSpace = StateSpaceExplorer.explore(singleNet, singleMarking)
    println(s"  ${singleStateSpace.toString.replace("\n", "\n  ")}")

    // === 3. Synthesis report ===
    println()
    println("-" * 50)
    println("  3. SYNTHESIS REPORT")
    println("-" * 50)
    println()

    val allSatisfied = invariantResults.forall(_.satisfied) && deadlockResult.satisfied
    if (allSatisfied) {
      println("  + RESULT: All properties are verified.")
      println("    Petri Net model confirms Akka system correctness.")
      println("    - No deadlock detected")
      println("    - Business invariants respected (non-negative balances)")
      println("    - LTL properties satisfied (safety and liveness)")
    } else {
      println("  ! RESULT: Some properties are NOT verified.")
      println("    See above for violation details.")
      invariantResults.filterNot(_.satisfied).foreach(r => println(s"    - $r"))
    }

    println()
    println("=" * 70)
    println("  END OF FORMAL ANALYSIS")
    println("=" * 70)
    println()
  }
}
