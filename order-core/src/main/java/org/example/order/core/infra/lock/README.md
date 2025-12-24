# 🔒 Lock 모듈 (DB NamedLock + Redis RedissonLock)

Spring Boot에서 **DB 기반 NamedLock** 또는 **Redis 기반 RedissonLock** 을 선택적으로 사용하여 분산락을 구현하는 모듈입니다.  
애노테이션 한 줄로 락 종류와 트랜잭션 전파(기존/새 트랜잭션)를 지정할 수 있으며,  
Kafka / S3 / TSID / Secrets 모듈과 동일한 **설정 기반(@Bean) + 단일 조립(@Import)** 패턴으로 동작합니다.

--------------------------------------------------------------------------------
## 1) 구성 개요

| 구성 요소 | 설명 |
|---|---|
| `@DistributedLock` | 기존 트랜잭션(`REQUIRED`) 유지 후 임계영역 실행 |
| `@DistributedLockT` | 새 트랜잭션(`REQUIRES_NEW`)에서 임계영역 실행 |
| `DistributedLockAspect` | 애노테이션 파라미터 파싱 → 키 생성기/실행기 선택 → 트랜잭션 래퍼 호출 |
| `LockKeyGenerator` | 키 생성 전략 SPI |
| `SHA256LockKeyGenerator` | 메서드 인자들을 결합 후 SHA-256 해시 |
| `SpelLockKeyGenerator` | SpEL(`'ORD:' + #orderId`) 평가 결과를 키로 사용 |
| `SimpleLockKeyGenerator` | 리터럴 문자열 결합 전용 단순 키 생성 |
| `LockExecutor` | 실행기 SPI |
| `NamedLockExecutor` | DB `GET_LOCK / RELEASE_LOCK` 기반 분산락 |
| `RedissonLockExecutor` | Redis `RLock.tryLock()` 기반 분산락 |
| `LockKeyGeneratorFactory` | keyStrategy 이름으로 키 생성기 선택 |
| `LockExecutorFactory` | type 이름으로 실행기 선택 |
| **`LockInfraConfig`** | 단일 설정 진입점, 조건부 Bean 조립 |
| `TransactionalOperator` | `REQUIRED` / `REQUIRES_NEW` 트랜잭션 래핑 |
| `NamedLockProperties` | DB NamedLock 설정 바인딩 |
| `RedissonLockProperties` | Redisson 락 설정 바인딩 |

변경 요약
- 여러 개의 락 설정 클래스를 **LockInfraConfig 하나로 통합**
- 전역 enable 스위치 제거
- 실행기별 스위치만 사용
  - `lock.named.enabled`
  - `lock.redisson.enabled`
- Redisson 설정은 **lock.redisson.* 만 사용** (spring.redis.* 미사용)

--------------------------------------------------------------------------------
## 2) 동작 모드

### 2.1 OFF (기본)

아무 실행기도 활성화하지 않으면 락 인프라는 로딩되지 않습니다.

    lock:
      named:
        enabled: false
      redisson:
        enabled: false

- Aspect / Factory / Executor 전부 미등록
- 애노테이션 사용 시 즉시 예외 발생

--------------------------------------------------------------------------------
### 2.2 NamedLock(DB) 모드

    lock:
      named:
        enabled: true
        wait-time: 3000
        retry-interval: 150
      redisson:
        enabled: false

- MySQL / MariaDB 의 `GET_LOCK`, `RELEASE_LOCK` 사용
- `DataSource` 빈 필수
- 커넥션은 `DataSourceUtils` 로 획득/반납
- 재시도 기반 획득 로직 내장

--------------------------------------------------------------------------------
### 2.3 RedissonLock(REDIS) 모드

    lock:
      named:
        enabled: false
      redisson:
        enabled: true
        host: 127.0.0.1
        port: 6379
        database: 0
        password:
        wait-time: 3000
        lease-time: 10000
        retry-interval: 150

- RedissonClient 를 **직접 생성**
- `lock.redisson.host + port` 필수
- `redis://`, `rediss://` 자동 보정
- Spring Redis AutoConfiguration 과 완전히 분리

--------------------------------------------------------------------------------
## 3) 동작 흐름

    Caller (@DistributedLock / @DistributedLockT)
     └─ DistributedLockAspect
         1) 애노테이션 파라미터 추출
            - key
            - type
            - keyStrategy
            - waitTime
            - leaseTime
         2) LockKeyGeneratorFactory.getGenerator(keyStrategy)
         3) LockExecutorFactory.getExecutor(type)
         4) executor.execute(key, wait, lease, callback)
               └─ callback
                   ├─ @DistributedLock  → TransactionalOperator.runWithExistingTransaction
                   └─ @DistributedLockT → TransactionalOperator.runWithNewTransaction

