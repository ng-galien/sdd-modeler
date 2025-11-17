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
6. Runs 10 functional tests with real data
7. Cleans up database and container

**Output:**
Generated files are saved to `build/test-output/`:

- `functional-test.sql` - Generated DDL
- `ddl-apply.log` - DDL application output
- `test1.log` through `test10.log` - Individual test outputs

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

## Requirements

- Bash shell (macOS/Linux)
- Java 21+ (managed by Gradle toolchain)
- Gradle wrapper (included in project)
- **For functional tests**: PostgreSQL client (`psql`) **OR** Docker

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
