#!/usr/bin/env bash
set -euo pipefail
if command -v aws >/dev/null 2>&1; then
  aws --endpoint-url=http://localhost:4566 dynamodb list-tables
else
  docker exec -it localstack-aws awslocal dynamodb list-tables
fi
