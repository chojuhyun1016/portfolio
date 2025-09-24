#!/usr/bin/env bash
set -euo pipefail
echo "[bootstrap] start"

# ===== S3 =====
BUCKET="my-local-bucket"
PREFIX="logs"
awslocal s3 mb s3://${BUCKET} || true
echo "" | awslocal s3 cp - s3://${BUCKET}/${PREFIX}/__init || true
echo "[bootstrap] S3 ready: s3://${BUCKET}/${PREFIX}/"

# ===== DynamoDB =====
TABLE="order_dynamo"
if awslocal dynamodb describe-table --table-name "${TABLE}" >/dev/null 2>&1; then
  echo "[bootstrap] DynamoDB: table ${TABLE} already exists"
else
  awslocal dynamodb create-table     --table-name "${TABLE}"     --attribute-definitions AttributeName=id,AttributeType=N     --key-schema AttributeName=id,KeyType=HASH     --provisioned-throughput ReadCapacityUnits=5,WriteCapacityUnits=5
  echo "[bootstrap] DynamoDB: table ${TABLE} created"
fi

# seed 1 row
awslocal dynamodb put-item   --table-name "${TABLE}"   --item '{
    "id": {"N": "1"},
    "user_id": {"N": "1001"},
    "order_id": {"N": "5001"},
    "status": {"S": "CREATED"},
    "created_at": {"S": "2025-09-15T09:00:00Z"}
  }' || true
echo "[bootstrap] DynamoDB seed inserted"

# ===== Secrets Manager =====
SECRET_NAME="myapp/secret-key"
SECRET_JSON='{"aes128":"dGhpc2lzMTZieXRla2V5IQ==","aes256":"bXlTZWNyZXRLZXlTMjU2MjU2MjU2MjU2MjU2MjU2MjU=","aesgcm":"bXlTMzJiYnl0ZXNnY21rZXlzdXBlcnNlY3JldGtleTE="}'
if awslocal secretsmanager describe-secret --secret-id "${SECRET_NAME}" >/dev/null 2>&1; then
  echo "[bootstrap] Secrets: ${SECRET_NAME} already exists"
else
  awslocal secretsmanager create-secret --name "${SECRET_NAME}" --secret-string "${SECRET_JSON}"
  echo "[bootstrap] Secrets: ${SECRET_NAME} created"
fi

echo "[bootstrap] done"
