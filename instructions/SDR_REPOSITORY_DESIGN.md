# SDR Repository - Design Document

## 🎯 Objectif

Créer un système de gestion de modèles SDD versionnés avec persistence H2 embarquée, permettant :

1. **Stockage** : Persister les SDR (State Definition Records) générés par `SdrFactory`
2. **Versioning** : Gérer plusieurs versions d'un même modèle
3. **Comparison** : Comparer des versions (schema + DDL diffs)
4. **Migration** : Générer des scripts SQL de migration entre versions
5. **CLI** : Interface en ligne de commande pour manipuler le repository

## 📦 Architecture

```
state-modeler-repository (nouveau module)
├── src/main/java/io/statemodeler/repository/
│   ├── SdrRepository.java              # Interface du repository
│   ├── H2SdrRepository.java            # Implémentation H2
│   ├── SdrMetadata.java                # DTO lightweight
│   ├── comparison/
│   │   ├── SdrComparisonService.java   # Service de comparaison
│   │   ├── SchemaComparison.java       # Résultat diff schema
│   │   └── DdlComparison.java          # Résultat diff DDL
│   └── migration/
│       ├── MigrationGenerator.java     # Générateur de migrations
│       ├── MigrationScript.java        # Script de migration
│       ├── MigrationStep.java          # Étape atomique
│       └── StepType.java               # Enum (ADD_TABLE, DROP_COLUMN, etc.)
└── src/test/java/...

state-modeler-cli (module existant - extensions)
└── src/main/java/io/statemodeler/cli/
    ├── RegisterCommand.java            # NEW: sdd-modeler register
    ├── ListCommand.java                # NEW: sdd-modeler list
    ├── ShowCommand.java                # NEW: sdd-modeler show
    ├── CompareCommand.java             # NEW: sdd-modeler compare
    ├── MigrateCommand.java             # NEW: sdd-modeler migrate
    └── DeleteCommand.java              # NEW: sdd-modeler delete
```

## 🗄️ Schema H2

```sql
-- Table principale pour stocker les SDR
CREATE TABLE sdr_records (
    -- Clé primaire : hash du schema (unique, déterministe)
    schema_hash VARCHAR(64) PRIMARY KEY,
    
    -- Métadonnées du modèle
    model_name VARCHAR(255) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    
    -- Contenu du SDR
    schema_json CLOB NOT NULL,           -- SDD model normalisé (JSON canonique)
    content_type VARCHAR(100) NOT NULL,  -- Format original (application/yaml, etc.)
    ddl_sql CLOB NOT NULL,               -- DDL généré
    ddl_hash VARCHAR(64) NOT NULL,       -- Hash du DDL
    
    -- Versioning SDR factory
    sdr_version VARCHAR(20) NOT NULL,    -- Version du générateur
    build_fingerprint VARCHAR(64) NOT NULL,  -- Hash combiné (schema+DDL+version)
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes pour recherche rapide
    INDEX idx_model_name (model_name),
    INDEX idx_model_version (model_name, model_version),
    INDEX idx_created_at (created_at)
);

-- Table pour l'historique des migrations appliquées (future extension)
CREATE TABLE migration_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    from_hash VARCHAR(64) NOT NULL,
    to_hash VARCHAR(64) NOT NULL,
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    applied_by VARCHAR(100),  -- username or system
    status VARCHAR(20) NOT NULL,  -- SUCCESS, FAILED, ROLLED_BACK
    error_message CLOB,
    
    FOREIGN KEY (from_hash) REFERENCES sdr_records(schema_hash),
    FOREIGN KEY (to_hash) REFERENCES sdr_records(schema_hash)
);
```

**Location** : `~/.sdd-modeler/repository.h2` (user home directory)

## 🔌 API du Repository

### SdrRepository Interface

