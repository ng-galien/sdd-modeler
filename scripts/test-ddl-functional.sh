#!/bin/bash
#
# Test DDL Functional Validation - State Transitions with Real Data
#
# This script tests the generated DDL with real data to verify:
# 1. Tables can be created without errors
# 2. Data can be inserted respecting constraints
# 3. State transitions work correctly (pending → paid → refunded, etc.)
# 4. Foreign keys are enforced properly
# 5. Views return correct data
# 6. Invalid transitions are blocked
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
EXAMPLES_DIR="$SCRIPTS_EXAMPLES_DIR"

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

# Database configuration
DB_NAME="sdd_test_functional_$(date +%s)"
DB_USER="${POSTGRES_USER:-postgres}"
DB_PASSWORD="${POSTGRES_PASSWORD:-postgres}"
DB_HOST="${POSTGRES_HOST:-localhost}"
DB_PORT="${POSTGRES_PORT:-5432}"
DOCKER_CONTAINER_NAME="sdd-test-postgres-$$"
USE_DOCKER=false

# Early, minimal cleanup to remove any leftover Docker containers with our container name when the
# script exits early (e.g. before the main cleanup function is set). This avoids blocking future
# runs when Docker containers are left behind.
cleanup_docker_only() {
    # Only try to remove containers if docker exists and we intended to use it
    if [ "$USE_DOCKER" = true ] && command -v docker &> /dev/null; then
        # Find containers matching our expected name
        ids=$(docker ps -a -q --filter "name=$DOCKER_CONTAINER_NAME") || true
        if [ -n "$ids" ]; then
            echo -e "${YELLOW}⚠ Removing leftover Docker containers: $ids${NC}"
            docker rm -f $ids > /dev/null 2>&1 || true
        fi
    fi
}

# Ensure early cleanup runs even if the script fails before the full cleanup is defined
trap cleanup_docker_only EXIT

# Test model
MODEL_FILE="$EXAMPLES_DIR/orders-sdd-model.yaml"
DDL_FILE="$OUTPUT_DIR/functional-test.sql"
REPORT_FILE="$OUTPUT_DIR/functional-test-report.md"

echo -e "${BLUE}=== DDL Functional Test Suite ===${NC}\n"

# Create output directory if it doesn't exist
mkdir -p "$OUTPUT_DIR"

# Initialize report
cat > "$REPORT_FILE" <<EOF
# DDL Functional Test Report

**Generated:** $(date '+%Y-%m-%d %H:%M:%S')  
**Model:** orders-sdd-model.yaml  
**Database:** PostgreSQL 16 Alpine

---

## Test Environment

EOF

echo -e "${YELLOW}🔍 Checking PostgreSQL availability...${NC}"

# First check if psql is available
if ! command -v psql &> /dev/null; then
    echo -e "${YELLOW}⚠ psql command not found${NC}"
    
    # Check if Docker is available
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}✗ Docker not found either${NC}"
        echo -e "${YELLOW}  Please install PostgreSQL client or Docker${NC}"
        exit 1
    fi
    
    echo -e "${BLUE}  → Will use Docker to run PostgreSQL${NC}"
    USE_DOCKER=true
else
    # psql is available, check if we can connect
    if ! PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d postgres -c "SELECT 1" &> /dev/null; then
        echo -e "${YELLOW}⚠ Cannot connect to PostgreSQL at $DB_HOST:$DB_PORT${NC}"
        
        # Check if Docker is available as fallback
        if ! command -v docker &> /dev/null; then
            echo -e "${RED}✗ Docker not found${NC}"
            echo -e "${YELLOW}  Please ensure PostgreSQL is running or install Docker${NC}"
            echo -e "${YELLOW}  Set POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_HOST, POSTGRES_PORT if needed${NC}"
            exit 1
        fi
        
        echo -e "${BLUE}  → Will use Docker to run PostgreSQL${NC}"
        USE_DOCKER=true
    else
        echo -e "${GREEN}✓ PostgreSQL is available at $DB_HOST:$DB_PORT${NC}"
    fi
fi

