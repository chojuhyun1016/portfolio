# ⚡ order-cache 모듈 (Redis Cache AutoConfiguration Layer)

`order-cache` 모듈은 **Redis 기반 캐시 인프라를 명시적 토글과 Fail-Fast 정책으로 자동 구성**하는 전용 모듈입니다.  
Spring Boot 환경에서 **Lettuce + commons-pool2 풀링**, **JSON 직렬화 RedisTemplate**, **범용 RedisRepository**,  
그리고 **도메인 전용 캐시 Repository(OrderCache)** 를 일관된 방식으로 제공합니다.

> 핵심 포지션
> - ❌ `spring.redis.*` 미사용
> - ✅ `order.cache.*` 전용 네임스페이스
> - ✅ AutoConfiguration 기반 “꽂으면 바로 동작”
> - ✅ endpoint 미설정 시 **즉시 실패** (숨은 localhost 연결 금지)
> - ✅ 캐시 스키마/버전/TTL은 **캐시 레이어에서만 관리**

---

## 1) 모듈 구성 개요

| 구성 요소 | 설명 |
|-----------|------|
| **`CacheToggleProperties`** | 캐시 전역/구현별 활성화 토글 (`order.cache.enabled`, `order.cache.redis.enabled`) |
| **`RedisCacheAutoConfiguration`** | Redis 캐시 자동 구성의 중심. Lettuce, Pool, RedisTemplate, Repository 계층을 조건부로 조립 |
| **`OrderRedisProperties`** | `order.cache.redis.*` 전용 설정 바인딩 (URI/host/port/풀/타임아웃/clientName 등) |
| **`RedisRepository`** | Value/Hash/List/Set/ZSet/TTL/SCAN/Tx 등 **범용 Redis 연산 포트** |
| **`RedisRepositoryImpl`** | `RedisTemplate` 기반 구현 (컴포넌트 스캔 X, 설정에서만 `@Bean` 등록) |
| **`RedisSerializerFactory`** | JSR-310 지원 JSON 직렬화기 (`GenericJackson2JsonRedisSerializer`) |
| **`RedisKeyManager`** | 키 패턴 + TTL 규약 관리(Enum) |
| **`OrderCacheRepository`** | 주문 도메인 캐시 포트 |
| **`OrderCacheRepositoryImpl`** | 관용 역직렬화(Map/String/Object) + TTL 유지 재저장 전략 |
| **`OrderCacheKeys`** | 주문 캐시 키 네임스페이스/버전 관리 (`v1`) |
| **`OrderCacheRecord`** | Redis 캐시 전용 DTO (조회/인덱싱 최소 필드) |
| **`OrderCacheConverters`** | 스키마 Drift 흡수를 위한 방어적 변환 유틸 |

---

## 2) 활성화 정책 (명시적 토글)

### 2.1 전역/구현 토글

```yaml
order:
  cache:
    enabled: true            # 전역 캐시 토글
    redis:
      enabled: true          # Redis 구현 토글
```

- 두 조건은 **AND** 로 평가됩니다.
- 하나라도 false 이면 **아무 Redis 빈도 등록되지 않습니다.**

---

## 3) Redis 엔드포인트 결정 규칙 (Fail-Fast)

### 3.1 URI 우선

```yaml
order:
  cache:
    redis:
      uri: rediss://:secret@redis.example.com:6380/0
```

- `redis://`, `rediss://` 모두 지원
- `rediss://` 인 경우 SSL 자동 활성화
- username/password/database 파싱 지원(가능 범위 내)

### 3.2 Host / Port 경로

```yaml
order:
  cache:
    redis:
      host: redis.example.com
      port: 6379
      password: secret
      database: 0
```

### 3.3 예외 정책

- `order.cache.redis.enabled=true` 인데 `uri` **도 없고** `host` **도 없으면** → **즉시 IllegalStateException**
- 의도: *개발/운영 환경에서 의도치 않은 localhost 연결 차단*

---

## 4) Lettuce 풀 / 타임아웃 / ClientName 정책

### 4.1 풀 설정 (commons-pool2)

