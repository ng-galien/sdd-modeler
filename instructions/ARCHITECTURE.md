# Architecture — sdd-modeler

Ce document décrit l’architecture cible de **sdd-modeler** avant l’implémentation.
L’objectif est de stabiliser les responsabilités des modules, packages et classes
principales autour de Java 25 + Gradle multi‑module.

## Vue d’ensemble

**sdd-modeler** est une **bibliothèque Java + CLI** permettant de :

1. Décrire un domaine selon le **State‑Driven Design (SDD)** dans un fichier
   YAML/JSON (entités, états, transitions, extensions, projections).
2. Charger et valider ce modèle dans une représentation interne Java.
3. Générer un **schéma SQL SDD** (DDL PostgreSQL dans un premier temps).
4. À terme, générer également du **code Java/Spring** à partir du même modèle.

Le projet est découpé en plusieurs modules Gradle :

- `state-modeler-core` — cœur métier (modèle SDD, parsing, validation,
  génération de plan SQL, rendu DDL PostgreSQL).
- `state-modeler-cli` — outil en ligne de commande au‑dessus du core.
- (optionnel, plus tard) `state-modeler-spring` — génération de code Java/Spring.

---

## Principes du State‑Driven Design (rappel)

Cette section résume les principes SDD que l’architecture de sdd-modeler doit
respecter. Elle ne remplace pas les articles du blog, mais sert de référence
rapide pour guider les décisions techniques.

1. **Séparer entités et états**
   - Une **entité** (ex. `order`) porte l'identité et des attributs stables
     (id, références, montants initiaux, dates de création…).
   - Les **états métier** (`PENDING`, `PAID`, `CANCELLED`, `REFUNDED`, …) ne
     sont pas des valeurs dans une colonne `status`, mais des objets / lignes
     distincts.
   - En SQL, cela se traduit par une table d'entité (`orders`) et des tables
     d'états (`order_pending`, `order_paid`, …).
   - **Séparation au niveau des schémas** : pour renforcer l'isolation de l'ADT
     des états, sdd-modeler génère les tables d'entités dans un schéma (ex. `public`)
     et les tables d'états, extensions, transitions OR et projections dans un
     schéma dédié (ex. `public_states`). Cette séparation rend la structure
     SDD plus explicite et facilite la gestion des droits d'accès et des migrations.

2. **Un état = un fait immuable**
   - Chaque état métier est représenté comme un **fait daté**, append‑only.
   - On ne « modifie » pas un état, on ajoute un nouvel état / fait.
   - Les attributs porteurs d’état (raison d’annulation, montant remboursé,
     méthode de paiement…) sont **non nuls** et enfermés dans la table
     d’état correspondante.

3. **Transitions explicites (graphe d’états)**
   - Le domaine est modélisé comme un **graphe d’états** :
     - nœuds = états ;
     - arêtes = transitions autorisées.
   - Chaque transition est explicite dans le modèle :
     - `from` : transition simple d’un état vers un autre ;
     - `from_any_of` : transition pouvant partir de plusieurs états (nécessite
       parfois une table de mapping, comme `canceled_source`).
   - En SQL, les transitions se matérialisent par des clés étrangères vers les
     états précédents.

4. **Optionnels non‑décisionnels (extensions)**
   - Les données **optionnelles, mutables et non décisionnelles** (notes,
     commentaires, métadonnées) ne doivent pas troubler la structure des
     états.
   - Elles sont placées dans des **tables d’extensions** (ex.
     `order_paid_extensions`, `order_cancelled_extensions`) qui n’affectent pas
     le graphe d’états.
   - En Java, ces extensions sont des objets séparés, associés à un état sans
     modifier sa nature.

5. **États dérivés plutôt que stockés**
   - On évite une colonne `status` censée représenter « l’état courant ».
   - L’état courant est **dérivé** à partir de l’historique des faits (états)
     via des projections (vues SQL ou requêtes) :
     - vue d’intervalles (`state_intervals`) ;
     - vue « current_state » filtrant les intervalles ouverts (`end_at IS NULL`).
   - sdd-modeler doit donc générer des **projections** à partir du modèle
     (et non se contenter de colonnes `status`).

6. **Invariants exprimés au niveau du modèle**
   - Les invariants métier doivent être exprimables dans le modèle / schéma :
     - clés étrangères ;
     - contraintes `CHECK` ;
     - éventuellement triggers / validations côté code.
   - L’objectif est de **réduire la quantité de logique implicite** dans le
     code applicatif en déplaçant une partie des règles dans la structure SDD.

7. **Modèle stable, projections multiples**
   - Le modèle SDD (entités + états + transitions) doit rester relativement
     stable dans le temps.
   - Les besoins de lecture (projections, rapports, APIs) peuvent évoluer et
     être servis par des projections/DTOs générés à partir de ce modèle.
   - sdd-modeler doit donc séparer clairement le **modèle SDD** de ses
     différentes projections (SQL, Spring, etc.).

---

