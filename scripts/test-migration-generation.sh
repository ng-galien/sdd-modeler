#!/bin/bash
#
# Test Migration Generation with LLM
#
# This script tests the LLM-based migration generation:
# 1. Check if Ollama is available
# 2. Register two model versions in SDR
# 3. Generate migration using Ollama with qwen2.5:0.5b model
# 4. Generate a detailed report with DDL and LLM response
#
# Requirements:
# - Ollama server running (ollama serve)
# - qwen2.5:0.5b model available (ollama pull qwen2.5:0.5b)
#

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
EXAMPLES_DIR="$PROJECT_ROOT/instructions/examples"
OUTPUT_DIR="$PROJECT_ROOT/build/test-migration-output"
REPORT_FILE="$OUTPUT_DIR/migration-report.md"
GRADLE="$PROJECT_ROOT/gradlew"

# Test files
MODEL_V1="$EXAMPLES_DIR/orders-sdd-model.yaml"
MODEL_V2="$EXAMPLES_DIR/orders-sdd-model-v2.yaml"

# LLM configuration
# Read OPENAI_API_KEY from environment if present, CLI --openai-key overrides
OPENAI_API_KEY="${OPENAI_API_KEY:-}"
# Default provider: use OpenAI if API key is present, otherwise use Ollama
if [[ -n "$OPENAI_API_KEY" ]]; then
    LLM_PROVIDER="openai"
    LLM_MODEL="gpt-4o-mini"
else
    LLM_PROVIDER="ollama"
    LLM_MODEL="qwen3:8b"
fi
OLLAMA_URL="http://localhost:11434"

echo -e "${BLUE}╔════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  LLM Migration Generation Test Suite      ║${NC}"
echo -e "${BLUE}╔════════════════════════════════════════════╗${NC}\n"

