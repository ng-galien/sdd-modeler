#!/usr/bin/env bash
# Generates code from an SDD model by invoking the `sdd-modeler` CLI via Gradle
# Usage: ./scripts/generate-code.sh [-m <model-file>] [-o <out-dir>] [-l <language>] [--skip-build] [--target-module <module>]
#
# Defaults:
#   model-file  -> scripts/examples/orders-sdd-model.yaml
#   out-dir     -> sample/src/main/java
#   language    -> java
#   target-module -> sample

set -euo pipefail

# Defaults
MODEL_FILE="scripts/examples/orders-sdd-model.yaml"
# Default out dir: sample module build-generated sources
OUT_DIR="sample/build/generated/sdd"
LANGUAGE="java"
SKIP_BUILD=false
TARGET_MODULE=""
FORMAT_AFTER_GENERATION=true
FORMAT_PRE_BUILD=true
ARGS=()

usage() {
  cat <<EOF
Usage: ${0##*/} [OPTIONS]

Options:
  -m, --model FILE        Path to SDD model file (YAML/JSON). Default: $MODEL_FILE
  -o, --outdir DIR        Output directory for generated sources. Default: $OUT_DIR
  -l, --language LANG     Generation language. Default: $LANGUAGE
  -t, --target-module MOD Target module to build after generation (e.g., sample). Default: none
      --no-format         Do not run Spotless format (spotlessApply) on target module before build
      --skip-build       Skip running a full build before generation
  -h, --help              Show this help and exit

Examples:
  # Generate Java sources from the example SDD into the sample module
  ${0##*/} -m scripts/examples/orders-sdd-model.yaml -o sample/src/main/java -t sample

  # Use a different output dir
  ${0##*/} -m scripts/examples/orders-sdd-mini-model.yaml -o sample/src/generated/java

EOF
}

# Parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    -m|--model)
      MODEL_FILE="$2"
      shift 2
      ;;
    -o|--outdir)
      OUT_DIR="$2"
      shift 2
      ;;
    -l|--language)
      LANGUAGE="$2"
      shift 2
      ;;
    -t|--target-module)
      TARGET_MODULE="$2"
      shift 2
      ;;
      --no-format)
        FORMAT_AFTER_GENERATION=false
        FORMAT_PRE_BUILD=false
        shift
        ;;
    --skip-build)
      SKIP_BUILD=true
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    --) # end of options
      shift
      break
      ;;
    -*)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
    *)
      ARGS+=("$1")
      shift
      ;;
  esac
done

# Normalize paths
MODEL_FILE="${MODEL_FILE/#~/$HOME}"
OUT_DIR="${OUT_DIR/#~/$HOME}"

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
EXEC_GRADLE="$ROOT_DIR/gradlew"

if [[ ! -f "$EXEC_GRADLE" ]]; then
  echo "Gradle wrapper not found at $EXEC_GRADLE" >&2
  echo "Please run this script from the repository root or ensure gradlew is present." >&2
  exit 2
fi

# Resolve absolute paths for model file and out dir
MODEL_FILE_ABS=$(cd "$(dirname "$MODEL_FILE")" 2>/dev/null && pwd || pwd)/$(basename "$MODEL_FILE")
# If OUT_DIR is absolute, keep it, otherwise resolve relative to project root
if [[ "$OUT_DIR" = /* ]]; then
  OUT_DIR_ABS="$OUT_DIR"
else
  OUT_DIR_ABS="$ROOT_DIR/$OUT_DIR"
fi

if [[ ! -f "$MODEL_FILE" && ! -f "$MODEL_FILE_ABS" ]]; then
  echo "Model file not found: $MODEL_FILE" >&2
  exit 3
fi

# Ensure out dir exists or can be created
mkdir -p "$OUT_DIR_ABS" || true

# Build the CLI module if not skipped (keep this fast and avoid checking unrelated modules)
if [[ "$SKIP_BUILD" = false ]]; then
  echo "Building the CLI module (:state-modeler-app)..."
  if [[ "$FORMAT_PRE_BUILD" = true ]]; then
    echo "Applying Spotless formatting to :state-modeler-app..."
    "$EXEC_GRADLE" :state-modeler-app:spotlessApply || true
  fi
  "$EXEC_GRADLE" :state-modeler-app:build -x test
fi

# Execute the CLI generate subcommand via Gradle run
# Compose arguments: generate modelFile -o outdir --language language
ARGS_STR="generate $MODEL_FILE_ABS -o $OUT_DIR_ABS --language $LANGUAGE"

echo "Invoking sdd-modeler CLI: $ARGS_STR"

# Execute the app
# Use the run task on the state-modeler-app module so it uses project classpath
"$EXEC_GRADLE" :state-modeler-app:run --console=plain --quiet --args="$ARGS_STR"
EXIT_CODE=$?

if [[ $EXIT_CODE -ne 0 ]]; then
  echo "Generation failed with exit code: $EXIT_CODE" >&2
  exit $EXIT_CODE
fi

  # Optional: Build the target module to verify compilation if requested
if [[ -n "$TARGET_MODULE" ]]; then
  echo "Building module '$TARGET_MODULE' to verify compilation..."
  if [[ "$FORMAT_AFTER_GENERATION" = true ]]; then
    echo "Applying Spotless formatting to $TARGET_MODULE..."
    # Run format and ignore failures to avoid script stopping on lint vs format exceptions
    "$EXEC_GRADLE" :$TARGET_MODULE:spotlessApply || true
  fi
  # Try building – if Spotless still fails we surface the exit code
  "$EXEC_GRADLE" :$TARGET_MODULE:build -x test -x spotlessCheck
fi

echo "Generation completed successfully. Generated sources are in: $OUT_DIR"
exit 0
