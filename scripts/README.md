# Manual Testing Scripts

This directory contains shell scripts for manual testing and validation of the SDD Modeler.

## Available Scripts

### `test-ddl-generation.sh`

Tests the complete DDL generation pipeline from YAML/JSON models to PostgreSQL DDL.

**What it tests:**

- ✅ YAML model validation
- ✅ DDL generation from schema (v1 and v2 models)
- ✅ DDL structural verification (tables, views, indexes, constraints)
- ✅ Comparison with expected DDL output
- ✅ Statistics extraction (table count, view count, FK count, etc.)
- ✅ V1 vs V2 model differences

**Usage:**

```bash
./scripts/test-ddl-generation.sh
```

**Output:**
Generated files are saved to `build/test-output/`:

- `generated-v1.sql` - DDL from orders-sdd-model.yaml
- `generated-v2.sql` - DDL from orders-sdd-model-v2.yaml
- `ddl-diff.txt` - Diff between generated and expected DDL
- `v1-v2-diff.txt` - Diff between v1 and v2 models
- `validate.log` - Validation output

**Exit codes:**

- `0` - All tests passed
- `1` - One or more tests failed

---

### `test-ddl-functional.sh`

Tests generated DDL with real PostgreSQL database and data operations. Automatically uses Docker if PostgreSQL is not available locally.

**What it tests:**

- ✅ Entity creation (orders table)
- ✅ Initial state insertion (pending)
- ✅ Valid state transitions (pending → paid → refunded)
- ✅ SDD behavior (allows state re-entry)
- ✅ OR transitions (pending → cancelled via source table)
- ✅ Projection views (intervals, current_state)
- ✅ State timeline consistency
- ✅ Foreign key constraint enforcement
- ✅ Data integrity across states

**Usage:**

```bash
./scripts/test-ddl-functional.sh
```

**Requirements:**

- **Option 1**: PostgreSQL client (`psql`) + running PostgreSQL server
- **Option 2**: Docker (automatically pulls `postgres:16-alpine` if needed)

**Configuration** (optional environment variables):

```bash
export POSTGRES_USER=myuser          # default: postgres
export POSTGRES_PASSWORD=mypassword  # default: postgres
export POSTGRES_HOST=localhost       # default: localhost
export POSTGRES_PORT=5432            # default: 5432

./scripts/test-ddl-functional.sh
```

**How it works:**

1. Checks if `psql` is available and can connect to PostgreSQL
2. If not, checks if Docker is available
3. If Docker is available, automatically:
   - Pulls `postgres:16-alpine` image (if not present)
   - Starts temporary PostgreSQL container
   - Waits for PostgreSQL to be ready
   - Runs all tests via `docker exec`
   - Cleans up container on exit
4. Creates temporary test database
5. Applies generated DDL
6. Runs 11 functional tests with real data
7. Cleans up database and container

**Output:**
Generated files are saved to `build/test-output/`:

- `functional-test.sql` - Generated DDL
- `ddl-apply.log` - DDL application output
- `test1.log` through `test11.log` - Individual test outputs
- **`functional-test-report.md`** - **Markdown test report with tabular data**

**Markdown Report Contents:**

The functional test script generates a comprehensive Markdown report with:

1. **Metadata & Environment**
   - Generation timestamp
   - Model file name
   - PostgreSQL version
   - Database type (Docker or local)

2. **DDL Statistics**
   - Number of tables created
   - Number of views created
   - Number of foreign keys
   - Number of UNIQUE constraints

3. **Test Results with SQL Code Blocks**
   - Test 1: Entity creation with SQL statement
   - Test 7: Projection views with **tabular data from database**
   - Test 10: Data integrity summary with counts

4. **Tabular View Data**
   - **State Intervals View:** Shows order_id, state_type, start_at, end_at (timeline)
   - **Current Order States:** Shows active states (where end_at IS NULL)
   - **Data Integrity Summary:** Row counts for all states and transitions

5. **Summary**
   - All 11 tests with descriptions
   - Key SDD validations (acyclic graph, immutability, referential integrity)

**Example Report Section:**

```markdown
### Test 7: Projection Views
✅ **State intervals view contains 5 rows**

**State Intervals View:**

\`\`\`
 order_id | state_type |      start_at       |       end_at        
----------+------------+---------------------+---------------------
        1 | PENDING    | 2025-11-17 07:52:49 | 2025-11-17 07:52:49
        1 | PAID       | 2025-11-17 07:52:49 | 2025-11-17 07:52:49
        1 | REFUNDED   | 2025-11-17 07:52:49 | 
        2 | PENDING    | 2025-11-17 07:52:49 | 2025-11-17 07:52:49
        2 | CANCELLED  | 2025-11-17 07:52:49 | 
(5 rows)
\`\`\`

**Current Order States:**

\`\`\`
 order_id | state_type |           start_at            
----------+------------+-------------------------------
        1 | REFUNDED   | 2025-11-17 07:52:49.3526+00
        2 | CANCELLED  | 2025-11-17 07:52:49.731884+00
(2 rows)
\`\`\`
```

**Exit codes:**

- `0` - All tests passed
- `1` - One or more tests failed or PostgreSQL/Docker unavailable

**Example output:**

