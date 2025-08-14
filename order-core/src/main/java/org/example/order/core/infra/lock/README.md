# 🔒 Lock 모듈 (DB NamedLock + Redis RedissonLock)

Spring Boot에서 **DB 기반 NamedLock** 또는 **Redis 기반 RedissonLock** 을 선택적으로 사용하여 분산락을 구현하는 모듈입니다.  
애노테이션 한 줄로 락 종류와 전파(트랜잭션) 전략을 지정할 수 있으며, **전역/세부 토글**로 실행기(Executor) 로딩 여부를 제어합니다.

---

## 1) 구성 개요

| 클래스/인터페이스                | 설명 |
|-----------------------------------|------|
| `DistributedLock`                 | 기존 트랜잭션(`REQUIRED`) 유지 락 애노테이션 |
| `DistributedLockT`                | 새 트랜잭션(`REQUIRES_NEW`) 생성 락 애노테이션 |
| `DistributedLockAspect`           | 애노테이션 파라미터 파싱 → 키 생성기/실행기 선택 → 트랜잭션 래퍼 호출 |
| `LockKeyGenerator`                | 키 생성 전략 인터페이스 |
| `SHA256LockKeyGenerator`          | SHA-256 해시 기반 키 생성 |
| `SpelLockKeyGenerator`            | SpEL 기반 키 생성 (`#param` 형식) |
| `SimpleLockKeyGenerator`          | 단순 문자열 결합 키 생성 |
| `LockExecutor`                    | 락 실행기 인터페이스 |
| `NamedLockExecutor`               | DB `GET_LOCK` / `RELEASE_LOCK` 기반 실행기 |
| `RedissonLockExecutor`            | Redis `RLock.tryLock()` 기반 실행기 |
| `LockKeyGeneratorFactory`         | 이름 기반 키 생성기 조회 |
| `LockExecutorFactory`             | 이름 기반 실행기 조회 |
| `LockManualConfig`                | 전역 토글(`lock.enabled`) 기반 빈 등록 |
| `NamedLockAutoConfig`             | NamedLock 실행기 설정 바인딩 |
| `RedissonLockAutoConfig`          | RedissonClient 자동 구성 |

---

## 2) 동작 모드

### 2.1 OFF (기본)
```properties
lock.enabled=false
```
- 어떤 빈도 등록되지 않음
- 다른 모듈에 영향 없음

### 2.2 NamedLock(DB) 모드
```properties
lock.enabled=true
lock.named.enabled=true
lock.redisson.enabled=false
```
- DB `GET_LOCK`, `RELEASE_LOCK` 사용
- `DataSource` 필수
- `NamedLockProperties` 바인딩 지원

### 2.3 RedissonLock(REDIS) 모드
```properties
lock.enabled=true
lock.named.enabled=false
lock.redisson.enabled=true
```
- Redis `RLock.tryLock()` 사용
- `RedissonClient` 필수
- `RedissonLockProperties` 바인딩 지원

> **전역 스위치**: `lock.enabled` 가 false면 어떤 실행기도 등록되지 않음  
> **세부 스위치**: `lock.named.enabled`, `lock.redisson.enabled` 로 실행기 선택

---

## 3) 동작 흐름

```
Caller (@DistributedLock)
 └─> DistributedLockAspect
      1) key/type/strategy/wait/lease 파라미터 읽기
      2) LockKeyGeneratorFactory.getGenerator(strategy) → 키 생성
      3) LockExecutorFactory.getExecutor(type) → 실행기 선택
      4) executor.execute(key, wait, lease, callback)
            └─ callback: TransactionalOperator.runWith(REQUIRED or REQUIRES_NEW)
```

- `@DistributedLock` → REQUIRED
- `@DistributedLockT` → REQUIRES_NEW

---

## 4) 빠른 시작

