#!/usr/bin/env bash
set -euo pipefail

# --- detect compose binary (v2 preferred) ---
if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  COMPOSE_BIN=("docker" "compose")
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_BIN=("docker-compose")
else
  echo "ERROR: docker compose / docker-compose not found." >&2
  exit 1
fi

STACKS=(aws kafka mysql redis)

usage() {
  cat <<'EOF'
Usage:
  ./start.sh                       # up all stacks (aws, kafka, mysql, redis)
  ./start.sh aws kafka             # up selected stacks only

Options:
  --recreate            run "compose down" for each selected stack before up (default)
  --no-recreate         skip the down step
  --build               pass --build to compose up
  -p, --project-prefix  prefix for docker compose project name (default: none)
  -n, --file-name       compose file name inside each stack dir (default: docker-compose.yml)
  -h, --help            show help

Notes:
  * This script expects per-stack compose files at:
      ./aws/docker-compose.yml
      ./kafka/docker-compose.yml
      ./mysql/docker-compose.yml
      ./redis/docker-compose.yml
  * Project name is set per stack as: <prefix><stack>, e.g. "dev_aws"
EOF
}

RECREATE=1
BUILD=0
PROJECT_PREFIX=""
FILE_NAME="docker-compose.yml"
SELECTS=()

# --- parse args ---
while [ $# -gt 0 ]; do
  case "$1" in
    --recreate) RECREATE=1; shift ;;
    --no-recreate) RECREATE=0; shift ;;
    --build) BUILD=1; shift ;;
    -p|--project-prefix)
      PROJECT_PREFIX="${2:-}"; [ -z "$PROJECT_PREFIX" ] && { echo "Missing value for $1" >&2; exit 1; }
      shift 2
      ;;
    -n|--file-name)
      FILE_NAME="${2:-}"; [ -z "$FILE_NAME" ] && { echo "Missing value for $1" >&2; exit 1; }
      shift 2
      ;;
    -h|--help) usage; exit 0 ;;
    *)
      SELECTS+=("$1"); shift ;;
  esac
done

# default selection: all stacks
if [ ${#SELECTS[@]} -eq 0 ]; then
  SELECTS=("${STACKS[@]}")
fi

# validate names
for s in "${SELECTS[@]}"; do
  case "$s" in
    aws|kafka|mysql|redis) : ;;
    *) echo "Unknown stack: $s" >&2; exit 1;;
  esac
done

compose_cmd() {
  # usage: compose_cmd <stack> <extra args...>
  local stack="$1"; shift
  local file="./${stack}/${FILE_NAME}"
  local proj="${PROJECT_PREFIX}${stack}"

  if [ ! -f "$file" ]; then
    echo "ERROR: compose file not found: $file" >&2
    return 2
  fi

  # shellcheck disable=SC2145
  "${COMPOSE_BIN[@]}" -f "$file" -p "$proj" "$@"
}

wait_health_of_stack() {
  local stack="$1"
  local services
  services="$(compose_cmd "$stack" config --services 2>/dev/null || true)"
  if [ -z "$services" ]; then
    echo "   [$stack] no services detected (skip health wait)"
    return 0
  fi

  # For each service in that stack, wait for healthy if healthcheck is defined
  # best-effort: 180 seconds per service
  local svc
  for svc in $services; do
    # get container id
    local cid
    cid="$(compose_cmd "$stack" ps -q "$svc" 2>/dev/null || true)"
    local timeout=60
    while [ -z "$cid" ] && [ $timeout -gt 0 ]; do
      sleep 1
      cid="$(compose_cmd "$stack" ps -q "$svc" 2>/dev/null || true)"
      timeout=$((timeout-1))
    done
    if [ -z "$cid" ]; then
      echo "   [$stack/$svc] container id not found (skip)"
      continue
    fi

    local status
    status="$(docker inspect -f '{{.State.Health.Status}}' "$cid" 2>/dev/null || echo 'none')"
    if [ "$status" = "none" ] || [ -z "$status" ]; then
      echo "   [$stack/$svc] no healthcheck (ready)"
      continue
    fi

    timeout=180
    while : ; do
      status="$(docker inspect -f '{{.State.Health.Status}}' "$cid" 2>/dev/null || echo 'starting')"
      case "$status" in
        healthy)   echo "   [$stack/$svc] healthy"; break ;;
        unhealthy) echo "   [$stack/$svc] UNHEALTHY (continue)"; break ;;
        starting)  : ;;
        *)         : ;;
      esac
      timeout=$((timeout-1))
      [ $timeout -le 0 ] && { echo "   [$stack/$svc] health wait timeout (continue)"; break; }
      sleep 1
    done
  done
}

# --- recreate (optional) & up per stack ---
for stack in "${SELECTS[@]}"; do
  if [ $RECREATE -eq 1 ]; then
    echo "==> [$stack] down (recreate)"
    compose_cmd "$stack" down --remove-orphans || true
  fi

  echo "==> [$stack] up"
  if [ $BUILD -eq 1 ]; then
    compose_cmd "$stack" up -d --build
  else
    compose_cmd "$stack" up -d
  fi

  echo "==> [$stack] wait health..."
  wait_health_of_stack "$stack"

  echo "==> [$stack] ps"
  compose_cmd "$stack" ps || true
done

echo "Done."
