# 🔒 Lock 모듈 테스트 가이드 (README)

---

## 📌 무엇을 테스트하나요?

아래 4가지를 **단위/경량 통합** 수준에서 검증합니다.

1) **NamedLock(DB 기반) 동시성**: 경합 상황에서 직렬화되는지 (`DistributedNamedLockTest`)
2) **Redisson(레디스 기반) 동시성**: 경합 상황에서 직렬화되는지 (`DistributedRedissonLockTest`)
3) **트랜잭션 전파 보장**: `REQUIRED` vs `REQUIRES_NEW` 동작 차이 (`DistributedTransactionTest`)
4) **설정 토글에 따른 빈 로딩 결과**: 켜고/끄기에 따라 생성되는 빈 확인 (`LockAutoConfigurationToggleTest`)

---

## 🧩 사용 기술 & 포인트

- **조건부 자동 구성 토글**  
  `@ConditionalOnProperty`, `@ConditionalOnBean`, `@ConditionalOnClass` 로 **필요할 때만** 로딩  
  → `lock.enabled`, `lock.named.enabled`, `lock.redisson.enabled` 로 제어

- **ApplicationContextRunner**  
  전체 컨텍스트 없이 **토글별 빈 로딩 결과**를 빠르게 검증 (메인 클래스 불필요)

- **@SpringBootTest + Testcontainers**  
  Redis 컨테이너로 **RedissonClient** 주입 (`TestRedisConfig`)  
  NamedLock은 **DB 제공**이 전제(실제 GET_LOCK/RELEASE_LOCK 제공 DB)

- **AOP + 트랜잭션 전파**  
  `@DistributedLock` → 기존 트랜잭션(REQUIRED)  
  `@DistributedLockT` → 새로운 트랜잭션(REQUIRES_NEW)

---

## ⚙️ 속성 기반 모드 제어

- **전역 스위치**
  - `lock.enabled=true` 일 때만 Lock 모듈이 동작

- **실행기(Executor) 선택**
  - NamedLock(DB) : `lock.named.enabled=true`
  - Redisson(REDIS) : `lock.redisson.enabled=true`

- **조합 가이드**
  - DB만 사용 → `lock.enabled=true`, `lock.named.enabled=true`, `lock.redisson.enabled=false`
  - Redis만 사용 → `lock.enabled=true`, `lock.named.enabled=false`, `lock.redisson.enabled=true`
  - 둘 다 끔 → 어떤 실행기도 생성되지 않음 (KeyGenerator/Aspect만)

예시(테스트에서 사용한 패턴과 동일):

```properties
# 공통
lock.enabled=true

# (1) NamedLock만
lock.named.enabled=true
lock.redisson.enabled=false

# (2) Redisson만
lock.named.enabled=false
lock.redisson.enabled=true
```

---

## 🧪 포함된 테스트 (요약 + 핵심 코드)

### 1) NamedLock(DB 기반) 동시성 — `DistributedNamedLockTest`

- 목적: 동시에 같은 키로 접근 시, DB NamedLock으로 **직렬화**되는지 확인
- 전제: 테스트 환경이 **GET_LOCK/RELEASE_LOCK** 함수를 제공(예: MySQL/호환)
- 속성: `lock.enabled=true`, `lock.named.enabled=true`, `lock.redisson.enabled=false`

```java
// 동일 key("test-key")로 10개 스레드 경합 → 일부는 대기/실패 처리
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "lock.enabled=true",
    "lock.named.enabled=true",
    "lock.redisson.enabled=false"
})
class DistributedNamedLockTest {
    @Test
    void testConcurrentLocking() { /* ... 경합 제출 → 성공/실패 카운트 검증 ... */ }
    @Test
    void testConcurrentLockingT() { /* ... REQUIRES_NEW 트랜잭션 경합 ... */ }
}
```

---

### 2) Redisson(레디스 기반) 동시성 — `DistributedRedissonLockTest`

- 목적: Redis 기반 분산락으로 **직렬화**되는지 확인
- 전제: `TestRedisConfig` 가 Testcontainers로 Redis & RedissonClient 제공
- 속성: `lock.enabled=true`, `lock.redisson.enabled=true`, `lock.named.enabled=false`

```java
// Testcontainers Redis 컨테이너 + RedissonClient 주입
@TestPropertySource(properties = {
    "lock.enabled=true",
    "lock.redisson.enabled=true",
    "lock.named.enabled=false"
})
class DistributedRedissonLockTest {
    @DistributedLock(key = "'lock-test'", type = "redissonLock", waitTime = 3000, leaseTime = 5000)
    public void criticalSection() { /* ... 경합 유발 sleep ... */ }

    @RepeatedTest(1)
    void testConcurrentLocking() { /* ... 10개 스레드 → counter == threadCount 검증 ... */ }
}
```

> **참고**: `TestRedisConfig` 가 `redis:7.0.5` 컨테이너를 띄우고 `RedissonClient` 를 구성합니다.

---

### 3) 트랜잭션 전파 보장 — `DistributedTransactionTest`

- 목적: 애스펙트 내부에서 **전파 속성**이 의도대로 작동하는지 검증
  - `@DistributedLock` → **REQUIRED**: 상위 트랜잭션 유지 → **동일 TX ID**
  - `@DistributedLockT` → **REQUIRES_NEW**: 새 트랜잭션 → **다른 TX ID**
