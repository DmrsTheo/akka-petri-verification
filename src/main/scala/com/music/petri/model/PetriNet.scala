package com.music.petri.model

/**
 * Structures de données pour les réseaux de Pétri.
 *
 * Un réseau de Pétri est un graphe biparti composé de :
 * - Places : représentent les états/conditions (contiennent des jetons)
 * - Transitions : représentent les actions/événements
 * - Arcs : connectent places et transitions (avec poids)
 *
 * Le marquage (Marking) définit la distribution courante des jetons.
 */

/** Une place dans le réseau de Pétri */
case class Place(id: String, name: String)

/** Une transition dans le réseau de Pétri */
case class Transition(id: String, name: String)

/** Type d'arc */
sealed trait ArcType
case object Normal extends ArcType
case object Inhibitor extends ArcType

/** Arc reliant une place à une transition ou vice-versa */
sealed trait Arc {
  def weight: Int
  def arcType: ArcType
}

/** Arc d'une place vers une transition (arc d'entrée) */
case class InputArc(place: Place, transition: Transition, weight: Int = 1, arcType: ArcType = Normal) extends Arc

/** Arc d'une transition vers une place (arc de sortie) */
case class OutputArc(transition: Transition, place: Place, weight: Int = 1, arcType: ArcType = Normal) extends Arc

/** Marquage : distribution des jetons dans les places */
case class Marking(tokens: Map[Place, Int]) {

  def getTokens(place: Place): Int = tokens.getOrElse(place, 0)

  def setTokens(place: Place, count: Int): Marking = {
    require(count >= 0, s"Le nombre de jetons ne peut pas être négatif pour ${place.name}")
    Marking(tokens + (place -> count))
  }

  def addTokens(place: Place, count: Int): Marking =
    setTokens(place, getTokens(place) + count)

  def removeTokens(place: Place, count: Int): Marking =
    setTokens(place, getTokens(place) - count)

  override def toString: String =
    tokens.toList.sortBy(_._1.id).map { case (p, n) => s"${p.name}=$n" }.mkString("[", ", ", "]")
}

object Marking {
  def empty: Marking = Marking(Map.empty[Place, Int])

  def apply(entries: (Place, Int)*): Marking =
    Marking(entries.toMap)
}

/**
 * Réseau de Pétri complet.
 *
 * @param places      ensemble des places
 * @param transitions ensemble des transitions
 * @param inputArcs   arcs place → transition
 * @param outputArcs  arcs transition → place
 */
case class PetriNet(
  places: Set[Place],
  transitions: Set[Transition],
  inputArcs: Set[InputArc],
  outputArcs: Set[OutputArc]
) {

  /** Arcs d'entrée pour une transition donnée */
  def inputsOf(t: Transition): Set[InputArc] =
    inputArcs.filter(_.transition == t)

  /** Arcs de sortie pour une transition donnée */
  def outputsOf(t: Transition): Set[OutputArc] =
    outputArcs.filter(_.transition == t)

  /** Vérifie si une transition est activée (franchissable) pour un marquage donné */
  def isEnabled(t: Transition, marking: Marking): Boolean = {
    inputsOf(t).forall { arc =>
      arc.arcType match {
        case Normal    => marking.getTokens(arc.place) >= arc.weight
        case Inhibitor => marking.getTokens(arc.place) < arc.weight
      }
    }
  }

  /** Retourne les transitions activées pour un marquage donné */
  def enabledTransitions(marking: Marking): Set[Transition] =
    transitions.filter(isEnabled(_, marking))

  /**
   * Franchit (fire) une transition et retourne le nouveau marquage.
   * Pré-condition : la transition doit être activée.
   */
  def fire(t: Transition, marking: Marking): Marking = {
    require(isEnabled(t, marking), s"La transition ${t.name} n'est pas activée!")

    // Retirer les jetons des places d'entrée
    val afterConsume = inputsOf(t).foldLeft(marking) { (m, arc) =>
      arc.arcType match {
        case Normal    => m.removeTokens(arc.place, arc.weight)
        case Inhibitor => m // Les arcs inhibiteurs ne consomment pas de jetons
      }
    }

    // Ajouter les jetons aux places de sortie
    outputsOf(t).foldLeft(afterConsume) { (m, arc) =>
      m.addTokens(arc.place, arc.weight)
    }
  }
}
