# sdd-modeler

State‑Modeler est une bibliothèque Java + CLI pour implémenter concrètement le State‑Driven Design (SDD).

L'objectif principal est de **générer du DDL SQL** (d'abord PostgreSQL) à partir d'un schéma déclaratif (JSON ou YAML) décrivant un modèle SDD : entités, états, transitions, extensions, projections. À terme, le même modèle servira également à générer du code Java (par exemple pour Spring).

## Vision

- Décrire un domaine orienté états (SDD) dans un fichier YAML/JSON simple à lire et versionner.
- Charger et valider ce modèle via une bibliothèque Java.
- Générer à partir de ce modèle :
  - un schéma SQL SDD (tables d'entités, d'états, d'extensions, vues d'état courant, etc.) ;
  - plus tard, du code Java/Spring (entités, services, squelettes d'API).

Un premier exemple concret est disponible dans le dossier `instructions/examples/` :

- `instructions/examples/orders-sdd-model.yaml` — modèle YAML d'une commande e‑commerce avec états `pending`, `paid`, `cancelled`, `refunded`.
- `instructions/examples/orders-sdd-ddl.sql` — DDL PostgreSQL correspondant (tables d'états, tables d'extensions et vues d'intervalles / état courant).

## Stack technique

- Langage : Java 25
- Build : Gradle (multi‑module)
- Modules prévus :
  - `state-modeler-core` : modèle interne SDD, parseur YAML/JSON, validation, génération de plan SQL, rendu DDL PostgreSQL.
  - `state-modeler-cli` : application en ligne de commande s'appuyant sur le core.
  - (éventuel) `state-modeler-spring` : génération de code Java/Spring à partir du même modèle.

Pour une vue détaillée des modules, packages et principes SDD, voir
`instructions/ARCHITECTURE.md`.

## Fonctionnalités prévues (MVP)

1. **Librairie core**
   - Charger un modèle SDD depuis un fichier YAML/JSON.
   - Valider le modèle (cohérence du graphe d'états, état initial, transitions, projections).
   - Générer un plan abstrait de schéma SQL (tables, colonnes, contraintes, vues).
   - Rendre ce plan en DDL PostgreSQL.

2. **CLI**
   - `state-modeler validate model.yaml` — valider un modèle SDD et afficher les erreurs éventuelles.
   - `state-modeler sql model.yaml --dialect postgres` — générer le DDL SQL correspondant (vers stdout ou un fichier).

## DSL SDD (esquisse)

Le DSL (YAML/JSON) décrit :

- des `entities` (ex. `order`) avec leurs attributs stables ;
- des `states` (ex. `pending`, `paid`, `cancelled`, `refunded`) avec :
  - `initial` pour l'état de départ,
  - `from` pour les transitions simples,
  - `from_any_of` pour les transitions « OU » (qui impliquent des tables de mapping) ;
- des `extensions` pour les données optionnelles / mutables non décisionnelles ;
- des `projections` pour les vues dérivées (intervalles d'état, état courant, etc.).

Un premier exemple de schéma YAML est défini dans le dossier `todo/state-modeler` (projet de pilotage) et servira de référence pour les premiers tests.

## État actuel

Ce dépôt est en phase de cadrage :

- la vision, la stack et l'arborescence cible sont définies dans le projet compagnon `todo/state-modeler` ;
- les modules Gradle et les classes Java restent à initialiser.

Les prochaines étapes :

- initialiser le projet Gradle multi‑module (core + cli) ;
- implémenter le modèle interne SDD et le parseur YAML/JSON ;
- générer un premier DDL PostgreSQL à partir du modèle d'exemple (commande e‑commerce SDD).
