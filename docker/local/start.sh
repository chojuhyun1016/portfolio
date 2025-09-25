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

# 스택 목록 (ui 포함)
STACKS=(aws kafka mysql redis ui)

usage() {
  cat <<'EOF'
Usage:
  ./start.sh                       # up all stacks (aws, kafka, mysql, redis, ui)
  ./start.sh aws kafka             # up selected stacks only

Options:
  --recreate            run "compose down" for each selected stack before up (default)
  --no-recreate         skip the down step
  --build               pass --build to compose up
  -p, --project-prefix  prefix for docker compose project name (default: none)
  -n, --file-name       compose file name inside each stack dir (default: docker-compose.yml)
  --no-wait             do not wait for healthchecks
  -h, --help            show help

Env:
  HEALTH_WAIT_TIMEOUT   seconds to wait per service when healthcheck exists (default: 60)
EOF
}

RECREATE=1
BUILD=0
PROJECT_PREFIX=""
FILE_NAME="docker-compose.yml"
SELECTS=()
NO_WAIT=0

HEALTH_WAIT_TIMEOUT="${HEALTH_WAIT_TIMEOUT:-60}"

# --- parse args ---
while [ $# -gt 0 ]; do
  case "$1" in
    --recreate) RECREATE=1; shift ;;
    --no-recreate) RECREATE=0; shift ;;
    --build) BUILD=1; shift ;;
    --no-wait) NO_WAIT=1; shift ;;
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

# -------------------------------------------------------
# SKIP_WAIT 패치: 특정 서비스는 헬스대기를 스킵
#   형식: "<stack>/<service>"
#   서비스명은 compose yml 안의 "services:" 키 이름
# -------------------------------------------------------
SKIP_WAIT=(
  "aws/localstack"
  "kafka/zookeeper"
  "ui/adminer"
  "ui/redisinsight"
  "ui/dynamodb-admin"
)

should_skip_wait() {
  local key="$1/$2"
  for s in "${SKIP_WAIT[@]}"; do
    if [ "$s" = "$key" ]; then
      return 0
    fi
  done
  return 1
}

wait_health_of_stack() {
  local stack="$1"

  # 전역 옵션으로 대기 자체를 끄는 경우
  if [ "$NO_WAIT" -eq 1 ]; then
    echo "   [$stack] skip all health waits (--no-wait)"
    return 0
  fi

  local services
  services="$(compose_cmd "$stack" config --services 2>/dev/null || true)"
  if [ -z "$services" ]; then
    echo "   [$stack] no services detected (skip health wait)"
    return 0
  fi

  local svc
  for svc in $services; do
    # 컨테이너 ID 대기 (최대 60초)
    local cid timeout status
    timeout=60
    cid="$(compose_cmd "$stack" ps -q "$svc" 2>/dev/null || true)"
    while [ -z "$cid" ] && [ $timeout -gt 0 ]; do
      sleep 1
      cid="$(compose_cmd "$stack" ps -q "$svc" 2>/dev/null || true)"
      timeout=$((timeout-1))
    done
    if [ -z "$cid" ]; then
      echo "   [$stack/$svc] container id not found (skip)"
      continue
    fi

    # SKIP_WAIT 대상이면 바로 통과
    if should_skip_wait "$stack" "$svc"; then
      echo "   [$stack/$svc] skip health wait (preconfigured)"
      continue
    fi

    # healthcheck 유무 확인
    status="$(docker inspect -f '{{.State.Health.Status}}' "$cid" 2>/dev/null || echo 'none')"
    if [ "$status" = "none" ] || [ -z "$status" ]; then
      echo "   [$stack/$svc] no healthcheck (ready)"
      continue
    fi

    # health 대기 (기본 60초, 환경변수로 조절)
    timeout="$HEALTH_WAIT_TIMEOUT"
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
  else
    echo "==> [$stack] down (skip, no containers)"
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