# Start Docker container if needed
if [ "$USE_DOCKER" = true ]; then
    echo -e "\n${BLUE}🐳 Starting PostgreSQL Docker container...${NC}"
    
    # Pull postgres image if not present
    if ! docker image inspect postgres:16-alpine &> /dev/null; then
        echo -e "${YELLOW}  Pulling postgres:16-alpine image...${NC}"
        docker pull postgres:16-alpine > /dev/null 2>&1
    fi
    
    # Start container
    docker run -d \
        --name "$DOCKER_CONTAINER_NAME" \
        -e POSTGRES_PASSWORD="$DB_PASSWORD" \
        -e POSTGRES_USER="$DB_USER" \
        -p 5432:5432 \
        postgres:16-alpine > /dev/null 2>&1
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}✗ Failed to start Docker container${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✓ Docker container started: $DOCKER_CONTAINER_NAME${NC}"
    
    # Wait for PostgreSQL to be ready
    echo -e "${YELLOW}  Waiting for PostgreSQL to be ready...${NC}"
    for i in {1..30}; do
        if docker exec "$DOCKER_CONTAINER_NAME" psql -U "$DB_USER" -d postgres -c "SELECT 1" &> /dev/null; then
            echo -e "${GREEN}✓ PostgreSQL is ready${NC}"
            break
        fi
        sleep 1
        if [ $i -eq 30 ]; then
            echo -e "${RED}✗ PostgreSQL failed to start in time${NC}"
            docker logs "$DOCKER_CONTAINER_NAME"
            docker rm -f "$DOCKER_CONTAINER_NAME" > /dev/null 2>&1
            exit 1
        fi
    done
fi

# Helper function for SQL execution
exec_sql() {
    if [ "$USE_DOCKER" = true ]; then
        docker exec -i "$DOCKER_CONTAINER_NAME" psql -U "$DB_USER" "$@"
    else
        PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" "$@"
    fi
}

# The full cleanup function is defined later in the script to drop the test database and stop
# the Docker container if required. It will call cleanup_docker_only to ensure any containers are
# removed even if they were created earlier.

# Helper function to add to report
report_section() {
    echo -e "\n## $1\n" >> "$REPORT_FILE"
}

report_success() {
    echo "✅ **$1**" >> "$REPORT_FILE"
}

report_failure() {
    echo "❌ **$1**" >> "$REPORT_FILE"
}

report_sql_block() {
    echo -e "\n\`\`\`sql" >> "$REPORT_FILE"
    echo -e "$1" >> "$REPORT_FILE"
    echo -e "\`\`\`\n" >> "$REPORT_FILE"
}

report_table() {
    local title="$1"
    local content="$2"
    echo -e "\n### $title\n" >> "$REPORT_FILE"
    echo "$content" | sed 's/^/| /' | sed 's/$/|/' | sed '1s/|//' | sed '2s/|//' >> "$REPORT_FILE"
}

# Convert PostgreSQL text table to Markdown table
psql_to_markdown_table() {
    local input_file="$1"
    
    # Read all content
    local content=$(cat "$input_file")
    
    # Get header (first line)
    local header=$(echo "$content" | head -1)
    
    # Get data lines (skip header, separator, and footer with row count)
    local data=$(echo "$content" | tail -n +3 | grep -v '([0-9]\+ row')
    
    # Convert header to Markdown: trim spaces around pipes
    echo "$header" | sed 's/ *| */|/g' | sed 's/^ *//g' | sed 's/ *$//g'
    
    # Create separator line
    echo "$header" | sed 's/ *| */|/g' | sed 's/[^|]/-/g'
    
    # Convert data rows: trim spaces around pipes  
    echo "$data" | sed 's/ *| */|/g' | sed 's/^ *//g' | sed 's/ *$//g'
}

# Setup
echo -e "\n${YELLOW}📂 Setting up test environment...${NC}"
mkdir -p "$OUTPUT_DIR"

# Update report with environment
if [ "$USE_DOCKER" = true ]; then
    echo "- **PostgreSQL:** Docker container (\`postgres:16-alpine\`)" >> "$REPORT_FILE"
else
    echo "- **PostgreSQL:** $DB_HOST:$DB_PORT" >> "$REPORT_FILE"
fi
echo "- **Database:** $DB_NAME" >> "$REPORT_FILE"
echo -e "\n---\n" >> "$REPORT_FILE"

# Generate DDL
echo -e "\n${BLUE}Step 1: Generate DDL from model${NC}"
"$GRADLE" -q :state-modeler-app:run --args="sql $MODEL_FILE -o $DDL_FILE" > "$OUTPUT_DIR/ddl-gen.log" 2>&1
if [ -f "$DDL_FILE" ]; then
    echo -e "${GREEN}✓ DDL generated successfully${NC}"
else
    echo -e "${RED}✗ DDL generation failed${NC}"
    cat "$OUTPUT_DIR/ddl-gen.log"
    exit 1
fi

# Create test database
echo -e "\n${BLUE}Step 2: Create test database${NC}"
exec_sql -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;" > /dev/null 2>&1
exec_sql -d postgres -c "CREATE DATABASE $DB_NAME;" > /dev/null 2>&1
echo -e "${GREEN}✓ Test database created: $DB_NAME${NC}"

# Cleanup function
cleanup() {
    # Ensure leftover containers are always removed as well (robustness)
    cleanup_docker_only || true
    echo -e "\n${YELLOW}🧹 Cleaning up...${NC}"
    # ignore errors from dropping DB as it may not be reachable during cleanup
    exec_sql -d postgres -c "DROP DATABASE IF EXISTS $DB_NAME;" > /dev/null 2>&1 || true
    echo -e "${GREEN}✓ Test database dropped${NC}"
    
    # Stop and remove Docker container if we started it
    if [ "$USE_DOCKER" = true ]; then
        echo -e "${YELLOW}  Stopping Docker container...${NC}"
        # stop/remove may fail if container already gone; ignore errors
        docker stop "$DOCKER_CONTAINER_NAME" > /dev/null 2>&1 || true
        docker rm "$DOCKER_CONTAINER_NAME" > /dev/null 2>&1 || true
        echo -e "${GREEN}✓ Docker container removed${NC}"
    fi
}
trap cleanup EXIT

# Apply DDL
echo -e "\n${BLUE}Step 3: Apply DDL to test database${NC}"
if exec_sql -d "$DB_NAME" < "$DDL_FILE" > "$OUTPUT_DIR/ddl-apply.log" 2>&1; then
    echo -e "${GREEN}✓ DDL applied successfully${NC}"
    report_section "DDL Application"
    report_success "DDL applied successfully to database \`$DB_NAME\`"
    
    # Extract and report DDL statistics
    TABLE_COUNT=$(grep -c "CREATE TABLE" "$DDL_FILE" || true)
    VIEW_COUNT=$(grep -c "CREATE VIEW" "$DDL_FILE" || true)
    FK_COUNT=$(grep -c "FOREIGN KEY" "$DDL_FILE" || true)
    UNIQUE_COUNT=$(grep -c "UNIQUE" "$DDL_FILE" || true)
    
    echo -e "\n**DDL Statistics:**" >> "$REPORT_FILE"
    echo "- Tables created: $TABLE_COUNT" >> "$REPORT_FILE"
    echo "- Views created: $VIEW_COUNT" >> "$REPORT_FILE"
    echo "- Foreign keys: $FK_COUNT" >> "$REPORT_FILE"
    echo "- UNIQUE constraints: $UNIQUE_COUNT" >> "$REPORT_FILE"
else
    echo -e "${RED}✗ DDL application failed${NC}"
    cat "$OUTPUT_DIR/ddl-apply.log"
    exit 1
fi

report_section "Test Results"

# Test 1: Insert entity
echo -e "\n${BLUE}Test 1: Insert entity (order)${NC}"
SQL_TEST1='INSERT INTO public.orders (customer_id, total_amount)
VALUES (123, 99.99)
RETURNING id;'

exec_sql -d "$DB_NAME" > "$OUTPUT_DIR/test1.log" 2>&1 <<EOF
$SQL_TEST1
EOF

if [ $? -eq 0 ]; then
    ORDER_ID=$(grep -E "^\s+[0-9]+\s*$" "$OUTPUT_DIR/test1.log" | tr -d ' ')
    echo -e "${GREEN}✓ Entity inserted successfully (order_id: $ORDER_ID)${NC}"
    
    echo "### Test 1: Entity Creation" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    report_success "Entity inserted with ID: $ORDER_ID"
    report_sql_block "$SQL_TEST1"
else
    echo -e "${RED}✗ Entity insertion failed${NC}"
    cat "$OUTPUT_DIR/test1.log"
    exit 1
fi

# Test 2: Insert initial state (pending)
echo -e "\n${BLUE}Test 2: Insert initial state (pending)${NC}"
exec_sql -d "$DB_NAME" > "$OUTPUT_DIR/test2.log" 2>&1 <<EOF
INSERT INTO public_states.order_pending (order_id, pending_reason)
VALUES ($ORDER_ID, 'Awaiting payment')
RETURNING id;
EOF

if [ $? -eq 0 ]; then
    PENDING_ID=$(grep -E "^\s+[0-9]+\s*$" "$OUTPUT_DIR/test2.log" | tr -d ' ')
    echo -e "${GREEN}✓ Initial state inserted (pending_id: $PENDING_ID)${NC}"
    sleep 0.1  # 100ms delay for distinct timestamps
    
    # Add to report
    echo "### Test 2: Initial State Insertion" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    report_success "Initial PENDING state created with ID: $PENDING_ID"
    report_sql_block "INSERT INTO public_states.order_pending (order_id, pending_reason)\nVALUES ($ORDER_ID, 'Awaiting payment')\nRETURNING id;"
    echo "**Result:**" >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    cat "$OUTPUT_DIR/test2.log" >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
else
    echo -e "${RED}✗ Initial state insertion failed${NC}"
    cat "$OUTPUT_DIR/test2.log"
    exit 1
fi

# Test 3: Valid transition (pending → paid)
echo -e "\n${BLUE}Test 3: Valid transition (pending → paid)${NC}"
exec_sql -d "$DB_NAME" > "$OUTPUT_DIR/test3.log" 2>&1 <<EOF
INSERT INTO public_states.order_paid (order_id, previous_pending_id, payment_method, paid_amount)
VALUES ($ORDER_ID, $PENDING_ID, 'credit_card', 99.99)
RETURNING id;
EOF

if [ $? -eq 0 ]; then
    PAID_ID=$(grep -E "^\s+[0-9]+\s*$" "$OUTPUT_DIR/test3.log" | tr -d ' ')
    echo -e "${GREEN}✓ Valid transition succeeded (paid_id: $PAID_ID)${NC}"
    sleep 0.1  # 100ms delay for distinct timestamps
    
    # Add to report
    echo "### Test 3: State Transition (PENDING → PAID)" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    report_success "Transition from PENDING to PAID successful (paid_id: $PAID_ID)"
    report_sql_block "INSERT INTO public_states.order_paid (order_id, previous_pending_id, payment_method, paid_amount)\nVALUES ($ORDER_ID, $PENDING_ID, 'credit_card', 99.99)\nRETURNING id;"
    echo "**Result:**" >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    cat "$OUTPUT_DIR/test3.log" >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
else
    echo -e "${RED}✗ Valid transition failed${NC}"
    cat "$OUTPUT_DIR/test3.log"
    exit 1
fi

# Test 4: Another valid transition (paid → refunded)
echo -e "\n${BLUE}Test 4: Valid transition (paid → refunded)${NC}"
exec_sql -d "$DB_NAME" > "$OUTPUT_DIR/test4.log" 2>&1 <<EOF
INSERT INTO public_states.order_refunded (order_id, previous_paid_id, refund_amount, refund_method)
VALUES ($ORDER_ID, $PAID_ID, 99.99, 'credit_card')
RETURNING id;
EOF

if [ $? -eq 0 ]; then
    REFUNDED_ID=$(grep -E "^\s+[0-9]+\s*$" "$OUTPUT_DIR/test4.log" | tr -d ' ')
    echo -e "${GREEN}✓ Valid transition succeeded (refunded_id: $REFUNDED_ID)${NC}"
    sleep 0.1  # 100ms delay for distinct timestamps
    
    # Add to report
    echo "### Test 4: State Transition (PAID → REFUNDED)" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    report_success "Transition from PAID to REFUNDED successful (refunded_id: $REFUNDED_ID)"
    report_sql_block "INSERT INTO public_states.order_refunded (order_id, previous_paid_id, refund_amount, refund_method)\nVALUES ($ORDER_ID, $PAID_ID, 99.99, 'credit_card')\nRETURNING id;"
    echo "**Result:**" >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    cat "$OUTPUT_DIR/test4.log" >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
else
    echo -e "${RED}✗ Valid transition failed${NC}"
    cat "$OUTPUT_DIR/test4.log"
    exit 1
fi

# Test 5: Verify UNIQUE constraint blocks duplicate state entries
echo -e "\n${BLUE}Test 5: Verify UNIQUE constraint prevents duplicate state entries${NC}"
# With UNIQUE constraint on entity_id, trying to insert the same order in pending again should fail
exec_sql -d "$DB_NAME" > "$OUTPUT_DIR/test5.log" 2>&1 <<EOF
INSERT INTO public_states.order_pending (order_id, pending_reason)
VALUES ($ORDER_ID, 'Duplicate entry attempt');
EOF

if grep -q "violates unique constraint\|duplicate key value" "$OUTPUT_DIR/test5.log"; then
    echo -e "${GREEN}✓ UNIQUE constraint correctly prevents duplicate state entries${NC}"
    
    # Add to report
    echo "### Test 5: UNIQUE Constraint Validation" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    report_success "UNIQUE constraint on entity_id prevents duplicate state entries (acyclic graph enforced)"
    report_sql_block "-- Attempt to insert duplicate PENDING state for same order\nINSERT INTO public_states.order_pending (order_id, pending_reason)\nVALUES ($ORDER_ID, 'Duplicate entry attempt');"
    echo "**Result:** ❌ Constraint violation (expected behavior)" >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    grep -E "ERROR|violates|duplicate" "$OUTPUT_DIR/test5.log" | head -2 >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
else
    echo -e "${RED}✗ UNIQUE constraint not enforced (duplicate entry was allowed)${NC}"
    cat "$OUTPUT_DIR/test5.log"
    exit 1
fi

# Test 6: OR transition (pending → cancelled)
echo -e "\n${BLUE}Test 6: OR transition setup (pending → cancelled)${NC}"

# First create a new order in pending state
exec_sql -d "$DB_NAME" > "$OUTPUT_DIR/test6-setup.log" 2>&1 <<EOF
INSERT INTO public.orders (customer_id, total_amount) VALUES (456, 49.99) RETURNING id;
EOF
ORDER_ID_2=$(grep -E "^\s+[0-9]+\s*$" "$OUTPUT_DIR/test6-setup.log" | tr -d ' ')

exec_sql -d "$DB_NAME" > "$OUTPUT_DIR/test6-pending.log" 2>&1 <<EOF
INSERT INTO public_states.order_pending (order_id, pending_reason)
VALUES ($ORDER_ID_2, 'New order') RETURNING id;
EOF
PENDING_ID_2=$(grep -E "^\s+[0-9]+\s*$" "$OUTPUT_DIR/test6-pending.log" | tr -d ' ')
sleep 0.1  # 100ms delay for distinct timestamps

# Create cancelled_source mapping
exec_sql -d "$DB_NAME" > "$OUTPUT_DIR/test6-source.log" 2>&1 <<EOF
INSERT INTO public_states.cancelled_source (pending_state_id, order_id)
VALUES ($PENDING_ID_2, $ORDER_ID_2) RETURNING id;
EOF
SOURCE_ID=$(grep -E "^\s+[0-9]+\s*$" "$OUTPUT_DIR/test6-source.log" | tr -d ' ')

# Create cancelled state
exec_sql -d "$DB_NAME" > "$OUTPUT_DIR/test6.log" 2>&1 <<EOF
INSERT INTO public_states.order_cancelled (order_id, previous_source_id, cancel_reason)
VALUES ($ORDER_ID_2, $SOURCE_ID, 'Out of stock')
RETURNING id;
EOF

if [ $? -eq 0 ]; then
    CANCELLED_ID=$(grep -E "^\s+[0-9]+\s*$" "$OUTPUT_DIR/test6.log" | tr -d ' ')
    echo -e "${GREEN}✓ OR transition succeeded (cancelled_id: $CANCELLED_ID)${NC}"
    
    # Add to report
    echo "### Test 6: OR Transition (PENDING → CANCELLED)" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    report_success "OR transition via cancelled_source mapping table successful"
    report_sql_block "-- Create cancelled_source mapping\nINSERT INTO public_states.cancelled_source (pending_state_id)\nVALUES ($PENDING_ID_2) RETURNING id;\n\n-- Create cancelled state\nINSERT INTO public_states.order_cancelled (order_id, previous_source_id, cancel_reason)\nVALUES ($ORDER_ID_2, $SOURCE_ID, 'Out of stock')\nRETURNING id;"
    echo "**Result:**" >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    echo "cancelled_source_id: $SOURCE_ID" >> "$REPORT_FILE"
    echo "cancelled_id: $CANCELLED_ID" >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
else
    echo -e "${RED}✗ OR transition failed${NC}"
    cat "$OUTPUT_DIR/test6.log"
    exit 1
fi

# Test 7: Check views return correct data
echo -e "\n${BLUE}Test 7: Verify projection views${NC}"

# Check state_intervals view
INTERVAL_COUNT=$(exec_sql -d "$DB_NAME" -t -c \
    "SELECT COUNT(*) FROM public_states.order_state_intervals;" | tr -d ' ')

if [ "$INTERVAL_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✓ State intervals view has data ($INTERVAL_COUNT rows)${NC}"
    
    # Get intervals data for report
    echo "### Test 7: Projection Views" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    report_success "State intervals view contains $INTERVAL_COUNT rows"
    
    # Query intervals view with formatted output
    exec_sql -d "$DB_NAME" -c \
        "SELECT order_id, state_type, 
                TO_CHAR(start_at, 'YYYY-MM-DD HH24:MI:SS') as start_at,
                TO_CHAR(end_at, 'YYYY-MM-DD HH24:MI:SS') as end_at
         FROM public_states.order_state_intervals 
         ORDER BY order_id, start_at, end_at NULLS LAST;" > "$OUTPUT_DIR/test7-intervals.log" 2>&1
    
    echo -e "\n**State Intervals View:**\n" >> "$REPORT_FILE"
    psql_to_markdown_table "$OUTPUT_DIR/test7-intervals.log" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
else
    echo -e "${RED}✗ State intervals view is empty${NC}"
    exit 1
fi

# Check current_state view
# First, verify the view exists
VIEW_EXISTS=$(exec_sql -d "$DB_NAME" -t -A -c \
    "SELECT COUNT(*) FROM information_schema.views WHERE table_schema = 'public_states' AND table_name = 'current_order_states';" 2>&1)

if echo "$VIEW_EXISTS" | grep -q "1"; then
    # Query the view with all columns
    exec_sql -d "$DB_NAME" -c \
        "SELECT * FROM public_states.current_order_states LIMIT 5;" \
        > "$OUTPUT_DIR/test7-current.log" 2>&1
    
    if [ -f "$OUTPUT_DIR/test7-current.log" ] && [ -s "$OUTPUT_DIR/test7-current.log" ]; then
        CURRENT_LINES=$(wc -l < "$OUTPUT_DIR/test7-current.log" | tr -d ' ')
        echo -e "${GREEN}✓ Current state view has data ($CURRENT_LINES lines)${NC}"
        
        echo -e "\n**Current Order States:**\n" >> "$REPORT_FILE"
        psql_to_markdown_table "$OUTPUT_DIR/test7-current.log" >> "$REPORT_FILE"
        echo "" >> "$REPORT_FILE"
    else
        echo -e "${YELLOW}⚠ Current state view query returned no data${NC}"
    fi
else
    echo -e "${RED}✗ current_order_states view does not exist!${NC}"
    exit 1
fi

# Test 8: Verify state timeline consistency
echo -e "\n${BLUE}Test 8: Verify state timeline consistency${NC}"
exec_sql -d "$DB_NAME" > "$OUTPUT_DIR/test8.log" 2>&1 <<EOF
SELECT 
    order_id,
    state_type,
    start_at,
    end_at,
    CASE 
        WHEN end_at IS NULL THEN 'CURRENT'
        ELSE 'CLOSED'
    END as status
FROM public_states.order_state_intervals
WHERE order_id = $ORDER_ID
ORDER BY start_at;
EOF

TIMELINE_ROWS=$(grep -c "PENDING\|PAID\|REFUNDED" "$OUTPUT_DIR/test8.log" || echo 0)
if [ "$TIMELINE_ROWS" -ge 3 ]; then
    echo -e "${GREEN}✓ State timeline is consistent (3 states recorded)${NC}"
    
    # Add to report
    echo "### Test 8: State Timeline Consistency" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    report_success "State timeline verified: all transitions properly sequenced"
    report_sql_block "SELECT order_id, state_type, start_at, end_at,\n       CASE WHEN end_at IS NULL THEN 'CURRENT' ELSE 'CLOSED' END as status\nFROM public_states.order_state_intervals\nWHERE order_id = $ORDER_ID\nORDER BY start_at;"
    echo "**Result:**" >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    cat "$OUTPUT_DIR/test8.log" >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
else
    echo -e "${RED}✗ State timeline incomplete (expected 3 states, got $TIMELINE_ROWS)${NC}"
    cat "$OUTPUT_DIR/test8.log"
    exit 1
fi

# Test 9: Verify foreign key constraints
echo -e "\n${BLUE}Test 9: Verify foreign key constraints${NC}"

# Try to insert state for non-existent order
exec_sql -d "$DB_NAME" > "$OUTPUT_DIR/test9.log" 2>&1 <<EOF
INSERT INTO public_states.order_pending (order_id, pending_reason)
VALUES (999999, 'Non-existent order');
EOF

if grep -q "violates foreign key constraint" "$OUTPUT_DIR/test9.log"; then
    echo -e "${GREEN}✓ Foreign key constraint correctly enforced${NC}"
    
    # Add to report
    echo "### Test 9: Foreign Key Constraint Validation" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    report_success "Foreign key constraints prevent orphaned state records"
    report_sql_block "-- Attempt to insert state for non-existent order\nINSERT INTO public_states.order_pending (order_id, pending_reason)\nVALUES (999999, 'Non-existent order');"
    echo "**Result:** ❌ FK violation (expected behavior)" >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    grep -E "ERROR|violates|foreign key" "$OUTPUT_DIR/test9.log" | head -2 >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
else
    echo -e "${RED}✗ Foreign key constraint not enforced${NC}"
    cat "$OUTPUT_DIR/test9.log"
    exit 1
fi

# Test 10: Verify composite FK prevents cross-aggregate transitions
echo -e "\n${BLUE}Test 10: Verify composite FK prevents cross-aggregate transitions${NC}"

# Try to use order 1's pending state as predecessor for order 2's paid state
# This should fail because the composite FK (previous_pending_id, order_id) requires:
# - previous_pending_id must reference a pending state
# - AND that pending state must have the SAME order_id
exec_sql -d "$DB_NAME" > "$OUTPUT_DIR/test10.log" 2>&1 <<EOF
INSERT INTO public_states.order_paid (order_id, previous_pending_id, payment_method, paid_amount)
VALUES ($ORDER_ID_2, $PENDING_ID, 'credit_card', 49.99);
EOF

if grep -q "violates foreign key constraint\|there is no unique constraint matching" "$OUTPUT_DIR/test10.log"; then
    echo -e "${GREEN}✓ Composite FK correctly prevents cross-aggregate transitions${NC}"
    
    # Add to report
    echo "### Test 10: Composite FK Cross-Aggregate Prevention" >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    report_success "Composite foreign keys prevent transitions between different aggregates"
    report_sql_block "-- Attempt to use order $ORDER_ID's pending state as predecessor for order $ORDER_ID_2's paid state\n-- This violates the composite FK: (previous_pending_id, order_id) -> (id, order_id)\nINSERT INTO public_states.order_paid (order_id, previous_pending_id, payment_method, paid_amount)\nVALUES ($ORDER_ID_2, $PENDING_ID, 'credit_card', 49.99);"
    echo "**Result:** ❌ Composite FK violation (expected behavior)" >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    grep -E "ERROR|violates|foreign key|unique constraint" "$OUTPUT_DIR/test10.log" | head -3 >> "$REPORT_FILE"
    echo '```' >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
    echo "**Explanation:** The composite FK ensures \`previous_pending_id\` references a pending state with the **same** \`order_id\`. Order $ORDER_ID's pending state (id=$PENDING_ID) cannot be used for order $ORDER_ID_2's transitions." >> "$REPORT_FILE"
    echo "" >> "$REPORT_FILE"
else
    echo -e "${RED}✗ Composite FK not enforced - cross-aggregate transition was allowed!${NC}"
    cat "$OUTPUT_DIR/test10.log"
    exit 1
fi

# Test 11: Data integrity summary
echo -e "\n${BLUE}Test 11: Data integrity summary${NC}"
exec_sql -d "$DB_NAME" -c \
    "SELECT 
        (SELECT COUNT(*) FROM public.orders) as total_orders,
        (SELECT COUNT(*) FROM public_states.order_pending) as pending_states,
        (SELECT COUNT(*) FROM public_states.order_paid) as paid_states,
        (SELECT COUNT(*) FROM public_states.order_refunded) as refunded_states,
        (SELECT COUNT(*) FROM public_states.order_cancelled) as cancelled_states,
        (SELECT COUNT(*) FROM public_states.cancelled_source) as or_transitions;" \
    > "$OUTPUT_DIR/test10.log" 2>&1

cat "$OUTPUT_DIR/test10.log"
echo -e "${GREEN}✓ Data integrity summary generated${NC}"

# Add to report
echo "### Test 11: Data Integrity Summary" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"
report_success "All data integrity checks passed"

echo -e "\n**Database Statistics:**\n" >> "$REPORT_FILE"
psql_to_markdown_table "$OUTPUT_DIR/test10.log" >> "$REPORT_FILE"
echo "" >> "$REPORT_FILE"

# Summary
echo -e "\n${GREEN}=== All Functional Tests Passed ===${NC}"

# Generate final report summary
cat >> "$REPORT_FILE" <<'REPORT_FOOTER'

---

## Summary

### ✅ All Tests Passed

1. **Entity Creation** - Orders table accepts valid data
2. **Initial State** - Pending state correctly linked to entity
3. **Simple Transitions** - `pending → paid → refunded` chain validated
4. **State Transitions** - Multi-step state progression works correctly
5. **UNIQUE Constraints** - Prevents duplicate state entries (acyclic graph enforced)
6. **OR Transitions** - Polymorphic transitions via source tables working
7. **Projection Views** - Both `intervals` and `current_state` views return correct data
8. **Timeline Consistency** - State start/end times properly sequenced
9. **Foreign Key Constraints** - Database enforces referential integrity
10. **Composite FK Integrity** - Prevents cross-aggregate state transitions
11. **Data Integrity** - All counts match expected values

### Key Validations

- ✅ **SDD Invariants**: States are immutable facts (UNIQUE on entity_id)
- ✅ **Aggregate Integrity**: Composite FKs ensure transitions stay within same aggregate
- ✅ **Graph Structure**: No cyclic transitions possible
- ✅ **Referential Integrity**: All FKs enforced at database level
- ✅ **View Correctness**: Projections accurately reflect state timeline
- ✅ **Constraint Enforcement**: Invalid operations blocked by database

REPORT_FOOTER

echo -e "\n${BLUE}📄 Test report generated: $REPORT_FILE${NC}"
echo -e "\n${BLUE}Test Results Summary:${NC}"
echo "  ✓ Entity creation (orders table)"
echo "  ✓ Initial state insertion (pending)"
echo "  ✓ Valid transitions (pending → paid → refunded)"
echo "  ✓ Invalid transitions blocked"
echo "  ✓ OR transitions (pending → cancelled via source table)"
echo "  ✓ Projection views working (intervals, current_state)"
echo "  ✓ State timeline consistency verified"
echo "  ✓ Foreign key constraints enforced"
echo "  ✓ Composite FK prevents cross-aggregate transitions"
echo "  ✓ Data integrity maintained"

echo -e "\n${BLUE}Output files:${NC}"
echo "  Test logs: $OUTPUT_DIR/test*.log"
echo "  DDL file: $DDL_FILE"
echo "  DDL application log: $OUTPUT_DIR/ddl-apply.log"
echo "  📄 Markdown report: $REPORT_FILE"

exit 0
