#!/usr/bin/env bash
set -euo pipefail

echo "[bootstrap] start"

# -------- util: retry --------
retry() {
  local tries=${1:-20}; shift
  local i=0
  until "$@"; do
    i=$((i+1))
    if [ "$i" -ge "$tries" ]; then
      return 1
    fi
    sleep 1
  done
  return 0
}

# -------- S3 --------
BUCKET="my-local-bucket"
PREFIX="logs"
awslocal s3 mb "s3://${BUCKET}" >/dev/null 2>&1 || true
echo "" | awslocal s3 cp - "s3://${BUCKET}/${PREFIX}/__init" >/dev/null 2>&1 || true
echo "[bootstrap] S3 ready: s3://${BUCKET}/${PREFIX}/"

# -------- Secrets Manager --------
SECRET_NAME="myapp/secret-key"

SECRET_JSON=$(cat <<'JSON'
{
  "order.aesgcm": [
    {
      "kid": "key-2025-09-27",
      "version": 2,
      "algorithm": "AES-256-GCM",
      "key": "qC8y3g7b6cJ1nJcL2mJY8j2jvQbqJp2J0J2bqQzq3rA"
    },
    {
      "kid": "key-2024-12-01",
      "version": 1,
      "algorithm": "AES-256-GCM",
      "key": "r9QbVgYg0rYd8mQ0XvJqf1mGxw2nQy7jJbJ3Wz6u1hE"
    }
  ],
  "order.aes256": {
    "kid": "key-2025-07-01",
    "version": 3,
    "algorithm": "AES-256",
    "key": "Zl1u0dUu3UeOe9oS6u4xSx1m3mK6jvKfJ2oJ8sWc0nQ"
  },
  "order.aes128": [
    {
      "kid": "key-2025-03-15",
      "version": 2,
      "algorithm": "AES-128",
      "key": "m3Qe7fK0dU9pWc2Lx4q7n0sVg9bJ2rQ5"
    },
    {
      "kid": "key-2024-10-01",
      "version": 1,
      "algorithm": "AES-128",
      "key": "b1N8wL3pQ7rT2yF5k9H0sD4u"
    }
  ],
  "order.hmac": [
    {
      "kid": "key-2025-01-10",
      "version": 5,
      "algorithm": "HMAC-SHA256",
      "key": "Q0uN8yJk3wT1sV7rM2pL6aX4hE9cB5dF0gR2kL8m"
    },
    {
      "kid": "key-2024-05-20",
      "version": 4,
      "algorithm": "HMAC-SHA256",
      "key": "hD3kL8pQ1rS7uV2wM6nX4eC9bF5dG0tR2yK8mL1a"
    }
  ]
}
JSON
)

# 서비스 준비 대기(최대 20초)
if retry 20 awslocal secretsmanager list-secrets >/dev/null 2>&1; then
  if awslocal secretsmanager describe-secret --secret-id "${SECRET_NAME}" >/dev/null 2>&1; then
    awslocal secretsmanager put-secret-value \
      --secret-id "${SECRET_NAME}" \
      --secret-string "${SECRET_JSON}" >/dev/null
    echo "[bootstrap] Secrets: updated ${SECRET_NAME}"
  else
    awslocal secretsmanager create-secret \
      --name "${SECRET_NAME}" \
      --secret-string "${SECRET_JSON}" >/dev/null
    echo "[bootstrap] Secrets: created ${SECRET_NAME}"
  fi
else
  echo "[bootstrap][WARN] Secrets Manager not ready → skip seeding (non-fatal)"
fi

echo "[bootstrap] done"
