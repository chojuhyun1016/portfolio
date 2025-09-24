#!/usr/bin/env bash
set -euo pipefail
NAME="${1:-myapp/secret-key}"
if command -v aws >/dev/null 2>&1; then
  aws --endpoint-url=http://localhost:4566 secretsmanager get-secret-value --secret-id "${NAME}" --query SecretString --output text
else
  docker exec -it localstack-aws awslocal secretsmanager get-secret-value --secret-id "${NAME}" --query SecretString --output text
fi
