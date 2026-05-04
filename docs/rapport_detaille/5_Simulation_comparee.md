# Simulation Comparée : Akka vs Réseaux de Pétri

## 1. Objectif de la Simulation
La dernière étape consiste à confronter le modèle formel (réseaux de Pétri) au comportement asynchrone réel du code. Grâce aux dernières itérations, cette simulation s'observe désormais via un écosystème complet : Console, Logs, et Interface Graphique.

## 2. Analyse Formelle vs Exécution Réelle

### 2.1. Le Rapport Formel (Théorie)
L'exécution de `SimulationRunner` produit le graphe d'accessibilité en mémoire. Le moteur visite tous les états (ex: pour un solde de 5, avec transfert de 2).
*Le Model Checker LTL confirme que le graphe répond "SATISFAIT" à toutes les contraintes de sûreté.*

### 2.2. L'Interface Graphique (Pratique)
En démarrant le backend Akka HTTP (`ApiServer`) et le client Vite (`payment-frontend`), l'utilisateur initie de véritables transferts.
- Lorsqu'un utilisateur clique sur "Transférer 100€" : le Frontend appelle l'API REST `POST /api/transfer`.
- Le `TransactionManager` exécute la logique de débits/crédits.
- L'audit (`LoggingActor`) horodate les actions : `[TRANSFER_INITIATED]`, `[TRANSFER_DEBIT_SUCCESS]`, puis `[TRANSFER_SUCCESS]`.

## 3. Analyse des Comportements aux Limites
Si l'utilisateur tente de transférer un montant supérieur au solde via l'interface graphique :
- **Dans le modèle Pétri** : L'arc inhibiteur s'active, empêchant le tir de `DebitSource` et guidant le jeton vers `TransferFailed`.
- **Dans le système Akka** : L'interface reçoit une erreur "Fonds insuffisants" (statut 400). Les logs serveurs tracent un `[TRANSFER_FAILED]`.

**Conclusion** : Le comportement asynchrone perçu par l'utilisateur (via le réseau HTTP REST et les acteurs) est en parfaite corrélation avec l'abstraction mathématique théorique de notre modèle LTL.
