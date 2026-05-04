# Vérification : Tests, Invariants et Logique LTL

## 1. Introduction à la Phase de Vérification
L'exploration de l'espace d'états permet de confronter le graphe généré à des formules mathématiques rigoureuses (`InvariantChecker` et `LTLChecker`). En parallèle, le code Scala est validé par une suite complète de tests BDD (via ScalaTest, utilisant le style `AnyFreeSpec`).

## 2. Vérification Mathématique
### 2.1. Absence d'Interblocage (Deadlock Freedom)
- **Définition** : Un système bloqué où aucune transition n'est possible.
- **Résultat** : L'algorithme valide l'absence de deadlock. Une erreur précédente sur l'interprétation mathématique des arcs inhibiteurs a été corrigée, confirmant que les transferts échouent proprement au lieu de figer le système.

### 2.2. Invariants Métier (Sûreté)
- **Conservation Stricte de l'Argent** : La somme (Solde A + Solde B + Argent en Transit) est toujours constante (aucun jeton ne se perd).
- **Non-Négativité** : `□(Solde ≥ 0)`. Il est mathématiquement impossible de tomber en découvert, validé sur tous les chemins du graphe.

### 2.3. Propriétés LTL (Vivacité)
- `◇(Transfert Terminé ou Échoué)` : Tout transfert finit inévitablement par produire une réponse, écartant tout "livelock".

## 3. Couverture par les Tests (ScalaTest)
Les vérifications mathématiques sont couvertes par 32 tests unitaires automatisés qui passent à **100% de succès** :
- `BankAccountSpec` : Comportement des comptes locaux et vérification des rejets.
- `TransactionManagerSpec` : Scénarios de transferts (réussite, échec) et intégration avec les logs.
- `LoggingActorSpec` : Vérifie que le système d'audit enregistre fidèlement tous les événements (horodatage, montants, types).
- `PetriNetSpec` & `LTLCheckerSpec` : Valident les algorithmes d'exploration mathématique.
