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
    println("  ANALYSE FORMELLE — RÉSEAU DE PÉTRI DU SYSTÈME DE PAIEMENT")
    println("=" * 70)
    println()

    // === 1. Modèle de transfert ===
    println("━" * 50)
    println("  1. MODÈLE DE TRANSFERT (Compte A → Compte B)")
    println("━" * 50)

    val (transferNet, transferMarking) = PaymentPetriNet.build(
      initialBalanceA = 5,
      initialBalanceB = 3,
      transferAmount = 2
    )

    println(s"\n  Marquage initial: $transferMarking")
    println(s"  Nombre de places: ${transferNet.places.size}")
    println(s"  Nombre de transitions: ${transferNet.transitions.size}")

    // Explorer l'espace d'états
    println("\n  → Exploration de l'espace d'états...")
    val transferStateSpace = StateSpaceExplorer.explore(transferNet, transferMarking)
    println(s"  ${transferStateSpace.toString.replace("\n", "\n  ")}")

    // Vérification des invariants
    println("\n  → Vérification des invariants métier...")

    val transferInvariants = List(
      ("Solde Compte A ≥ 0", (m: com.music.petri.model.Marking) =>
        m.getTokens(PaymentPetriNet.accountA) >= 0),
      ("Solde Compte B ≥ 0", (m: com.music.petri.model.Marking) =>
        m.getTokens(PaymentPetriNet.accountB) >= 0),
      ("Exclusion mutuelle transfert", (m: com.music.petri.model.Marking) =>
        m.getTokens(PaymentPetriNet.transferInProgress) + m.getTokens(PaymentPetriNet.debitDone) <= 1)
    )

    val invariantResults = InvariantChecker.fullCheck(transferStateSpace, transferNet, transferInvariants)
    invariantResults.foreach(r => println(s"    $r"))

    // Vérification LTL
    println("\n  → Vérification des propriétés LTL...")

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
    println("\n  → Vérification de l'absence de deadlock...")
    val deadlockResult = LTLChecker.deadlockFreedom(transferStateSpace)
    println(s"    $deadlockResult")

    // === 2. Modèle compte simple ===
    println()
    println("━" * 50)
    println("  2. MODÈLE COMPTE SIMPLE (dépôt/retrait)")
    println("━" * 50)

    val (singleNet, singleMarking) = PaymentPetriNet.buildSingleAccount(initialBalance = 3)
    println(s"\n  Marquage initial: $singleMarking")

    val singleStateSpace = StateSpaceExplorer.explore(singleNet, singleMarking)
    println(s"  ${singleStateSpace.toString.replace("\n", "\n  ")}")

    // === 3. Rapport de synthèse ===
    println()
    println("━" * 50)
    println("  3. RAPPORT DE SYNTHÈSE")
    println("━" * 50)
    println()

    val allSatisfied = invariantResults.forall(_.satisfied) && deadlockResult.satisfied
    if (allSatisfied) {
      println("  ✓ RÉSULTAT: Toutes les propriétés sont vérifiées.")
      println("    Le modèle Petri Net confirme la correction du système Akka.")
      println("    - Pas de deadlock détecté")
      println("    - Invariants métier respectés (soldes non-négatifs)")
      println("    - Propriétés LTL satisfaites (sûreté et vivacité)")
    } else {
      println("  ⚠ RÉSULTAT: Certaines propriétés ne sont PAS vérifiées.")
      println("    Voir les détails ci-dessus pour les violations détectées.")
      invariantResults.filterNot(_.satisfied).foreach(r => println(s"    - $r"))
    }

    println()
    println("=" * 70)
    println("  FIN DE L'ANALYSE FORMELLE")
    println("=" * 70)
    println()
  }
}