```java
public interface SdrRepository {
    
    /**
     * Persiste un SDR dans le repository.
     * @throws IllegalArgumentException si un SDR avec ce hash existe déjà
     */
    void save(SdrRecord sdr, String modelName, String modelVersion);
    
    /**
     * Retrouve un SDR par son hash de schema.
     */
    Optional<SdrRecord> findByHash(String schemaHash);
    
    /**
     * Liste toutes les versions d'un modèle par son nom.
     * @return Liste triée par date de création (plus récent en premier)
     */
    List<SdrMetadata> findByName(String modelName);
    
    /**
     * Retrouve un SDR par nom et version exacte.
     */
    Optional<SdrRecord> findByNameAndVersion(String modelName, String version);
    
    /**
     * Liste tous les modèles (metadata seulement pour performance).
     * @return Liste de metadata triée par date de création
     */
    List<SdrMetadata> listAll();
    
    /**
     * Liste les N derniers SDR créés.
     */
    List<SdrMetadata> findRecent(int limit);
    
    /**
     * Supprime un SDR du repository.
     * @return true si supprimé, false si non trouvé
     */
    boolean delete(String schemaHash);
    
    /**
     * Compte le nombre total de SDR dans le repository.
     */
    long count();
    
    /**
     * Vérifie si un SDR existe avec ce hash.
     */
    boolean exists(String schemaHash);
}
```

### SdrMetadata Record

```java
/**
 * Lightweight metadata for SDR listing (no CLOB fields).
 */
public record SdrMetadata(
    String schemaHash,
    String modelName,
    String modelVersion,
    String sdrVersion,
    String buildFingerprint,
    Instant createdAt
) {
    public String shortHash() {
        return schemaHash.substring(0, 8);
    }
}
```

## 🔍 Comparison Service

### SdrComparisonService

```java
public class SdrComparisonService {
    
    /**
     * Compare les schemas de deux SDR (JSON diff).
     * Utilise zjsonpatch ou javers pour générer un patch JSON.
     */
    public SchemaComparison compareSchemas(SdrRecord from, SdrRecord to) {
        // Parse JSON, compute JSON patch (RFC 6902)
        // Return structured diff with: added, removed, modified paths
    }
    
    /**
     * Compare les DDL de deux SDR (line-by-line diff).
     */
    public DdlComparison compareDdl(SdrRecord from, SdrRecord to) {
        // Utilise diff algorithm (Myers, Hunt-McIlroy)
        // Return unified diff ou side-by-side
    }
}
```

### SchemaComparison Record

```java
public record SchemaComparison(
    List<JsonChange> changes,   // Added, Removed, Modified fields
    boolean hasBreakingChanges,
    List<String> breakingChangeReasons
) {
    public record JsonChange(
        ChangeType type,    // ADD, REMOVE, REPLACE
        String path,        // JSON path (/entities/order/states/pending)
        Object oldValue,    // null if ADD
        Object newValue     // null if REMOVE
    ) {}
}
```

### DdlComparison Record

```java
public record DdlComparison(
    String unifiedDiff,         // Unified diff format
    List<DdlDiffLine> lines,    // Parsed diff lines
    int addedLines,
    int removedLines
) {
    public enum LineType { CONTEXT, ADDED, REMOVED }
    
    public record DdlDiffLine(
        LineType type,
        int lineNumber,
        String content
    ) {}
}
```

## 🔄 Migration Generator

### MigrationGenerator

```java
public class MigrationGenerator {
    
    /**
     * Génère un script de migration entre deux SDR.
     * 
     * @param from Version source
     * @param to Version cible
     * @param dialect Dialecte SQL (postgres, mysql, etc.)
     * @return Script de migration avec étapes et warnings
     */
    public MigrationScript generate(SdrRecord from, SdrRecord to, String dialect) {
        // 1. Parse DDL from/to
        // 2. Identifier différences structurelles
        // 3. Générer étapes de migration
        // 4. Détecter breaking changes
        // 5. Retourner script ordonné
    }
}
```

