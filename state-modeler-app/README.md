# SDD Modeler CLI

The Command Line Interface (CLI) for SDD Modeler, providing tools for validation, generation, repository management, and AI-powered migrations.

## 📦 Installation & Usage

### Build from Source

```bash
git clone https://github.com/ng-galien/sdd-modeler.git
cd sdd-modeler
./gradlew :state-modeler-app:installDist
```

The executable will be available in `state-modeler-app/build/install/state-modeler-app/bin/state-modeler`.

### Run via Gradle

You can also run commands directly via Gradle:

```bash
./gradlew :state-modeler-app:run --args="<command> <args>"
```

## 🚀 Commands

### Validation & Generation

```bash
# Validate your model
./state-modeler validate model.yaml

# Generate PostgreSQL DDL
./state-modeler sql model.yaml --output schema.sql
```

### Repository Management

Manage your State Definition Records (SDR) in a local repository (`~/.sdd-modeler/repository`).

```bash
# Register model in local repository
./state-modeler register model.yaml --name my-model --version 1.0.0

# List registered models
./state-modeler list --format table

# Show model details
./state-modeler show my-model:1.0.0

# Delete a model
./state-modeler delete <hash>
```

### Migration & Diff

```bash
# Compare DDL between two versions
./state-modeler diff my-model:1.0 my-model:2.0

# Generate migration script using AI
./state-modeler migrate my-model:1.0 my-model:2.0 --output migration.sql
```

## ✨ Key Features

### SDR Repository Management

- **State Definition Records (SDR)**: Immutable snapshots of your models with cryptographic hashes
- **Local H2 database**: Automatic persistence in `~/.sdd-modeler/repository`
- **Version tracking**: Compare models across versions
- **Hash-based integrity**: SHA-256 ensures model consistency
- **Multiple output formats**: Table, JSON, or YAML for listing models

### AI-Powered Migration Generation

- **LLM-based migration scripts**: Automatic SQL migration generation using LangChain4j
- **DDL comparison service**: Structural diff analysis between model versions
- **Migration caching**: Persisted migrations to avoid regeneration costs
- **Ollama integration**: Server-based LLM for intelligent migration generation
- **Intelligent prompts**: PostgreSQL-specific patterns with safety guidelines
