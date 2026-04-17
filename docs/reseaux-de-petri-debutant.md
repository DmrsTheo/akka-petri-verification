# Réseaux de Pétri pour ce projet (version débutant, courte)

## 1. Pourquoi on utilise ça ici

Dans ce projet, on veut vérifier qu’un système de transfert bancaire reste correct, même quand plusieurs actions peuvent se produire en parallèle.

Un test classique vérifie quelques scénarios.
Un réseau de Pétri permet de raisonner sur tous les états possibles du système (dans une limite d’exploration).

Objectif concret du projet:
- éviter un solde négatif
- garantir qu’un transfert finit soit en succès, soit en échec contrôlé
- détecter les blocages (deadlocks)

## 2. Les 4 briques à retenir

Un réseau de Pétri est un graphe avec 4 éléments simples.

1. Place
Une place représente une ressource ou une condition.
Exemples dans ce projet: fonds du compte A, fonds du compte B, transfert prêt, transfert en cours.

2. Transition
Une transition représente une action.
Exemples: initier le transfert, débiter la source, créditer la destination.

3. Jeton
Un jeton représente une unité de ressource.
Ici, on peut l’interpréter comme une unité de fonds disponible ou un indicateur d’état.

4. Arc
Un arc relie une place à une transition (ou l’inverse) et peut avoir un poids.
- Arc normal: consomme ou produit des jetons
- Arc inhibiteur: autorise une transition seulement si une place est vide

## 3. Comment le réseau “tourne”

Deux idées suffisent.

1. Activation
Une transition est activée si toutes ses préconditions sont remplies:
- assez de jetons sur les arcs d’entrée
- et, pour un arc inhibiteur, la place surveillée doit être vide

2. Tir
Quand une transition tire:
- elle retire des jetons des places d’entrée
- elle ajoute des jetons dans les places de sortie

En bref: on transforme un état du système en un autre état.
Cet état s’appelle le marquage (distribution des jetons).

## 4. Exemple concret du projet: transfert de 2 unités

Idée de départ:
- Compte A a 5
- Compte B a 3
- un transfert est prêt

Séquence typique:
1. InitierTransfert
Le jeton passe de “TransfertPrêt” vers “TransfertEnCours”.

2. DébiterSource
On retire 2 jetons du compte A (si possible).
On marque que le débit est fait.

3. CréditerDestination
On ajoute 2 jetons au compte B.
Le transfert est marqué terminé.

Cas fonds insuffisants:
Une transition de rejet peut être activée (avec logique d’inhibition) pour terminer proprement en échec, sans créer un état incohérent.

## 5. Ce que le projet vérifie automatiquement

Le code explore l’espace des états atteignables (BFS) et vérifie des propriétés.

1. Invariants (toujours vrais)
- solde jamais négatif
- cohérence globale des jetons selon les règles du modèle

2. Propriétés temporelles simples (LTL)
- Always(P): P est vrai dans tous les états explorés
- Eventually(P): P devient vrai à un moment

Exemple métier:
- Always(solde >= 0)
- Eventually(transfertTermine ou transfertEchoue)

## 6. Lien direct avec les fichiers du projet

Préfixe commun: `src/main/scala/com/music/petri/`

- Modèle Petri (places, transitions, arcs, marquage):
  `model/PetriNet.scala`
- Réseau de paiement concret:
  `models/PaymentPetriNet.scala`
- Exploration des états et détection de deadlock:
  `engine/StateSpaceExplorer.scala`
- Vérification des invariants:
  `engine/InvariantChecker.scala`
- Vérification LTL:
  `engine/LTLChecker.scala`

## 7. Erreurs fréquentes de débutant

1. Penser qu’une transition peut tirer “quand on veut”
Faux: elle doit être activée par les jetons.

2. Penser qu’un arc inhibiteur consomme des jetons
Faux: il sert de condition, il ne consomme pas.

3. Confondre modèle et implémentation réelle
Le réseau de Pétri est une abstraction logique du système Akka, pas une copie exacte des timings/messages réels.

## 8. Résumé en une phrase

Dans ce projet, le réseau de Pétri sert à modéliser le transfert bancaire comme des mouvements de jetons pour prouver automatiquement que le système reste sûr (pas de solde négatif) et progresse correctement (le transfert finit).