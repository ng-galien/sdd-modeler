# SDR Repository Guide

## Overview

The **State Definition Record (SDR) Repository** is a local database that stores versioned snapshots of your SDD models with cryptographic integrity verification. It enables model versioning, comparison, and migration management.

## Key Concepts

### State Definition Record (SDR)

An SDR is an immutable snapshot containing:

- **Schema**: The SDD model in canonical JSON format
- **DDL**: Generated PostgreSQL DDL for the model
- **Metadata**: Name, version, creation timestamp
- **Hashes**: SHA-256 hashes for integrity verification

```java
record SdrRecord(
    String schema,          // Canonical JSON (format-independent)
    String contentType,     // "application/yaml" or "application/json"
    String ddl,             // Generated PostgreSQL DDL
    String schemaHash,      // SHA-256 of schema
    String ddlHash,         // SHA-256 of DDL
    String version          // SDR format version (e.g., "1.0.0")
)

record SdrMetadata(
    String schemaHash,      // Primary key
    String modelName,       // User-defined name
    String modelVersion,    // User-defined version
    String sdrVersion,      // SDR format version
    String buildFingerprint,// schemaHash + ddlHash + version
    Instant createdAt       // Creation timestamp
)
```

### Hash-Based Integrity

- **Format-independent**: YAML and JSON inputs produce identical hashes
- **Cryptographic**: SHA-256 ensures model consistency
- **Fingerprint**: Composite hash for complete model identity

## Repository Location

The repository is stored as an embedded H2 database with automatic path resolution:

1. **CLI option**: `--repository /custom/path`
2. **Environment variable**: `SDD_REPOSITORY_PATH`
3. **Default**: `~/.sdd-modeler/repository`

Example paths:

- macOS/Linux: `~/.sdd-modeler/repository.mv.db`
- Windows: `%USERPROFILE%\.sdd-modeler\repository.mv.db`

## CLI Commands

### 1. Register a Model

Save a model to the repository:

```bash
# Basic registration (auto-detects name/version from model)
./gradlew :state-modeler-app:run --args="register model.yaml"

# Custom name and version
./gradlew :state-modeler-app:run --args="register model.yaml --name my-model --version 2.0.0"

# Custom repository path
./gradlew :state-modeler-app:run --args="register model.yaml --repository /path/to/repo"
```

**Name/Version Resolution**:

1. **CLI arguments**: `--name` and `--version` flags
2. **Model metadata**: `name` and `version` fields in YAML/JSON
3. **Filename**: Derived from file name (e.g., `my-model.yaml` → name: `my-model`)
4. **Default version**: `1.0.0` if not specified

**Exit Codes**:

- `0`: Success
- `1`: Error (invalid model, file not found, validation failure)
- `2`: Duplicate (model with same hash already exists)

**Example Output**:

```
✓ Successfully registered SDR
  Name: orders-sdd-example
  Version: 1.0.0
  Schema Hash: 222fa0d3e1b4c5d6a7f8e9d0c1b2a3f4...
  DDL Hash: 5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d...
  Build Fingerprint: 222fa0d3...5a6b7c8d...1.0.0
```

### 2. List Models

Display all registered models:

```bash
# Table format (default)
./gradlew :state-modeler-app:run --args="list"

# JSON format
./gradlew :state-modeler-app:run --args="list --format json"

# YAML format
./gradlew :state-modeler-app:run --args="list --format yaml"

# Limit results
./gradlew :state-modeler-app:run --args="list --limit 10"
```

**Table Format Example**:

```
Hash (short)    Name                Version    Created At
222fa0d3...     orders-sdd-example  1.0.0      2024-11-16 14:30:15
5a6b7c8d...     payment-model       2.1.0      2024-11-16 10:22:03
9e0f1a2b...     user-model          1.5.2      2024-11-15 16:45:30
```

**JSON Format Example**:

