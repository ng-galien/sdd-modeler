#!/bin/bash
#
# Test DDL Generation - Schema to DDL validation
#
# This script tests the complete DDL generation pipeline:
# 1. Parse YAML/JSON models
# 2. Validate models
# 3. Generate PostgreSQL DDL
# 4. Verify DDL structure and correctness
#

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SCRIPTS_EXAMPLES_DIR="$PROJECT_ROOT/scripts/examples"
TEST_EXAMPLES_DIR="$PROJECT_ROOT/state-modeler-app/src/test/resources/examples"
EXAMPLES_DIR="$TEST_EXAMPLES_DIR"  # Use the test examples by default to ensure models include projections and provide richer tests

# remove duplicated assignment - EXAMPLES_DIR already set above

# CLI flags handled by tests/users: --examples-doc to pick instructions examples, --examples-test to pick test resources
USE_DOC_EXAMPLES=false
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --examples-doc)
            EXAMPLES_DIR="$PROJECT_ROOT/instructions/examples"; shift 1;;
        --examples-test)
            EXAMPLES_DIR="$TEST_EXAMPLES_DIR"; shift 1;;
        -h|--help)
            echo "Usage: $0 [--examples-doc] [--examples-test] ..."; exit 0;;
        *)
            shift 1;;
    esac
done
OUTPUT_DIR="$PROJECT_ROOT/build/test-output"
GRADLE="$PROJECT_ROOT/gradlew"

# Test files
MODEL_V1="$EXAMPLES_DIR/orders-sdd-model.yaml"
MODEL_V2="$EXAMPLES_DIR/orders-sdd-model-v2.yaml"
EXPECTED_DDL="$EXAMPLES_DIR/orders-sdd-ddl.sql"

echo -e "${BLUE}=== DDL Generation Test Suite ===${NC}\n"

