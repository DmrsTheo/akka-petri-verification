# Modèle Formel : Traduction vers les Réseaux de Pétri

## 1. Objectif de la Traduction
L'architecture distribuée en Akka présente un espace d'états asynchrone complexe. Pour la vérifier formellement, nous avons abstrait et traduit le comportement du `TransactionManager` sous la forme d'un Réseau de Pétri (`PaymentPetriNet.scala`).

## 2. Le Modèle de Transfert (PaymentPetriNet)

### 2.1. Les Places (États)
- `AccountA_Funds` et `AccountB_Funds` : Représentent la quantité de fonds.
- `TransferReady` : Indique qu'une transaction peut être initiée.
- `TransferInProgress` : Phase 1 amorcée (requête de retrait en cours).
- `DebitDone` : La monnaie est "en transit" (débitée de A, en attente du crédit sur B).
- `TransferDone` / `TransferFailed` : États terminaux.

### 2.2. Les Transitions (Actions)
- `InitiateTransfer` : Simule la réception du `TransferRequest`.
- `DebitSource` : Simule l'acceptation d'un retrait.
- `CreditDestination` : Simule l'acceptation d'un dépôt.
- `RejectInsufficient` : Simule le refus d'un transfert par l'acteur.

### 2.3. Précision de Modélisation (Arcs Spéciaux)
Pour capturer la sémantique de garde ("garde-fou") de Scala (`if amount > balance throw Error`), nous avons utilisé :
- **Des Arcs Pondérés** : `weight = transferAmount` pour l'arc classique.
- **Un Arc Inhibiteur** : L'arc de `AccountA_Funds` vers `RejectInsufficient`. Cet arc a été spécifiquement codé dans notre moteur (`PetriNet.scala`) pour ne s'activer **que si les fonds sont strictement inférieurs au montant demandé**. C'est cette traduction mathématique précise qui permet au Model Checker de valider correctement les cas de rejet d'acteurs Akka.

## 3. Exploration BFS
Le module `StateSpaceExplorer` calcule exhaustivement tous les chemins (et donc tous les entrelacements possibles de messages) depuis l'état initial, produisant le graphe d'accessibilité utilisé pour nos preuves.
