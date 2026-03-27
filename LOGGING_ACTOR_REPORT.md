# LoggingActor - Récapitulatif des modifications

## 📋 Résumé

Un nouvel acteur Akka appelé **LoggingActor** a été créé pour enregistrer toutes les transactions et opérations du système bancaire distribué. Cet acteur maintient un historique centralisé et horodaté de tous les événements.

## 🆕 Fichiers créés

### 1. [LoggingActor.scala](src/main/scala/com/music/payment/actors/LoggingActor.scala)
- **Package**: `com.music.payment.actors`
- **Responsabilité**: Enregistrer et maintenir l'historique des transactions
- **Fonctionnalités**:
  - Enregistre les transferts entre comptes avec statuts (SUCCESS, FAILED, etc.)
  - Enregistre les opérations de compte (dépôt, retrait, création)
  - Horodate chaque entrée de log
  - Fournit un journal complet consultable
  - Maintient un buffer en mémoire de toutes les transactions

### 2. [LoggingActorSpec.scala](src/test/scala/com/music/payment/LoggingActorSpec.scala)
- **Tests unitaires** pour le LoggingActor
- **6 tests** validant:
  - Enregistrement des transferts
  - Enregistrement des opérations de compte
  - Maintien de l'historique multi-transactions
  - Enregistrement des erreurs et échecs
  - Horodatage correct des entrées
  - Gestion des montants optionnels

## 📝 Modifications aux fichiers existants

### [Messages.scala](src/main/scala/com/music/payment/messages/Messages.scala)
Ajout des messages pour communiquer avec le LoggingActor:

```scala
sealed trait LoggingCommand
case class LogTransaction(
  transactionType: String,
  fromAccountId: Option[String],
  toAccountId: Option[String],
  amount: Option[Double],
  status: String,
  details: String,
  timestamp: Long = System.currentTimeMillis()
) extends LoggingCommand

case class LogAccountOperation(
  operationType: String,
  accountId: String,
  amount: Option[Double],
  newBalance: Double,
  status: String,
  details: String,
  timestamp: Long = System.currentTimeMillis()
) extends LoggingCommand

case class GetTransactionLog(replyTo: ActorRef[TransactionLogResponse])

case class TransactionLogResponse(transactions: List[String])
```

### [BankSupervisor.scala](src/main/scala/com/music/payment/actors/BankSupervisor.scala)
- Création du LoggingActor au démarrage du supervisor
- Passage du LoggingActor aux TransactionManagers
- Enregistrement de la création des comptes

### [TransactionManager.scala](src/main/scala/com/music/payment/actors/TransactionManager.scala)
Intégration complète du logging pour tous les événements de transfert:

```scala
// Exemple de cas enregistrés
- TRANSFER_INITIATED: Quand un transfert est lancé
- TRANSFER_DEBIT_SUCCESS: Quand le débit du compte source réussit
- TRANSFER_SUCCESS: Quand le transfert est entièrement complété
- TRANSFER_FAILED: Quand le transfert échoue (fonds insuffisants, compte inexistant, etc.)
- TRANSFER_CRITICAL_FAILURE: Quand il y a une erreur critique (débit réussi mais crédit échoue)
```

### [TransactionManagerSpec.scala](src/test/scala/com/music/payment/TransactionManagerSpec.scala)
Tests modifiés pour vérifier que les logs sont correctement remplis:

```scala
// Vérification que les logs contiennent
- Les détails du transfert (comptes source/destination)
- Les montants transférés
- Les statuts (SUCCESS, FAILED)
- Les raisons d'échec
```

## 🧪 Tests

### Résultats de test
```
✅ All tests passed: 32 tests
- 6 tests LoggingActorSpec (LoggingActor spécifique)
- 4 tests TransactionManagerSpec (avec logs)
- 6 tests BankAccountSpec
- 8 tests LTLCheckerSpec
- 8 tests PetriNetSpec
```

### Format des logs
Les logs sont horodatés au format ISO:
```
[2026-03-26 10:24:26.284] [ACCOUNT_CREATION] ACC-001 | Montant: 1000.0 | Nouveau solde: 1000.0 | Statut: SUCCESS | Détails: Compte créé pour Alice

[2026-03-26 10:24:26.377] [TRANSFER_INITIATED] ACC-001 → ACC-002 | Montant: 300.0 | Statut: INITIATED | Détails: Transfert initié de ACC-001 vers ACC-002

[2026-03-26 10:24:26.439] [TRANSFER_DEBIT_SUCCESS] ACC-001 → ACC-002 | Montant: 300.0 | Statut: DEBIT_SUCCESSFUL | Détails: Montant débité du compte ACC-001

[2026-03-26 10:24:26.450] [TRANSFER_SUCCESS] N/A → ACC-002 | Montant: N/A | Statut: SUCCESS | Détails: Transfert complété avec succès, nouveau solde de ACC-002: 700.0
```

## 🎯 Cas d'utilisation testés

### 1. Transfert réussi
- ✅ Enregistre l'initiation du transfert
- ✅ Enregistre le débit du compte source
- ✅ Enregistre le succès du transfert
- ✅ Tous les logs sont accessibles via `GetTransactionLog`

### 2. Transfert avec fonds insuffisants
- ✅ Enregistre l'initiation
- ✅ Enregistre l'échec avec raison détaillée
- ✅ Les logs indiquent le solde et le montant demandé

### 3. Transfert depuis compte inexistant
- ✅ Enregistre l'erreur
- ✅ Les logs contiennent l'ID du compte inexistant

### 4. Création de compte
- ✅ Enregistre la création avec solde initial
- ✅ Horodatage précis

## 🚀 Exécution

Pour voir le LoggingActor en action:
```bash
cd h:\Desktop\S4\projet_scala\akka-petri-verification
.\sbt\sbt\bin\sbt.bat run
```

Pour exécuter les tests:
```bash
.\sbt\sbt\bin\sbt.bat test
```

## 📊 Architecture

```
BankSupervisor
    ├── creates → LoggingActor
    ├── creates → BankAccount (multiple)
    └── creates → TransactionManager (per transfer)
                      └── logs to → LoggingActor

Flux de logging pour un transfert:
1. TransactionManager demande le transfert
2. Enregistre "TRANSFER_INITIATED"
3. Débite le compte source
4. Enregistre "TRANSFER_DEBIT_SUCCESS"
5. Crédite le compte destination
6. Enregistre "TRANSFER_SUCCESS" ou "TRANSFER_FAILED"
```

## ✨ Avantages

- **Audit trail complet**: Toutes les transactions sont tracées
- **Horodatage précis**: Chaque événement a un timestamp
- **Traçabilité des erreurs**: Chaque échec est documenté avec sa raison
- **Historique centralisé**: Un seul endroit pour consulter tous les logs
- **Pas de données perdues**: Les logs persiste en mémoire pendant la durée de vie de l'acteur
- **Extensible**: Facile d'ajouter de nouveaux types d'événements à logger
