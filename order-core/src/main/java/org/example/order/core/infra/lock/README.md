# 🔒 Lock 모듈 (DB NamedLock + Redis RedissonLock)

Spring Boot에서 **DB 기반 NamedLock** 또는 **Redis 기반 RedissonLock** 을 선택적으로 사용하여 분산락을 구현하는 모듈입니다.  
애노테이션 한 줄로 락 종류와 트랜잭션 전파(기존/새 트랜잭션)를 지정할 수 있으며, Kafka / S3 / TSID / Secrets 모듈과 동일하게 **설정 기반(@Bean) + 단일 조립(@Import)** 패턴으로 동작합니다.

---

## 1) 구성 개요

| 구성 요소 | 설명 |
|---|---|
| `@DistributedLock` | 기존 트랜잭션(`REQUIRED`) 유지 후 임계영역 실행 |
| `@DistributedLockT` | 새 트랜잭션(`REQUIRES_NEW`)에서 임계영역 실행 |
| `DistributedLockAspect` | 애노테이션 파라미터 파싱 → 키 생성기/실행기 선택 → 트랜잭션 래퍼 호출 |
| `LockKeyGenerator` | 키 생성 전략 SPI |
| `SHA256LockKeyGenerator` | 인자들을 이어붙인 문자열의 SHA-256 해시 |
| `SpelLockKeyGenerator` | SpEL(`'ORD:' + #orderId`) 평가 결과를 키로 사용 |
| `SimpleLockKeyGenerator` | `"prefix_arg1_arg2"` 형태의 단순 결합 |
| `LockExecutor` | 실행기 SPI |
| `NamedLockExecutor` | DB `GET_LOCK/RELEASE_LOCK` 기반 분산락 |
| `RedissonLockExecutor` | Redis `RLock.tryLock()` 기반 분산락 |
| `LockKeyGeneratorFactory` / `LockExecutorFactory` | 이름으로 전략/실행기 조회 |
| **`LockInfraConfig`** | 전역 단일 설정(신규). 조건부 Bean 등록과 조립 담당 |
| `TransactionalOperator` | `REQUIRED` / `REQUIRES_NEW` 트랜잭션 래핑 유틸 |
| `NamedLockProperties` / `RedissonLockProperties` | 설정 프로퍼티 바인딩 |

> 변경 요약
> - `LockManualConfig`, `NamedLockAutoConfig`, `RedissonLockAutoConfig`를 **`LockInfraConfig` 하나**로 통합했습니다.
> - 전역 스위치(`lock.enabled`)가 `false`이면 어떤 Bean도 등록되지 않습니다.
> - 실행기별 스위치(`lock.named.enabled`, `lock.redisson.enabled`)로 개별 실행기를 선택적으로 로딩합니다.
> - Redisson 주소는 `lock.redisson.address` → `lock.redisson.uri` → `spring.data.redis.host/port` 우선순위로 결정합니다.

---

## 2) 동작 모드

### 2.1 OFF (기본)

    lock:
      enabled: false

- 어떤 빈도 등록되지 않음(Aspect/Factory/Executor 전부 미로딩)

### 2.2 NamedLock(DB) 모드

    lock:
      enabled: true
      named:
        enabled: true
        wait-time: 3000       # ms
        retry-interval: 150   # ms
      redisson:
        enabled: false

- MySQL/MariaDB 의 `GET_LOCK`, `RELEASE_LOCK` 사용
- `DataSource` 빈 필수

### 2.3 RedissonLock(REDIS) 모드

    lock:
      enabled: true
      named:
        enabled: false
      redisson:
        enabled: true
        address: redis://127.0.0.1:6379
        database: 0
        wait-time: 3000       # ms (획득 대기 상한)
        lease-time: 10000     # ms (임대 시간)
        retry-interval: 150   # ms (재시도 간격)

- `RedissonClient`를 설정에서 직접 구성(Starter 유무와 무관)
- TLS 사용 시 `rediss://host:port`

> 전역 스위치: `lock.enabled=false`면 어떤 실행기도 등록되지 않습니다.  
> 세부 스위치: `lock.named.enabled`, `lock.redisson.enabled` 로 실행기 선택/병행 활성화 가능(메서드별 `type`으로 분기).

