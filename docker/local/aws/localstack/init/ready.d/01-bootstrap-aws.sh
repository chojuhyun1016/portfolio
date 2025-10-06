#!/usr/bin/env bash
set -euo pipefail

echo "[bootstrap] start"

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

BUCKET="my-local-bucket"
PREFIX="logs"
awslocal s3 mb "s3://${BUCKET}" >/ddocker exec -it aws-localstack-1 shev/null 2>&1 || true
echo "" | awslocal s3 cp - "s3://${BUCKET}/${PREFIX}/__init" >/dev/null 2>&1 || true
echo "[bootstrap] S3 ready: s3://${BUCKET}/${PREFIX}/"

SECRET_NAME="myapp/secret-key"

# 표준 Base64만 사용(+,/ 포함, = 패딩 포함) / 길이 규약 충족
SECRET_JSON=$(cat <<'JSON'
{
  "order.aesgcm": [
    {
      "kid": "key-2024-12-01",
      "version": 1,
      "algorithm": "AES-GCM",
      "key": "FyT8ax5/8VsXcHMOO7cIgJ5deAAODozcxBsF5FTqwFc="
    },
    {
      "kid": "key-2025-09-27",
      "version": 2,
      "algorithm": "AES-GCM",
      "key": "qqZJlZZKrHErb5pzzERMfOWCDBtAY8PGLegiv77oTVE="
    }
  ],
  "order.aes256": {
    "kid": "key-2025-07-01",
    "version": 3,
    "algorithm": "AES-256",
    "key": "wKcFnY/2rCzOj2u9TUdVRc7rWhjWvXjJUpkbSC0sVng="
  },
  "order.aes128": [
    {
      "kid": "key-2024-10-01",
      "version": 1,
      "algorithm": "AES-128",
      "key": "omfKYUPHyx37KLCLnUI41Q=="
    },
    {
      "kid": "key-2025-03-15",
      "version": 2,
      "algorithm": "AES-128",
      "key": "fsiw3PLOHd7wTeOr/+0BFg=="
    }
  ],
  "order.hmac": [
    {
      "kid": "key-2024-05-20",
      "version": 4,
      "algorithm": "HMAC_SHA256",
      "key": "SSxjtbndxbEgbV/F69+YeNFP9RNik5Lp2NLhSqTuxaA="
    },
    {
      "kid": "key-2025-01-10",
      "version": 5,
      "algorithm": "HMAC_SHA256",
      "key": "8kHG7TfGbAvQjg4n90c8mFMDA30gYgzZ+BstB5Bwp+c="
    }
  ]
}
JSON
)

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
