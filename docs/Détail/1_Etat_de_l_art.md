# État de l'Art : Modélisation et Vérification Formelle

## 1. Introduction aux Systèmes Distribués Critiques
Les systèmes distribués critiques (comme notre **Système de Virement Bancaire Distribué** basé sur le modèle d'acteurs Akka) reposent sur la coordination d'entités indépendantes communiquant via un réseau asynchrone. La moindre défaillance, interblocage (deadlock) ou violation d'invariant peut entraîner des pertes financières majeures. Par conséquent, de simples tests unitaires (comme ceux de `ScalaTest`) peuvent parfois manquer des scénarios asynchrones très rares ("corner cases"). Une garantie mathématique est requise : c'est le rôle de la **vérification formelle**.

## 2. La Vérification Formelle
La vérification formelle consiste à prouver mathématiquement qu'un système satisfait un ensemble de propriétés spécifiées. Contrairement à la simulation classique qui explore certains chemins d'exécution aléatoires, la vérification formelle que nous implémentons via notre `StateSpaceExplorer.scala` explore de manière exhaustive l'intégralité du graphe d'accessibilité (tous les entrelacements possibles des messages Akka).

## 3. Les Réseaux de Pétri
Les Réseaux de Pétri (RdP) sont un outil mathématique et graphique idéal pour modéliser des systèmes concurrents.
Un réseau de Pétri est composé de :
- **Places (cercles)** : représentent l'état local du système (ex: la place `AccountA_Funds` dans notre modèle abstrait `PaymentPetriNet.scala` représente concrètement la variable `balance` au sein de l'acteur `BankAccount`).
- **Transitions (rectangles)** : représentent les événements ou actions (ex: la transition `InitiateTransfer` modélise la réception effective du message Akka `TransferRequest` par notre `TransactionManager`).
- **Arcs orientés** : définissent le flux d'exécution et les conditions de tir (les fameux `weight` dans notre code).
- **Jetons (points)** : circulent dans les places. Dans notre projet, le nombre de jetons dans la place `AccountA_Funds` correspond exactement à la valeur numérique de l'argent disponible sur le compte.

**Avantages pour l'analyse de notre système :**
En générant le graphe d'accessibilité de notre `PaymentPetriNet`, nous pouvons vérifier mathématiquement :
- **L'absence de deadlock (Vivacité)** : garantit que notre `TransactionManager` ne restera jamais bloqué indéfiniment (livelock) en attendant une réponse d'un acteur `BankAccount` qui aurait crashé.
- **Les Invariants (Sûreté)** : garantit la conservation stricte de la monnaie. La somme des jetons dans `AccountA_Funds`, `AccountB_Funds` et la monnaie en cours de transit (`DebitDone`) reste mathématiquement constante.

## 4. Logique Temporelle Linéaire (LTL)
La logique LTL est utilisée pour formaliser les exigences du système sur son évolution temporelle infinie. L'implémentation de notre `LTLChecker.scala` s'appuie sur ces opérateurs :
- **□ (Toujours / Globally)** : Sûreté. Par exemple, l'expression formelle *□(SoldeA ≥ 0)* prouve mathématiquement que l'invariant métier `require(balance >= 0)` que nous avons défini dans le constructeur de `Account.scala` ne sera jamais violé par la concurrence Akka.
- **◇ (Éventuellement / Finally)** : Vivacité. Par exemple, l'expression *◇(TransferDone)* prouve que notre protocole de transaction en 2 phases finira inévitablement par produire un résultat au client (soit un `TransferSuccess`, soit un `TransferFailure` relayé au `LoggingActor`), écartant toute famine (starvation).

La traduction d'un code distribué Akka vers un Réseau de Pétri pour générer un espace d'états, couplée à un moteur LTL pour l'interroger, forme l'essence même de notre démarche d'ingénierie fiable.
