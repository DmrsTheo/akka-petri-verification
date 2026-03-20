package com.music.petri.engine

import com.music.petri.model._
import org.slf4j.LoggerFactory

/**
 * Vérificateur d'invariants métier sur les réseaux de Pétri.
 *
 * Vérifie que certaines propriétés sont respectées dans TOUS les
 * états accessibles du réseau. Les invariants typiques incluent :
 * - Conservation des jetons (somme constante)
 * - Non-négativité (pas de solde négatif)
 * - Bornes supérieures/inférieures sur les places
 */
object InvariantChecker {

  private val logger = LoggerFactory.getLogger(getClass)

  /** Résultat de la vérification d'un invariant */
  sealed trait InvariantResult {
    def name: String
    def satisfied: Boolean
  }

  case class InvariantSatisfied(name: String) extends InvariantResult {
    val satisfied = true
    override def toString: String = s"✓ SATISFAIT: $name"
  }

  case class InvariantViolated(name: String, violatingState: Marking, details: String) extends InvariantResult {
    val satisfied = false
    override def toString: String = s"✗ VIOLÉ: $name\n  État: $violatingState\n  Détails: $details"
  }

  /**
   * Vérifie un prédicat sur tous les états accessibles.
   *
   * @param name       nom de l'invariant
   * @param stateSpace espace d'états exploré
   * @param predicate  condition devant être vraie pour chaque marquage
   */
  def checkInvariant(
    name: String,
    stateSpace: StateSpaceExplorer.StateSpace,
    predicate: Marking => Boolean
  ): InvariantResult = {
    logger.info(s"Vérification de l'invariant: $name")

    stateSpace.reachableMarkings.find(m => !predicate(m)) match {
      case Some(violating) =>
        val result = InvariantViolated(name, violating, "Prédicat non satisfait")
        logger.warn(result.toString)
        result
      case None =>
        val result = InvariantSatisfied(name)
        logger.info(result.toString)
        result
    }
  }

  /**
   * Vérifie la conservation des jetons :
   * la somme des jetons dans les places spécifiées reste constante.
   */
  def checkTokenConservation(
    stateSpace: StateSpaceExplorer.StateSpace,
    conservedPlaces: Set[Place]
  ): InvariantResult = {
    val initialSum = conservedPlaces.toList.map(stateSpace.initialMarking.getTokens).sum

    checkInvariant(
      s"Conservation des jetons (somme attendue: $initialSum)",
      stateSpace,
      marking => conservedPlaces.toList.map(marking.getTokens).sum == initialSum
    )
  }

  /**
   * Vérifie qu'aucune place spécifiée n'a un nombre négatif de jetons.
   * (vérification structurelle, normalement garantie par le modèle)
   */
  def checkNonNegativity(
    stateSpace: StateSpaceExplorer.StateSpace,
    places: Set[Place]
  ): InvariantResult = {
    checkInvariant(
      "Non-négativité des places",
      stateSpace,
      marking => places.forall(p => marking.getTokens(p) >= 0)
    )
  }

  /**
   * Vérifie qu'une place ne dépasse jamais une borne supérieure.
   */
  def checkUpperBound(
    stateSpace: StateSpaceExplorer.StateSpace,
    place: Place,
    maxTokens: Int
  ): InvariantResult = {
    checkInvariant(
      s"Borne supérieure de ${place.name} ≤ $maxTokens",
      stateSpace,
      marking => marking.getTokens(place) <= maxTokens
    )
  }

  /**
   * Exécute toutes les vérifications standard et retourne un rapport.
   */
  def fullCheck(
    stateSpace: StateSpaceExplorer.StateSpace,
    net: PetriNet,
    businessInvariants: List[(String, Marking => Boolean)] = Nil
  ): List[InvariantResult] = {
    logger.info("=== Début de la vérification complète des invariants ===")

    val results = scala.collection.mutable.ListBuffer[InvariantResult]()

    // 1. Non-négativité
    results += checkNonNegativity(stateSpace, net.places)

    // 2. Vérification de deadlock
    if (stateSpace.hasDeadlock) {
      results += InvariantViolated(
        "Absence de deadlock",
        stateSpace.deadlockStates.head,
        s"${stateSpace.deadlockStates.size} état(s) de deadlock détecté(s)"
      )
    } else {
      results += InvariantSatisfied("Absence de deadlock")
    }

    // 3. Bornitude (1-safe check)
    val bounded = stateSpace.isBounded(100)
    if (bounded) {
      results += InvariantSatisfied("Bornitude (100-borné)")
    } else {
      results += InvariantViolated("Bornitude (100-borné)", stateSpace.initialMarking, "Réseau non borné")
    }

    // 4. Invariants métier personnalisés
    for ((name, predicate) <- businessInvariants) {
      results += checkInvariant(name, stateSpace, predicate)
    }

    logger.info(s"=== Vérification terminée: ${results.count(_.satisfied)}/${results.size} invariants satisfaits ===")
    results.toList
  }
}
