# Architecture : Modélisation Fonctionnelle et Concurrente (Full-Stack)

## 1. Choix du Cas d'Usage
Le projet modélise un **Système de Virement Bancaire Distribué**. Ce cas d'usage est intrinsèquement critique : les erreurs de concurrence peuvent causer des pertes d'argent ou bloquer des comptes.

## 2. Architecture Backend (Acteurs Akka)
L'application repose sur le framework Akka en Scala (Modèle d'Acteurs), garantissant concurrence, asynchronisme et tolérance aux pannes.
L'architecture métier comprend :
- **BankAccount** : Gère l'état d'un compte individuel et protège l'invariant mathématique `Solde >= 0`.
- **BankSupervisor** : Chef d'orchestre, gère le cycle de vie des comptes et la tolérance aux erreurs.
- **TransactionManager** : Implémente le protocole de transfert en 2 phases (Débit -> Crédit) avec système de `Rollback` en cas d'échec de la phase 2.
- **LoggingActor** : Un composant transversal dédié à l'audit. Il intercepte et journalise toutes les transactions et opérations pour assurer la traçabilité complète du système (SUCCESS, FAILED, CRITICAL_FAILURE).

## 3. Architecture API REST (Akka HTTP)
Pour exposer notre modèle d'acteurs de manière standard, nous avons mis en place une couche réseau :
- **ApiServer & ApiRoutes** : Déploient un serveur HTTP sur le port 8080.
- Expose des endpoints RESTful (`POST /api/transfer`, `POST /api/accounts/:id/deposit`, etc.).
- Convertit les requêtes HTTP JSON en messages asynchrones envoyés au `BankSupervisor`, puis attend les réponses (Pattern `ask`).

## 4. Architecture Frontend (Interface Graphique Vite/Vue-React)
La dernière itération du système (présente dans `payment-frontend`) a introduit une application client riche :
- **Stack Technologique** : Vite, TailwindCSS.
- **Rôle** : Fournir une interface d'interaction pour les utilisateurs finaux (dépôts, retraits, transferts).
- Le frontend communique directement avec l'API REST Akka, permettant d'observer la réactivité du système d'acteurs en temps réel. Le `LoggingActor` trace toutes ces interactions.