### MigrationScript Record

```java
public record MigrationScript(
    String fromHash,
    String toHash,
    List<MigrationStep> steps,
    List<String> warnings,
    String sqlScript,           // Concatenation of all step SQL
    boolean hasBreakingChanges
) {
    public String renderToFile() {
        // Generate commented SQL file with:
        // - Header (from/to versions, date)
        // - Warnings section
        // - Migration steps with comments
    }
}
```

### MigrationStep Record

```java
public record MigrationStep(
    StepType type,
    String target,              // Table/column/constraint name
    String sql,                 // SQL statement
    boolean breaking,           // True if data loss risk
    String description          // Human-readable description
) {}

public enum StepType {
    // Safe operations
    ADD_TABLE,
    ADD_COLUMN,
    ADD_CONSTRAINT,
    ADD_INDEX,
    CREATE_VIEW,
    
    // Potentially breaking
    DROP_TABLE,
    DROP_COLUMN,
    DROP_CONSTRAINT,
    ALTER_COLUMN_TYPE,
    RENAME_TABLE,
    RENAME_COLUMN,
    
    // Complex
    CUSTOM_SQL
}
```

## 🖥️ CLI Commands

### 1. `register` - Enregistrer un modèle

```bash
sdd-modeler register <model-file.yaml> [options]

Options:
  --name <name>        Override model name (default: from model)
  --version <version>  Override version (default: from model)
  --dialect <dialect>  SQL dialect (default: postgres)
  --force              Overwrite if hash exists

Output:
  ✓ Model registered successfully
  Hash: a3f5c8d9...
  Name: orders-system
  Version: 1.0.0
```

**Implémentation** :
1. Parse model file via `ModelLoader`
2. Validate via `ModelValidators`
3. Create SDR via `SdrFactory.create()`
4. Extract name/version from model or use CLI args
5. Save to repository via `SdrRepository.save()`
6. Display hash + metadata

### 2. `list` - Lister les modèles

```bash
sdd-modeler list [options]

Options:
  --name <filter>      Filter by model name (substring match)
  --format <format>    Output format: table (default), json, yaml
  --recent <n>         Show only N most recent

Output (table format):
  HASH      NAME            VERSION  SDR_VERSION  CREATED
  a3f5c8d9  orders-system   1.0.0    0.1.0        2025-11-16 10:30
  b7e2a4f1  orders-system   1.1.0    0.1.0        2025-11-16 14:22
  c9d8f3e2  user-mgmt       2.0.0    0.1.0        2025-11-15 09:15
```

### 3. `show` - Afficher détails d'un SDR

```bash
sdd-modeler show <hash|name:version> [options]

Options:
  --schema-only        Display only schema JSON
  --ddl-only           Display only DDL SQL
  --json               Output as JSON

Examples:
  sdd-modeler show a3f5c8d9
  sdd-modeler show orders-system:1.0.0
  sdd-modeler show a3f5c8d9 --ddl-only
```

### 4. `compare` - Comparer deux SDR

```bash
sdd-modeler compare <from-hash> <to-hash> [options]

Options:
  --schema-only        Compare schemas only (JSON diff)
  --ddl-only           Compare DDL only (SQL diff)
  --format <format>    Diff format: unified (default), split, json

Output:
  Schema Changes:
    ✓ No breaking changes
    + Added: /entities/order/states/shipped
    ~ Modified: /entities/order/states/paid/attributes/amount
    
  DDL Changes:
    + CREATE TABLE public_states.order_shipped (...);
    ~ ALTER TABLE public_states.order_paid ADD COLUMN notes TEXT;
    
    Additions: 15 lines
    Removals: 3 lines
```

### 5. `migrate` - Générer migration SQL

