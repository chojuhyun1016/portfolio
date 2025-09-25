# ☁️ Local AWS Mock — LocalStack (S3/DynamoDB/SecretsManager)

로컬 환경에서 AWS 서비스(S3, DynamoDB, Secrets Manager 등)를 모킹(Mock)하여 개발/테스트에 활용할 수 있는 `localstack` 구성입니다.  
데이터는 **네임드 볼륨(localstack_data)** 에 영구 저장되므로, 컨테이너 재기동 시에도 DynamoDB/S3 데이터가 유지됩니다.

---

## 1) 요구 사항

- Docker Desktop / Docker Engine
- Docker Compose v2
- 권장 리소스: CPU 2코어, RAM 2GB+

---

## 2) 시작하기

```bash
cd aws
docker compose up -d
```

> 최초 실행 시 이미지를 Pull 하므로 수십 초 이상 걸릴 수 있습니다.

---

## 3) 서비스 포트

| 서비스 | 포트 | 설명 |
|--------|------|------|
| LocalStack | 4566 | 모든 서비스 엔드포인트 통합(Edge 포트) |

> 기본값은 `${LOCALSTACK_PORT:-4566}` 이며, 환경변수로 덮어쓸 수 있습니다.

---

## 4) 지원 서비스

기본값:

```text
s3, dynamodb, secretsmanager
```

환경변수 `LOCALSTACK_SERVICES` 로 원하는 서비스만 선택할 수 있습니다. 예:

```bash
LOCALSTACK_SERVICES="s3,dynamodb" docker compose up -d
```

---

## 5) 데이터 지속성 (Persistence)

- LocalStack은 기본적으로 **임시 파일시스템**에서 실행됩니다.
- 아래 설정으로 데이터를 **네임드 볼륨(localstack_data)** 에 보존합니다:

```yaml
volumes:
  - localstack_data:/var/lib/localstack
```

> DynamoDB/S3의 테이블/버킷은 `./stop.sh` 시 컨테이너가 내려가도 유지됩니다.  
> 완전 초기화하려면 `./stop.sh --volumes` 로 네임드 볼륨까지 삭제해야 합니다.

---

## 6) 초기화 스크립트

`./localstack/init/ready.d` 경로에 스크립트를 두면 컨테이너가 준비된 뒤 자동 실행됩니다.  
예시:

```bash
# ./localstack/init/ready.d/init-dynamodb.sh
awslocal dynamodb create-table \
  --table-name demo \
  --attribute-definitions AttributeName=id,AttributeType=S \
  --key-schema AttributeName=id,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST
```

---

## 7) Healthcheck

LocalStack은 `_localstack/health` 엔드포인트를 통해 상태를 확인합니다.

- **Healthy** 조건: `"initialized": true`
- `start.sh`는 최대 **30회(약 150초)** 대기 후 계속 진행합니다.

수동 확인:

```bash
curl -s http://localhost:4566/_localstack/health | jq .
```

---

## 8) Compose 파일 (전체)

```yaml
services:
  localstack:
    image: localstack/localstack:3
    restart: unless-stopped
    ports:
      - "${LOCALSTACK_PORT:-4566}:4566"
    environment:
      - SERVICES=${LOCALSTACK_SERVICES:-s3,dynamodb,secretsmanager}
      - AWS_DEFAULT_REGION=${AWS_REGION:-ap-northeast-2}
      - DEBUG=${LOCALSTACK_DEBUG:-1}
      - PERSISTENCE=1
      - LS_TMP=/tmp/localstack
    volumes:
      - localstack_data:/var/lib/localstack
      - ./localstack/init/ready.d:/etc/localstack/init/ready.d:ro
    healthcheck:
      test: [ "CMD-SHELL", "curl -fsS http://localhost:4566/_localstack/health | grep -q '\"initialized\": true'" ]
      interval: 5s
      timeout: 3s
      retries: 30
      start_period: 10s

volumes:
  localstack_data:
    name: localstack_data

networks:
  default:
    name: aws_default
    driver: bridge
```

---

## 9) 종료 & 초기화

### 종료 (데이터 유지)
```bash
./stop.sh aws
```

### 종료 + 데이터 초기화
```bash
./stop.sh aws --volumes
```

---

## 10) 유용한 명령어

- **로컬 AWS CLI 접속**
```bash
docker exec -it aws-localstack-1 awslocal s3 ls
```

- **DynamoDB 테이블 조회**
```bash
docker exec -it aws-localstack-1 awslocal dynamodb list-tables
```

- **Secrets Manager 조회**
```bash
docker exec -it aws-localstack-1 awslocal secretsmanager list-secrets
```

---

## 11) 주의

- LocalStack은 운영 대체용이 아니라, **로컬 개발/테스트 전용**입니다.
- 일부 서비스는 AWS 완전 구현이 아닌 Mock 수준일 수 있습니다.
- 데이터 유지 필요 없을 시 `PERSISTENCE=0` 로 설정해도 됩니다.

---
