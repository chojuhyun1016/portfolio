# Portfolio Project
====

## Local 환경 구축

### 1) 스크립트 권한 설정(최초 1회)
```bash
cd docker/local
chmod 755 start.sh
chmod 755 stop.sh
```

### 2) docker compose 실행/종료(일괄 또는 선택)
> **실행 경로**는 `docker/local` 입니다. 이 경로에는 `./aws`, `./kafka`, `./mysql`, `./redis` 하위 디렉토리가 있고, 각 디렉토리 안에 `docker-compose.yml` 가 있습니다.

```bash
# 모든 스택 기동(aws, kafka, mysql, redis)
./start.sh

# 특정 스택만 기동(예: kafka, mysql)
./start.sh kafka mysql

# (옵션) 재빌드
./start.sh --build           # 모든 스택
./start.sh kafka --build     # 선택 스택만

# (옵션) 재생성(skip)
./start.sh --no-recreate

# 모든 스택 종료(데이터 유지)
./stop.sh

# 특정 스택만 종료
./stop.sh kafka mysql

# (옵션) 볼륨까지 삭제(데이터 삭제 주의)
./stop.sh --volumes          # 전체
./stop.sh mysql --volumes    # 선택 스택만
```

**동작 개요**
- `start.sh`
  - 각 하위 디렉토리(aws/kafka/mysql/redis)의 `docker-compose.yml` 를 **독립 프로젝트**로 실행합니다.
  - 기본은 **재생성 모드**(stop → rm → up)로 컨테이너 이름 충돌을 예방합니다.
  - 컨테이너별 **healthcheck** 를 감지해 “wait health…” 단계에서 **최대 N초** 대기합니다(정상화 확인).


- `stop.sh`
  - 선택한 스택만 **stop + rm -f** 하며, `--volumes` 옵션 시 명명된 볼륨까지 삭제합니다(데이터 소거).

> 참고: **healthcheck 지연**은 특히 Kafka(Zookeeper) 초기화나 LocalStack 서비스 준비에 따라 시간이 늘어날 수 있습니다. 신뢰성(충돌/레이스 방지)이 중요하다면 현재 방식 유지가 권장됩니다. 속도가 더 중요하다면 `start.sh` 에서 헬스 대기 타임아웃을 줄일 수 있습니다.

---

## 도커 컴포즈 구성(요약)

각 스택은 **독립 디렉토리**에서 돌아갑니다.

- **MySQL (`./mysql/docker-compose.yml`)**
  - 이미지: `mysql:8.3`
  - 컨테이너: `mysql-local`
  - 포트: `3306:3306`
  - 환경: `root/root`, DB `order_local`, 유저 `order/order1234`, TZ `Asia/Seoul`
  - 볼륨: `mysql_data` (데이터 유지)
  - 헬스체크: `mysqladmin ping` 기반
  - 권장: 애플리케이션 `spring.datasource.url` 은 `jdbc:mysql://localhost:3306/order_local...`


- **Redis (`./redis/docker-compose.yml`)**
  - 이미지: `redis:${REDIS_VERSION}` (예: 7.2)
  - 컨테이너: `redis-local`
  - 포트: `${REDIS_PORT}:6379` (예: 6379)
  - 볼륨: `./data:/data` (AOF/스냅샷 데이터 유지)
  - 비밀번호: `REDIS_PASSWORD` 환경변수 사용 시 `--requirepass` 자동 적용
  - 헬스체크: `redis-cli ping`


- **LocalStack (`./aws/docker-compose.yml`)**
  - 이미지: `localstack/localstack:3`
  - 컨테이너: `localstack-aws`
  - 포트: `4566:4566` (Edge)
  - 서비스: `s3,dynamodb,secretsmanager`
  - 볼륨: `localstack_data` (데이터 유지), 초기 스크립트 `./localstack/init/ready.d/`


- **Kafka (`./kafka/docker-compose.yml`)**
  - Zookeeper: `confluentinc/cp-zookeeper:7.6.1`, 포트 `2181:2181`
  - Kafka: `confluentinc/cp-kafka:7.6.1`, 포트 `9092:9092`, `29092:29092`
    - `ADVERTISED_LISTENERS`: `PLAINTEXT://kafka:9092, PLAINTEXT_HOST://localhost:29092`
  - Kafka UI: `provectuslabs/kafka-ui:latest`, 포트 `8080:8080`
  - 헬스체크: Kafka/UI는 healthy, Zookeeper는 일부 OS에서 **UNHEALTHY 로 보이나 동작엔 문제 없음**(로그로 확인).