---

## 3) 동작 흐름

    Caller (@DistributedLock or @DistributedLockT)
     └─ DistributedLockAspect
         1) annotation 파라미터 추출(key, type, keyStrategy, waitTime, leaseTime)
         2) LockKeyGeneratorFactory.getGenerator(keyStrategy) → 키 생성
         3) LockExecutorFactory.getExecutor(type) → 실행기 선택
         4) executor.execute(key, wait, lease, callback)
               └─ callback: TransactionalOperator.runWith(REQUIRED or REQUIRES_NEW)

- `@DistributedLock`  → 기존 트랜잭션(REQUIRED)
- `@DistributedLockT` → 새 트랜잭션(REQUIRES_NEW)

---

## 4) 빠른 시작 (설정 기반 + @Import 조립)

### 4.1 의존성 (예: Gradle)

    dependencies {
      implementation "org.springframework.boot:spring-boot-starter-aop"     // Aspect 동작 필수
      implementation "org.springframework.boot:spring-boot-starter-jdbc"    // NamedLock(DB)
      implementation "org.redisson:redisson:3.27.2"                         // Redisson 클라이언트
      // redisson-spring-boot-starter를 쓰는 경우, 테스트 환경에서 자동구성 충돌을 피하려면 exclude 고려
    }

### 4.2 구성 조립

    // @SpringBootApplication 클래스 혹은 별도 설정 클래스
    @Import(org.example.order.core.infra.lock.config.LockInfraConfig.class)
    public class App {}

### 4.3 설정(YAML)

    lock:
      enabled: true
      named:
        enabled: true
        wait-time: 3000
        retry-interval: 150
      redisson:
        enabled: true
        address: redis://127.0.0.1:6379
        database: 0
        wait-time: 3000
        lease-time: 10000
        retry-interval: 150

---

## 5) 사용 예시 (애노테이션 한 줄)

### 5.1 NamedLock + 기존 트랜잭션 유지(REQUIRED)

    @DistributedLock(
      key = "'ORD:' + #orderId",
      type = "namedLock",
      keyStrategy = "spell",     // spell|sha256|simple
      waitTime = 3000,
      leaseTime = 10000
    )
    public void processOrder(String orderId) {
      // 임계 구역 (현재 트랜잭션에서 실행)
    }

### 5.2 RedissonLock + 새 트랜잭션(REQUIRES_NEW)

    @DistributedLockT(
      key = "'INV:' + #invoiceId",
      type = "redissonLock",
      keyStrategy = "spell",
      waitTime = 5000,
      leaseTime = 15000
    )
    public void settleInvoice(String invoiceId) {
      // 임계 구역 (새 트랜잭션으로 실행)
    }

### 5.3 키 전략 선택 가이드
- `spell`  : SpEL. 예) `"'USER:' + #userId"`
- `sha256` : 메서드 인자들을 이어붙여 해시(SHA-256). 길이 고정, 키 노출 방지에 유리
- `simple` : `"prefix_arg1_arg2"` 형식 단순 결합. 로그/디버깅 가독성 우수

> 실무 팁: 외부 노출 위험이 있다면 `sha256`, 내부/디버깅 편의가 우선이면 `simple`, 파라미터 조합이 복잡하면 `spell` 권장.

---

## 6) 고급 설정/운영 팁

### 6.1 Redisson 엔드포인트 우선순위
- 주소 선택 순서: `lock.redisson.address` → `lock.redisson.uri` → `spring.data.redis.host/port`
- 단일 서버 예시:

        lock:
          redisson:
            enabled: true
            address: redis://cache.example.com:6379
            database: 1
            wait-time: 2000
            lease-time: 8000
            retry-interval: 100

- TLS:

        lock:
          redisson:
            enabled: true
            address: rediss://cache.example.com:6380
            database: 0

- Redisson Starter를 사용하는 경우, 테스트에서 자동구성이 로컬 6379로 붙으면서 충돌할 수 있습니다.  
  필요 시 테스트 프로파일에 다음과 같이 제외합니다:

        spring:
          autoconfigure:
            exclude:
              - org.redisson.spring.starter.RedissonAutoConfigurationV2
              - org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
              - org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration
              - org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration

