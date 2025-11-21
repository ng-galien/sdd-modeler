#!/usr/bin/env bash
set -euo pipefail

HOOK_DIR=".git/hooks"
if [ ! -d "$HOOK_DIR" ]; then
  echo "This script must be run from the root of a git working copy. Exiting."
  exit 1
fi

mkdir -p "$HOOK_DIR"
cp -f scripts/git-hooks/pre-push "$HOOK_DIR/pre-push"
chmod +x "$HOOK_DIR/pre-push"

echo "Installed pre-push hook. To skip, use 'git push --no-verify'."
