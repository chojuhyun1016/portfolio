#!/usr/bin/env bash
set -euo pipefail
LOG_DIR="${1:-/app/logs}"
mkdir -p "${LOG_DIR}"
FILE="${LOG_DIR}/app-$(hostname).log"
echo "$(date +'%F %T') hello from $(hostname)" >> "${FILE}"
echo "[make-log] wrote ${FILE}"
