package com.music.petri.models

import com.music.petri.model._

/**
 * Modèle de réseau de Pétri pour le système de paiement distribué.
 *
 * Ce modèle capture le comportement du système Akka en termes de places et transitions :
 *
 * Places :
 * - AccountA_Funds / AccountB_Funds : représentent les fonds dans chaque compte
 * - TransferReady : un transfert est prêt à être initié
 * - TransferInProgress : un transfert est en cours
 * - TransferDone : transfert réussi
 * - TransferFailed : transfert échoué (fonds insuffisants)
 *
 * Transitions :
 * - InitiateTransfer : début du transfert
 * - DebitSource : débit du compte source
 * - CreditDestination : crédit du compte destination
 * - RejectInsufficient : rejet pour fonds insuffisants
 */
object PaymentPetriNet {

  // ===== Places =====
  val accountA = Place("p1", "CompteA_Fonds")
  val accountB = Place("p2", "CompteB_Fonds")
  val transferReady = Place("p3", "TransfertPrêt")
  val transferInProgress = Place("p4", "TransfertEnCours")
  val debitDone = Place("p5", "DébitEffectué")
  val transferDone = Place("p6", "TransfertTerminé")
  val transferFailed = Place("p7", "TransfertÉchoué")

  // ===== Transitions =====
  val initiateTransfer = Transition("t1", "InitierTransfert")
  val debitSource = Transition("t2", "DébiterSource")
  val creditDestination = Transition("t3", "CréditerDestination")
  val rejectInsufficient = Transition("t4", "RejeterInsufffisant")
  val resetSuccess = Transition("t5", "RéinitialiserSuccès")
  val resetFailure = Transition("t6", "RéinitialiserÉchec")

  /**
   * Construit le réseau de Pétri pour un scénario de transfert A → B.
   *
   * @param initialBalanceA solde initial du compte A (en jetons)
   * @param initialBalanceB solde initial du compte B (en jetons)
   * @param transferAmount  montant du transfert (en jetons)
   */
  def build(initialBalanceA: Int = 5, initialBalanceB: Int = 3, transferAmount: Int = 2): (PetriNet, Marking) = {

    val places = Set(accountA, accountB, transferReady, transferInProgress, debitDone, transferDone, transferFailed)
    val transitions = Set(initiateTransfer, debitSource, creditDestination, rejectInsufficient, resetSuccess, resetFailure)

    val inputArcs = Set(
      // InitierTransfert : consomme un jeton de TransfertPrêt
      InputArc(transferReady, initiateTransfer),

      // DébiterSource : consomme des jetons du compte A et une demande en cours
      InputArc(accountA, debitSource, weight = transferAmount),
      InputArc(transferInProgress, debitSource),

      // CréditerDestination : consomme le jeton DébitEffectué
      InputArc(debitDone, creditDestination),

      // RejeterInsuffisant : activé quand le compte A a insuffisamment de fonds
      // et qu'il y a un transfert en cours
      InputArc(transferInProgress, rejectInsufficient),
      InputArc(accountA, rejectInsufficient, weight = transferAmount, arcType = Inhibitor),

      // Réinitialiser après succès
      InputArc(transferDone, resetSuccess),

      // Réinitialiser après échec
      InputArc(transferFailed, resetFailure)
    )

    val outputArcs = Set(
      // InitierTransfert → TransfertEnCours
      OutputArc(initiateTransfer, transferInProgress),

      // DébiterSource → DébitEffectué
      OutputArc(debitSource, debitDone),

      // CréditerDestination → CompteB_Fonds + TransfertTerminé
      OutputArc(creditDestination, accountB, weight = transferAmount),
      OutputArc(creditDestination, transferDone),

      // RejeterInsuffisant → TransfertÉchoué
      OutputArc(rejectInsufficient, transferFailed),

      // Réinitialiser → TransfertPrêt (cycle)
      OutputArc(resetSuccess, transferReady),
      OutputArc(resetFailure, transferReady)
    )

    val net = PetriNet(places, transitions, inputArcs, outputArcs)

    val initialMarking = Marking(
      accountA -> initialBalanceA,
      accountB -> initialBalanceB,
      transferReady -> 1,
      transferInProgress -> 0,
      debitDone -> 0,
      transferDone -> 0,
      transferFailed -> 0
    )

    (net, initialMarking)
  }

  /**
   * Construit un modèle simple de dépôt/retrait sur un seul compte.
   */
  def buildSingleAccount(initialBalance: Int = 5): (PetriNet, Marking) = {
    val account = Place("sa1", "Compte_Fonds")
    val depositReady = Place("sa2", "DépôtPrêt")
    val withdrawReady = Place("sa3", "RetraitPrêt")
    val opDone = Place("sa4", "OpérationTerminée")

    val doDeposit = Transition("st1", "EffectuerDépôt")
    val doWithdraw = Transition("st2", "EffectuerRetrait")
    val resetOp = Transition("st3", "Réinitialiser")

    val places = Set(account, depositReady, withdrawReady, opDone)
    val transitions = Set(doDeposit, doWithdraw, resetOp)

    val inputArcs = Set(
      InputArc(depositReady, doDeposit),
      InputArc(withdrawReady, doWithdraw),
      InputArc(account, doWithdraw, weight = 1),
      InputArc(opDone, resetOp)
    )

    val outputArcs = Set(
      OutputArc(doDeposit, account, weight = 1),
      OutputArc(doDeposit, opDone),
      OutputArc(doWithdraw, opDone),
      OutputArc(resetOp, depositReady),
      OutputArc(resetOp, withdrawReady)
    )

    val net = PetriNet(places, transitions, inputArcs, outputArcs)

    val initialMarking = Marking(
      account -> initialBalance,
      depositReady -> 1,
      withdrawReady -> 1,
      opDone -> 0
    )

    (net, initialMarking)
  }
}
