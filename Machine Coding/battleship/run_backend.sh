#!/usr/bin/env bash

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="${PROJECT_DIR}/backend"
VENV_DIR="${BACKEND_DIR}/.venv"

if [[ ! -d "${VENV_DIR}" ]]; then
  python3 -m venv "${VENV_DIR}"
fi

if ! "${VENV_DIR}/bin/python" -c 'import fastapi, uvicorn' 2>/dev/null; then
  "${VENV_DIR}/bin/pip" install -r "${BACKEND_DIR}/requirements.txt"
fi

cd "${BACKEND_DIR}"
exec "${VENV_DIR}/bin/python" -m uvicorn app.main:app --reload --port 8101