### 4.1 NamedLock 예시
```java
@DistributedLock(
    key = "#orderId",
    type = "namedLock",
    keyStrategy = "spell",
    waitTime = 3000,
    leaseTime = 10000
)
public void processOrder(String orderId) {
    // 임계 구역
}
```

### 4.2 RedissonLock 예시
```java
@DistributedLockT(
    key = "'INV:' + #invoiceId",
    type = "redissonLock",
    keyStrategy = "spell",
    waitTime = 5000,
    leaseTime = 15000
)
public void settleInvoice(String invoiceId) {
    // 새로운 트랜잭션에서 임계 구역 실행
}
```

---

## 5) 설정 샘플

```properties
# 전역 스위치
lock.enabled=true

# NamedLock(DB) 사용
lock.named.enabled=true
lock.redisson.enabled=false
lock.named.wait-time=3000
lock.named.retry-interval=150

# RedissonLock 사용
# lock.named.enabled=false
# lock.redisson.enabled=true
# lock.redisson.address=redis://127.0.0.1:6379
# lock.redisson.database=0
# lock.redisson.wait-time=3000
# lock.redisson.lease-time=10000
# lock.redisson.retry-interval=150
```

---

## 6) 테스트 가이드

### 6.1 NamedLock 동시성 테스트
```java
@Test
void testConcurrentNamedLock() {
    // 10개 스레드에서 같은 키로 경합
    // 일부 성공, 일부 대기/실패
}
```

### 6.2 RedissonLock 동시성 테스트
```java
@Test
void testConcurrentRedissonLock() {
    // Redis 기반 락으로 직렬화 보장
}
```

### 6.3 트랜잭션 전파 테스트
```java
@Test @Transactional
void testTransactionPropagation() {
    // REQUIRED → 동일 TX ID
    // REQUIRES_NEW → 다른 TX ID
}
```

---

## 7) 보안/성능 권장사항
- **키 네임스페이스**: `service:domain:resourceId` 형태로 설계
- **작업 시간 ≤ leaseTime**: 타임아웃과 재시도 고려
- **DB/Redis 분리**: 캐시 트래픽과 락 트래픽을 분리해 영향 최소화
- **관측성 확보**: 경합/실패율 모니터링

---

## 8) 에러/예외 메시지
- `LockAcquisitionException`: waitTime 내 락 획득 실패
- `InterruptedException`: 스레드 인터럽트 시 플래그 복구 후 예외 전파
- `IllegalStateException`: 키 생성기/실행기 미등록

---

## 9) 설정 레퍼런스

### 9.1 NamedLock
```properties
lock.enabled=true
lock.named.enabled=true
lock.redisson.enabled=false
```

### 9.2 RedissonLock
```properties
lock.enabled=true
lock.named.enabled=false
lock.redisson.enabled=true
```

---

## 10) 클래스 다이어그램 (개념)

```
LockManualConfig ─┬─> LockKeyGeneratorFactory
                  ├─> LockExecutorFactory
                  ├─> NamedLockExecutor
                  └─> RedissonLockExecutor

DistributedLockAspect ──> LockKeyGenerator ──> LockExecutor
```

---

## 11) FAQ

**Q1. NamedLock과 RedissonLock을 동시에 켤 수 있나요?**  
A. 가능. 애노테이션의 `type` 파라미터로 메서드별 실행기 선택.

**Q2. NamedLock은 어떤 DB에서 가능한가요?**  
A. MySQL/MariaDB 계열처럼 `GET_LOCK`, `RELEASE_LOCK` 함수를 지원하는 DB.

**Q3. Redis는 어떤 버전을 써야 하나요?**  
A. Redisson이 지원하는 범위 내에서 최신 버전 권장. 테스트에서는 `redis:7.0.5` 사용.

---

## 12) 마지막 한 줄 요약
전역/세부 토글과 애노테이션으로 **DB·Redis 분산락**을 손쉽게 제어할 수 있는 모듈.  
트랜잭션 전파까지 함께 제어 가능합니다.