### 6.2 NamedLock 주의사항
- MySQL/MariaDB에서 `GET_LOCK(name, timeoutSeconds)` / `RELEASE_LOCK(name)` 사용
- H2로 대체 테스트 시 `MODE=MySQL` 등 호환 모드 사용 권장
- 키 네임스페이스: `서비스:도메인:리소스ID` 형태(예: `ord:payment:123`)

### 6.3 트랜잭션 전파
- `@DistributedLock`  → `REQUIRED`
- `@DistributedLockT` → `REQUIRES_NEW`
- 임계영역의 수행 시간이 `lease-time`을 초과하지 않도록 쿼리/로직 분리 권장

### 6.4 관측성
- 경합률/획득 실패율/재시도 횟수 등을 메트릭/로그로 관찰
- 실패 시 원인 구분: 대기타임아웃(wait-time), 임대만료(lease-time), 외부(네트워크/DB/Redis) 문제

---

## 7) 예외/오류

- `LockAcquisitionException` : `waitTime` 내 락 획득 실패
- `IllegalArgumentException` : 존재하지 않는 `type`/`keyStrategy` 요청, 실행기 미등록
- `InterruptedException`     : 재시도 대기 중 인터럽트(플래그 복구 후 예외 전파)
- DB/Redis 예외는 로그와 함께 상위로 전파됩니다.

---

## 8) 테스트 가이드 (yml 기준)

### 8.1 단위 테스트 기본

    spring:
      main:
        web-application-type: none
      datasource:
        url: jdbc:h2:mem:unit;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false
        driver-class-name: org.h2.Driver
        username: sa
        password:
      jpa:
        hibernate:
          ddl-auto: none
        show-sql: false
        properties:
          hibernate:
            dialect: org.hibernate.dialect.H2Dialect

    lock:
      enabled: false   # 기본 OFF (테스트별로 필요 시 켭니다)

### 8.2 통합 테스트 샘플 (Testcontainers로 엔드포인트 주입)

    lock:
      enabled: true

      named:
        enabled: true
        wait-time: 3000
        retry-interval: 150

      redisson:
        enabled: true
        database: 0
        wait-time: 3000
        lease-time: 10000
        retry-interval: 150

- Redis 컨테이너 기동 후 동적으로 주소 주입(예: JUnit5 `@DynamicPropertySource`):

        @DynamicPropertySource
        static void redisProps(DynamicPropertyRegistry r) {
            String host = REDIS.getHost();
            Integer port = REDIS.getMappedPort(6379);
            String uri = "redis://" + host + ":" + port;

            r.add("lock.redisson.address", () -> uri); // LockInfraConfig가 읽음
            r.add("spring.data.redis.host", () -> host);
            r.add("spring.data.redis.port", () -> port);
        }

- NamedLock IT는 MySQL Testcontainers(Hikari)로 구성하고, DB URL/계정은 동일하게 `@DynamicPropertySource`로 주입

---

## 9) 자주 묻는 질문(FAQ)

**Q1. NamedLock 과 RedissonLock 을 동시에 켤 수 있나요?**  
A. 예. 둘 다 활성화해 두고, 메서드별로 애노테이션 `type`(namedLock/redissonLock) 으로 선택합니다.

**Q2. NamedLock 은 어떤 DB가 필요한가요?**  
A. MySQL/MariaDB 처럼 `GET_LOCK/RELEASE_LOCK` 함수를 지원하는 DB가 필요합니다(H2 테스트는 호환 모드로 대체).

**Q3. Redisson 주소는 어떻게 주나요?**  
A. 우선순위: `lock.redisson.address` → `lock.redisson.uri` → `spring.data.redis.host/port`. TLS 는 `rediss://`.

**Q4. 실행기를 하나도 안 켰는데 애노테이션을 붙이면?**  
A. `IllegalArgumentException` 이 발생합니다. 적어도 하나의 실행기를 활성화하세요.

---

## 10) 마지막 한 줄 요약

**전역/세부 토글 + 애노테이션 한 줄**로 **DB·Redis 분산락**을 손쉽게 적용합니다.  
`LockInfraConfig` 하나만 Import 하면, 환경설정에 따라 필요한 컴포넌트가 자동 조립됩니다.
