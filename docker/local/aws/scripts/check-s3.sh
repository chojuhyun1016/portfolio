#!/usr/bin/env bash
set -euo pipefail
BUCKET="${1:-my-local-bucket}"
PREFIX="${2:-logs/}"
if command -v aws >/dev/null 2>&1; then
  aws --endpoint-url=http://localhost:4566 s3 ls "s3://${BUCKET}/${PREFIX}"
else
  docker exec -it localstack-aws awslocal s3 ls "s3://${BUCKET}/${PREFIX}"
fi