```text
=== DDL Functional Test Suite ===

🔍 Checking PostgreSQL availability...
⚠ psql command not found
  → Will use Docker to run PostgreSQL

🐳 Starting PostgreSQL Docker container...
✓ Docker container started: sdd-test-postgres-12345
  Waiting for PostgreSQL to be ready...
✓ PostgreSQL is ready

📂 Setting up test environment...

Step 1: Generate DDL from model
✓ DDL generated successfully

Step 2: Create test database
✓ Test database created: sdd_test_functional_1763364498

Step 3: Apply DDL to test database
✓ DDL applied successfully

Test 1: Insert entity (order)
✓ Entity inserted successfully (order_id: 1)

Test 2: Insert initial state (pending)
✓ Initial state inserted (pending_id: 1)

[... 8 more tests ...]

Test 10: Data integrity summary
✓ Data integrity summary generated

=== All Functional Tests Passed ===

🧹 Cleaning up...
✓ Test database dropped
  Stopping Docker container...
✓ Docker container removed
```

---

### `test-migration-generation.sh`

Tests LLM-based migration generation using Ollama with structured outputs.

**What it tests:**

- ✅ Ollama server availability check
- ✅ Model verification/auto-pull (qwen2.5:0.5b)
- ✅ DDL generation for both model versions
- ✅ SDR repository registration
- ✅ LLM-based migration generation
- ✅ Structured outputs (confidence, comments)
- ✅ Comprehensive Markdown report generation

**Usage:**

```bash
./scripts/test-migration-generation.sh
```

**Requirements:**

- Ollama server running at `http://localhost:11434`
- Model `qwen2.5:0.5b` (automatically pulled if missing)

**How it works:**

1. Checks Ollama server availability
2. Verifies qwen2.5:0.5b model (pulls if needed)
3. Generates DDL for orders v1 and v2 models
4. Registers both versions in SDR repository
5. Generates migration using Ollama LLM
6. Extracts confidence score and comments from logs
7. Creates comprehensive Markdown report
8. Opens report automatically (macOS)

**Output:**
Generated files are saved to `build/test-output/`:

- `migration-v1.sql` - DDL for orders v1
- `migration-v2.sql` - DDL for orders v2
- `migration-v1-to-v2.sql` - LLM-generated migration script
- `migration.log` - Full migration generation logs
- **`migration-report.md`** - **Comprehensive Markdown report**

**Markdown Report Contents:**

1. **Metadata**
   - Generation timestamp
   - Source/target versions
   - LLM model used
   - Generation time

2. **DDL for Both Versions** (collapsible)
   - Complete DDL for v1 (source)
   - Complete DDL for v2 (target)

3. **Generated Migration Script**
   - Full migration DDL from Ollama
   - ALTER TABLE statements
   - Comments explaining changes

4. **LLM Analysis**
   - **Confidence Score** (0.0 - 1.0)
   - **Comments/Reasoning** from LLM

5. **Full Logs** (collapsible)
   - Complete CLI output
   - Debug information

6. **File Locations**
   - Paths to all generated files

**Graceful Degradation:**

- Exits with code 0 (not failure) if Ollama is unavailable
- Displays helpful message about installing Ollama
- Safe to run in CI where Ollama may not be present

**Example output:**

```text
=== LLM Migration Generation Test ===

🔍 Checking Ollama availability...
✓ Ollama server is running at http://localhost:11434

🔍 Checking for model qwen2.5:0.5b...
✓ Model qwen2.5:0.5b is available

📂 Setting up test environment...
✓ Test output directory ready: build/test-output

Step 1: Generate DDL for orders v1
✓ DDL generated successfully: migration-v1.sql

Step 2: Generate DDL for orders v2
✓ DDL generated successfully: migration-v2.sql

Step 3: Register v1 in SDR repository
✓ Registered: orders:1.0

Step 4: Register v2 in SDR repository
✓ Registered: orders:2.0

Step 5: Generate migration using Ollama
⏱ Generating migration with LLM (this may take a few seconds)...
✓ Migration generated successfully: migration-v1-to-v2.sql
  Generation time: 12.5 seconds
  Confidence: 0.85
  LLM Comments: Migration adds new tables and constraints...

📄 Creating comprehensive Markdown report...
✓ Report created: migration-report.md

🎉 All tests passed!
📖 Opening report in default viewer...
```

**Exit codes:**

- `0` - All tests passed OR Ollama unavailable (test skipped)
- `1` - Test failed (migration generation error)

---

## Requirements

- Bash shell (macOS/Linux)
- Java 21+ (managed by Gradle toolchain)
- Gradle wrapper (included in project)
- **For functional tests**: PostgreSQL client (`psql`) **OR** Docker
- **For migration tests**: Ollama server with qwen2.5:0.5b model

## Adding New Tests

To add a new test script:

1. Create a new `.sh` file in this directory
2. Make it executable: `chmod +x scripts/your-test.sh`
3. Use the same structure as existing scripts:
   - Set up test environment
   - Run tests with clear output (✓/✗)
   - Save output to `build/test-output/`
   - Exit with appropriate code (0 = success, 1 = failure)
4. Document it in this README

## CI Integration

These scripts can be run in CI pipelines to verify:

- DDL generation correctness
- Model validation
- Backward compatibility
- Performance benchmarks
- **Functional DDL testing** (using Docker)

Example GitHub Actions usage:

```yaml
- name: Run DDL generation tests
  run: |
    ./scripts/test-ddl-generation.sh

- name: Run functional tests with Docker
  run: |
    ./scripts/test-ddl-functional.sh
  # Docker is pre-installed on GitHub Actions runners
```