| 속성 | 설명 |
|----|----|
| `pool-max-active` | 최대 활성 커넥션 |
| `pool-max-idle` | 최대 idle |
| `pool-min-idle` | 최소 idle |
| `pool-max-wait` | 커넥션 대기(ms) |

### 4.2 타임아웃

| 속성 | 기본 |
|----|----|
| `command-timeout-seconds` | 3초 |
| `shutdown-timeout-seconds` | 3초 |

### 4.3 clientName 해석 우선순위

1) `order.cache.redis.client-name`(또는 `clientName`)
2) `enable-default-client-name=false` → 미설정 시 **clientName 미주입**
3) `order.cache.redis.default-client-name`
4) `spring.application.name`
5) 호스트네임
6) `"order-cache"` (최종 기본값)

---

## 5) RedisTemplate & 직렬화 정책

- `RedisTemplate<String, Object>` 제공
- Serializer:
  - `StringRedisSerializer` (key/hashKey)
  - `GenericJackson2JsonRedisSerializer` (value/hashValue)
- JSON 정책:
  - JSR-310(LocalDateTime 등) 지원
  - **Default typing 비활성화** (WRAPPER_ARRAY 제거)
  - 필드 가시성 ALL
- `trustedPackage`:
  - `order.cache.redis.trusted-package`
  - 미설정 시 `org.example.order.cache` 기본 사용

---

## 6) RedisRepository (범용 포트)

### 6.1 제공 연산

- **Value**: set/get/delete (+ TTL)
- **Hash**: put/get/putAll/delete
- **List**: leftPush/rightPop (blocking/non-blocking 옵션)
- **Set**: add/members/remove
- **ZSet**: add/range/score/remove
- **TTL**: expire/persist/getExpire
- **Keys**: SCAN 기반 keys(pattern)
- **Transaction**: multi/exec 기반 hash/set 조합

### 6.2 List Pop 옵션

```java
RedisListPopOptions.builder()
  .loop(5)
  .blockingTimeoutSeconds(3L)
  .build();
```

---

## 7) OrderCache (도메인 캐시 계층)

### 7.1 설계 원칙

- 서비스/파사드는 **`OrderCacheRepository` 포트만 의존**
- 캐시 스키마/버전/TTL은 **캐시 레이어 전담**
- 과거 캐시 값과의 호환을 위해 **관용 역직렬화 전략** 채택

### 7.2 관용 역직렬화 흐름

1) Redis 값이 `OrderCacheRecord` → 그대로 사용
2) Redis 값이 `Map` → 필드 매핑 후 **표준 DTO로 재저장**
3) Redis 값이 `String(JSON)` → Map 파싱 → **표준 DTO로 재저장**
4) TTL은 **기존 TTL 유지**(가능한 범위에서 보존)

### 7.3 캐시 키 규약

```text
order:v1:order:{orderId}
```

- 버전 변경은 `OrderCacheKeys` 에서만 관리

---

## 8) 빠른 시작

### 8.1 의존성

```groovy
implementation project(":order-cache")
implementation "org.springframework.boot:spring-boot-starter-data-redis"
implementation "org.apache.commons:commons-pool2"
```

### 8.2 AutoConfiguration 등록 (Boot 3.x)

경로:
`order-cache/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

내용:

```text
org.example.order.cache.autoconfig.RedisCacheAutoConfiguration
```

### 8.3 설정 예시 (URI 우선)

```yaml
order:
  cache:
    enabled: true
    redis:
      enabled: true
      uri: redis://localhost:6379/0
      trusted-package: org.example.order.cache
      command-timeout-seconds: 3
      shutdown-timeout-seconds: 3
      pool-max-active: 64
      pool-max-idle: 32
      pool-min-idle: 8
      pool-max-wait: 2000
```

### 8.4 설정 예시 (host/port)

```yaml
order:
  cache:
    enabled: true
    redis:
      enabled: true
      host: redis.example.com
      port: 6379
      password: secret
      database: 0
      trusted-package: org.example.order.cache
      command-timeout-seconds: 4
      shutdown-timeout-seconds: 4
      pool-max-active: 128
      pool-max-idle: 64
      pool-min-idle: 16
      pool-max-wait: 5000
