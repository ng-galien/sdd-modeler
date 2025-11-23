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
DOC_EXAMPLES_DIR="$PROJECT_ROOT/instructions/examples"
TEST_EXAMPLES_DIR="$PROJECT_ROOT/state-modeler-app/src/test/resources/examples"
SCRIPTS_EXAMPLES_DIR="$PROJECT_ROOT/scripts/examples"
EXAMPLES_DIR="$SCRIPTS_EXAMPLES_DIR"
OUTPUT_DIR="$PROJECT_ROOT/build/test-migration-output"
REPORT_FILE="$OUTPUT_DIR/migration-report.md"
GRADLE="$PROJECT_ROOT/gradlew"

# Test files
MODEL_V1="$EXAMPLES_DIR/orders-sdd-model.yaml"
MODEL_V2="$EXAMPLES_DIR/orders-sdd-model-v2.yaml"
MINI_MODEL_V1="$EXAMPLES_DIR/orders-sdd-mini-model.yaml"
MINI_MODEL_V2="$EXAMPLES_DIR/orders-sdd-mini-model-v2.yaml"

# LLM configuration
# Read OPENAI_API_KEY from environment if present, CLI --openai-key overrides
# Accept several common names for env vars for convenience
OPENAI_API_KEY="${OPENAI_API_KEY:-${OPENAI_KEY:-${OPENAI_APIKEY:-}}}"
# Default provider: use OpenAI if API key is present, otherwise use Ollama
if [[ -n "$OPENAI_API_KEY" ]]; then
    LLM_PROVIDER="openai"
    LLM_MODEL="gpt-4o-mini"
else
    LLM_PROVIDER="ollama"
    LLM_MODEL="qwen3:8b"
fi

# Whether the user explicitly passed --llm (avoid overriding explicit choices)
LLM_PROVIDER_EXPLICIT=false
OLLAMA_URL="http://localhost:11434"

echo -e "${BLUE}╔════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║  LLM Migration Generation Test Suite      ║${NC}"
echo -e "${BLUE}╔════════════════════════════════════════════╗${NC}\n"

