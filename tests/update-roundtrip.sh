#!/usr/bin/env bash
# Proves that projects generated from an older template version update cleanly: generate from <from-ref>, commit,
# `copier update` to <to-ref>, then require no conflicts, the answers file pointing at <to-ref>, and (unless
# NO_BUILD=1) a passing server-side build.
#
# Usage: tests/update-roundtrip.sh <from-ref> <to-ref> [--data key=value ...]
#   Refs are git refs of the template repository (tags, in practice). COPIER and TEMPLATE as in generate-and-build.sh;
#   TEMPLATE must be a git repository that has both refs.
set -euo pipefail

repo=$(cd "$(dirname "$0")/.." && pwd)
from=$1
to=$2
shift 2
copier=${COPIER:-copier}
template=${TEMPLATE:-$repo}
out=$(mktemp -d)/project
trap 'rm -rf "$(dirname "$out")"' EXIT

echo "==> Generating from $from"
$copier copy --trust --defaults --vcs-ref "$from" --data author_name="Template Test" "$@" "$template" "$out"
cd "$out"
git init --quiet 2>/dev/null || true
git -c user.name=test -c user.email=test@example.com add -A
git -c user.name=test -c user.email=test@example.com commit --quiet -m "Generated from $from"

echo "==> Updating to $to"
$copier update --trust --defaults --vcs-ref "$to"

problems=0
if [ -n "$(git diff --name-only --diff-filter=U)" ]; then
  echo "FAIL: conflicts in: $(git diff --name-only --diff-filter=U | tr '\n' ' ')"
  problems=1
fi
if git grep -lI '^<<<<<<< ' -- . >/dev/null 2>&1; then
  echo "FAIL: conflict markers in: $(git grep -lI '^<<<<<<< ' -- . | tr '\n' ' ')"
  problems=1
fi
if find . -name '*.rej' -not -path './.git/*' | grep -q .; then
  echo "FAIL: rejected hunks: $(find . -name '*.rej' -not -path './.git/*' | tr '\n' ' ')"
  problems=1
fi
expected=$(git -C "$template" describe --tags --exact-match "$to" 2>/dev/null || git -C "$template" rev-parse --short "$to")
recorded=$(python3 -c "import yaml; print(yaml.safe_load(open('.copier-answers.yml'))['_commit'])")
if [ "$recorded" != "$expected" ]; then
  echo "FAIL: .copier-answers.yml records $recorded, expected $expected"
  problems=1
fi
[ "$problems" -eq 0 ] || exit 1
echo "Updated cleanly: $(git status --short | wc -l) file(s) changed"

if [ "${NO_BUILD:-0}" != 1 ]; then
  echo "==> Building the updated project"
  sbt --error "db/testFull" "server/testFull"
fi
echo "OK"