## Séparation des schémas PostgreSQL

Pour renforcer la cohérence de l'ADT des états et matérialiser la séparation
conceptuelle entre entités et états, sdd-modeler génère un DDL PostgreSQL qui
utilise **deux schémas distincts** :

### Schéma entité (`schema`)

Contient les tables d'entités avec leurs attributs stables :

- Tables d'entités (ex. `public.orders`)
- Colonnes : identifiant, références, attributs immuables ou rarement modifiés

### Schéma d'états (`stateSchema`)

Contient tout ce qui relève de la gestion des états métier :

- **Tables d'états** (ex. `public_states.order_pending`, `public_states.order_paid`)
- **Tables d'extensions** (ex. `public_states.order_paid_extensions`)
- **Tables de mapping OR** pour les transitions `from_any_of` (ex. `public_states.cancelled_source`)
- **Vues de projections** (ex. `public_states.order_state_intervals`, `public_states.current_order_states`)

### Configuration

Dans le DSL YAML/JSON, le schéma d'états est configurable :

```yaml
database:
  dialect: postgres
  schema: public           # Schéma pour les entités
  state_schema: my_states  # Optionnel, défaut: <schema>_states
```

Si `state_schema` est omis, la valeur par défaut est `<schema>_states` :

- `schema: public` → `stateSchema: public_states`
- `schema: myapp` → `stateSchema: myapp_states`
- `schema: null` → `stateSchema: states`

### Avantages

- **Isolation conceptuelle** : séparation claire entre données stables et gestion d'états
- **Gestion des droits** : permissions différenciées (lecture seule sur entités, lecture/écriture sur états)
- **Migrations** : évolutions du modèle d'états sans toucher aux entités
- **Clarté** : structure du schéma reflète l'architecture SDD

---

## Modules Gradle

### 1. state-modeler-core

**Type** : bibliothèque Java (`java-library`).

**Responsabilités** :

- Modèle interne SDD (entités, états, transitions, extensions, projections).
- Parsing du DSL SDD (YAML/JSON) vers ce modèle interne.
- Validation du modèle (graphe d’états, contraintes, cohérence).
- Génération d’un **plan SQL abstrait** (indépendant du dialecte).
- Rendu DDL spécifique à PostgreSQL.

**Packages envisagés** :

