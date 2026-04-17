# Schéma Visuel du Réseau de Pétri - Système de Paiement Distribué

Ce document modélise visuellement le réseau de Pétri de notre système de paiement distribué (implémenté avec Akka). Il est généré avec Mermaid.js et peut être rendu directement sur GitHub, Notion ou d'autres éditeurs Markdown. 

Cette version représente la logique du projet avec des paramètres généralisés : le nombre de jetons sur les comptes ("Fonds: X" et "Fonds: Y") et le montant de la transaction de manière dynamique.

Les places (en rouge/rond) représentent les états ou ressources.
Les transitions (en bleu/rectangle) représentent les actions franchissables.

```mermaid
flowchart TD
    classDef place fill:#f9d0c4,stroke:#e95f5c,stroke-width:2px,color:#000
    classDef transition fill:#adcce9,stroke:#4a86e8,stroke-width:2px,color:#000
    
    P1((CompteA<br>Fonds: X)):::place
    P2((CompteB<br>Fonds: Y)):::place
    P3((Transfert<br>Prêt: 1 jeton)):::place
    P4((Transfert<br>En Cours: 0)):::place
    P5((Débit<br>Effectué: 0)):::place
    P6((Transfert<br>Terminé: 0)):::place
    P7((Transfert<br>Échoué: 0)):::place
    
    T1[Initier<br>Transfert]:::transition
    T2[Débiter<br>Source]:::transition
    T3[Créditer<br>Destination]:::transition
    T4[Rejeter<br>Fonds Insuffisants]:::transition
    T5[Réinit<br>Succès]:::transition
    T6[Réinit<br>Échec]:::transition
    
    P3 --> T1
    T1 --> P4
    
    P1 -- "Montant du transfert" --> T2
    P4 --> T2
    T2 --> P5
    
    P5 --> T3
    T3 --> P6
    T3 -- "Montant du transfert" --> P2
    
    P4 --> T4
    P1 -. "Fonds < Montant du transfert\n(Arc inhibiteur)" .-> T4
    T4 --> P7
    
    P6 --> T5
    T5 --> P3
    
    P7 --> T6
    T6 --> P3
```

## Description des Règles
- **InitierTransfert** consomme le jeton "Transfert Prêt" et place un jeton dans "Transfert En Cours".
- **DébiterSource** nécessite d'avoir le "Montant du transfert" en jetons dans le "CompteA" et 1 jeton dans "Transfert En Cours" pour pouvoir exécuter le débit avec succès.
- **RejeterFondsInsuffisants** possède un *arc inhibiteur* vers le CompteA : cette transition est activée si et seulement s'il y a un transfert en cours MAIS un solde strictement inférieur au montant du transfert. Elle mène vers "Transfert Échoué".
- **CréditerDestination** transfère les fonds vers le "CompteB" (+ "Montant du transfert" en jetons) une fois le débit effectué et marque le transfert comme Terminé.
- Les transitions de **Réinit** (succès ou échec) permettent de rendre à nouveau le système disponible pour une nouvelle transaction (vivacité et absence de deadlock terminal).
