#!/usr/bin/env bash
# Generic awslocal wrapper: ./scripts/aws-cmd.sh s3 ls
set -euo pipefail
if command -v aws >/dev/null 2>&1; then
  aws --endpoint-url=http://localhost:4566 "$@"
else
  docker exec -it localstack-aws awslocal "$@"
fi