```

### 8.5 OFF 예시

```yaml
order:
  cache:
    enabled: false
    redis:
      enabled: false
```

---

## 9) 사용 예시

### 9.1 RedisRepository

```java
// Value
redisRepository.set("user:1:name", "Alice");
Object name = redisRepository.get("user:1:name");

// TTL
redisRepository.set("session:abc", "OK", 1800);
Long ttl = redisRepository.getExpire("session:abc");

// Hash
redisRepository.putHash("user:1", "email", "a@b.com");
Object email = redisRepository.getHash("user:1", "email");

// List
redisRepository.leftPush("queue:job", "job1");
Object job = redisRepository.rightPop("queue:job");

// Set
redisRepository.addSet("tag:hot", "item-1");
boolean contains = redisRepository.isSetMember("tag:hot", "item-1");

// ZSet
redisRepository.zAdd("rank:score", "user-1", 123.4);
Double score = redisRepository.zScore("rank:score", "user-1");

// Keys (SCAN)
Set<String> keys = redisRepository.keys("user:*");

// Transaction
redisRepository.transactionPutAllHash("user:2", Map.of("n","Bob","e","b@c.com"));
```

### 9.2 OrderCacheRepository

```java
// 조회
orderCacheRepository.get(orderId).ifPresent(cache -> {
  // cache: OrderCacheRecord
});

// 저장 (TTL seconds)
orderCacheRepository.put(record, 300L);

// 제거
orderCacheRepository.evict(orderId);
```

---

## 10) 보안/성능 체크리스트

- 캐시 활성화는 **항상 명시적으로**(기본 OFF 권장)
- 운영 환경은 `rediss://` 권장
- TTL 없는 캐시는 최소화(세션/락/큐는 TTL 필수)
- 키 네임스페이스: `서비스:버전:도메인:식별자` 형태 유지
- `trusted-package` 는 최소 범위로 제한
- SCAN 사용 시 패턴을 좁혀서 호출(과도한 `*` 지양)
- 값 타입 혼재(Map/String/Object)를 고려한 방어 로직 유지

---

## 11) 동작 흐름

```text
Application
 └─ RedisCacheAutoConfiguration (@AutoConfiguration)
     ├─ CacheToggleProperties (order.cache.*)
     ├─ OrderRedisProperties (order.cache.redis.*)
     ├─ ClientResources (destroyMethod=shutdown)
     ├─ LettuceConnectionFactory (Pooling)
     ├─ RedisTemplate<String,Object> (JSON Serializer)
     ├─ RedisRepositoryImpl
     └─ OrderCacheRepositoryImpl (loose value normalize + TTL keep)
```

---

## 12) FAQ

**Q1. enabled=true인데 아무 동작도 하지 않습니다.**  
A. `order.cache.enabled=true` 와 `order.cache.redis.enabled=true` 둘 다 필요합니다.

**Q2. endpoint를 안 주면 왜 바로 죽게 했나요?**  
A. “조용한 localhost 연결”을 막기 위해서입니다. 운영/테스트 환경에서 사고를 줄이는 목적입니다.

**Q3. spring.redis.* 왜 안 쓰나요?**  
A. 캐시 모듈은 캐시 전용 네임스페이스(`order.cache.redis.*`)로 통제하여, 앱의 다른 Redis 사용과 충돌/혼선을 줄이려는 구조입니다.

**Q4. 캐시 값이 Map/String 으로 들어오는 경우가 있는데요?**  
A. 과거 캐시 값/직렬화 정책 변경으로 타입이 혼재될 수 있어, `OrderCacheRepositoryImpl` 이 관용 매핑 후 **표준 DTO로 재저장**합니다.

---

## 13) 마지막 한 줄 요약

**`order.cache.enabled=true` + `order.cache.redis.enabled=true` + (URI 또는 host/port)** 만으로  
Fail-Fast·풀링·JSON 직렬화·범용 Redis 연산·주문 캐시 레포지토리까지 한 번에 조립되는 Redis 캐시 모듈입니다.