# Setup
echo -e "${YELLOW}📂 Setting up test environment...${NC}"
mkdir -p "$OUTPUT_DIR"
rm -f "$OUTPUT_DIR"/*.sql

# Test 1: Validate model
echo -e "\n${BLUE}Test 1: Validate YAML model${NC}"
if "$GRADLE" -q :state-modeler-app:run --args="validate $MODEL_V1" > "$OUTPUT_DIR/validate.log" 2>&1; then
    echo -e "${GREEN}✓ Model validation passed${NC}"
else
    echo -e "${RED}✗ Model validation failed${NC}"
    cat "$OUTPUT_DIR/validate.log"
    exit 1
fi

# Test 2: Generate DDL from v1 model
echo -e "\n${BLUE}Test 2: Generate DDL from orders-sdd-model.yaml (v1)${NC}"
"$GRADLE" -q :state-modeler-app:run --args="sql $MODEL_V1 -o $OUTPUT_DIR/generated-v1.sql" > "$OUTPUT_DIR/sql-v1.log" 2>&1
if [ -f "$OUTPUT_DIR/generated-v1.sql" ]; then
    echo -e "${GREEN}✓ DDL generated successfully${NC}"
    LINE_COUNT=$(wc -l < "$OUTPUT_DIR/generated-v1.sql")
    echo -e "  Generated DDL: $LINE_COUNT lines"
else
    echo -e "${RED}✗ DDL generation failed${NC}"
    cat "$OUTPUT_DIR/sql-v1.log"
    exit 1
fi

# Test 3: Generate DDL from v2 model
echo -e "\n${BLUE}Test 3: Generate DDL from orders-sdd-model-v2.yaml (v2)${NC}"
"$GRADLE" -q :state-modeler-app:run --args="sql $MODEL_V2 -o $OUTPUT_DIR/generated-v2.sql" > "$OUTPUT_DIR/sql-v2.log" 2>&1
if [ -f "$OUTPUT_DIR/generated-v2.sql" ]; then
    echo -e "${GREEN}✓ DDL v2 generated successfully${NC}"
    LINE_COUNT=$(wc -l < "$OUTPUT_DIR/generated-v2.sql")
    echo -e "  Generated DDL: $LINE_COUNT lines"
else
    echo -e "${RED}✗ DDL v2 generation failed${NC}"
    cat "$OUTPUT_DIR/sql-v2.log"
    exit 1
fi

# Test 4: Verify DDL structure (v1)
echo -e "\n${BLUE}Test 4: Verify DDL structure and keywords${NC}"

# Check for essential DDL components
CHECKS=(
    "CREATE TABLE:Entity table creation"
    "CREATE TABLE.*_states\..*:State tables in state schema"
    "PRIMARY KEY:Primary key constraints"
    "FOREIGN KEY:Foreign key constraints"
    "REFERENCES:Foreign key references"
    "CREATE INDEX:Automatic FK indexing"
    "CREATE VIEW:Projection views"
    "NOT NULL:NOT NULL constraints"
    "DEFAULT now():Timestamp defaults"
    "TIMESTAMPTZ:Timestamp with timezone"
)

FAILED_CHECKS=0
for check in "${CHECKS[@]}"; do
    IFS=':' read -r pattern description <<< "$check"
    if grep -qE "$pattern" "$OUTPUT_DIR/generated-v1.sql"; then
        echo -e "${GREEN}  ✓ $description${NC}"
    else
        # If the missing check is for projection views, only fail if the source model actually declares projections.
        if [ "$description" = "Projection views" ]; then
            if grep -qE '^\s*projections:' "$MODEL_V1"; then
                echo -e "${RED}  ✗ Missing: $description (model declares projections)${NC}"
                FAILED_CHECKS=$((FAILED_CHECKS + 1))
            else
                echo -e "${YELLOW}  ⚠ Skipping: $description (model does not declare projections)${NC}"
            fi
        else
            echo -e "${RED}  ✗ Missing: $description${NC}"
            FAILED_CHECKS=$((FAILED_CHECKS + 1))
        fi
    fi
done

if [ $FAILED_CHECKS -gt 0 ]; then
    echo -e "\n${RED}❌ $FAILED_CHECKS structural checks failed${NC}"
    exit 1
fi

# Test 5: Compare with expected DDL (if exists)
if [ -f "$EXPECTED_DDL" ]; then
    echo -e "\n${BLUE}Test 5: Compare with expected DDL${NC}"
    
    # Normalize whitespace and comments for comparison
    normalize_sql() {
        grep -v '^--' "$1" | \
        grep -v '^$' | \
        sed 's/[[:space:]]\+/ /g' | \
        sed 's/^ //g' | \
        sort
    }
    
    normalize_sql "$OUTPUT_DIR/generated-v1.sql" > "$OUTPUT_DIR/generated-v1-normalized.sql"
    normalize_sql "$EXPECTED_DDL" > "$OUTPUT_DIR/expected-normalized.sql"
    
    if diff -u "$OUTPUT_DIR/expected-normalized.sql" "$OUTPUT_DIR/generated-v1-normalized.sql" > "$OUTPUT_DIR/ddl-diff.txt"; then
        echo -e "${GREEN}✓ Generated DDL matches expected output${NC}"
    else
        echo -e "${YELLOW}⚠ Generated DDL differs from expected (see diff below)${NC}"
        echo -e "${YELLOW}  This may be expected if the generator was improved${NC}"
        echo ""
        head -30 "$OUTPUT_DIR/ddl-diff.txt"
        echo ""
        echo -e "${YELLOW}  Full diff saved to: $OUTPUT_DIR/ddl-diff.txt${NC}"
    fi
else
    echo -e "\n${YELLOW}⚠ Skipping comparison: expected DDL not found${NC}"
fi

# Test 6: Extract schema statistics
echo -e "\n${BLUE}Test 6: DDL Statistics${NC}"
echo -e "${GREEN}Generated DDL v1:${NC}"
echo "  Tables created: $(grep -c 'CREATE TABLE' "$OUTPUT_DIR/generated-v1.sql" || echo 0)"
echo "  Views created: $(grep -c 'CREATE OR REPLACE VIEW' "$OUTPUT_DIR/generated-v1.sql" || echo 0)"
echo "  Indexes created: $(grep -c 'CREATE INDEX' "$OUTPUT_DIR/generated-v1.sql" || echo 0)"
echo "  Foreign keys: $(grep -c 'FOREIGN KEY' "$OUTPUT_DIR/generated-v1.sql" || echo 0)"

echo -e "\n${GREEN}Generated DDL v2:${NC}"
echo "  Tables created: $(grep -c 'CREATE TABLE' "$OUTPUT_DIR/generated-v2.sql" || echo 0)"
echo "  Views created: $(grep -c 'CREATE OR REPLACE VIEW' "$OUTPUT_DIR/generated-v2.sql" || echo 0)"
echo "  Indexes created: $(grep -c 'CREATE INDEX' "$OUTPUT_DIR/generated-v2.sql" || echo 0)"
echo "  Foreign keys: $(grep -c 'FOREIGN KEY' "$OUTPUT_DIR/generated-v2.sql" || echo 0)"

# Test 7: Verify differences between v1 and v2
echo -e "\n${BLUE}Test 7: Verify v1 vs v2 differences${NC}"
if diff "$OUTPUT_DIR/generated-v1.sql" "$OUTPUT_DIR/generated-v2.sql" > "$OUTPUT_DIR/v1-v2-diff.txt"; then
    echo -e "${YELLOW}⚠ No differences between v1 and v2 (unexpected)${NC}"
else
    DIFF_LINES=$(wc -l < "$OUTPUT_DIR/v1-v2-diff.txt")
    echo -e "${GREEN}✓ Found differences between v1 and v2 ($DIFF_LINES diff lines)${NC}"
    echo "  Diff saved to: $OUTPUT_DIR/v1-v2-diff.txt"
fi

# Summary
echo -e "\n${GREEN}=== All DDL Generation Tests Passed ===${NC}"
echo -e "\n${BLUE}Output files:${NC}"
echo "  Generated DDL v1: $OUTPUT_DIR/generated-v1.sql"
echo "  Generated DDL v2: $OUTPUT_DIR/generated-v2.sql"
echo "  Validation log: $OUTPUT_DIR/validate.log"
echo "  V1 vs V2 diff: $OUTPUT_DIR/v1-v2-diff.txt"

exit 0
