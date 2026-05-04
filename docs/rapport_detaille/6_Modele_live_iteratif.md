# Modèle Live Itératif : Évolution du Système

## 1. Une Démarche de Conception Continue
Le projet a suivi une approche "live" et incrémentale. Le code (Scala/Akka) et le modèle mathématique se sont influencés mutuellement.

## 2. Historique des Itérations

### Itération 1 : Modèle de Compte Simple
- **Implémentation** : Création de `BankAccount`.
- **Analyse** : Réseau de Pétri à 4 places pour valider les dépôts/retraits en vase clos. Pas de deadlock détecté sur la boucle.

### Itération 2 : Distribution et Protocole de Transfert (2 Phases)
- **Implémentation** : Introduction du `TransactionManager` et du rollback asynchrone.
- **Analyse** : Les premiers Model Checks ont détecté une violation de la **conservation de monnaie** (l'argent restait "bloqué" lors d'un crash de la destination). Ce défaut a été corrigé dans Akka grâce au modèle formel !

### Itération 3 : LTL et Arcs Inhibiteurs
- **Implémentation** : Modélisation précise des règles "if balance < amount" via les arcs inhibiteurs.
- **Correction Live** : Une erreur de sémantique dans le moteur de Pétri causait un faux positif de deadlock lors des rejets. Le modèle mathématique a été affiné (`weight < amount`).

### Itération 4 : Audit, API et Frontend
- **Backend** : Ajout du `LoggingActor` pour traçabilité (SUCCESS / FAILED). Ajout du serveur Akka HTTP (`ApiServer`).
- **Frontend** : Création du `payment-frontend` en Vite/Vue-React pour interagir dynamiquement avec l'API. Le système, initialement pur concept mathématique en console, est devenu un véritable logiciel Full-Stack testable par l'utilisateur.

## 3. Pistes Futures
Le système distribué est fiable. Pour l'avenir, le modèle pourrait intégrer :
- Une topologie multi-JVM (Akka Cluster).
- La modélisation mathématique formelle des délais réseau via des Réseaux de Pétri Temporisés.
