# sdd-modeler — Roadmap & TODO

## Prochaines étapes prioritaires

### 1. ✅ Amélioration de la génération SQL PostgreSQL (TERMINÉ)
- [x] Génération d'index sur les foreign keys
- [x] Support des types PostgreSQL standards pour validation
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
- [x] Génération de diagrammes Mermaid
- [ ] Mode interactif pour créer un modèle
- [ ] Support de PlantUML pour diagrammes
- [ ] Diff entre deux versions de modèle
- [ ] Migration SQL automatique (ALTER TABLE)

### 6. 🎯 **SDR Repository avec H2 (PRIORITAIRE)**
Système de gestion de schémas versionnés basé sur `SdrFactory` et base H2 embarquée.

#### 6.1. Infrastructure de persistance
- [ ] **SdrRepository interface** : API pour CRUD des SDR
  - `save(SdrRecord): void` - Persister un SDR
  - `findByHash(String): Optional<SdrRecord>` - Retrouver par hash
  - `findByName(String): List<SdrRecord>` - Lister versions par nom
  - `findByNameAndVersion(String, String): Optional<SdrRecord>` - Retrouver version spécifique
  - `listAll(): List<SdrMetadata>` - Lister tous les modèles (metadata seulement)
  - `delete(String hash): boolean` - Supprimer un SDR

- [ ] **H2SdrRepository implementation**
  - Base H2 embarquée (file-based: `~/.sdd-modeler/repository.h2`)
  - Schema SQL : table `sdr_records` avec colonnes :
    ```sql
    CREATE TABLE sdr_records (
      hash VARCHAR(128) PRIMARY KEY,        -- schemaHash (unique identifier)
      model_name VARCHAR(255) NOT NULL,     -- extracted from schema JSON
      version VARCHAR(50) NOT NULL,         -- model version
      schema_json CLOB NOT NULL,            -- normalized JSON
      content_type VARCHAR(100),            -- original format
      ddl_sql CLOB NOT NULL,                -- generated DDL
      ddl_hash VARCHAR(64) NOT NULL,        -- DDL hash
      sdr_version VARCHAR(20) NOT NULL,     -- SDR factory version
      build_fingerprint VARCHAR(64),        -- combined fingerprint
      created_at TIMESTAMP DEFAULT NOW(),
      INDEX idx_model_name (model_name),
      INDEX idx_version (model_name, version)
    );
    ```

- [ ] **SdrMetadata record** : Lightweight DTO pour listing
  - `(String hash, String modelName, String version, String sdrVersion, Instant createdAt)`

#### 6.2. Commandes CLI
- [ ] **`sdd-modeler register <model-file>`**
  - Parse le modèle, crée SDR via `SdrFactory`, persiste dans H2
  - Options : `--name <override-name>`, `--version <override-version>`
  - Output : hash du SDR enregistré

- [ ] **`sdd-modeler list`**
  - Liste tous les modèles avec : name, version, hash (8 premiers chars), date
  - Options : `--name <filter>`, `--format <table|json|yaml>`

- [ ] **`sdd-modeler show <hash|name:version>`**
  - Affiche détails complets d'un SDR (schema, DDL, hashes, fingerprint)
  - Options : `--schema-only`, `--ddl-only`, `--json`

- [ ] **`sdd-modeler compare <hash1> <hash2>`**
  - Compare deux SDR (schema diff + DDL diff)
  - Output : différences structurelles (JSON diff) + SQL diff side-by-side
  - Options : `--format <unified|split|json>`

- [ ] **`sdd-modeler migrate <from-hash> <to-hash>`**
  - Génère script de migration SQL (ALTER TABLE, etc.)
  - Analyse différences et produit :
    * Ajout de colonnes (ALTER TABLE ADD COLUMN)
    * Ajout de tables
    * Ajout de contraintes
    * ⚠️ Détection de breaking changes (DROP, ALTER incompatible)
  - Options : `--output <file>`, `--apply` (exec direct si DB config)

- [ ] **`sdd-modeler delete <hash>`**
  - Supprime un SDR du repository
  - Options : `--force` (skip confirmation)

#### 6.3. Service layer
- [ ] **SdrComparisonService**
  - `compareSchemas(SdrRecord v1, SdrRecord v2): SchemaComparison`
  - `compareDdl(SdrRecord v1, SdrRecord v2): DdlComparison`
  - Utilise JSON diff library (e.g., `javers`, `zjsonpatch`)

- [ ] **MigrationGenerator**
  - `generateMigration(SdrRecord from, SdrRecord to): MigrationScript`
  - Analyse DDL diffs et génère :
    * Liste de `MigrationStep` (ADD_TABLE, ADD_COLUMN, ADD_CONSTRAINT, etc.)
    * SQL statements pour chaque step
    * Warnings pour breaking changes
  - Support PostgreSQL (extensible pour autres dialectes)

- [ ] **MigrationScript record**
  - `(List<MigrationStep> steps, List<String> warnings, String sql)`
  - `MigrationStep`: `(StepType type, String target, String sql, boolean breaking)`

#### 6.4. Tests et documentation
- [ ] Tests d'intégration avec H2 (comme PostgresDdlIntegrationTest)
- [ ] Tests de migration (from v1 → v2 avec changements connus)
- [ ] Documentation CLI (`README.md` + `DEV_README.md`)
- [ ] Exemples de workflows :
  * Enregistrer modèle initial
  * Modifier modèle, re-enregistrer
  * Comparer versions
  * Générer migration
  * Appliquer migration

#### 6.5. Extensions futures
- [ ] Export/Import repository (backup/restore)
- [ ] Support tags/labels sur SDR (staging, production, etc.)
- [ ] Historique de migrations appliquées
- [ ] Rollback capabilities
- [ ] Multi-dialect support (même modèle → plusieurs DDL)
- [ ] Web UI pour browser le repository (future)

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