```json
[
  {
    "schemaHash": "222fa0d3e1b4c5d6a7f8e9d0c1b2a3f4...",
    "modelName": "orders-sdd-example",
    "modelVersion": "1.0.0",
    "sdrVersion": "1.0.0",
    "buildFingerprint": "222fa0d3...5a6b7c8d...1.0.0",
    "createdAt": "2024-11-16T14:30:15Z"
  }
]
```

### 3. Show Model Details

Display detailed information about a model:

```bash
# By hash (full hash required)
./gradlew :state-modeler-app:run --args="show 222fa0d3e1b4c5d6a7f8e9d0c1b2a3f4..."

# By name (latest version)
./gradlew :state-modeler-app:run --args="show orders-sdd-example"

# By name and version
./gradlew :state-modeler-app:run --args="show orders-sdd-example:1.0.0"
```

**Output Formats**:

```bash
# All sections (default)
--format all

# Metadata only
--format metadata

# Schema (JSON) only
--format schema

# DDL (SQL) only
--format ddl
```

**Example Output**:

```
=== SDR Metadata ===
Schema Hash: 222fa0d3e1b4c5d6a7f8e9d0c1b2a3f4...
Model Name: orders-sdd-example
Model Version: 1.0.0
SDR Version: 1.0.0
Build Fingerprint: 222fa0d3...5a6b7c8d...1.0.0
Created At: 2024-11-16 14:30:15 CET

=== Schema (JSON) ===
{
  "version": "0.1.0",
  "name": "orders-sdd-example",
  "database": {
    "dialect": "postgres",
    "schema": "public"
  },
  ...
}

=== DDL (SQL) ===
CREATE TABLE public.orders (
    id SERIAL PRIMARY KEY,
    customer_id INT NOT NULL,
    ...
);
```

### 4. Delete a Model

Remove a model from the repository:

```bash
# Interactive confirmation (default)
./gradlew :state-modeler-app:run --args="delete 222fa0d3e1b4c5d6a7f8e9d0c1b2a3f4..."

# Skip confirmation
./gradlew :state-modeler-app:run --args="delete 222fa0d3... --yes"
```

**Interactive Confirmation**:

```
About to delete SDR:
  Hash: 222fa0d3e1b4c5d6a7f8e9d0c1b2a3f4...
  Version: 1.0.0
  Build Fingerprint: 222fa0d3...5a6b7c8d...1.0.0

Are you sure you want to delete this SDR? (yes/no): yes

✓ Successfully deleted SDR
  Hash: 222fa0d3e1b4c5d6a7f8e9d0c1b2a3f4...
```

**Exit Codes**:

- `0`: Success or cancelled by user
- `1`: Error (hash not found, repository error)

## Typical Workflows

### Version Management

Track model evolution:

```bash
# Version 1.0.0
./gradlew :state-modeler-app:run --args="register model-v1.yaml --name orders --version 1.0.0"

# Version 2.0.0 (breaking changes)
./gradlew :state-modeler-app:run --args="register model-v2.yaml --name orders --version 2.0.0"

# Version 2.1.0 (minor update)
./gradlew :state-modeler-app:run --args="register model-v2.1.yaml --name orders --version 2.1.0"

# List all versions
./gradlew :state-modeler-app:run --args="list" | grep orders
```

### Model Comparison (Planned)

Compare two model versions:

```bash
# Compare latest vs specific version
./gradlew :state-modeler-app:run --args="compare orders:2.1.0 orders:1.0.0"

# Compare by hash
./gradlew :state-modeler-app:run --args="compare 222fa0d3... 5a6b7c8d..."
```

### Migration Generation (Planned)

Generate migration scripts:

```bash
# Generate ALTER scripts from v1 to v2
./gradlew :state-modeler-app:run --args="migrate orders:1.0.0 orders:2.0.0 --output migration.sql"
```

## Advanced Usage

### Environment Variable Configuration

```bash
# Set custom repository path
export SDD_REPOSITORY_PATH=/projects/shared-repo

# Now all commands use this path
./gradlew :state-modeler-app:run --args="list"
```

### Multiple Repositories

