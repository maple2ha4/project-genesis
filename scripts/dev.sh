#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
export PRISM_DATABASE_URL="${PRISM_DATABASE_URL:-jdbc:h2:file:$root/data/prism;DB_CLOSE_ON_EXIT=FALSE}"
cd "$root/backend"
"$root/scripts/mvn.sh" spring-boot:run & backend_pid=$!
trap 'kill "$backend_pid" "${frontend_pid:-}" 2>/dev/null || true' EXIT INT TERM
cd "$root/frontend"
npm run dev & frontend_pid=$!
wait -n "$backend_pid" "$frontend_pid"
