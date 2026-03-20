# Réseaux de Pétri — Document Explicatif Complet

## Table des matières

1. [Introduction aux réseaux de Pétri](#1-introduction)
2. [Éléments constitutifs](#2-éléments-constitutifs)
3. [Dynamique et règles de franchissement](#3-dynamique)
4. [Exploration de l'espace d'états](#4-espace-détats)
5. [Propriétés structurelles](#5-propriétés-structurelles)
6. [Invariants métier](#6-invariants-métier)
7. [Logique temporelle linéaire (LTL)](#7-logique-ltl)
8. [Application au système de paiement](#8-application)
9. [Correspondance Akka ↔ Réseau de Pétri](#9-correspondance-akka--petri)
10. [Références bibliographiques](#10-références)

---

## 1. Introduction

### Qu'est-ce qu'un réseau de Pétri ?

Un **réseau de Pétri** (Petri Net) est un modèle mathématique introduit par **Carl Adam Petri** en 1962 dans sa thèse de doctorat. C'est un outil formel de modélisation permettant de représenter et d'analyser des systèmes :
- **Concurrents** (plusieurs processus s'exécutant en parallèle)
- **Asynchrones** (pas d'horloge globale)
- **Distribués** (composants répartis sur plusieurs nœuds)
- **Non-déterministes** (plusieurs évolutions possibles)

### Pourquoi utiliser les réseaux de Pétri ?

| Avantage | Description |
|----------|-------------|
| **Représentation graphique** | Modèle visuel intuitif (graphe biparti) |
| **Fondement mathématique** | Analyse formelle rigoureuse des propriétés |
| **Expressivité** | Capture la concurrence, les conflits, la synchronisation |
| **Vérification automatique** | Détection de deadlocks, violations d'invariants |
| **Indépendance d'implémentation** | Modèle abstrait applicable à tout système |

### Contexte : vérification formelle

La **vérification formelle** consiste à prouver mathématiquement qu'un système satisfait un ensemble de propriétés. Contrairement au test (qui ne couvre que certains cas), la vérification formelle examine **tous les comportements possibles** du système.

```
Test classique :     "Le système fonctionne pour ces 100 scénarios"
Vérification formelle : "Le système fonctionne pour TOUS les scénarios possibles"
```

---

## 2. Éléments constitutifs

Un réseau de Pétri est un **graphe biparti** composé de deux types de nœuds reliés par des arcs.

### 2.1 Places (○)

Les **places** représentent les **conditions**, **états** ou **ressources** du système. Graphiquement, ce sont des cercles.

Exemples dans notre système de paiement :
- `CompteA_Fonds` — fonds disponibles dans le compte A
- `TransfertPrêt` — un transfert est prêt à être initié
- `TransfertEnCours` — un transfert est en cours d'exécution

### 2.2 Transitions (□)

Les **transitions** représentent les **actions**, **événements** ou **transformations**. Graphiquement, ce sont des rectangles ou barres.

Exemples :
- `InitierTransfert` — début d'un transfert
- `DébiterSource` — retrait sur le compte source
- `CréditerDestination` — dépôt sur le compte destination

### 2.3 Arcs (→)

Les **arcs** relient places et transitions (jamais deux places ou deux transitions entre elles). Ils sont **orientés** et portent un **poids** (par défaut 1).

| Type d'arc | Notation | Rôle |
|------------|----------|------|
| **Arc d'entrée** | Place → Transition | Consomme des jetons de la place |
| **Arc de sortie** | Transition → Place | Produit des jetons dans la place |
| **Arc inhibiteur** | Place ○→ Transition | Active la transition si la place est **vide** |

### 2.4 Jetons (●) et Marquage

Les **jetons** sont des entités placées dans les places, représentant la **quantité de ressource** disponible. La distribution des jetons dans toutes les places à un instant donné s'appelle le **marquage** (*marking*).

```
Marquage initial M₀ = [CompteA_Fonds=5, CompteB_Fonds=3, TransfertPrêt=1]
```

### 2.5 Définition formelle

Un réseau de Pétri est un quintuplet **N = (P, T, I, O, M₀)** où :
- **P** = {p₁, p₂, ..., pₙ} : ensemble fini de places
- **T** = {t₁, t₂, ..., tₘ} : ensemble fini de transitions (P ∩ T = ∅)
- **I** : T → P^∞ : fonction d'entrée (arcs place → transition)
- **O** : T → P^∞ : fonction de sortie (arcs transition → place)
- **M₀** : P → ℕ : marquage initial

Dans notre code Scala, cela correspond à :
```scala
case class PetriNet(
  places: Set[Place],          // P
  transitions: Set[Transition], // T
  inputArcs: Set[InputArc],    // I (avec poids)
  outputArcs: Set[OutputArc]   // O (avec poids)
)
case class Marking(tokens: Map[Place, Int])  // M
```

---

## 3. Dynamique et règles de franchissement

### 3.1 Activation d'une transition

Une transition **t** est **activée** (*enabled*) pour un marquage **M** si et seulement si chaque place d'entrée contient au moins autant de jetons que le poids de l'arc correspondant :

```
∀ p ∈ •t : M(p) ≥ W(p, t)
```

Où :
- **•t** = ensemble des places d'entrée de t (pré-conditions)
- **W(p, t)** = poids de l'arc de p vers t

Pour les **arcs inhibiteurs**, la condition est inversée : la place doit être **vide** (0 jetons).

### 3.2 Franchissement (firing)

Quand une transition activée **t** est franchie, le nouveau marquage **M'** est calculé :

```
∀ p ∈ P : M'(p) = M(p) - W(p, t) + W(t, p)
```

C'est-à-dire :
1. **Retirer** W(p, t) jetons de chaque place d'entrée
2. **Ajouter** W(t, p) jetons dans chaque place de sortie

### 3.3 Exemple concret

```
État initial :          CompteA = 5 jetons, TransfertPrêt = 1 jeton
Transition :            DébiterSource (poids d'entrée : CompteA=2, TransfertEnCours=1)
                        
Vérification :          CompteA (5) ≥ 2 ✓  et  TransfertEnCours a un jeton ? 
                        → Il faut d'abord franchir InitierTransfert

Après InitierTransfert : TransfertPrêt = 0, TransfertEnCours = 1
Après DébiterSource :    CompteA = 5-2 = 3, TransfertEnCours = 0, DébitEffectué = 1
Après CréditerDest. :    CompteB = 3+2 = 5, DébitEffectué = 0, TransfertTerminé = 1
```

### 3.4 Séquence de franchissement

Une **séquence de franchissement** est une suite ordonnée de transitions :

```
σ = t₁ · t₂ · t₃ = InitierTransfert · DébiterSource · CréditerDestination
M₀ →[t₁] M₁ →[t₂] M₂ →[t₃] M₃
```

---

## 4. Exploration de l'espace d'états

### 4.1 Graphe d'accessibilité

Le **graphe d'accessibilité** (ou graphe de couverture) contient :
- **Nœuds** : tous les marquages accessibles depuis M₀
- **Arcs** : transitions franchies entre marquages

```
R(M₀) = { M | ∃ séquence σ telle que M₀ →[σ] M }
```

### 4.2 Algorithme d'exploration (BFS)

Notre implémentation utilise un parcours en largeur (BFS) :

```
Algorithme ExplorerEspaceÉtats(Réseau, M₀):
    visités ← {M₀}
    file ← [M₀]
    deadlocks ← {}
    
    TANT QUE file non vide ET |visités| < MAX_ÉTATS:
        M_courant ← défiler(file)
        transitions_activées ← {t ∈ T | t est activée dans M_courant}
        
        SI transitions_activées est vide:
            ajouter M_courant à deadlocks    // État de deadlock !
        SINON:
            POUR CHAQUE t ∈ transitions_activées:
                M_suivant ← franchir(t, M_courant)
                SI M_suivant ∉ visités:
                    ajouter M_suivant à visités
                    enfiler M_suivant dans file
    
    RETOURNER (visités, deadlocks)
```

### 4.3 Problème de l'explosion combinatoire

⚠️ L'espace d'états peut croître **exponentiellement** avec le nombre de places et de jetons. C'est pourquoi notre implémentation impose une limite (`maxStates = 10000`).

---

## 5. Propriétés structurelles

### 5.1 Vivacité (Liveness)

Un réseau est **vivant** si, depuis tout marquage accessible, chaque transition peut éventuellement être franchie.

- **Vivacité L0** : une transition peut être franchie au moins une fois
- **Vivacité L1** : une transition peut être franchie depuis tout marquage accessible
- **Vivacité L4 (vivant)** : depuis tout marquage accessible, toute transition est franchissable via une certaine séquence

### 5.2 Bornitude (Boundedness)

Un réseau est **k-borné** si aucune place ne contient jamais plus de k jetons dans tout marquage accessible :

```
∀ M ∈ R(M₀), ∀ p ∈ P : M(p) ≤ k
```

Un réseau **1-borné** est dit **sauf** (*safe*) — chaque place contient au plus 1 jeton.

### 5.3 Absence de deadlock (Deadlock-freedom)

Un **deadlock** (ou blocage) est un marquage dans lequel **aucune transition n'est activée** :

```
Deadlock(M) ⟺ ∀ t ∈ T : ¬enabled(t, M)
```

Le réseau est **sans deadlock** si aucun marquage accessible n'est un deadlock.

### 5.4 Réversibilité

Un réseau est **réversible** si le marquage initial M₀ est accessible depuis tout marquage accessible (le système peut toujours revenir à son état initial).

### 5.5 Conservation

Un réseau est **conservatif** si la somme pondérée des jetons reste constante :

```
∀ M ∈ R(M₀) : Σ γᵢ · M(pᵢ) = Σ γᵢ · M₀(pᵢ)
```

avec γᵢ des poids positifs. Si tous les γᵢ = 1, c'est une **conservation stricte**.

---

## 6. Invariants métier

### 6.1 Qu'est-ce qu'un invariant ?

Un **invariant** est une propriété qui doit être vraie dans **tous** les états accessibles du système. C'est une contrainte globale.

### 6.2 Types d'invariants dans le système de paiement

| Invariant | Formulation | Signification |
|-----------|-------------|---------------|
| **Non-négativité** | ∀ M ∈ R(M₀) : M(CompteA) ≥ 0 | Un compte ne peut jamais être à découvert |
| **Conservation des fonds** | ∀ M : M(A) + M(B) = M₀(A) + M₀(B) | L'argent total dans le système est constant |
| **Exclusion mutuelle** | ∀ M : M(EnCours) + M(DébitOK) ≤ 1 | Au plus un transfert à la fois |

### 6.3 Vérification

```
Algorithme VérifierInvariant(EspaceÉtats, Prédicat):
    POUR CHAQUE M ∈ EspaceÉtats.marquages_accessibles:
        SI ¬Prédicat(M):
            RETOURNER VIOLÉ(M)    // Contre-exemple trouvé
    RETOURNER SATISFAIT
```

---

## 7. Logique Temporelle Linéaire (LTL)

### 7.1 Introduction à la LTL

La **LTL** (Linear Temporal Logic) est une logique formelle utilisée en **model checking** pour exprimer des propriétés sur l'évolution d'un système dans le temps. Elle étend la logique propositionnelle avec des **opérateurs temporels**.

### 7.2 Syntaxe

```
φ ::= p           (proposition atomique)
    | ¬φ          (négation)
    | φ₁ ∧ φ₂     (conjonction)
    | φ₁ ∨ φ₂     (disjonction)
    | φ₁ → φ₂     (implication)
    | □φ           (Always / Globally — G)
    | ◇φ           (Eventually / Finally — F)
    | ○φ           (Next — X)
    | φ₁ U φ₂     (Until — U)
```

### 7.3 Opérateurs temporels

| Opérateur | Symbole | Signification | Exemple |
|-----------|---------|---------------|---------|
| **Always** | □ (G) | φ est vrai dans **tous** les états futurs | □(solde ≥ 0) |
| **Eventually** | ◇ (F) | φ sera vrai dans **au moins un** état futur | ◇(transfert_terminé) |
| **Next** | ○ (X) | φ est vrai dans l'état **suivant** | ○(en_cours) |
| **Until** | U | φ₁ reste vrai **jusqu'à ce que** φ₂ devienne vrai | en_cours U terminé |

### 7.4 Propriétés de sûreté et de vivacité

**Propriétés de sûreté** (*safety*) — "Quelque chose de mauvais n'arrive **jamais**" :
```
□(solde ≥ 0)                        "Le solde est toujours non-négatif"
□(¬deadlock)                         "Le système ne se bloque jamais"
□(transfert_en_cours → exclusion)    "Pas deux transferts simultanés"
```

**Propriétés de vivacité** (*liveness*) — "Quelque chose de bon finit **toujours** par arriver" :
```
◇(transfert_terminé)                 "Le transfert finira par aboutir"
□(demande → ◇réponse)               "Chaque demande reçoit une réponse"
□(débit → ◇crédit)                  "Chaque débit est suivi d'un crédit"
```

### 7.5 Model checking LTL

Le **model checking** consiste à vérifier automatiquement si un modèle (le graphe d'accessibilité) satisfait une formule LTL.

```
Algorithme CheckLTL(formule, espace_états):
    SELON formule:
        CAS □(φ):    // Always
            Chercher un état M où φ est FAUX
            SI trouvé: RETOURNER NON_SATISFAIT(M)  // Contre-exemple
            SINON: RETOURNER SATISFAIT
            
        CAS ◇(φ):    // Eventually
            Chercher un état M où φ est VRAI
            SI trouvé: RETOURNER SATISFAIT
            SINON: RETOURNER NON_SATISFAIT
```

### 7.6 Correspondance dans notre code

```scala
// Formules LTL dans LTLChecker.scala
sealed trait LTLFormula
case class Atom(name: String, predicate: Marking => Boolean)  // Proposition atomique
case class Always(formula: LTLFormula)                         // □ — Globally
case class Eventually(formula: LTLFormula)                     // ◇ — Finally
case class Not(formula: LTLFormula)                            // ¬
case class And(left: LTLFormula, right: LTLFormula)            // ∧
case class Or(left: LTLFormula, right: LTLFormula)             // ∨

// Exemple de vérification :
val safety = Always(Atom("solde ≥ 0", m => m.getTokens(compteA) >= 0))
val liveness = Eventually(Atom("transfert OK", m => m.getTokens(transfertDone) > 0))
```

---

## 8. Application au système de paiement

### 8.1 Modèle de réseau de Pétri

Notre système de paiement est modélisé par le réseau de Pétri suivant :

```
         ┌─────────────────┐
         │  TransfertPrêt  │ ●
         └────────┬────────┘
                  │
           ┌──────┴──────┐
           │ InitierTransf│
           └──────┬──────┘
                  │
         ┌────────┴────────┐
         │TransfertEnCours │
         └───┬─────────┬───┘
             │         │
    ┌────────┴───┐ ┌───┴────────┐
    │DébiterSrc  │ │RejeterInsuf│
    └────────┬───┘ └───┬────────┘
             │         │
    ┌────────┴───┐ ┌───┴────────┐
    │DébitEffectué│ │TransfÉchoué│
    └────────┬───┘ └───┬────────┘
             │         │
    ┌────────┴────┐ ┌──┴──────────┐
    │CréditerDest │ │RéinitÉchec  │
    └────────┬────┘ └──┬──────────┘
             │         │
    ┌────────┴────┐    │
    │TransfTerminé│    │
    └────────┬────┘    │
             │         │
    ┌────────┴─────┐   │
    │RéinitSuccès  │   │
    └────────┬─────┘   │
             │         │
             └────┬────┘
                  │
         (retour à TransfertPrêt)
```

### 8.2 Places et leur signification

| Place | ID | Jetons initiaux | Représente |
|-------|-----|-----------------|------------|
| CompteA_Fonds | p1 | 5 | Fonds disponibles dans le compte A |
| CompteB_Fonds | p2 | 3 | Fonds disponibles dans le compte B |
| TransfertPrêt | p3 | 1 | Le système est prêt pour un nouveau transfert |
| TransfertEnCours | p4 | 0 | Un transfert est en cours de traitement |
| DébitEffectué | p5 | 0 | Le débit a été réalisé avec succès |
| TransfertTerminé | p6 | 0 | Le transfert est terminé (succès) |
| TransfertÉchoué | p7 | 0 | Le transfert a échoué |

### 8.3 Transitions et leur signification

| Transition | ID | Pré-conditions | Post-conditions |
|------------|-----|----------------|-----------------|
| InitierTransfert | t1 | TransfertPrêt ≥ 1 | TransfertEnCours + 1 |
| DébiterSource | t2 | CompteA ≥ 2, EnCours ≥ 1 | CompteA − 2, DébitOK + 1 |
| CréditerDest. | t3 | DébitOK ≥ 1 | CompteB + 2, TransfTerminé + 1 |
| RejeterInsuff. | t4 | EnCours ≥ 1, CompteA = 0 (inhib.) | TransfÉchoué + 1 |
| RéinitSuccès | t5 | TransfTerminé ≥ 1 | TransfertPrêt + 1 |
| RéinitÉchec | t6 | TransfÉchoué ≥ 1 | TransfertPrêt + 1 |

### 8.4 Propriétés vérifiées

✅ **Sûreté** : `□(CompteA_Fonds ≥ 0)` — Le solde ne devient jamais négatif
✅ **Sûreté** : `□(CompteB_Fonds ≥ 0)` — Idem pour le compte B
✅ **Vivacité** : `◇(TransfertTerminé > 0)` — Le transfert peut aboutir
✅ **Non-négativité** : Toutes les places restent ≥ 0
✅ **Exclusion mutuelle** : Au plus un transfert en cours

---

## 9. Correspondance Akka ↔ Réseau de Pétri

### 9.1 Tableau de correspondance

| Concept Akka | Concept Petri Net |
|-------------|-------------------|
| **Acteur** (BankAccount) | Ensemble de **places** (état du compte) |
| **Message** (DepositCmd, WithdrawCmd) | **Transition** (action déclenchée) |
| **État interne** (balance) | **Marquage** (nombre de jetons) |
| **Envoi de message** | **Arc de sortie** (production de jetons) |
| **Réception de message** | **Arc d'entrée** (consommation de jetons) |
| **Supervision** (restart) | **Réversibilité** du réseau |
| **Ask pattern** (requête/réponse) | **Séquence** de transitions |

### 9.2 Validation croisée

La simulation Akka et l'analyse Petri Net doivent converger :

| Scénario | Résultat Akka | Résultat Petri Net |
|----------|---------------|---------------------|
| Transfert A→B (fonds suffisants) | ✅ Succès | ✅ État TransfertTerminé accessible |
| Transfert A→B (fonds insuffisants) | ❌ Refusé | ✅ État TransfertÉchoué accessible |
| Solde négatif | ❌ Impossible | ✅ □(solde ≥ 0) satisfait |
| Deadlock | ❌ Ne se produit pas | ✅ Pas de deadlock dans graphe* |

*Note : Certains états terminaux (transfert complété) sont des deadlocks au sens Petri. Les transitions de réinitialisation (t5, t6) permettent de les éviter.

---

## 10. Références bibliographiques

1. **Petri, C.A.** (1962). *Kommunikation mit Automaten*. Thèse de doctorat, Technische Universität Darmstadt. — L'article fondateur des réseaux de Pétri.

2. **Murata, T.** (1989). "Petri Nets: Properties, Analysis and Applications". *Proceedings of the IEEE*, 77(4), pp. 541-580. — Survol complet et très cité des propriétés des réseaux de Pétri.

3. **Pnueli, A.** (1977). "The temporal logic of programs". *18th Annual Symposium on Foundations of Computer Science*, IEEE. — Introduction de la logique temporelle pour la vérification de programmes.

4. **Clarke, E.M., Grumberg, O., Peled, D.A.** (2018). *Model Checking*. MIT Press, 2ème édition. — Référence sur le model checking, incluant LTL.

5. **Baier, C., Katoen, J.-P.** (2008). *Principles of Model Checking*. MIT Press. — Manuel complet sur la vérification formelle et le model checking.

6. **Hewitt, C., Bishop, P., Steiger, R.** (1973). "A Universal Modular ACTOR Formalism for Artificial Intelligence". *IJCAI*. — Article fondateur du modèle d'acteurs.

7. **Documentation Akka** : https://doc.akka.io/docs/akka/current/ — Documentation officielle du framework Akka.

8. **Reisig, W.** (2013). *Understanding Petri Nets: Modeling Techniques, Analysis Methods, Case Studies*. Springer. — Introduction moderne et accessible aux réseaux de Pétri.