- `io.statemodeler.core`
  - Types de base du modèle SDD :
    - `SddModel` — racine du modèle (ensemble d'entités).
    - `EntityDef` — entité « neutre » (table principale, attributs stables).
    - `StateDef` — état métier (table d'état, attributs propres, métadonnées).
    - `TransitionDef` — relation entre états (`from`, `fromAnyOf`).
    - `ExtensionDef` — tables d'extension non décisionnelles.
    - `ProjectionDef` — projections/vues dérivées (intervals, current_state, etc.).
    - `DatabaseConfig` — configuration base de données incluant `dialect`, `schema`
      (pour les entités) et `stateSchema` (pour les états, extensions, transitions
      OR et projections). Si `stateSchema` est null, utilise `<schema>_states` par défaut.

- `io.statemodeler.dsl`
  - Chargement et mapping YAML/JSON → `SddModel` :
    - `ModelLoader` — interface générique pour charger un modèle SDD
      (prend un `InputStream` ou un chemin de fichier, retourne `SddModel`).
    - `YamlModelLoader` — implémentation basée sur Jackson YAML.
    - `JsonModelLoader` — implémentation basée sur Jackson JSON.
  - Objets « DTO » si nécessaire pour mapper la structure YAML/JSON
    avant la transformation en `SddModel`.

- `io.statemodeler.validation`
  - Validation fonctionnelle du modèle :
    - `ModelValidator` — point d'entrée de la validation (prend un `SddModel`,
      renvoie une liste d'erreurs ou lève une exception type
      `ModelValidationException`).
    - **Validation des types d'attributs** : pour le dialecte PostgreSQL,
      vérifie que tous les types d'attributs (entité, états, extensions) sont
      des types PostgreSQL valides. Rejette les modèles avec types invalides
      (ex: "string" au lieu de "TEXT").
    - Règles possibles :
      - chaque entité possède au moins un état (`states` non vide) ;
      - au moins un état `initial: true` par entité ;
      - transitions cohérentes (pas de référence à un état inexistant) ;
      - structure cohérente pour `from_any_of` ;
      - projections correspondantes à des états existants.

- `io.statemodeler.sql`
  - Représentation abstraite d'un schéma SQL SDD :
    - `SqlPlan` — agrégat contenant les tables, vues, contraintes, et index.
    - `IndexDefinition` — représentation abstraite d'un index SQL (nom, table, colonnes, unique).
    - `TableDef`, `ColumnDef`, `ForeignKeyDef`, `CheckConstraintDef`, `ViewDef`.
  - Génération de plan à partir d’un `SddModel` :
    - `SqlPlanGenerator` — transforme `SddModel` → `SqlPlan`.

- `io.statemodeler.sql.postgres`
  - Rendu du plan SQL en DDL PostgreSQL :
    - `PostgresDdlGenerator` — génère le DDL PostgreSQL complet à partir d'un
      `SddModel`. Gère la séparation des schémas : tables d'entités dans le
      schéma entité (`schema`), tables d'états/extensions/OR transitions/projections
      dans le schéma d'états (`stateSchema` ou `<schema>_states` par défaut).
    - Génère automatiquement `CREATE SCHEMA IF NOT EXISTS` pour les schémas nécessaires.
    - **Génération automatique d'index** : crée un index pour chaque colonne de 
      foreign key (nommage : `idx_<table>_<column>`). Améliore les performances 
      des JOINs sur les tables d'états, extensions et transitions OR.
    - `PostgresTypeValidator` — valide que les types d'attributs sont des types
      PostgreSQL valides (TEXT, INTEGER, BIGINT, NUMERIC(p,s), TIMESTAMPTZ, JSONB, etc.).
      Supporte les types paramétrés et arrays. Validation case-insensitive.
  - Possibles helpers : formatage, indentation, gestion de types PostgreSQL.

**Tests (core)** :

- Tests unitaires sur le parsing YAML/JSON.
- Tests de validation (cas valides / cas invalides de graphe d’états).
- Tests de génération SQL : à partir d’un YAML connu, vérifier que le DDL
  généré correspond aux attentes (approche snapshot ou comparaison de fichiers
  `.sql` attendus).

---

### 2. state-modeler-cli

**Type** : application Java (`application`).

**Responsabilités** :

- Exposer une CLI ergonomique au‑dessus de `state-modeler-core`.
- Orchestrer : chargement du modèle, validation, génération SQL, gestion des
  options et des codes de retour.

**Dépendance** : `implementation(project(":state-modeler-core"))`.

**Packages envisagés** :

- `io.statemodeler.cli`
  - `Main` — point d’entrée de l’application (déclare les sous‑commandes).
  - `ValidateCommand` — commande `validate` :
    - arguments : chemin du fichier modèle, format (optionnel), etc. ;
    - sortie : liste d’erreurs ou message « modèle valide ».
  - `SqlCommand` — commande `sql` :
    - arguments : chemin du fichier modèle, dialecte (pour l’instant `postgres`),
      sortie vers stdout ou fichier.
  - Plus tard : `SpringCommand` pour générer du code Java/Spring.

On pourra s’appuyer sur un framework CLI (ex. Picocli) pour gérer
les options, l’aide, les codes de retour.

**Tests (CLI)** :

- Tests de commande « bout‑en‑bout » avec exécution de la CLI en mode test
  (framework CLI + tests JUnit) pour vérifier l’ergonomie et les messages.

---

### 3. state-modeler-spring (optionnel, plus tard)

**Type** : bibliothèque Java.

**Responsabilités** :

- Générer du code Java/Spring (entités JPA, services de transitions, etc.) à
  partir du même `SddModel` utilisé pour la génération SQL.

**Packages envisagés** :

- `io.statemodeler.spring`
  - `SpringModelGenerator` — point d’entrée pour la génération Java/Spring.
  - Génération d’entités, de services et éventuellement de contrôleurs.

Ce module pourra rester expérimental tant que le cœur SQL n’est pas stabilisé.

---

## Flux de données / pipeline

1. **Chargement**
   - La CLI ou une application consommatrice appelle `ModelLoader` avec un
     fichier YAML/JSON.
   - `YamlModelLoader` / `JsonModelLoader` construisent un `SddModel`.

2. **Validation**
   - `ModelValidator` inspecte le `SddModel`.
   - En cas d’erreurs, la CLI les affiche avec un code de retour non nul.

3. **Génération SQL**
   - `SqlPlanGenerator` transforme le `SddModel` validé en `SqlPlan`.
   - `PostgresDdlRenderer` rend ce plan en texte DDL PostgreSQL.
   - La CLI écrit le résultat sur stdout ou dans un fichier.

4. **(Plus tard) Génération Java/Spring**
   - `SpringModelGenerator` consommera le même `SddModel` pour produire
     des classes Java (entités, services, etc.).

---

## Non‑objectifs initiaux

- Support immédiat de tous les dialectes SQL (focus initial : PostgreSQL).
- Génération d’UI ou d’applications complètes prêtes à l’emploi.
- Gestion de workflow runtime (orchestrateur d’états) — l’objectif est la
  **modélisation et la génération de schémas/code**, pas l’exécution.

---

## Relation avec le projet de pilotage

Le dépôt `todo/state-modeler` sert de **projet de pilotage** :

- il contient la définition du DSL SDD et des exemples de modèles (commande
  e‑commerce, etc.) ;
- il suit l’avancement (itérations, TODO) ;
- il documente les décisions d’architecture et les futurs axes (génération
  Spring, support d’autres dialectes, etc.).

Le dépôt `sdd-modeler` (ce répertoire) est l’implémentation Java concrète de
cette vision.
