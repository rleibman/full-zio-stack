#!/usr/bin/env bash
# Generates a project from this repository's template and proves it works:
#   1. the AI docs only name files that exist (tests/check-ai-docs.py generated),
#   2. sbt testFull passes (for full-stack projects, everything but the client unless WITH_CLIENT=1, because the
#      client needs the ScalablyTyped facades from stLib/, which need a locally published converter),
#   3. the server starts and answers /health and a GraphQL query (and, with WITH_CLIENT=1, serves a client that works
#      in headless Chrome: tests/ui-check.mjs),
#   4. the chosen packaging builds.
#
# Usage: tests/generate-and-build.sh <output directory> [--data key=value ...]
#   COPIER    how to run copier (default: copier; e.g. "pipx run --spec copier copier")
#   TEMPLATE  where the template is (default: this repository, including uncommitted changes)
set -euo pipefail

repo=$(cd "$(dirname "$0")/.." && pwd)
out=$1
shift
copier=${COPIER:-copier}
template=${TEMPLATE:-$repo}

step() { printf '\n==> %s\n' "$*"; }

step "Generating $out"
rm -rf "$out"
$copier copy --trust --defaults --data author_name="Template Test" "$@" "$template" "$out"
cd "$out"

answer() { python3 -c "import sys, yaml; print(yaml.safe_load(open('.copier-answers.yml')).get('$1', ''))"; }
components=$(answer components)
database=$(answer database)
packaging=$(answer packaging)
port=$(answer http_port)

# However the script ends: stop the server (sbt run forks a JVM; found by the port it listens on) and the database.
cleanup() {
  local pid
  pid=$(ss -ltnp 2>/dev/null | grep ":$port " | grep -o 'pid=[0-9]*' | head -1 | cut -d= -f2 || true)
  if [ -n "$pid" ]; then kill "$pid" 2>/dev/null || true; fi
  if [ -f docker-compose.yml ]; then docker compose down -v >/dev/null 2>&1 || true; fi
}
trap cleanup EXIT

step "Checking the AI docs"
python3 "$repo/tests/check-ai-docs.py" generated .

step "sbt testFull"
if [ "$components" = full-stack ] && [ "${WITH_CLIENT:-0}" != 1 ]; then
  sbt --error "db/testFull" "server/testFull"
else
  sbt --error testFull
fi

with_client=0
if [ "$components" = full-stack ] && [ "${WITH_CLIENT:-0}" = 1 ]; then
  with_client=1
  step "Building the client"
  sbt --error "client/webDebugDist"
fi

step "Starting the server"
if [ -f docker-compose.yml ]; then
  docker compose up -d --wait
fi
if [ "$with_client" = 1 ]; then export STATIC_CONTENT_DIR=debugDist; fi
sbt server/run > server.log 2>&1 &
for _ in $(seq 1 120); do
  grep -q "Listening on" server.log && break
  grep -q "\[error\]" server.log && { cat server.log; exit 1; }
  sleep 2
done
grep -q "Listening on" server.log || { cat server.log; echo "The server didn't start"; exit 1; }
curl -fsS "http://localhost:$port/health"
echo
response=$(curl -fsS -H 'Content-Type: application/json' -d '{"query":"{ version modelObjects(search: {includeDeleted: false, offset: 0, limit: 5}) { total } }"}' "http://localhost:$port/api/graphql")
echo "$response"
echo "$response" | grep -q '"total":0' || { echo "Unexpected GraphQL response"; exit 1; }
if [ "$with_client" = 1 ]; then
  step "Checking the client in headless Chrome"
  node "$repo/tests/ui-check.mjs" "http://localhost:$port"
fi
cleanup
wait || true

if [ "$components" = full-stack ] && [ "$with_client" != 1 ]; then
  # Packaging includes the production client build.
  step "Skipping packaging: it needs the client (WITH_CLIENT=1)"
  packaging=skipped
fi
case "$packaging" in
docker)
  step "Staging the Docker image"
  sbt --error "server/Docker/stage"
  ;;
systemd)
  step "Building the Debian package"
  sbt --error "server/Debian/packageBin"
  ;;
esac

step "OK: $out ($components, $database, packaging: $packaging)"
