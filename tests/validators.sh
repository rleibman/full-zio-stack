#!/usr/bin/env bash
# Invalid answers must be rejected by copier.yml's validators, not turned into a broken project.
#
# Usage: tests/validators.sh      (COPIER and TEMPLATE as in generate-and-build.sh)
set -uo pipefail

repo=$(cd "$(dirname "$0")/.." && pwd)
copier=${COPIER:-copier}
template=${TEMPLATE:-$repo}
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
failures=0

# expect_rejected <description> <question that must be named in the error> <copier arguments...>
expect_rejected() {
  local description=$1 question=$2
  shift 2
  if $copier copy --trust --defaults "$@" "$template" "$work/out" >"$work/log" 2>&1; then
    echo "FAIL: accepted $description"
    failures=$((failures + 1))
  elif ! grep -q "question '$question'\|Question \"$question\"\|for '$question'" "$work/log"; then
    echo "FAIL: rejected $description, but not because of $question:"
    tail -3 "$work/log"
    failures=$((failures + 1))
  else
    echo "ok: rejected $description"
  fi
  rm -rf "$work/out"
}

expect_rejected "a missing author" author_name --data project_name=Demo
expect_rejected "an invalid base package" base_package --data author_name=A --data base_package=Bad.Package
expect_rejected "an invalid organization" organization --data author_name=A --data organization="acme corp"
expect_rejected "an invalid project slug" project_slug --data author_name=A --data project_slug="Bad Slug"
expect_rejected "an invalid class prefix" class_prefix --data author_name=A --data class_prefix=lowercase
expect_rejected "an unknown database" database --data author_name=A --data database=oracle
expect_rejected "an unknown HTTP server" http_server --data author_name=A --data http_server=akka-http

if [ "$failures" -gt 0 ]; then
  echo "$failures invalid answer(s) accepted"
  exit 1
fi
echo "All invalid answers rejected"
