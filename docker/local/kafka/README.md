# 🐳 Kafka Local via Docker on macOS — Quick Start

로컬 개발을 위한 **단일 브로커 Kafka + Zookeeper** 환경을 `docker compose`로 빠르게 구성합니다.  
내부/외부 리스너를 분리해 **컨테이너 간(`kafka:9092`)**과 **호스트 앱(`localhost:29092`)** 모두 안정적으로 접속할 수 있습니다.

---

## 0) 요구 사항

- **Docker Desktop for Mac** (또는 Linux/WSL2)
- Docker Desktop 리소스 권장: **CPU 2코어, RAM 2GB+**

---

## 1) 시작하기

```bash
# 예시 디렉터리
cd kafka-local

# 백그라운드 기동
docker compose up -d

# (선택) Kafka 로그 확인
docker logs -f kafka
```

> 기동 후 **10~20초** 정도 기다리세요. Healthcheck가 준비되면 정상 동작합니다.

---

## 2) Kafka UI (선택)

- 기본 Compose에는 UI가 **포함되어 있지 않습니다**.
- UI가 필요하면 아래 *추가 서비스* 스니펫을 `docker-compose.yml`에 붙여넣고 기동하세요.

```yaml
# (선택) Kafka UI
  kafka-ui:
    image: provectuslabs/kafka-ui:latest
    depends_on:
      - kafka
    ports:
      - "8080:8080"
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
```

접속: <http://localhost:8080>  
클러스터 이름: `local` (위 환경변수 기준)

---

## 3) 토픽 생성 (호스트에서 컨테이너 CLI 사용)

```bash
# 예: demo-topic 생성
docker exec -it kafka \
  kafka-topics --bootstrap-server localhost:29092 \
  --create --topic demo-topic --partitions 1 --replication-factor 1
```

> ※ `--bootstrap-server localhost:29092` 는 **호스트용(EXTERNAL)** 입니다.  
> 컨테이너 안에서 내부 통신을 쓸 경우 `kafka:9092(INTERNAL)` 을 사용하세요.

---

## 4) 메시지 발행 (Producer)

```bash
docker exec -it kafka \
  kafka-console-producer --bootstrap-server localhost:29092 --topic demo-topic
# 이후 입력:
# hello
# world
# (종료: Ctrl+C)
```

---

## 5) 메시지 구독 (Consumer)

```bash
docker exec -it kafka \
  kafka-console-consumer --bootstrap-server localhost:29092 --topic demo-topic --from-beginning
```

---

## 6) 애플리케이션에서 접속 (호스트 → Kafka)

```text
bootstrap-servers = localhost:29092
```

Spring Boot 예:

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:29092
```

> 주의: 호스트 앱이 `kafka:9092` 로 붙으면 **UnknownHostException** 이 납니다.  
> 호스트에서는 반드시 **`localhost:29092`** 를 사용하세요.

---

## 7) 중지/내리기

```bash
# 컨테이너 중지 (리소스 유지)
docker compose stop

# 컨테이너/네트워크 제거 (볼륨 유지)
docker compose down

# 전부 제거 (볼륨까지 삭제 — 데이터 초기화)
docker compose down --volumes --remove-orphans
```

---

## 8) 트러블슈팅

- **호스트에서 연결 안 됨** → `localhost:29092` 사용 여부 확인
- **컨테이너 재시작 반복** → Docker Desktop 리소스(CPU/RAM) 늘리기
- **포트 충돌** → 아래 Compose의 `ports` 매핑을 다른 값으로 변경
   - 예) `kafka: "29092:29092" → "39092:29092"`
- **AdminClient Timeout/UnknownHost** → 애플리케이션이 내부 리스너(`kafka:9092`)로 붙지 않게 설정
- **토픽이 자동으로 생기지 않음** → 로컬 테스트에서만 `KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"` 가능(운영 비권장)

---

## 9) 유용한 명령어

```bash
# 브로커 상태 (토픽 목록)
docker exec -it kafka \
  kafka-topics --bootstrap-server localhost:29092 --list

# 특정 토픽 상세
docker exec -it kafka \
  kafka-topics --bootstrap-server localhost:29092 --describe --topic demo-topic

# 컨테이너 상태
docker compose ps

# 서비스 로그 팔로우
docker compose logs -f kafka
```

---

## 10) 디렉터리 구조(예시)

```
kafka-local/
├─ docker-compose.yml
└─ README.md (현재 문서)
```

---

## 11) docker-compose.yml (전체)

> 내부/외부 리스너를 분리해 **컨테이너 간은 `kafka:9092`**, **호스트 앱은 `localhost:29092`** 를 사용합니다.

```yaml
version: "3.9"

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.6.1
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "2181:2181"
    healthcheck:
      test: [ "CMD", "bash", "-c", "echo ruok | nc -w 2 localhost 2181 | grep imok" ]
      interval: 5s
      timeout: 3s
      retries: 20
    volumes:
      - zk_data:/var/lib/zookeeper/data
      - zk_datalog:/var/lib/zookeeper/log

  kafka:
    image: confluentinc/cp-kafka:7.6.1
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"    # 내부 네트워크(INTERNAL)
      - "29092:29092"  # 호스트(EXTERNAL)
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181

      # 리스너/프로토콜 맵
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: INTERNAL:PLAINTEXT,EXTERNAL:PLAINTEXT

      # 바인드 포트
      KAFKA_LISTENERS: INTERNAL://0.0.0.0:9092,EXTERNAL://0.0.0.0:29092

      # 광고 주소: 내부 컨테이너는 kafka:9092, 호스트 앱은 localhost:29092
      KAFKA_ADVERTISED_LISTENERS: INTERNAL://kafka:9092,EXTERNAL://localhost:29092

      # 브로커 간 통신은 INTERNAL
      KAFKA_INTER_BROKER_LISTENER_NAME: INTERNAL

      # 로컬 단일 브로커에서만 허용 (운영 비권장)
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
      KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1
      KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS: 0
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"  # 필요 시 true (로컬 전용)

    healthcheck:
      test: [ "CMD", "bash", "-lc", "kafka-topics --bootstrap-server localhost:29092 --list >/dev/null 2>&1" ]
      interval: 5s
      timeout: 5s
      retries: 20

    # (선택) 다른 컨테이너가 호스트 네트워크(예: http://host.docker.internal)를 호출할 때 필요
    extra_hosts:
      - "host.docker.internal:host-gateway"

    volumes:
      - kafka_data:/var/lib/kafka/data

volumes:
  zk_data:
    name: zk_data
  zk_datalog:
    name: zk_datalog
  kafka_data:
    name: kafka_data

networks:
  default:
    name: kafka_default
    driver: bridge
```

---

## 12) 포트 변경 예시

다른 프로세스와 충돌하면 좌측(호스트 포트)만 바꾸면 됩니다.

```yaml
    ports:
      - "9092:9092"    # 내부 유지
      - "39092:29092"  # 호스트 포트만 39092로 변경
```

앱 설정도 함께 변경:

```text
bootstrap-servers = localhost:39092
```

---

## 13) 참고 (운영/보안)

- 본 설정은 **로컬 개발** 전용입니다.
- 운영에서는
   - **SASL/SSL** 등 보안 설정 필수
   - **멀티 브로커** + 적정 **Replication Factor**
   - **`KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`** 유지

---
