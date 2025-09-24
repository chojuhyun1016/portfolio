#!/usr/bin/env bash
set -euo pipefail
PROFILE="${1:-local}"
LOG_DIR="/app/logs"

docker compose up -d
echo "[start] waiting localstack..."; sleep 3
docker logs localstack-aws --tail 80 || true

./scripts/make-log.sh "${LOG_DIR}"

export AWS_ACCESS_KEY_ID=local
export AWS_SECRET_ACCESS_KEY=local
export AWS_DEFAULT_REGION=ap-northeast-2
export HOSTNAME="$(hostname)"

cat <<EOF

[start] LocalStack (S3/DynamoDB/Secrets) ready.

Run Spring Boot with:
  ./gradlew :order-worker:bootRun --args='--spring.profiles.active=${PROFILE}'
  # or
  java -Dspring.profiles.active=${PROFILE} \
       -Dlogging.file.path=${LOG_DIR} \
       -jar ./order-worker/build/libs/order-worker.jar

Useful endpoints: http://localhost:4566
EOF
