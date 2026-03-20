package com.music.petri.engine

import com.music.petri.model._
import org.slf4j.LoggerFactory

/**
 * Vérificateur de propriétés LTL (Linear Temporal Logic) sur les réseaux de Pétri.
 *
 * LTL permet d'exprimer des propriétés temporelles :
 * - Sûreté (Safety) : "Quelque chose de mauvais n'arrive jamais"
 *     → Always(P) : P est vrai dans tous les états accessibles
 * - Vivacité (Liveness) : "Quelque chose de bon finit par arriver"
 *     → Eventually(P) : P est vrai dans au moins un état accessible
 *
 * Les formules sont évaluées sur le graphe de couverture (espace d'états).
 */
object LTLChecker {

  private val logger = LoggerFactory.getLogger(getClass)

  // ===== Formules LTL =====

  sealed trait LTLFormula {
    def prettyPrint: String
  }

  /** Proposition atomique : prédicat sur un marquage */
  case class Atom(name: String, predicate: Marking => Boolean) extends LTLFormula {
    def prettyPrint: String = name
  }

  /** Négation */
  case class Not(formula: LTLFormula) extends LTLFormula {
    def prettyPrint: String = s"¬(${formula.prettyPrint})"
  }

  /** Conjonction */
  case class And(left: LTLFormula, right: LTLFormula) extends LTLFormula {
    def prettyPrint: String = s"(${left.prettyPrint} ∧ ${right.prettyPrint})"
  }

  /** Disjonction */
  case class Or(left: LTLFormula, right: LTLFormula) extends LTLFormula {
    def prettyPrint: String = s"(${left.prettyPrint} ∨ ${right.prettyPrint})"
  }

  /** Implication */
  case class Implies(left: LTLFormula, right: LTLFormula) extends LTLFormula {
    def prettyPrint: String = s"(${left.prettyPrint} → ${right.prettyPrint})"
  }

  /** Toujours (dans tous les états accessibles) - G (Globally) */
  case class Always(formula: LTLFormula) extends LTLFormula {
    def prettyPrint: String = s"□(${formula.prettyPrint})"
  }

  /** Éventuellement (dans au moins un état) - F (Finally) */
  case class Eventually(formula: LTLFormula) extends LTLFormula {
    def prettyPrint: String = s"◇(${formula.prettyPrint})"
  }

  // ===== Résultat de vérification =====

  sealed trait LTLResult {
    def formula: LTLFormula
    def satisfied: Boolean
  }

  case class LTLSatisfied(formula: LTLFormula) extends LTLResult {
    val satisfied = true
    override def toString: String = s"✓ SATISFAIT: ${formula.prettyPrint}"
  }

  case class LTLUnsatisfied(formula: LTLFormula, counterExample: Option[Marking] = None) extends LTLResult {
    val satisfied = false
    override def toString: String = {
      val ce = counterExample.map(m => s"\n  Contre-exemple: $m").getOrElse("")
      s"✗ NON SATISFAIT: ${formula.prettyPrint}$ce"
    }
  }

  // ===== Évaluation =====

  /**
   * Évalue une formule LTL sur l'espace d'états.
   *
   * Sémantique simplifiée pour les opérateurs temporels :
   * - Always(P) : P vrai dans TOUS les états accessibles
   * - Eventually(P) : P vrai dans AU MOINS UN état accessible
   */
  def check(formula: LTLFormula, stateSpace: StateSpaceExplorer.StateSpace): LTLResult = {
    logger.info(s"Vérification LTL: ${formula.prettyPrint}")

    formula match {
      case Always(inner) =>
        // Trouver un état où inner est faux
        val violated = stateSpace.reachableMarkings.find(m => !evaluate(inner, m))
        violated match {
          case Some(counterEx) =>
            val result = LTLUnsatisfied(formula, Some(counterEx))
            logger.warn(result.toString)
            result
          case None =>
            val result = LTLSatisfied(formula)
            logger.info(result.toString)
            result
        }

      case Eventually(inner) =>
        // Trouver au moins un état où inner est vrai
        val found = stateSpace.reachableMarkings.exists(m => evaluate(inner, m))
        if (found) {
          val result = LTLSatisfied(formula)
          logger.info(result.toString)
          result
        } else {
          val result = LTLUnsatisfied(formula, None)
          logger.warn(result.toString)
          result
        }

      case atom: Atom =>
        // Vérification ponctuelle sur l'état initial
        if (evaluate(atom, stateSpace.initialMarking)) LTLSatisfied(formula)
        else LTLUnsatisfied(formula, Some(stateSpace.initialMarking))

      case Not(inner) =>
        val innerResult = check(inner, stateSpace)
        if (innerResult.satisfied) LTLUnsatisfied(formula) else LTLSatisfied(formula)

      case And(left, right) =>
        val lr = check(left, stateSpace)
        val rr = check(right, stateSpace)
        if (lr.satisfied && rr.satisfied) LTLSatisfied(formula)
        else LTLUnsatisfied(formula, lr match {
          case u: LTLUnsatisfied => u.counterExample
          case _ => (rr match { case u: LTLUnsatisfied => u.counterExample; case _ => None })
        })

      case Or(left, right) =>
        val lr = check(left, stateSpace)
        val rr = check(right, stateSpace)
        if (lr.satisfied || rr.satisfied) LTLSatisfied(formula)
        else LTLUnsatisfied(formula)

      case Implies(left, right) =>
        // P → Q ≡ ¬P ∨ Q
        check(Or(Not(left), right), stateSpace)
    }
  }

  /**
   * Évalue un prédicat atomique sur un marquage donné.
   */
  private def evaluate(formula: LTLFormula, marking: Marking): Boolean = {
    formula match {
      case Atom(_, predicate) => predicate(marking)
      case Not(inner)         => !evaluate(inner, marking)
      case And(left, right)   => evaluate(left, marking) && evaluate(right, marking)
      case Or(left, right)    => evaluate(left, marking) || evaluate(right, marking)
      case Implies(l, r)      => !evaluate(l, marking) || evaluate(r, marking)
      case Always(_)          => true  // Cannot evaluate temporal ops on single state
      case Eventually(_)      => true
    }
  }

  // ===== Formules prédéfinies pour le système de paiement =====

  /**
   * Crée une formule de sûreté : le solde d'un compte ne peut jamais être négatif.
   */
  def safetyBalanceNonNegative(accountPlace: Place): LTLFormula =
    Always(Atom(s"${accountPlace.name} ≥ 0", m => m.getTokens(accountPlace) >= 0))

  /**
   * Crée une formule de vivacité : un transfert finit par être complété.
   */
  def livenessTransferCompletes(completedPlace: Place): LTLFormula =
    Eventually(Atom(s"${completedPlace.name} > 0", m => m.getTokens(completedPlace) > 0))

  /**
   * Absence de deadlock : il existe toujours au moins une transition activée.
   * (Vérification via l'espace d'états plutôt que par formule LTL)
   */
  def deadlockFreedom(stateSpace: StateSpaceExplorer.StateSpace): LTLResult = {
    if (stateSpace.hasDeadlock) {
      LTLUnsatisfied(
        Always(Atom("pas de deadlock", _ => true)),
        stateSpace.deadlockStates.headOption
      )
    } else {
      LTLSatisfied(Always(Atom("pas de deadlock", _ => true)))
    }
  }
}
