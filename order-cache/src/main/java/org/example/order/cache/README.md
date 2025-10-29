# ⚡ Redis 모듈 (Lettuce + Pooling + 단일 구성 @Import)

Spring Boot 환경에서 **Redis 연결**을 `enabled` 여부와 **엔드포인트 설정**(URI 또는 host/port)에 따라 안전하게 조립하는 경량 모듈입니다.  
`commons-pool2` 기반 풀링, `Lettuce` 클라이언트, JSON 직렬화 `RedisTemplate`, 그리고 범용 `Repository` 유틸을 포함합니다.

---

## 1) 구성 개요

| 구성 요소 | 설명 |
|-----------|------|
| `RedisInfraConfig` | **단일 인프라 설정(@Configuration)**. `spring.redis.enabled=true` 일 때만 동작. `uri`가 있으면 우선, 없으면 `host/port`로 `LettuceConnectionFactory` 구성. `RedisTemplate`/`RedisRepository`를 조건부(@Conditional*)로 등록 |
| `RedisProperties` | `spring.redis.*` 프로퍼티 바인딩 (enabled, uri, host/port, password, database, client-name, 풀/타임아웃 등) |
| `RedisRepository` | Redis 기본 연산 인터페이스 (Value/Hash/List/Set/ZSet/TTL/Tx 등) |
| `RedisRepositoryImpl` | `RedisTemplate` 기반 구현. 설정에서 @Bean 으로만 등록 (컴포넌트 스캔 X) |
| `RedisKeyManager` | 표준 키 패턴/TTL 관리 유틸 |
| `RedisSerializerFactory` | 신뢰 패키지 기반 JSON 직렬화기 생성(`GenericJackson2JsonRedisSerializer`) |

> **빈 등록 원칙**
> - 라이브러리 클래스에는 `@Component` 사용 금지
> - 모든 빈은 **설정 기반(@Bean) + 조건부(@ConditionalOnProperty/OnBean/OnMissingBean)** 로만 등록
> - `spring.redis.enabled` 가 없거나 `false` 면 **아무 것도 로딩되지 않음**

---

## 2) 동작 모드 (단일 구성 내부 판단)

- **OFF (기본)**  
  `spring.redis.enabled` 미설정/false → 어떤 빈도 등록되지 않음

- **URI 우선 모드**  
  `spring.redis.enabled=true` **AND** `spring.redis.uri` 존재 → `uri`(예: `redis://` 또는 `rediss://`) 기반 Standalone 구성  
  `rediss://` 사용 시 **SSL 자동 활성화**

- **Host/Port 모드**  
  `spring.redis.enabled=true` **AND** `uri` 미설정 **AND** `host` 존재 → `host/port/password/database` 기반 Standalone 구성

> `enabled=true` 인데 `uri` 도 없고 `host` 도 없으면 **명확한 예외**로 빠르게 실패 → 숨은 localhost 연결 금지

---

## 3) Lettuce 풀/타임아웃/클라이언트 이름

- **풀(commons-pool2)**
  - `pool-max-active`: 동시에 사용할 수 있는 최대 커넥션 수
  - `pool-max-idle`: idle 상태로 유지할 수 있는 최대 커넥션 수
  - `pool-min-idle`: 최소 idle 커넥션 확보 (예열용)
  - `pool-max-wait`: 커넥션 획득 대기 시간(ms)

- **타임아웃**
  - `command-timeout-seconds`: 명령 타임아웃 (기본 3초)
  - `shutdown-timeout-seconds`: 종료 타임아웃 (기본 3초)

- **clientName (우선순위)**
  1) `spring.redis.client-name`
  2) `enable-default-client-name=false` 이면 미지정 시 **호출 안함**
  3) `spring.redis.default-client-name`
  4) `spring.application.name`
  5) 호스트네임
  6) `"order-core"`

---

## 4) 빠른 시작 (설정 + @Import 조립)

### 4.1 의존성 (Gradle)
```groovy
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
implementation 'org.apache.commons:commons-pool2:2.12.0'
```

### 4.2 구성 조립(@Import)

```java
import org.example.order.cache.config.CacheInfraConfig;

// 애플리케이션 진입점(또는 인프라 조립용 @Configuration)
@Import(org.example.order.cache.config.CacheInfraConfig.class)
public class AppBoot {
}
```

### 4.3 설정(YAML) — URI 우선
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

### 4.4 설정(YAML) — host/port 경로
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

> OFF 예시:
> ```yaml
> spring:
>   redis:
>     enabled: false
> ```

---

## 5) Repository 사용 예시

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

// Keys
Set<String> keys = redisRepository.keys("user:*");

// Transaction
redisRepository.transactionPutAllHash("user:2", Map.of("n","Bob","e","b@c.com"));
```

---

## 6) 보안/성능 체크리스트

- 키 네임스페이스: `서비스:도메인:자원ID` 패턴 권장
- TTL 정책: 세션/락/큐 등은 TTL 필수
- SSL: 운영망은 `rediss://` 권장
- 풀 튜닝: 고부하 시 `pool-max-active`/`pool-max-wait` 조정
- 직렬화 신뢰 패키지: `trusted-package` 최소 범위 유지
- 모니터링: 연결 수, pool exhausted, 명령 지연 시간 지표 수집

---

## 7) 동작 흐름

```
Caller (RedisRepository API)
 └─ RedisRepositoryImpl
     └─ RedisTemplate<String,Object> (JSON Serializer)
         └─ LettuceConnectionFactory (Pooling)
             └─ RedisInfraConfig (enabled/endpoint 조건 판단)
                 └─ RedisProperties (spring.redis.*)
```

---

## 8) FAQ

**Q1. `enabled=true`인데 아무 동작도 하지 않습니다.**  
A. `uri` 또는 `host` 중 하나는 반드시 지정해야 합니다. 둘 다 없으면 예외 발생.

**Q2. Auto/Manual 구분이 사라졌나요?**  
A. 네. 단일 구성(`RedisInfraConfig`) 내부에서 **URI 우선 → host/port** 규칙으로 판단합니다.

**Q3. 클러스터/센티넬 지원은?**  
A. 현재 Standalone 전용입니다. 필요 시 확장 가능.

**Q4. 기본 직렬화는 무엇인가요?**  
A. `GenericJackson2JsonRedisSerializer`(+ trusted-package 제한) 입니다.

---

## 9) 마지막 한 줄 요약

**`@Import(RedisInfraConfig.class)` + `spring.redis.enabled=true` + (URI 또는 host/port)** 만으로  
안전한 풀링·직렬화·레포지토리까지 한 번에 조립되는 Redis 모듈.