- `@DistributedLock`  → 기존 트랜잭션(REQUIRED)
- `@DistributedLockT` → 새 트랜잭션(REQUIRES_NEW)

--------------------------------------------------------------------------------
## 4) 빠른 시작 (설정 기반 + @Import 조립)

### 4.1 의존성 (Gradle)

    dependencies {
      implementation "org.springframework.boot:spring-boot-starter-aop"
      implementation "org.springframework.boot:spring-boot-starter-jdbc"
      implementation "org.redisson:redisson:3.27.2"
    }

--------------------------------------------------------------------------------
### 4.2 구성 조립

    @Import(org.example.order.core.infra.lock.config.LockInfraConfig.class)
    public class App {
    }

--------------------------------------------------------------------------------
### 4.3 설정(YAML)

    lock:
      named:
        enabled: true
        wait-time: 3000
        retry-interval: 150

      redisson:
        enabled: true
        host: 127.0.0.1
        port: 6379
        database: 0
        wait-time: 3000
        lease-time: 10000
        retry-interval: 150

--------------------------------------------------------------------------------
## 5) 사용 예시 (애노테이션 한 줄)

### 5.1 NamedLock + 기존 트랜잭션(REQUIRED)

    @DistributedLock(
      key = "'ORD:' + #orderId",
      type = "namedLock",
      keyStrategy = "spell",
      waitTime = 3000,
      leaseTime = 10000
    )
    public void processOrder(String orderId) {
        // 임계영역
    }

--------------------------------------------------------------------------------
### 5.2 RedissonLock + 새 트랜잭션(REQUIRES_NEW)

    @DistributedLockT(
      key = "'INV:' + #invoiceId",
      type = "redissonLock",
      keyStrategy = "spell",
      waitTime = 5000,
      leaseTime = 15000
    )
    public void settleInvoice(String invoiceId) {
        // 임계영역
    }

--------------------------------------------------------------------------------
### 5.3 키 전략 선택 가이드

- spell
  - SpEL 기반
  - 복잡한 키 조합, 가독성 우수

- sha256
  - 키 길이 고정
  - 외부 노출 최소화

- simple
  - 리터럴 문자열 결합 전용
  - 디버깅/로그 가독성 최우선

--------------------------------------------------------------------------------
## 6) 고급 설정 / 운영 팁

### 6.1 NamedLock 주의사항
- MySQL / MariaDB 전용
- DB 커넥션 점유 시간 = 락 보유 시간
- 키 네임스페이스 명확히 분리 권장
  - 예: `order:payment:123`

--------------------------------------------------------------------------------
### 6.2 RedissonLock 주의사항
- lease-time 초과 시 자동 해제
- 긴 비즈니스 로직은 분리 권장
- retry-interval 은 너무 작지 않게 설정

--------------------------------------------------------------------------------
### 6.3 트랜잭션 경계
- 락 외부에서 트랜잭션 시작하지 말 것
- 락 내부에서 명확한 경계 유지

--------------------------------------------------------------------------------
## 7) 예외 / 오류

- `LockAcquisitionException`
  - 대기 시간 초과
  - 재시도 실패
  - 인터럽트 발생

- `IllegalArgumentException`
  - 존재하지 않는 type
  - 존재하지 않는 keyStrategy

모든 예외는 로그 후 상위로 전파됩니다.

--------------------------------------------------------------------------------
## 8) 테스트 가이드

### 8.1 단위 테스트 (락 미사용)

    lock:
      named:
        enabled: false
      redisson:
        enabled: false

--------------------------------------------------------------------------------
### 8.2 통합 테스트

- NamedLock
  - MySQL / MariaDB Testcontainers
  - `GET_LOCK` 동작 검증

- RedissonLock
  - Redis Testcontainers
  - `lock.redisson.host / port` 동적 주입

--------------------------------------------------------------------------------
## 9) FAQ

Q. NamedLock 과 RedissonLock 을 동시에 켤 수 있나요?  
A. 가능합니다. 메서드 단위로 `type` 으로 선택합니다.

Q. Redisson 은 spring.redis 설정을 사용하나요?  
A. 사용하지 않습니다. `lock.redisson.*` 만 사용합니다.

Q. 실행기 없이 애노테이션을 쓰면?  
A. 즉시 `IllegalArgumentException` 이 발생합니다.

--------------------------------------------------------------------------------
## 10) 마지막 한 줄 요약

**애노테이션 한 줄 + 실행기 스위치 설정**만으로  
DB / Redis 분산락을 명확하고 안전하게 적용합니다.