```bash
# Development repository
./gradlew :state-modeler-app:run --args="register model.yaml --repository ~/.sdd-dev"

# Production repository
./gradlew :state-modeler-app:run --args="register model.yaml --repository /prod/sdd-repo"
```

### CI/CD Integration

```yaml
# GitHub Actions example
- name: Register model
  run: |
    ./gradlew :state-modeler-app:run --args="register schema/model.yaml \
      --name ${{ github.event.repository.name }} \
      --version ${{ github.sha }}"

- name: Verify no duplicates
  run: |
    if ./gradlew :state-modeler-app:run --args="register schema/model.yaml"; then
      echo "Model registered successfully"
    elif [ $? -eq 2 ]; then
      echo "Model already exists (duplicate hash)"
      exit 0
    else
      echo "Registration failed"
      exit 1
    fi
```

## Database Schema

The H2 repository uses a simple schema:

```sql
CREATE TABLE sdr_records (
    schema_hash VARCHAR(64) PRIMARY KEY,
    model_name VARCHAR(255) NOT NULL,
    model_version VARCHAR(50) NOT NULL,
    schema CLOB NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    ddl CLOB NOT NULL,
    ddl_hash VARCHAR(64) NOT NULL,
    sdr_version VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_name ON sdr_records(model_name);
CREATE INDEX idx_name_version ON sdr_records(model_name, model_version);
```

## Troubleshooting

### Duplicate Hash Error

```
ERROR: Duplicate SDR detected
  Hash: 222fa0d3e1b4c5d6a7f8e9d0c1b2a3f4...
  Existing: orders-sdd-example:1.0.0
  Use --force to overwrite (planned)
```

**Solution**: The model already exists. Use `show` to view existing model or modify your model.

### Repository Not Found

```
ERROR: Failed to create repository
  Path: /invalid/path
```

**Solution**: Ensure the directory exists and is writable. Use default path or set `SDD_REPOSITORY_PATH`.

### Invalid Hash Format

```
ERROR: SDR not found
  Hash: 222fa0d3
  Use 'sdd-modeler list' to view registered SDRs
```

**Solution**: Short hashes not yet supported. Use full hash from `list` command.

## Implementation Details

### Repository Interface

```java
public interface SdrRepository extends AutoCloseable {
    Try<Void> save(SdrRecord sdr, String modelName, String modelVersion);
    Try<Optional<SdrRecord>> findByHash(String schemaHash);
    Try<List<SdrRecord>> findByName(String modelName);
    Try<Optional<SdrRecord>> findByNameAndVersion(String modelName, String modelVersion);
    Try<Boolean> exists(String schemaHash);
    Try<Boolean> delete(String schemaHash);
    Try<List<SdrMetadata>> listAll();
    Try<List<SdrMetadata>> findRecent(int limit);
    Try<Integer> count();
}
```

### Vavr Try<T> Pattern

All repository methods return `Try<T>` for functional error handling:

```java
repository.findByHash(hash)
    .map(opt -> opt.orElseThrow(() -> new IllegalStateException("Not found")))
    .onSuccess(sdr -> System.out.println("Found: " + sdr.schemaHash()))
    .onFailure(ex -> System.err.println("Error: " + ex.getMessage()));
```

## Future Enhancements

- [ ] **Short hash support**: Resolve by partial hash (e.g., `222fa0d3`)
- [ ] **Remote repositories**: Sync with remote servers
- [ ] **Export/Import**: Backup and restore repositories
- [ ] **Tags**: Label models with custom tags
- [ ] **Search**: Full-text search across models
- [ ] **Diff viewer**: Side-by-side schema/DDL comparison
- [ ] **Migration history**: Track applied migrations
- [ ] **Rollback support**: Revert to previous versions

## See Also

- [Main Documentation](README.md)
- [Core Library Documentation](state-modeler-core/README.md)
- [CLI Documentation](state-modeler-app/README.md)
- [Gradle Plugin Documentation](state-modeler-gradle-plugin/README.md)
- [Architecture Guide](instructions/ARCHITECTURE.md)
- [Examples](scripts/examples/)