> **컨테이너명 충돌 방지**: 각 compose 파일에서 **명시 컨테이너명** 또는 디폴트 네이밍을 일관되게 사용했고, `start.sh` 가 **stop → rm → up** 순으로 처리하여 충돌을 최소화했습니다. 수동으로 띄운 컨테이너가 남아 있다면 `docker rm -f <name>` 로 정리하세요.

---

## 애플리케이션 설정(핵심 발췌)

### 1) 데이터소스/JPA/Flyway(로컬 예시)
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/order_local?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul&allowPublicKeyRetrieval=true&useSSL=false
    username: order
    password: order1234
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        show_sql: true
        format_sql: true
  flyway:
    enabled: true
    locations: classpath:db/migration
```

### 2) Redis(필요 시)
```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0           # 기본 캐시용은 0번을 보편적으로 사용
    # password: your-pass
```

### 3) AWS(LocalStack) + SecretsManager
```yaml
aws:
  region: ap-northeast-2
  endpoint: http://localhost:4566
  credential:
    enabled: true
    access-key: local
    secret-key: local
  s3:
    enabled: true
    bucket: my-local-bucket
    default-folder: logs            # ✅ 버킷 내 prefix (시스템 경로가 아님)
  secrets-manager:
    enabled: true
    secret-name: myapp/secret-key
    scheduler-enabled: true
    refresh-interval-millis: 300000
    fail-fast: true
```

> **주의(경로 의미)**: `aws.s3.default-folder` 는 **S3 버킷 내부 프리픽스** 입니다. `/app/logs` 같은 **호스트/컨테이너 파일시스템 경로가 아닙니다**.  
> 애플리케이션 로그 파일 경로는 별도로 `logging.file.path: /app/logs` 등으로 설정하세요.

---

## 빌드/테스트

### 전체 빌드
```bash
./gradlew clean build
```

### 통합 테스트(예: order-worker)
```bash
./gradlew :order-worker:integrationTest
```

**테스트 팁**
- 외부 인프라가 필요 없는 테스트는 `spring.autoconfigure.exclude` 로 관련 오토컨피그를 제외하거나 슬라이스 테스트(`@DataJpaTest` 등)를 사용하세요.
- Embedded Kafka 테스트는 `spring-kafka-test` 로 **수동 Producer/Consumer 라운드트립**을 권장합니다.

---

## 트러블슈팅

**1) `no configuration file provided: not found`**
- `start.sh`/`stop.sh` 는 **`docker/local`** 에서 실행해야 합니다.  
  해당 경로에 `./aws/docker-compose.yml` 등 compose 파일이 있어야 합니다.

**2) 컨테이너 이름 충돌(Conflict. name is already in use)**
- 기존 수동 실행 컨테이너가 남아있을 수 있습니다.
```bash
docker ps -a | grep zookeeper
docker rm -f zookeeper    # 예시
```
- 또는 `./stop.sh` 로 먼저 정리 후 `./start.sh` 를 실행하세요.

**3) `wait health...` 가 오래 걸림**
- Kafka/Zookeeper 초기화, LocalStack 서비스 준비에 따라 수십 초가 필요할 수 있습니다.
- 신뢰성이 우선이면 **현 상태 유지** 권장.
- 속도가 더 중요하면 `start.sh` 의 헬스 대기 타임아웃을 줄이거나 특정 서비스 헬스 대기 자체를 비활성화할 수 있습니다.

**4) 데이터 보존/삭제**
- `./stop.sh` 만 실행 → **데이터 유지** (볼륨 보존)
- `./stop.sh --volumes` → **데이터 삭제** (명명된 볼륨 제거)

---

## 한 줄 요약

**`docker/local`** 에서 `start.sh` / `stop.sh` 로 **aws, kafka, mysql, redis** 를 일괄/선택 기동·종료하고,  
애플리케이션은 **Flyway + 헬스체크 기반**으로 안정적으로 개발/테스트할 수 있습니다.