```bash
sdd-modeler migrate <from-hash> <to-hash> [options]

Options:
  --output <file>      Write migration to file (default: stdout)
  --dialect <dialect>  Target dialect (default: postgres)
  --apply              Apply migration immediately (requires --db-url)
  --db-url <url>       Database connection string
  --dry-run            Show migration without applying

Output:
  -- Migration Script
  -- From: orders-system v1.0.0 (a3f5c8d9)
  -- To: orders-system v1.1.0 (b7e2a4f1)
  -- Generated: 2025-11-16 15:30:00
  
  -- WARNINGS:
  -- ⚠️ Step 3: ALTER COLUMN type may cause data loss
  
  -- Step 1: Add new state table
  CREATE TABLE public_states.order_shipped (
    id SERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL REFERENCES public.orders(id),
    tracking_number TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
  );
  
  -- Step 2: Add FK index
  CREATE INDEX idx_order_shipped_order_id 
    ON public_states.order_shipped(order_id);
```

### 6. `delete` - Supprimer un SDR

```bash
sdd-modeler delete <hash> [options]

Options:
  --force              Skip confirmation

Output:
  ⚠️ Delete SDR a3f5c8d9 (orders-system v1.0.0)?
  This cannot be undone. [y/N]: y
  ✓ SDR deleted successfully
```

## 🧪 Tests

### H2 Integration Tests

```java
@TestInstance(Lifecycle.PER_CLASS)
class H2SdrRepositoryTest {
    
    private H2SdrRepository repository;
    private Path tempDbPath;
    
    @BeforeAll
    void setup() {
        tempDbPath = Files.createTempDirectory("sdr-test");
        repository = new H2SdrRepository(tempDbPath);
    }
    
    @Test
    void shouldSaveAndRetrieveSdr() { /* ... */ }
    
    @Test
    void shouldListByModelName() { /* ... */ }
    
    @Test
    void shouldRejectDuplicateHash() { /* ... */ }
    
    @AfterAll
    void cleanup() {
        repository.close();
        Files.deleteIfExists(tempDbPath);
    }
}
```

### Migration Tests

```java
class MigrationGeneratorTest {
    
    @Test
    void shouldGenerateMigrationForAddedState() {
        // Given - Two SDR with known diff (added state)
        var from = createSdrV1();  // Has: pending, paid
        var to = createSdrV2();    // Has: pending, paid, shipped
        
        // When
        var migration = new MigrationGenerator().generate(from, to, "postgres");
        
        // Then
        assertThat(migration.steps()).hasSize(2);  // CREATE TABLE + INDEX
        assertThat(migration.hasBreakingChanges()).isFalse();
        assertThat(migration.sqlScript()).contains("CREATE TABLE");
    }
    
    @Test
    void shouldDetectBreakingChangeOnDropColumn() { /* ... */ }
}
```

## 📚 Dépendances

### Nouvelles dépendances

```kotlin
// state-modeler-repository/build.gradle.kts
dependencies {
    implementation(project(":state-modeler-core"))
    
    // H2 Database
    implementation("com.h2database:h2:2.2.224")
    
    // JSON diff (pour SchemaComparison)
    implementation("com.flipkart.zjsonpatch:zjsonpatch:0.4.14")
    
    // Diff algorithm (pour DdlComparison)
    implementation("io.github.java-diff-utils:java-diff-utils:4.12")
    
    // Tests
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
}
```

## 🚀 Roadmap de développement (IMPLÉMENTATION ITÉRATIVE)

### ✅ Phase 1 : Repository Core
**Step 1.1** - Module setup
- [ ] Créer module `state-modeler-repository` avec build.gradle.kts
- [ ] Ajouter dépendances (H2, zjsonpatch, java-diff-utils)
- [ ] Configuration Gradle multi-module

**Step 1.2** - Core interfaces & records
- [ ] `SdrMetadata` record (lightweight DTO)
- [ ] `SdrRepository` interface (CRUD operations)
- [ ] Package structure (`io.statemodeler.repository`)

