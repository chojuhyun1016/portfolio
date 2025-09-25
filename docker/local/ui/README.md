# 🌐 Local Infra UI Suite (Docker Compose)

로컬에서 기동한 Kafka, MySQL, Redis, LocalStack(DynamoDB 등)을 **웹 UI로 관리**하기 위한 통합 Compose 설정입니다.  
Kafka UI · Adminer · RedisInsight · DynamoDB Admin을 한 번에 띄울 수 있습니다.

---

## 📂 구조

```
ui/
├─ docker-compose.yml
├─ .env
```

- `.env` 파일에 포트/엔드포인트/시간대(TZ) 등을 정의
- 외부 네트워크(`*_default`)에 연결하여 각 서비스와 연동

---

## 🚀 빠른 시작

```bash
cd ui
docker compose up -d
docker compose ps
```

확인:
```bash
docker compose logs -f kafka-ui
```

---

## 🔑 접속 URL

| 서비스           | 주소                           | 설명 |
|------------------|-------------------------------|------|
| **Kafka UI**     | [http://localhost:${KAFKA_UI_PORT}](http://localhost:${KAFKA_UI_PORT}) | Kafka 브로커/토픽/메시지 관리 |
| **Adminer**      | [http://localhost:${ADMINER_PORT}](http://localhost:${ADMINER_PORT})   | MySQL DB 관리 |
| **RedisInsight** | [http://localhost:${REDISINSIGHT_PORT}](http://localhost:${REDISINSIGHT_PORT}) | Redis 관리/모니터링 |
| **DynamoDB Admin** | [http://localhost:${DYNAMODB_ADMIN_PORT}](http://localhost:${DYNAMODB_ADMIN_PORT}) | LocalStack DynamoDB UI |

---

## ⚙️ 사용 방법

### Kafka UI
- 접속: `http://localhost:${KAFKA_UI_PORT}`
- Cluster name: **local**
- 연결: 내부 네트워크로 `kafka:9092` 사용 (자동 구성됨)
- 주요 기능:
    - Topic 생성/삭제
    - Consumer Group 모니터링
    - 메시지 조회/전송

### Adminer (MySQL UI)
- 접속: `http://localhost:${ADMINER_PORT}`
- 서버: `${MYSQL_HOST}` (보통 `mysql`)
- 사용자: `order`
- 패스워드: `order1234`
- DB: `order_local`

### RedisInsight
- 접속: `http://localhost:${REDISINSIGHT_PORT}`
- 처음 접속 시 **연결 추가** 필요:
    - Host: `redis`
    - Port: `6379`
    - Alias: `local-redis`
    - Password: `.env`에서 `REDIS_PASSWORD` 설정 시 입력
- 주요 기능:
    - Key 탐색
    - 성능 모니터링
    - Pub/Sub 시각화

### DynamoDB Admin
- 접속: `http://localhost:${DYNAMODB_ADMIN_PORT}`
- Endpoint: `${DYNAMODB_ENDPOINT}` (보통 `http://localstack:4566`)
- Region: `${AWS_REGION}` (예: `ap-northeast-2`)
- 주요 기능:
    - 테이블 생성/조회
    - 아이템 CRUD
    - 쿼리 실행

---

## 📄 `.env` 예시

```env
TZ=Asia/Seoul

# Kafka UI
KAFKA_UI_PORT=8081
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ZOOKEEPER_CONNECT=zookeeper:2181

# Adminer
ADMINER_PORT=8082
MYSQL_HOST=mysql

# RedisInsight
REDISINSIGHT_PORT=8083

# DynamoDB Admin
DYNAMODB_ADMIN_PORT=8084
DYNAMODB_ENDPOINT=http://localstack:4566
AWS_REGION=ap-northeast-2
```

---

## 📝 Compose 파일 (전체)

```yaml
networks:
  kafka_net:
    external: true
    name: kafka_default
  mysql_net:
    external: true
    name: mysql_default
  redis_net:
    external: true
    name: redis_default
  aws_net:
    external: true
    name: aws_default

services:
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    container_name: kafka-ui
    restart: unless-stopped
    networks:
      - kafka_net
    ports:
      - "${KAFKA_UI_PORT}:8080"
    environment:
      TZ: ${TZ}
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: ${KAFKA_BOOTSTRAP_SERVERS}
      KAFKA_CLUSTERS_0_ZOOKEEPER: ${ZOOKEEPER_CONNECT}
    healthcheck:
      test: ["CMD", "wget", "-qO-", "http://localhost:8080/actuator/health"]
      interval: 5s
      timeout: 3s
      retries: 30

  adminer:
    image: adminer:latest
    container_name: adminer
    restart: unless-stopped
    networks:
      - mysql_net
    ports:
      - "${ADMINER_PORT}:8080"
    environment:
      TZ: ${TZ}
      ADMINER_DEFAULT_SERVER: ${MYSQL_HOST}

  redisinsight:
    image: redis/redisinsight:latest
    container_name: redisinsight
    restart: unless-stopped
    networks:
      - redis_net
    ports:
      - "${REDISINSIGHT_PORT}:5540"
    environment:
      TZ: ${TZ}

  dynamodb-admin:
    image: aaronshaf/dynamodb-admin:latest
    container_name: dynamodb-admin
    restart: unless-stopped
    networks:
      - aws_net
    ports:
      - "${DYNAMODB_ADMIN_PORT}:8001"
    environment:
      TZ: ${TZ}
      DYNAMO_ENDPOINT: ${DYNAMODB_ENDPOINT}
      AWS_REGION: ${AWS_REGION}
    healthcheck:
      disable: true
```

---

## ✅ 특징

- 네트워크 분리(`kafka_default`, `mysql_default`, `redis_default`, `aws_default`)
- UI 서비스들이 해당 네트워크에 연결되어 자동으로 백엔드 컨테이너 인식
- `.env` 파일로 포트/호스트/엔드포인트 관리
- Healthcheck 내장 (Kafka UI)

---

## ⚡ 자주 쓰는 명령어

- 기동:
```bash
docker compose up -d
```

- 종료:
```bash
docker compose down
```

- 상태 확인:
```bash
docker compose ps
```

- 로그 확인:
```bash
docker compose logs -f kafka-ui
```

---

## 🎯 한 줄 요약

**“Kafka · MySQL · Redis · DynamoDB를 웹 UI에서 직관적으로 관리 — 개발 환경 필수 패키지.”**
