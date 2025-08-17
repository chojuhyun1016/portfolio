# ⚡ Redis 모듈 (Lettuce + Auto/Manual 감지 + Commons-Pool2)

Spring Boot 환경에서 **Redis 연결**을 자동/수동 조건에 따라 감지하여 구성하는 모듈입니다.  
`commons-pool2` 기반 커넥션 풀, `Lettuce` 클라이언트, `RedisTemplate` 직렬화, `Repository` 유틸을 포함합니다.

---

## 1) 구성 개요

| 클래스/인터페이스 | 설명 |
|-------------------|------|
| `RedisProperties` | spring.redis.* 프로퍼티 바인딩 클래스 (enabled/uri/host/port/pool/timeout 등) |
| `RedisAutoConfig` | 자동(Auto) Redis 설정. host/port 또는 uri 기반 LettuceConnectionFactory 생성 |
| `RedisManualConfig` | 수동(Manual) Redis 설정. uri만 설정되고 host/port 미설정 시 동작 |
| `RedisCommonConfig` | RedisTemplate 및 직렬화 설정. trustedPackage 기반 JSON 직렬화 사용 |
| `RedisRepository` | Redis 기본 연산 인터페이스 (Value/Hash/List/Set/ZSet/TTL/Tx 등) |
| `RedisRepositoryImpl` | RedisTemplate을 활용한 RedisRepository 구현체 |
| `RedisKeyManager` | 표준화된 키 패턴 및 TTL 관리 유틸리티 |
| `RedisSerializerFactory` | Jackson2 기반 RedisSerializer 생성 (trustedPackage 검증) |

---

## 2) 동작 모드

### 2.1 Auto 모드
- 조건: `spring.redis.enabled=true`
- `spring.redis.uri` 와 `host/port`가 함께 있거나, host/port 기반 설정이 존재하는 경우
- 동작:
    - uri 설정 시 → URI 기반 StandaloneConfig 매핑
    - 없으면 host/port/password/database 기반 StandaloneConfig 매핑

### 2.2 Manual 모드
- 조건: `spring.redis.enabled=true` AND `uri` 존재 AND host/port 미설정
- 동작:
    - 반드시 `uri` 기반 연결만 사용
    - 보안/운영 환경에서 프로퍼티 단순화를 위해 사용

---

## 3) Lettuce 풀/타임아웃 설정

- **풀링 (commons-pool2)**
    - `poolMaxActive`: 동시에 사용할 수 있는 최대 커넥션 수
    - `poolMaxIdle`: idle 상태로 유지할 수 있는 최대 커넥션 수
    - `poolMinIdle`: 최소 idle 커넥션 확보 (예열용)
    - `poolMaxWait`: 커넥션 획득 대기 시간(ms)

- **타임아웃**
    - `commandTimeoutSeconds`: 명령 실행 타임아웃 (기본 3초)
    - `shutdownTimeoutSeconds`: shutdown 타임아웃 (기본 3초)

- **기타**
    - `rediss://` 스킴 사용 시 SSL 자동 활성화
    - `clientName` 지정 가능

---

## 4) 빠른 시작

### 4.1 Auto – host/port 기반
```yaml
spring:
  redis:
    enabled: true
    host: redis.example.com
    port: 6379
    password: secret
    database: 0
    client-name: order-service
    trusted-package: org.example.order
    command-timeout-seconds: 5
    shutdown-timeout-seconds: 5
    pool-max-active: 128
    pool-max-idle: 64
    pool-min-idle: 16
    pool-max-wait: 5000
```

### 4.2 Auto – URI 기반
```yaml
spring:
  redis:
    enabled: true
    uri: redis://:secret@redis.auto.example.com:6380/1
    client-name: order-service
    trusted-package: org.example.order
    pool-max-active: 64
```

### 4.3 Manual – 최소 설정 (uri만)
```yaml
spring:
  redis:
    enabled: true
    uri: rediss://:secret@redis.prod.example.com:6380/0
    trusted-package: org.example.order
    command-timeout-seconds: 4
    shutdown-timeout-seconds: 4
    pool-max-active: 64
    pool-max-idle: 32
    pool-min-idle: 8
    pool-max-wait: 2000
```

---

## 5) Repository 사용 예시

```java
redisRepository.set("user:1:name", "Alice");
Object name = redisRepository.get("user:1:name");

redisRepository.set("session:abc", "OK", 1800);
Long ttl = redisRepository.getExpire("session:abc");

redisRepository.putHash("user:1", "email", "a@b.com");
Object email = redisRepository.getHash("user:1", "email");

redisRepository.leftPush("queue:job", "job1");
Object job = redisRepository.rightPop("queue:job");

redisRepository.addSet("tag:hot", "item-1");
boolean contains = redisRepository.isSetMember("tag:hot", "item-1");

redisRepository.zAdd("rank:score", "user-1", 123.4);
Double score = redisRepository.zScore("rank:score", "user-1");

Set<String> keys = redisRepository.keys("user:*");

redisRepository.transactionPutAllHash("user:2", Map.of("n","Bob","e","b@c.com"));
```

---

## 6) 보안/성능 권장사항
- **키 네임스페이스**: `서비스:도메인:리소스ID` 형태 일관성 유지
- **TTL 정책**: 세션/락 키 등은 TTL 반드시 부여
- **풀 튜닝**: 요청 부하에 맞춰 `poolMaxActive`, `poolMaxWait` 조정
- **SSL 사용**: 운영망에서는 `rediss://` 사용 권장
- **모니터링**: pool exhaust, latency, 연결 수 메트릭 수집

---

## 7) Gradle 의존성

```groovy
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
implementation 'org.apache.commons:commons-pool2:2.12.0'
```

---

## 8) 동작 흐름

```
Caller (RedisRepository API)
 └─> RedisRepositoryImpl
      └─> RedisTemplate
           └─> RedisConnectionFactory (Lettuce + Pool)
                └─> RedisAutoConfig / RedisManualConfig
                     └─> RedisProperties(spring.redis.*)
```

---

## 9) FAQ

**Q1. Auto와 Manual을 동시에 사용할 수 있나요?**  
A. 아니요. `uri`만 설정된 경우 Manual, 그 외는 Auto로 자동 판별됩니다.

**Q2. 클러스터/센티넬은 지원하나요?**  
A. 현재 코드는 Standalone 전용. 필요 시 확장이 가능합니다.

**Q3. trustedPackage는 왜 필요한가요?**  
A. Redis 직렬화 시 임의 클래스 로딩을 제한하여 보안성을 높이기 위함입니다.

---

## 10) 마지막 한 줄 요약
**설정 값 조합만으로 Auto/Manual 감지, 풀링, 직렬화까지 제공하는 Redis 모듈.**  
운영 환경에 안전하게 Redis를 붙일 수 있습니다.
