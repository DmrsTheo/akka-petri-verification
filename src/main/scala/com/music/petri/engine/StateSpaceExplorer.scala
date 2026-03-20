package com.music.petri.engine

import com.music.petri.model._
import org.slf4j.LoggerFactory
import scala.collection.mutable

/**
 * Explorateur d'espace d'états pour les réseaux de Pétri.
 *
 * Génère le graphe de couverture/accessibilité complet à partir
 * d'un marquage initial, permettant l'analyse de :
 * - Accessibilité (quels états sont atteignables)
 * - Deadlock (états sans transition activée)
 * - Bornitude (limites sur les jetons dans chaque place)
 */
object StateSpaceExplorer {

  private val logger = LoggerFactory.getLogger(getClass)

  /** Résultat de l'exploration de l'espace d'états */
  case class StateSpace(
    reachableMarkings: Set[Marking],
    transitions: Set[(Marking, Transition, Marking)],
    deadlockStates: Set[Marking],
    initialMarking: Marking
  ) {
    def hasDeadlock: Boolean = deadlockStates.nonEmpty
    def stateCount: Int = reachableMarkings.size
    def transitionCount: Int = transitions.size

    /** Vérifie si un marquage est accessible depuis l'état initial */
    def isReachable(marking: Marking): Boolean = reachableMarkings.contains(marking)

    /** Retourne le nombre maximum de jetons dans une place donnée */
    def maxTokensIn(place: Place): Int =
      reachableMarkings.map(_.getTokens(place)).max

    /** Vérifie si le réseau est borné (k-bounded) */
    def isBounded(k: Int): Boolean =
      reachableMarkings.forall { marking =>
        marking.tokens.values.forall(_ <= k)
      }

    override def toString: String =
      s"""Espace d'états:
         |  - États accessibles: $stateCount
         |  - Transitions franchies: $transitionCount
         |  - États de deadlock: ${deadlockStates.size}
         |  - Deadlock détecté: $hasDeadlock""".stripMargin
  }

  /**
   * Explore l'espace d'états complet du réseau de Pétri.
   *
   * @param net            le réseau de Pétri
   * @param initialMarking le marquage initial
   * @param maxStates      limite maximale d'états à explorer (sécurité)
   * @return l'espace d'états complet
   */
  def explore(net: PetriNet, initialMarking: Marking, maxStates: Int = 10000): StateSpace = {
    logger.info(s"Début de l'exploration de l'espace d'états depuis $initialMarking")

    val visited = mutable.Set[Marking]()
    val allTransitions = mutable.Set[(Marking, Transition, Marking)]()
    val deadlocks = mutable.Set[Marking]()
    val queue = mutable.Queue[Marking]()

    queue.enqueue(initialMarking)
    visited.add(initialMarking)

    while (queue.nonEmpty && visited.size < maxStates) {
      val current = queue.dequeue()
      val enabled = net.enabledTransitions(current)

      if (enabled.isEmpty) {
        deadlocks.add(current)
        logger.debug(s"État de deadlock détecté: $current")
      } else {
        for (t <- enabled) {
          val next = net.fire(t, current)
          allTransitions.add((current, t, next))

          if (!visited.contains(next)) {
            visited.add(next)
            queue.enqueue(next)
          }
        }
      }
    }

    if (visited.size >= maxStates) {
      logger.warn(s"Exploration arrêtée: limite de $maxStates états atteinte")
    }

    val result = StateSpace(visited.toSet, allTransitions.toSet, deadlocks.toSet, initialMarking)
    logger.info(result.toString)
    result
  }
}