- 속성: `lock.enabled=true`, `lock.named.enabled=true`, `lock.redisson.enabled=false`

```java
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "lock.enabled=true",
    "lock.named.enabled=true",
    "lock.redisson.enabled=false"
})
class DistributedTransactionTest {

    @Test @Transactional
    void testConcurrentLocking() {
        // REQUIRED: 내부 호출들이 모두 상위와 같은 TX ID인지 확인
    }

    @Test @Transactional
    void testConcurrentLockingT() {
        // REQUIRES_NEW: 내부 호출들이 상위와 다른 TX ID인지 확인
    }
}
```

---

### 4) 설정 토글 빈 로딩 검증 — `LockAutoConfigurationToggleTest`

- 목적: 전체 컨텍스트 없이 **토글별로 생성되는 빈**을 빠르게 확인
- 방법: `ApplicationContextRunner` 로 조합별 **KeyGenerator/Executor/Aspect** 존재여부 검증
- 장점: DB/Redis 없이도 CI에서 빠르고 안정적으로 수행

```java
class LockAutoConfigurationToggleTest {
    @Test void when_lock_disabled_no_beans_loaded() { /* ... */ }
    @Test void when_lock_enabled_but_no_executors_generators_and_aspect_only() { /* ... */ }
    @Test void when_named_enabled_named_executor_loaded() { /* H2 + NamedLockProperties 바인딩 검증 */ }
    @Test void when_redisson_enabled_redisson_executor_loaded() { /* Mock RedissonClient로 검증 */ }
}
```

---

## 🧾 테스트용 보조 구성

- **TestRedisConfig** (프로필 `test`)  
  Testcontainers로 Redis를 띄우고 `RedissonClient` 를 생성

```java
@TestConfiguration
@Profile("test")
public class TestRedisConfig {
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(GenericContainer<?> redisContainer) {
        String host = redisContainer.getHost();
        Integer port = redisContainer.getMappedPort(6379);
        Config cfg = new Config();
        cfg.useSingleServer().setAddress("redis://" + host + ":" + port);
        return Redisson.create(cfg);
    }
    @Bean(initMethod = "start", destroyMethod = "stop")
    public GenericContainer<?> redisContainer() {
        return new GenericContainer<>("redis:7.0.5").withExposedPorts(6379);
    }
}
```

---

## 🔑 애너테이션 사용 예

`DistributedLock` / `DistributedLockT` 는 **키 전략**과 **실행기 타입**을 선택적으로 지정합니다.

```java
@DistributedLock(
  key = "#orderId",            // SpEL 가능(SpelLockKeyGenerator)
  type = "namedLock",          // or "redissonLock"
  keyStrategy = "sha256",      // sha256 | simple | spell
  waitTime = 3000,             // 대기(ms)
  leaseTime = 10000            // 점유(ms)
)
public String doSomething(String orderId) { /* ... */ }
```

- `DistributedLock` → 기존 트랜잭션으로 실행(REQUIRED)
- `DistributedLockT` → 새로운 트랜잭션으로 실행(REQUIRES_NEW)

---

## 🚀 실행 방법

Gradle 기준(모듈명은 환경에 맞게 수정):

```bash
# 전체 락 테스트
./gradlew :order-core:test --tests "*Distributed*LockTest*"

# 개별 클래스
./gradlew :order-core:test --tests "org.example.order.core.infra.lock.DistributedNamedLockTest"
./gradlew :order-core:test --tests "org.example.order.core.infra.lock.DistributedRedissonLockTest"
./gradlew :order-core:test --tests "org.example.order.core.infra.lock.DistributedTransactionTest"
./gradlew :order-core:test --tests "org.example.order.core.infra.lock.LockAutoConfigurationToggleTest"
```

---

## 🧷 트러블슈팅

- **NamedLock**
  - DB가 `GET_LOCK(name, timeout)` / `RELEASE_LOCK(name)` 를 지원해야 합니다. (MySQL/MariaDB 계열)
  - H2 단독 사용 시 해당 함수가 없어 실패할 수 있습니다. 테스트 DB를 MySQL(호환)로 준비하세요.

- **Redisson**
  - 로컬 포트 충돌 시 컨테이너 포트를 확인하세요.
  - 사내 Redis 사용 시 `RedissonLockProperties.address` 를 `redis://host:port` 로 지정.

- **동시성 실패율**
  - `waitTime`, `retryInterval`, `leaseTime` 을 상황에 맞게 조정하세요.

---

## ✅ 체크리스트

- [x] `lock.enabled` 에 따라 모듈 전체 ON/OFF
- [x] DB(NamedLock) / Redis(Redisson) **선택적 활성화**
- [x] 경합 상황에서 **직렬화 보장** 확인
- [x] `REQUIRED` vs `REQUIRES_NEW` 전파 차이 검증
- [x] **ApplicationContextRunner** 로 토글별 빈 로딩 결과 검증

---

## 🧠 설계 의도(테스트 관점)

- **필요할 때만 로드**: 비용 높은 의존성(DB/Redis)을 조건부로 로딩
- **명확한 전파 제어**: 업무 로직 특성에 따라 `REQUIRED`/`REQUIRES_NEW` 선택
- **가벼운 피드백 루프**: 토글/빈 검증은 러너로, 실제 경합 검증은 통합 테스트로 분리
