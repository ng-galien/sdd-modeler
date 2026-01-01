#!/usr/bin/env bash
set -euo pipefail

# This script generates the SQL DDL from the SDD model, copies it into the running
# Postgres container and applies the DDL followed by sample data.
# Usage: ./apply-schema.sh [--fresh]

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
MODEL_LAYOUT="$REPO_ROOT/sample/src/main/resources/sdd.yaml"
SCHEMA_OUT="$REPO_ROOT/sample/build/generated/sdd/ddl/changelog.yaml"
SAMPLE_DATA="$REPO_ROOT/sample/scripts/sample-data.sql"
COMPOSE_FILE="$REPO_ROOT/sample/docker-compose.yml"

FRESH=0
if [[ "${1:-}" == "--fresh" ]]; then
  FRESH=1
fi

echo "Generating DDL for $MODEL_LAYOUT -> $SCHEMA_OUT"
"$REPO_ROOT/gradlew" :state-modeler-app:run --args="sql $MODEL_LAYOUT -o $SCHEMA_OUT" --no-daemon

if [[ $FRESH -eq 1 ]]; then
  echo "Stopping containers and removing volumes (fresh init)..."
  docker compose -f "$COMPOSE_FILE" down -v || true
fi

echo "(Re)starting Postgres container..."
docker compose -f "$COMPOSE_FILE" up -d postgres

CONTAINER=$(docker compose -f "$COMPOSE_FILE" ps -q postgres)
if [[ -z "$CONTAINER" ]]; then
  echo "Error: couldn't find the running Postgres container" >&2
  exit 1
fi

# Wait for postgres to be ready (using pg_isready inside container)
for i in {1..60}; do
  if docker exec -i "$CONTAINER" pg_isready -U postgres -d postgres >/dev/null 2>&1; then
    echo "Postgres is accepting connections"
    break
  fi
  echo "Waiting for Postgres to be ready... ($i/60)"
  sleep 1
done
if ! docker exec -i "$CONTAINER" pg_isready -U postgres -d postgres >/dev/null 2>&1; then
  echo "Postgres did not become ready in time" >&2
  exit 1
fi

echo "Creating uuid-ossp extension (if not present)"
docker exec -i "$CONTAINER" psql -U postgres -d postgres -c 'CREATE EXTENSION IF NOT EXISTS "uuid-ossp";'

# Patch the generated schema to ensure public.leads has a primary key & default UUID
# We need the extension created first so uuid_generate_v4() is available
if grep -q "CREATE TABLE public.leads" "$SCHEMA_OUT"; then
  echo "Ensuring public.leads has a primary key & default UUID in schema"
  perl -0777 -pe "s/CREATE TABLE public.leads\s*\(\s*id uuid NOT NULL\s*\);/CREATE TABLE public.leads (\n    id uuid NOT NULL DEFAULT uuid_generate_v4(),\n    PRIMARY KEY (id)\n);/s" -i "$SCHEMA_OUT"
fi

echo "Copying Liquibase changelog into container and applying it"
docker cp "$SCHEMA_OUT" "$CONTAINER":/tmp/changelog.yaml
docker exec -i "$CONTAINER" liquibase \ 
  --url="jdbc:postgresql://localhost:5432/postgres" \ 
  --username=postgres \ 
  --password=postgres \ 
  --changeLogFile=/tmp/changelog.yaml \ 
  update

echo "Copying sample data and applying it"
docker cp "$SAMPLE_DATA" "$CONTAINER":/tmp/sample-data.sql
docker exec -i "$CONTAINER" psql -U postgres -d postgres -f /tmp/sample-data.sql

echo "Done. Schema & sample-data applied to container: $CONTAINER"
