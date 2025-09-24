#!/usr/bin/env bash
set -euo pipefail
PORT="${REDIS_PORT:-6379}"
if [ -n "${REDIS_PASSWORD:-}" ]; then
  docker compose exec -e REDISCLI_AUTH="$REDIS_PASSWORD" redis redis-cli -p "$PORT" "$@"
else
  docker compose exec redis redis-cli -p "$PORT" "$@"
fi
