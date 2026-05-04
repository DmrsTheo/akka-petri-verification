# État de l'Art : Modélisation et Vérification Formelle

## 1. Introduction aux Systèmes Distribués Critiques
Les systèmes distribués critiques (comme les systèmes bancaires, les contrôles aériens ou les infrastructures médicales) reposent sur la coordination d'entités indépendantes communiquant via un réseau. La moindre défaillance, interblocage (deadlock) ou violation d'invariant peut entraîner des pertes financières majeures. Par conséquent, les approches classiques par tests sont insuffisantes. Une garantie mathématique est requise : c'est le rôle de la **vérification formelle**.

## 2. La Vérification Formelle
La vérification formelle consiste à prouver mathématiquement qu'un système satisfait un ensemble de propriétés spécifiées. Contrairement à la simulation qui explore certains chemins d'exécution aléatoires, la vérification formelle explore de manière exhaustive l'espace d'états d'un modèle.

## 3. Les Réseaux de Pétri
Les Réseaux de Pétri (RdP) sont un outil mathématique et graphique idéal pour modéliser des systèmes concurrents.
Un réseau de Pétri est composé de :
- **Places (cercles)** : représentent l'état du système (ex: les fonds d'un compte).
- **Transitions (rectangles)** : représentent les événements ou actions (ex: effectuer un virement).
- **Arcs orientés** : définissent le flux d'exécution.
- **Jetons (points)** : circulent dans les places.

**Avantages pour l'analyse :**
Les RdP permettent de calculer le graphe d'accessibilité. Nous pouvons vérifier :
- **Propriétés structurelles** : absence de deadlock (le système ne se fige jamais), vivacité.
- **Invariants** : propriétés toujours vraies, comme la conservation totale de la monnaie.

## 4. Logique Temporelle Linéaire (LTL)
La logique LTL est utilisée pour formaliser les exigences du système sur son évolution temporelle infinie.
- **□ (Toujours / Globally)** : Sûreté (ex: *□(Solde ≥ 0)*).
- **◇ (Éventuellement / Finally)** : Vivacité (ex: *◇(Transfert Terminé)*).

La combinaison des Réseaux de Pétri pour générer l'espace d'états, et de la logique LTL pour l'interroger, forme le cœur de la méthodologie de notre projet.
