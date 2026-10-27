#!/usr/bin/env bash
set -euo pipefail
root=$(cd "$(dirname "$0")/.." && pwd)
cd "$root/frontend"
npm ci --cache "${PRISM_NPM_CACHE:-/tmp/prism-npm-cache}" --no-audit --no-fund
npm run test
npm run build
cd "$root/backend"
"$root/scripts/mvn.sh" verify
