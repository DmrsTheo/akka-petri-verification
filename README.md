# Akka Petri Verification — Système de Paiement Distribué

## Description

Système de paiement distribué modélisé avec **Akka/Scala** et vérifié formellement à l'aide de **réseaux de Pétri** et de la logique **LTL** (Linear Temporal Logic).

Ce projet répond aux exigences de modélisation et vérification d'une application critique distribuée :
- **Simulation Akka** : acteurs gérant des comptes bancaires, dépôts, retraits et transferts
- **Vérification formelle** : modèle de réseau de Pétri avec exploration d'espace d'états
- **Propriétés vérifiées** : absence de deadlock, invariants métier (solde ≥ 0), sûreté et vivacité (LTL)

## Architecture

```
src/main/scala/com/music/
├── payment/
│   ├── actors/
│   │   ├── BankAccount.scala          # Acteur gérant un compte individuel
│   │   ├── TransactionManager.scala   # Orchestration des transferts (2 phases)
│   │   └── BankSupervisor.scala       # Superviseur + gestion des acteurs
│   ├── messages/
│   │   └── Messages.scala             # Protocole de messages
│   ├── model/
│   │   └── Account.scala              # Modèle de domaine
│   └── Main.scala                     # Point d'entrée
├── petri/
│   ├── model/
│   │   └── PetriNet.scala             # Structures : Place, Transition, Arc, Marking
│   ├── engine/
│   │   ├── StateSpaceExplorer.scala   # Exploration du graphe d'accessibilité
│   │   ├── InvariantChecker.scala     # Vérification d'invariants métier
│   │   └── LTLChecker.scala           # Vérificateur LTL (sûreté, vivacité)
│   └── models/
│       └── PaymentPetriNet.scala      # Modèle Petri du système de paiement
└── simulation/
    └── SimulationRunner.scala         # Simulation comparative
```

## Prérequis

- **Java JDK** 11+ (testé avec JDK 21)
- **SBT** 1.9+

## Lancer le projet

```bash
# Compiler
sbt compile

# Lancer les tests (25 tests)
sbt test

# Exécuter la simulation complète
sbt run
```

## Fonctionnalités clés

### Système Akka
- Création de comptes avec solde initial
- Dépôts et retraits avec vérification de solde
- Transferts entre comptes (protocole 2 phases : débit → crédit)
- Supervision et tolérance aux pannes
- **Invariant garanti** : le solde ne peut jamais être négatif

### Réseau de Pétri
- Modélisation complète du protocole de transfert
- Support des arcs pondérés et inhibiteurs
- Exploration BFS de l'espace d'états
- Détection automatique de deadlocks
- Vérification de bornitude

### Vérification LTL
- **Sûreté** : `□(solde ≥ 0)` — le solde est toujours non-négatif
- **Vivacité** : `◇(transfert terminé)` — tout transfert initié finit par compléter
- **Absence de deadlock** : le système peut toujours progresser
- Génération de contre-exemples en cas de violation

## Tests

| Suite | Tests | Description |
|-------|-------|-------------|
| `BankAccountSpec` | 6 | Dépôts, retraits, solde, refus overdraft |
| `TransactionManagerSpec` | 3 | Transferts réussis/échoués, comptes inexistants |
| `PetriNetSpec` | 9 | Firing, espace d'états, deadlock, invariants |
| `LTLCheckerSpec` | 6 | Safety, liveness, deadlock freedom, payment properties |

## Auteurs

Projet réalisé dans le cadre du cours de Programmation Fondamentale — Ingénierie 2ème année.