**Step 1.3** - H2 Repository implementation
- [ ] `H2SdrRepository` class avec connection pooling
- [ ] Schema DDL creation (sdr_records table)
- [ ] CRUD methods implementation

**Step 1.4** - Repository tests
- [ ] `H2SdrRepositoryTest` avec Testcontainers ou embedded H2
- [ ] Tests CRUD complets
- [ ] Tests edge cases (duplicate hash, not found, etc.)

### ✅ Phase 2 : CLI Commands - Basic CRUD
**Step 2.1** - Register command
- [ ] `RegisterCommand` class (extends Picocli Callable)
- [ ] Parse model → create SDR → save to repo
- [ ] Options: --name, --version, --dialect, --force
- [ ] Tests CLI avec model fixtures

**Step 2.2** - List command
- [ ] `ListCommand` class
- [ ] Format table (default), JSON, YAML outputs
- [ ] Filtering par --name, --recent
- [ ] Tests output formats

**Step 2.3** - Show command
- [ ] `ShowCommand` class
- [ ] Support hash ou name:version syntax
- [ ] Options: --schema-only, --ddl-only, --json
- [ ] Tests retrieval + formatting

**Step 2.4** - Delete command
- [ ] `DeleteCommand` class
- [ ] Confirmation prompt (skip with --force)
- [ ] Tests deletion workflow

### ✅ Phase 3 : Comparison Service
**Step 3.1** - Comparison interfaces
- [ ] `SchemaComparison` record (JSON diff result)
- [ ] `DdlComparison` record (SQL diff result)
- [ ] `SdrComparisonService` interface

**Step 3.2** - Schema comparison implementation
- [ ] Implémenter `compareSchemas()` avec zjsonpatch
- [ ] Detect breaking changes (removed entities/states)
- [ ] Tests avec fixtures (v1 → v2 known diffs)

**Step 3.3** - DDL comparison implementation
- [ ] Implémenter `compareDdl()` avec java-diff-utils
- [ ] Unified diff + line-by-line parsing
- [ ] Tests diff algorithms

**Step 3.4** - Compare command
- [ ] `CompareCommand` class
- [ ] Options: --schema-only, --ddl-only, --format
- [ ] Pretty-print diffs (colors, symbols)
- [ ] Tests comparison CLI output

### ⏸️ Phase 4 : Migration Generator (FUTUR - NON IMPLÉMENTÉ)
> **Note** : MigrationGenerator sera implémenté dans une phase ultérieure.
> Fonctionnalités prévues :
> - Génération automatique de scripts ALTER TABLE
> - Détection de breaking changes (DROP COLUMN, etc.)
> - Migration step-by-step avec rollback
> - `MigrateCommand` CLI

### ✅ Phase 5 : Polish & Documentation
**Step 5.1** - Integration tests end-to-end
- [ ] Test complet: register → list → show → compare → delete
- [ ] Tests avec vrais modèles (orders example)
- [ ] Performance tests (1000+ SDR inserts)

**Step 5.2** - Documentation
- [ ] README.md avec exemples CLI
- [ ] DEV_README.md updates (new commands)
- [ ] Copilot instructions updates
- [ ] Example workflow guides

**Step 5.3** - Code quality
- [ ] Coverage 100% pour repository module
- [ ] Spotless formatting
- [ ] Javadoc complète
- [ ] Error messages clairs et actionables

## 🔮 Extensions futures

- **Multi-dialect support** : Même modèle → plusieurs DDL (postgres + mysql)
- **Tags/Labels** : Marquer SDR (production, staging, etc.)
- **Migration history tracking** : Enregistrer migrations appliquées
- **Rollback capabilities** : Générer migrations inverses
- **Export/Import** : Backup/restore repository
- **Web UI** : Interface graphique pour browser repository
- **CI/CD integration** : GitHub Actions pour validation schema changes
- **Schema registry** : API REST pour découverte de schemas
