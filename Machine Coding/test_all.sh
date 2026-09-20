#!/usr/bin/env bash
set -euo pipefail

workspace_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

for project in battleship movie-ticket-booking calendar-meet-booking; do
  echo "Testing ${project}"
  (cd "${workspace_dir}/${project}/backend" && python3 -m pytest -q)
done
