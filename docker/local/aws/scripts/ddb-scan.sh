#!/usr/bin/env bash
set -euo pipefail
TABLE="${1:-order_dynamo}"
if command -v aws >/dev/null 2>&1; then
  aws --endpoint-url=http://localhost:4566 dynamodb scan --table-name "${TABLE}"
else
  docker exec -it localstack-aws awslocal dynamodb scan --table-name "${TABLE}"
fi
