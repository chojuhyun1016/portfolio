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

# ★ ui 추가
STACKS=(aws kafka mysql redis ui)

usage() {
  cat <<'EOF'
Usage:
  ./stop.sh                       # down all stacks (containers only)
  ./stop.sh mysql                 # down selected stacks only

Options:
  --volumes            also remove named volumes (dangerous)
  -p, --project-prefix prefix for docker compose project name (default: none)
  -n, --file-name      compose file name inside each stack dir (default: docker-compose.yml)
  -h, --help           show help

Notes:
  * This script expects per-stack compose files at:
      ./aws/docker-compose.yml
      ./kafka/docker-compose.yml
      ./mysql/docker-compose.yml
      ./redis/docker-compose.yml
      ./ui/docker-compose.yml
  * Project name is set per stack as: <prefix><stack>, e.g. "dev_mysql"
EOF
}

VOLUMES=0
PROJECT_PREFIX=""
FILE_NAME="docker-compose.yml"
SELECTS=()

# --- parse args ---
while [ $# -gt 0 ]; do
  case "$1" in
    --volumes) VOLUMES=1; shift ;;
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
    aws|kafka|mysql|redis|ui) : ;;
    *) echo "Unknown stack: $s" >&2; exit 1;;
  esac
done

compose_cmd() {
  # usage: compose_cmd <stack> <extra args...>
  local stack="$1"; shift
  local file="$FILE_NAME"
  local proj="${PROJECT_PREFIX}${stack}"

  if [ ! -f "./${stack}/${file}" ]; then
    echo "ERROR: compose file not found: ./${stack}/${file}" >&2
    return 2
  fi

  (
    cd "./${stack}"
    "${COMPOSE_BIN[@]}" -f "$file" -p "$proj" "$@"
  )
}

stack_has_containers() {
  local stack="$1"
  local ids
  ids="$(compose_cmd "$stack" ps -q 2>/dev/null || true)"
  [ -n "$ids" ]
}

for stack in "${SELECTS[@]}"; do
  echo "==> [$stack] down"
  if stack_has_containers "$stack"; then
    if [ $VOLUMES -eq 1 ]; then
      compose_cmd "$stack" down --volumes --remove-orphans || true
    else
      compose_cmd "$stack" down --remove-orphans || true
    fi
  else
    echo "   [$stack] no containers (skip)"
  fi
done

echo "Done."
