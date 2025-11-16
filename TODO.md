# sdd-modeler — Roadmap & TODO

## Prochaines étapes prioritaires

### 1. ✅ Amélioration de la génération SQL PostgreSQL (EN COURS)
- [ ] Génération d'index sur les foreign keys
- [ ] Support des types PostgreSQL standards pour validation
- [ ] Génération de triggers pour automatiser les transitions (future)
- [ ] Génération de fonctions PL/pgSQL pour validations métier (future)

### 2. Génération de code Java/Spring (`state-modeler-spring`)
- [ ] Génération d'entités JPA
- [ ] Génération de repositories Spring Data
- [ ] Génération de services avec logique de transition
- [ ] Génération de DTOs et mappers

### 3. Support d'autres dialectes SQL
- [ ] MySQL/MariaDB
- [ ] SQLite
- [ ] SQL Server
- [ ] Oracle

### 4. Projections avancées
- [ ] Projections avec agrégations
- [ ] Projections avec jointures custom
- [ ] Support de requêtes SQL personnalisées dans le DSL

### 5. Amélioration du CLI
- [ ] Mode interactif pour créer un modèle
- [ ] Génération de diagrammes (Mermaid, PlantUML)
- [ ] Diff entre deux versions de modèle
- [ ] Migration SQL automatique (ALTER TABLE)

### 6. Documentation et exemples
- [ ] Plus d'exemples métier (blog, e-commerce, workflow approbation)
- [ ] Guide de migration depuis approche traditionnelle
- [ ] Best practices SDD

---

## Détails des tâches en cours

### Amélioration génération SQL PostgreSQL

**Phase 1 : Index et types (EN COURS)**
- Génération automatique d'index sur toutes les foreign keys pour optimiser les jointures
- Validation des types PostgreSQL dans le DSL (reject invalid types)
- Support explicite des types courants : TEXT, INTEGER, BIGINT, NUMERIC, BOOLEAN, TIMESTAMPTZ, JSONB, etc.

**Phase 2 : Triggers et fonctions (future)**
- Triggers pour garantir l'immutabilité des états
- Triggers pour valider les transitions autorisées
- Fonctions PL/pgSQL pour encapsuler la logique métier

---

## Notes de conception

### Index sur FK
- Créer automatiquement un index pour chaque colonne de foreign key
- Nommage : `idx_<table>_<column>`
- Améliore les performances des JOIN et des contraintes référentielles

### Validation des types
- Maintenir une liste de types PostgreSQL valides
- Rejeter les types invalides lors de la validation du modèle
- Permettre types avec paramètres : `NUMERIC(10,2)`, `VARCHAR(255)`, etc.
