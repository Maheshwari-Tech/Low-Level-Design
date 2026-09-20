#!/usr/bin/env bash

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "${PROJECT_DIR}/frontend"

if [[ ! -d node_modules ]]; then
  npm install
fi

exec npm run dev