# Setup
echo -e "${YELLOW}📂 Setting up test environment...${NC}"
mkdir -p "$OUTPUT_DIR"
rm -f "$OUTPUT_DIR"/*.sql "$OUTPUT_DIR"/*.md "$OUTPUT_DIR"/*.log
# If an older report exists, back it up
if [ -f "$REPORT_FILE" ]; then
    cp "$REPORT_FILE" "$OUTPUT_DIR/migration-report-full.md" || true
fi

# Parse CLI args (allow overriding provider/model/urls/keys)
MINI=false
USE_DOC_EXAMPLES=false
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --llm)
            LLM_PROVIDER="$2"; LLM_PROVIDER_EXPLICIT=true; shift 2;;
        --model)
            LLM_MODEL="$2"; shift 2;;
        --ollama-url)
            OLLAMA_URL="$2"; shift 2;;
        --examples-doc)
            USE_DOC_EXAMPLES=true; shift 1;;
        --examples-test)
            USE_DOC_EXAMPLES=false; EXAMPLES_DIR="$TEST_EXAMPLES_DIR"; shift 1;;
        --mini)
            MINI=true; shift 1;;
        --openai-key)
            OPENAI_API_KEY="$2"; shift 2;;
        -h|--help)
            echo "Usage: $0 [--llm ollama|openai] [--model <model>] [--ollama-url <url>] [--openai-key <key>]"; exit 0;;
        *)
            echo "Unknown option: $1"; exit 1;;
    esac
done

# If specified anywhere, and user hasn't explicitly set --llm, prefer OpenAI
if [[ "$USE_DOC_EXAMPLES" == "true" ]]; then
    EXAMPLES_DIR="$DOC_EXAMPLES_DIR"
fi

if [[ -n "$OPENAI_API_KEY" && "$LLM_PROVIDER_EXPLICIT" != "true" && ( -z "$LLM_PROVIDER" || "$LLM_PROVIDER" == "ollama" ) ]]; then
    LLM_PROVIDER="openai"
    LLM_MODEL="gpt-4o-mini"
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

if [[ "$MINI" == "true" ]]; then
    echo -e "${YELLOW}Using mini schema models for readability...${NC}"
    MODEL_V1="$MINI_MODEL_V1"
    MODEL_V2="$MINI_MODEL_V2"
fi
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

echo -e "${CYAN}  Registering v1 as 'orders:1.0.0'...${NC}"
if ! "$GRADLE" -q :state-modeler-app:run --args="register $MODEL_V1 -n orders -v 1.0.0" > "$OUTPUT_DIR/register-v1.log" 2>&1; then
    echo -e "${RED}✗ Failed to register v1${NC}"
    cat "$OUTPUT_DIR/register-v1.log"
    exit 1
fi
V1_HASH=$(grep -o "hash: [a-f0-9]*" "$OUTPUT_DIR/register-v1.log" | head -1 | cut -d' ' -f2 || echo "unknown")
echo -e "${GREEN}  ✓ Registered with hash: $V1_HASH${NC}"

echo -e "${CYAN}  Registering v2 as 'orders:2.0.0'...${NC}"
if ! "$GRADLE" -q :state-modeler-app:run --args="register $MODEL_V2 -n orders -v 2.0.0" > "$OUTPUT_DIR/register-v2.log" 2>&1; then
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
MIGRATE_ARGS=("migrate" "orders:1.0.0" "orders:2.0.0" "--llm" "$LLM_PROVIDER" "--model" "$LLM_MODEL" "-o" "$OUTPUT_DIR/migration.sql" "--output-json" "$OUTPUT_DIR/llm-response.json")
if "$GRADLE" -q :state-modeler-app:run --args="${MIGRATE_ARGS[*]}" > "$OUTPUT_DIR/migrate.log" 2>&1; then
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    echo -e "${GREEN}✓ Migration generated successfully in ${DURATION}s${NC}"
    # Also call show-migration to export consolidated JSON report for the migration
    echo -e "\n${CYAN}  Fetching consolidated migration JSON via CLI (show-migration)...${NC}"
    if "$GRADLE" -q :state-modeler-app:run --args="show-migration orders:1.0.0 orders:2.0.0 --output-json $OUTPUT_DIR/show-migration.json" > "$OUTPUT_DIR/show-migration.log" 2>&1; then
        echo -e "${GREEN}  ✓ show-migration JSON written to $OUTPUT_DIR/show-migration.json${NC}"
    else
        echo -e "${YELLOW}  ⚠ show-migration failed, check $OUTPUT_DIR/show-migration.log${NC}"
    fi
else
    echo -e "${RED}✗ Migration generation failed${NC}"
    cat "$OUTPUT_DIR/migrate.log"
    exit 1
fi

# Compute compact previews and diffs for the report
DDL_PREVIEW_LINES=6
ddl_preview() {
    local file="$1"
    awk -v maxlines=$DDL_PREVIEW_LINES '
        BEGIN { inblock=0; printed=0 }
        /CREATE TABLE/ { print ""; print; inblock=1; printed=0; next }
        inblock && /\);/ { print "    -- ..."; inblock=0; next }
        inblock { if (printed < maxlines) { print; printed++ } else if (printed == maxlines) { print "    -- ...columns omitted..."; printed++ } }
    ' "$file"
}

diff -u -U 3 "$OUTPUT_DIR/orders-v1.sql" "$OUTPUT_DIR/orders-v2.sql" > "$OUTPUT_DIR/orders-ddl-udiff.txt" || true

MIGRATION_PREVIEW=$(head -n 200 "$OUTPUT_DIR/migration.sql" || true)
CONFIDENCE=""
COMMENTS=""
JSON_ORIGINAL_DDL=""
JSON_NEW_DDL=""
JSON_DIFF=""

# If CLI produced llm-response.json via --output-json, we can use it directly.
if [[ -f "$OUTPUT_DIR/llm-response.json" ]]; then
    echo -e "${GREEN}✓ Retrieved LLM JSON output from CLI: $OUTPUT_DIR/llm-response.json${NC}"
else
    # Fall back to extracting confidence/comments from migrate.log if llm-response.json is not present
    CONFIDENCE=$(grep -o "confidence: [0-9.]*" "$OUTPUT_DIR/migrate.log" | head -1 | cut -d' ' -f2 || echo "N/A")
    COMMENTS=$(grep "comments:" "$OUTPUT_DIR/migrate.log" | sed 's/.*comments: //' || echo "N/A")
    if command -v jq >/dev/null 2>&1; then
        jq -n --arg confidence "$CONFIDENCE" --arg comments "$COMMENTS" --arg migrationScript "$MIGRATION_PREVIEW" \
            '{confidence: (if ($confidence == "" or $confidence == "N/A") then null elif ($confidence|test("^[0-9]+(\\.[0-9]+)?$")) then ($confidence|tonumber) else null end), comments: $comments, migrationScript: $migrationScript}' \
            > "$OUTPUT_DIR/llm-response.json" 2>/dev/null || true
    else
        # Fallback minimal JSON (jq not available)
        printf '{"confidence": "%s", "comments": "%s", "migrationScript": "%s"}\n' "$CONFIDENCE" "$(echo "$COMMENTS" | sed 's/"/\\"/g')" "$(echo "$MIGRATION_PREVIEW" | sed 's/"/\\"/g')" > "$OUTPUT_DIR/llm-response.json" || true
    fi
fi

# Precompute previews and LLM JSON to avoid heredoc expansion issues
OLD_DDL_PREVIEW=$(ddl_preview "$OUTPUT_DIR/orders-v1.sql")
DDL_DIFF_PREVIEW=$(sed -n '1,400p' "$OUTPUT_DIR/orders-ddl-udiff.txt" 2>/dev/null || true)
LLM_JSON=$(cat "$OUTPUT_DIR/llm-response.json" 2>/dev/null || true)
SHOW_JSON=$(cat "$OUTPUT_DIR/show-migration.json" 2>/dev/null || true)

# Prefer JSON values when present (jq preferred, python fallback)
if command -v jq >/dev/null 2>&1; then
    JSON_CONF=$(jq -r '.confidence // empty' "$OUTPUT_DIR/llm-response.json" 2>/dev/null || echo "")
    JSON_COMMENTS=$(jq -r '.comments // empty' "$OUTPUT_DIR/llm-response.json" 2>/dev/null || echo "")
    JSON_MIGRATION_SCRIPT=$(jq -r '.migrationScript // empty' "$OUTPUT_DIR/llm-response.json" 2>/dev/null || echo "")
    # If we have show-migration.json with a persisted migration, prefer that structured info
    if [[ -f "$OUTPUT_DIR/show-migration.json" ]]; then
        JSON_CONF=$(jq -r '.migration.confidence // empty' "$OUTPUT_DIR/show-migration.json" 2>/dev/null || echo "$JSON_CONF")
        JSON_COMMENTS=$(jq -r '.migration.comments // empty' "$OUTPUT_DIR/show-migration.json" 2>/dev/null || echo "$JSON_COMMENTS")
        JSON_MIGRATION_SCRIPT=$(jq -r '.migration.ddl // .migration.migrationScript // empty' "$OUTPUT_DIR/show-migration.json" 2>/dev/null || echo "$JSON_MIGRATION_SCRIPT")
    fi
    # If show-migration JSON exists, prefer it for original/new DDL and diff
    if [[ -f "$OUTPUT_DIR/show-migration.json" ]]; then
        JSON_ORIGINAL_DDL=$(jq -r '.original.ddl // empty' "$OUTPUT_DIR/show-migration.json" 2>/dev/null || echo "")
        JSON_NEW_DDL=$(jq -r '.new.ddl // empty' "$OUTPUT_DIR/show-migration.json" 2>/dev/null || echo "")
        JSON_DIFF=$(jq -r '.diff // empty' "$OUTPUT_DIR/show-migration.json" 2>/dev/null || echo "")
    fi
else
    echo -e "${YELLOW}⚠ 'jq' not found. Falling back to simple parsing (less accurate). Install 'jq' for robust JSON parsing.${NC}" >&2
    JSON_CONF="$CONFIDENCE"
    JSON_COMMENTS="$COMMENTS"
    JSON_MIGRATION_SCRIPT="$MIGRATION_PREVIEW"
fi

# Use JSON values if they exist, otherwise fall back to grep results
CONFIDENCE=${JSON_CONF:-$CONFIDENCE}
COMMENTS=${JSON_COMMENTS:-$COMMENTS}
MIGRATION_SCRIPT=${JSON_MIGRATION_SCRIPT:-$MIGRATION_PREVIEW}
OLD_DDL_PREVIEW=${JSON_ORIGINAL_DDL:-$OLD_DDL_PREVIEW}
DDL_DIFF_PREVIEW=${JSON_DIFF:-$DDL_DIFF_PREVIEW}

# Generate detailed report (compact)
echo -e "\n${BLUE}Step 6: Generating migration report${NC}"

# Compact LLM Migration Report
rm -f "$REPORT_FILE"
{
    printf "**Generated:** %s\n" "$(date '+%Y-%m-%d %H:%M:%S')"
    printf "**LLM Provider:** %s\n" "$LLM_PROVIDER"
    printf "**LLM Model:** %s\n" "$LLM_MODEL"
    printf "**Generation Time:** %ss\n\n" "$DURATION"
    printf '%s\n\n' '---'
    printf "## Old DDL (preview)\n\n"
    printf '```sql\n'
    printf "%s\n" "$OLD_DDL_PREVIEW"
    printf '```\n\n'
    printf "## DDL Diff (context 3 lines)\n\n"
    printf '```diff\n'
    printf "%s\n" "$DDL_DIFF_PREVIEW"
    printf '```\n\n'
    printf "## LLM Response\n\n"
    printf "### Confidence\n\n"
    printf "%s\n" "$CONFIDENCE"
    printf "\n"
    printf "### Comments\n\n"
    printf "%s\n" "$COMMENTS"
    printf "\n"
    printf "### Migration Script\n\n"
    printf '```sql\n'
    printf "%s\n" "$MIGRATION_SCRIPT"
    printf '```\n'
    printf '%s\n' '---'
    # Use file:// links for clickable path references
    FILE_URI="file://$OUTPUT_DIR"
    printf 'Full artifacts are available in the output directory:\n'
    printf '%s\n' "- [DDL v1]($FILE_URI/orders-v1.sql)"
    printf '%s\n' "- [DDL v2]($FILE_URI/orders-v2.sql)"
    printf '%s\n' "- [Migration]($FILE_URI/migration.sql)"
    printf '%s\n' "- [Full report]($FILE_URI/migration-report-full.md)"
} >"$REPORT_FILE"
echo -e "${GREEN}✓ Compact report generated at: $REPORT_FILE${NC}"

# Also write the full detailed report (backup) to the output directory
# LLM Migration Generation Test Report
cat > "$OUTPUT_DIR/migration-report-full.md" <<FULL
# LLM Migration Generation Test Report

**Generated:** $(date '+%Y-%m-%d %H:%M:%S')  
**LLM Provider:** $LLM_PROVIDER  
**LLM Model:** $LLM_MODEL  
**Ollama URL:** $OLLAMA_URL  
**Generation Time:** ${DURATION}s

---

## Test Summary

✅ **Status:** SUCCESS  
📊 **LLM Confidence:** $CONFIDENCE  
🔄 **Migration Path:** orders:1.0.0 → orders:2.0.0  

---

## Model Versions

### Version 1.0 (Hash: $V1_HASH)

**Source:** \`orders-sdd-model.yaml\`

<details>
<summary>📄 View DDL v1</summary>

~~~sql
$(cat "$OUTPUT_DIR/orders-v1.sql")
~~~

</details>

### Version 2.0 (Hash: $V2_HASH)

**Source:** \`orders-sdd-model-v2.yaml\`

<details>
<summary>📄 View DDL v2</summary>

~~~sql
$(cat "$OUTPUT_DIR/orders-v2.sql")
~~~

</details>

---

## LLM Generated Migration

### Migration Script

~~~sql
$(cat "$OUTPUT_DIR/migration.sql")
~~~

### LLM Analysis

**Confidence Score:** $CONFIDENCE / 1.0

**Comments:**
> $COMMENTS

### Migration Script
~~~sql
$(cat "$OUTPUT_DIR/migration.sql")
~~~

---

## Technical Details

### Registration Logs

<details>
<summary>Version 1.0 Registration</summary>

~~~
$(cat "$OUTPUT_DIR/register-v1.log")
~~~

</details>

<details>
<summary>Version 2.0 Registration</summary>

~~~
$(cat "$OUTPUT_DIR/register-v2.log")
~~~

</details>

### Migration Generation Log

<details>
<summary>Full Migration Log</summary>

~~~
$(cat "$OUTPUT_DIR/migrate.log")
~~~

</details>

---

## Files Generated

- **DDL v1:** [$OUTPUT_DIR/orders-v1.sql](file://$OUTPUT_DIR/orders-v1.sql)
- **DDL v2:** [$OUTPUT_DIR/orders-v2.sql](file://$OUTPUT_DIR/orders-v2.sql)
- **Migration:** [$OUTPUT_DIR/migration.sql](file://$OUTPUT_DIR/migration.sql)
- **Report:** [$OUTPUT_DIR/migration-report.md](file://$OUTPUT_DIR/migration-report.md)

---

**Test completed successfully** ✅
FULL

echo -e "${GREEN}✓ Full report generated at: $OUTPUT_DIR/migration-report-full.md${NC}"

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
