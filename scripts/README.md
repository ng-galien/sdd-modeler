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

**Example output:**
```
=== DDL Generation Test Suite ===

📂 Setting up test environment...

Test 1: Validate YAML model
✓ Model validation passed

Test 2: Generate DDL from orders-sdd-model.yaml (v1)
✓ DDL generated successfully
  Generated DDL: 158 lines

Test 3: Generate DDL from orders-sdd-model-v2.yaml (v2)
✓ DDL v2 generated successfully
  Generated DDL: 68 lines

Test 4: Verify DDL structure and keywords
  ✓ Entity table creation
  ✓ State tables in state schema
  ✓ Primary key constraints
  ✓ Foreign key constraints
  ✓ Foreign key references
  ✓ Automatic FK indexing
  ✓ Projection views
  ✓ NOT NULL constraints
  ✓ Timestamp defaults
  ✓ Timestamp with timezone

=== All DDL Generation Tests Passed ===
```

## Requirements

- Bash shell (macOS/Linux)
- Java 21+ (managed by Gradle toolchain)
- Gradle wrapper (included in project)

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

Example GitHub Actions usage:
```yaml
- name: Run manual tests
  run: |
    ./scripts/test-ddl-generation.sh
```