# Setup
echo -e "${YELLOW}📂 Setting up test environment...${NC}"
mkdir -p "$OUTPUT_DIR"
rm -f "$OUTPUT_DIR"/*.sql "$OUTPUT_DIR"/*.md "$OUTPUT_DIR"/*.log

# Parse CLI args (allow overriding provider/model/urls/keys)
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --llm)
            LLM_PROVIDER="$2"; shift 2;;
        --model)
            LLM_MODEL="$2"; shift 2;;
        --ollama-url)
            OLLAMA_URL="$2"; shift 2;;
        --openai-key)
            OPENAI_API_KEY="$2"; shift 2;;
        -h|--help)
            echo "Usage: $0 [--llm ollama|openai] [--model <model>] [--ollama-url <url>] [--openai-key <key>]"; exit 0;;
        *)
            echo "Unknown option: $1"; exit 1;;
    esac
done

if [[ -n "$OPENAI_API_KEY" && "$LLM_PROVIDER" == "openai" ]]; then
    echo -e "${YELLOW}Detected OPENAI_API_KEY in environment; defaulting to OpenAI provider. Use --llm to override.${NC}"
fi

# Validate provider-specific settings and availability
echo -e "\n${BLUE}Step 1: Checking LLM provider availability (${LLM_PROVIDER})${NC}"
if [[ "$LLM_PROVIDER" == "ollama" ]]; then
    if ! curl -s "$OLLAMA_URL/api/version" > /dev/null 2>&1; then
        echo -e "${RED}✗ Ollama server not available at $OLLAMA_URL${NC}"
        echo -e "${YELLOW}  Please start Ollama with: ollama serve${NC}"
        echo -e "${YELLOW}  Skipping LLM migration test${NC}"
        exit 0
    fi
    echo -e "${GREEN}✓ Ollama server is running${NC}"
elif [[ "$LLM_PROVIDER" == "openai" ]]; then
    # If not provided via CLI, fallback to environment variable
    if [[ -z "$OPENAI_API_KEY" ]]; then
        echo -e "${YELLOW}⚠ OPENAI_API_KEY not set; tests requiring OpenAI will be skipped unless provided via --openai-key or env var.${NC}"
        echo -e "${YELLOW}  Skipping OpenAI migration test${NC}"
        exit 0
    fi
    export OPENAI_API_KEY
    echo -e "${GREEN}✓ OPENAI_API_KEY is set${NC}"
else
    echo -e "${RED}✗ Unsupported LLM provider: ${LLM_PROVIDER}${NC}"; exit 1
fi

if [[ "$LLM_PROVIDER" == "ollama" ]]; then
    # Check if model is available on Ollama server
    echo -e "\n${BLUE}Step 2: Checking if model '$LLM_MODEL' is available${NC}"
    if ! curl -s "$OLLAMA_URL/api/tags" | grep -q "\"name\":\"$LLM_MODEL\""; then
        echo -e "${YELLOW}⚠ Model '$LLM_MODEL' not found${NC}"
        echo -e "${YELLOW}  Attempting to pull model...${NC}"
        if ! ollama pull "$LLM_MODEL" > "$OUTPUT_DIR/model-pull.log" 2>&1; then
            echo -e "${RED}✗ Failed to pull model '$LLM_MODEL'${NC}"
            echo -e "${YELLOW}  Please run: ollama pull $LLM_MODEL${NC}"
            echo -e "${YELLOW}  Skipping LLM migration test${NC}"
            exit 0
        fi
        echo -e "${GREEN}✓ Model '$LLM_MODEL' pulled successfully${NC}"
    else
        echo -e "${GREEN}✓ Model '$LLM_MODEL' is available${NC}"
    fi
fi

# Generate DDL for both versions
echo -e "\n${BLUE}Step 3: Generating DDL for both model versions${NC}"

echo -e "${CYAN}  Generating DDL for v1...${NC}"
"$GRADLE" -q :state-modeler-app:run --args="sql $MODEL_V1 -o $OUTPUT_DIR/orders-v1.sql" > "$OUTPUT_DIR/sql-v1.log" 2>&1
echo -e "${GREEN}  ✓ DDL v1 generated${NC}"

echo -e "${CYAN}  Generating DDL for v2...${NC}"
"$GRADLE" -q :state-modeler-app:run --args="sql $MODEL_V2 -o $OUTPUT_DIR/orders-v2.sql" > "$OUTPUT_DIR/sql-v2.log" 2>&1
echo -e "${GREEN}  ✓ DDL v2 generated${NC}"

# Register models in SDR
echo -e "\n${BLUE}Step 4: Registering models in SDR repository${NC}"

# Clean up SDR repository for a fresh start
echo -e "${CYAN}  Cleaning up SDR repository...${NC}"
SDR_REPO_PATH="${HOME}/.sdd-modeler/repository.mv.db"
if [ -f "$SDR_REPO_PATH" ]; then
    rm -f "${HOME}/.sdd-modeler/repository"*.db
    echo -e "${GREEN}  ✓ Repository cleaned${NC}"
fi

echo -e "${CYAN}  Registering v1 as 'orders:1.0'...${NC}"
if ! "$GRADLE" -q :state-modeler-app:run --args="register $MODEL_V1 -n orders -v 1.0" > "$OUTPUT_DIR/register-v1.log" 2>&1; then
    echo -e "${RED}✗ Failed to register v1${NC}"
    cat "$OUTPUT_DIR/register-v1.log"
    exit 1
fi
V1_HASH=$(grep -o "hash: [a-f0-9]*" "$OUTPUT_DIR/register-v1.log" | head -1 | cut -d' ' -f2 || echo "unknown")
echo -e "${GREEN}  ✓ Registered with hash: $V1_HASH${NC}"

echo -e "${CYAN}  Registering v2 as 'orders:2.0'...${NC}"
if ! "$GRADLE" -q :state-modeler-app:run --args="register $MODEL_V2 -n orders -v 2.0" > "$OUTPUT_DIR/register-v2.log" 2>&1; then
    echo -e "${RED}✗ Failed to register v2${NC}"
    cat "$OUTPUT_DIR/register-v2.log"
    exit 1
fi
V2_HASH=$(grep -o "hash: [a-f0-9]*" "$OUTPUT_DIR/register-v2.log" | head -1 | cut -d' ' -f2 || echo "unknown")
echo -e "${GREEN}  ✓ Registered with hash: $V2_HASH${NC}"

# Generate migration with LLM
echo -e "\n${BLUE}Step 5: Generating migration with ${LLM_PROVIDER} (${LLM_MODEL})${NC}"
echo -e "${YELLOW}  This may take 30-60 seconds depending on model size...${NC}"

START_TIME=$(date +%s)
MIGRATE_ARGS=("migrate" "orders:1.0" "orders:2.0" "--llm" "$LLM_PROVIDER" "--model" "$LLM_MODEL" "-o" "$OUTPUT_DIR/migration.sql")
if "$GRADLE" -q :state-modeler-app:run --args="${MIGRATE_ARGS[*]}" > "$OUTPUT_DIR/migrate.log" 2>&1; then
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    echo -e "${GREEN}✓ Migration generated successfully in ${DURATION}s${NC}"
else
    echo -e "${RED}✗ Migration generation failed${NC}"
    cat "$OUTPUT_DIR/migrate.log"
    exit 1
fi

# Extract migration details from log
CONFIDENCE=$(grep -o "confidence: [0-9.]*" "$OUTPUT_DIR/migrate.log" | head -1 | cut -d' ' -f2 || echo "N/A")
COMMENTS=$(grep "comments:" "$OUTPUT_DIR/migrate.log" | sed 's/.*comments: //' || echo "N/A")

# Generate detailed report
echo -e "\n${BLUE}Step 6: Generating migration report${NC}"

cat > "$REPORT_FILE" <<EOF
# LLM Migration Generation Test Report

**Generated:** $(date '+%Y-%m-%d %H:%M:%S')  
**LLM Model:** $LLM_MODEL  
**Ollama URL:** $OLLAMA_URL  
**Generation Time:** ${DURATION}s

---

## Test Summary

✅ **Status:** SUCCESS  
📊 **LLM Confidence:** $CONFIDENCE  
🔄 **Migration Path:** orders:1.0 → orders:2.0  

---

## Model Versions

### Version 1.0 (Hash: $V1_HASH)

**Source:** \`orders-sdd-model.yaml\`

<details>
<summary>📄 View DDL v1</summary>

\`\`\`sql
$(cat "$OUTPUT_DIR/orders-v1.sql")
\`\`\`

</details>

### Version 2.0 (Hash: $V2_HASH)

**Source:** \`orders-sdd-model-v2.yaml\`

<details>
<summary>📄 View DDL v2</summary>

\`\`\`sql
$(cat "$OUTPUT_DIR/orders-v2.sql")
\`\`\`

</details>

---

## LLM Generated Migration

### Migration Script

\`\`\`sql
$(cat "$OUTPUT_DIR/migration.sql")
\`\`\`

### LLM Analysis

**Confidence Score:** $CONFIDENCE / 1.0

**Comments:**
> $COMMENTS

---

## Technical Details

### Registration Logs

<details>
<summary>Version 1.0 Registration</summary>

\`\`\`
$(cat "$OUTPUT_DIR/register-v1.log")
\`\`\`

</details>

<details>
<summary>Version 2.0 Registration</summary>

\`\`\`
$(cat "$OUTPUT_DIR/register-v2.log")
\`\`\`

</details>

### Migration Generation Log

<details>
<summary>Full Migration Log</summary>

\`\`\`
$(cat "$OUTPUT_DIR/migrate.log")
\`\`\`

</details>

---

## Files Generated

- **DDL v1:** \`$OUTPUT_DIR/orders-v1.sql\`
- **DDL v2:** \`$OUTPUT_DIR/orders-v2.sql\`
- **Migration:** \`$OUTPUT_DIR/migration.sql\`
- **Report:** \`$OUTPUT_DIR/migration-report.md\`

---

**Test completed successfully** ✅
EOF

echo -e "${GREEN}✓ Report generated at: $REPORT_FILE${NC}"

# Print summary
echo -e "\n${BLUE}╔════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  Test Completed Successfully              ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════╝${NC}\n"

echo -e "${CYAN}📊 Results Summary:${NC}"
echo -e "  • LLM Model: ${GREEN}$LLM_MODEL${NC}"
echo -e "  • Generation Time: ${GREEN}${DURATION}s${NC}"
echo -e "  • Confidence Score: ${GREEN}$CONFIDENCE${NC}"
echo -e "  • Migration Size: ${GREEN}$(wc -l < "$OUTPUT_DIR/migration.sql") lines${NC}"
echo -e "\n${CYAN}📁 Output Files:${NC}"
echo -e "  • Report: ${YELLOW}$REPORT_FILE${NC}"
echo -e "  • Migration: ${YELLOW}$OUTPUT_DIR/migration.sql${NC}"
echo -e "\n${GREEN}✓ All tests passed!${NC}\n"

# Open report in default viewer (optional)
if command -v open &> /dev/null; then
    echo -e "${YELLOW}Opening report in default viewer...${NC}"
    open "$REPORT_FILE"
fi
