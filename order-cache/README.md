# ⚡ order-cache (Redis Cache AutoConfiguration Layer)

`order-cache` 모듈은 **Redis 캐시 인프라를 “명시적 토글 + Fail-Fast” 정책으로 AutoConfiguration** 하는 전용 레이어입니다.  
API/Batch/Worker 등 상위 모듈은 이 레이어를 의존함으로써 **Redis 연결/풀링/직렬화/키·TTL 규약/캐시 포트**를 동일한 규칙으로 사용합니다.

---

## ✅ 핵심 정의

- **명시적 토글**: `order.cache.enabled=true` AND `order.cache.redis.enabled=true` 일 때만 캐시 빈을 등록합니다.
- **Fail-Fast**: Redis endpoint(URI 또는 host/port) 미설정 상태에서 활성화되면 **즉시 예외**로 종료합니다. (숨은 localhost 연결 금지)
- **캐시 레이어 전담 규약**: 캐시 키 네임스페이스/버전/TTL/직렬화 정책은 캐시 레이어에서만 관리합니다.
- **범용 Redis + 도메인 전용 캐시 포트**:
    - `RedisRepository` (Value/Hash/List/Set/ZSet/TTL/SCAN/Tx)
    - `OrderCacheRepository` (주문 캐시 전용 포트)

---

## 📦 구성 요소(요약)

- AutoConfiguration: `RedisCacheAutoConfiguration`
- Properties:
    - `CacheToggleProperties` (`order.cache.enabled`, `order.cache.redis.enabled`)
    - `OrderRedisProperties` (`order.cache.redis.*`)
- Core Beans:
    - `LettuceConnectionFactory` (commons-pool2 풀링)
    - `RedisTemplate<String,Object>` (JSON 직렬화)
    - `RedisRepository` / `RedisRepositoryImpl`
    - `OrderCacheRepository` / `OrderCacheRepositoryImpl` (관용 역직렬화 + TTL 유지)
- Key/Schema:
    - `OrderCacheKeys` (예: `order:v1:order:{orderId}`)
    - `OrderCacheRecord`, `OrderCacheConverters`

---

## 🧭 High-level 구조

    Application
     └─ RedisCacheAutoConfiguration
         ├─ CacheToggleProperties   (order.cache.*)
         ├─ OrderRedisProperties    (order.cache.redis.*)
         ├─ LettuceConnectionFactory (Pooling)
         ├─ RedisTemplate<String,Object> (JSON)
         ├─ RedisRepositoryImpl
         └─ OrderCacheRepositoryImpl (loose deserialize + TTL keep)

---

## ⚙️ 최소 설정 예시

아래 중 하나(URI 또는 host/port)가 필요합니다.

### 1) URI 경로

    order:
      cache:
        enabled: true
        redis:
          enabled: true
          uri: redis://localhost:6379/0

### 2) host/port 경로

    order:
      cache:
        enabled: true
        redis:
          enabled: true
          host: redis.example.com
          port: 6379
          database: 0

---

## 🔑 한 줄 요약

**`order-cache`는 “명시적 토글 + Fail-Fast”를 강제하면서, Redis 풀링/JSON 직렬화/범용 Redis 연산/주문 캐시 포트를 AutoConfiguration으로 한 번에 조립하는 캐시 전용 레이어입니다.**
