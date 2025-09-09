# 📦 order-core — 통합 분석 (application + infra)

----------------------------------------------------------------------------------------------------

## 1) 최상위 개요 (DDD + Clean Architecture)

의존 흐름(요지)

    [application] → [domain ports + mapper] ←implements— [infra adapters] —talks-to→ [DB/Kafka/Redis/External]

루트 모듈

    org.example.order.core
    ├─ application/     ← 유스케이스 계층 (현재 제공 코드: 주문 DTO/매퍼)
    └─ infra/           ← persistence/messaging/acl/jpa/dynamo/redis/lock/crypto/common/config

핵심 원칙

- 도메인 보호: Domain 은 Port(인터페이스)만 소유, 구현은 Infra Adapter
- 경계 보호: 외부/타 컨텍스트 스키마는 Domain 에 직접 노출 금지 → infra.acl 로 번역/격리
- 애그리거트 우선: 저장/조회 어댑터는 `persistence/<aggregate>/<tech>` 구조
- 설정 기반 조립: 각 인프라 설정은 `@ConditionalOnProperty`로 토글

----------------------------------------------------------------------------------------------------

## 2) application 계층 — 디렉터리 (제공 코드 반영)

루트

    org.example.order.core.application

하위 구조

    org.example.order.core.application
    └─ order/
        ├─ dto/
        │   ├─ command/     LocalOrderCommand
        │   ├─ internal/    LocalOrderDto, OrderDto, OrderEntityDto
        │   ├─ outgoing/    OrderApiOutgoingDto
        │   └─ query/       OrderDetailQueryDto, OrderResultQueryDto
        └─ mapper/          OrderMapper

요약

- Command: `LocalOrderCommand` (record, 불변)
- Internal: `LocalOrderDto`, `OrderDto`(래퍼), `OrderEntityDto`(Entity → DTO)
- Outgoing: `OrderApiOutgoingDto` → `OrderCloseMessage`
- Query: `OrderDetailQueryDto`, `OrderResultQueryDto`
- Mapper: `OrderMapper` (Entity ↔ DTO, Command ↔ Message 변환)

----------------------------------------------------------------------------------------------------

## 3) infra 계층 — 디렉터리 (제공 코드 반영)

루트

    org.example.order.core.infra

하위 구조

    org.example.order.core.infra
    ├─ common/
    │   ├─ aop/                AopConfig
    │   └─ idgen/tsid/         TsidFactoryHolder, CustomTsid, TsidConfig
    ├─ config/                 OrderCoreConfig
    ├─ crypto/                 CryptoInfraConfig, EncryptorFactory, EncryptProperties
    ├─ dynamo/                 DynamoInfraConfig, DynamoDbProperties, DynamoQuerySupport
    ├─ jpa/                    JpaInfraConfig, BooleanToYNConverter, QuerydslUtils 등
    ├─ messaging/
    │   └─ order/
    │       ├─ code/           DlqOrderType, MessageCategory
    │       └─ message/        OrderApiMessage, OrderCloseMessage, OrderCrudMessage, OrderLocalMessage
    ├─ persistence/
    │   └─ order/              OrderDynamoRepositoryImpl, OrderCommandRepositoryJdbcImpl, OrderQueryRepositoryJpaImpl, OrderRepositoryJpaImpl, RedisRepository, RedisRepositoryImpl
    ├─ redis/
    │   ├─ config/             RedisInfraConfig
    │   ├─ props/              RedisProperties
    │   └─ support/            RedisKeyManager, RedisSerializerFactory
    └─ lock/
        ├─ annotation/         DistributedLock, DistributedLockT
        ├─ aspect/             DistributedLockAspect
        ├─ config/             LockInfraConfig
        ├─ exception/          LockAcquisitionException
        ├─ factory/            LockExecutorFactory, LockKeyGeneratorFactory
        ├─ key/                LockKeyGenerator
        │   └─ impl/           SHA256LockKeyGenerator, SimpleLockKeyGenerator, SpelLockKeyGenerator
        ├─ lock/               LockExecutor, LockCallback
        │   └─ impl/           NamedLockExecutor, RedissonLockExecutor
        ├─ props/              NamedLockProperties, RedissonLockProperties
        └─ support/            TransactionalOperator

----------------------------------------------------------------------------------------------------

## 4) Messaging (제공 코드)

### 코드

- `DlqOrderType`: ORDER_LOCAL, ORDER_API, ORDER_CRUD, ORDER_REMOTE
- `MessageCategory`: ORDER_LOCAL, ORDER_API, ORDER_CRUD, ORDER_REMOTE, ORDER_DLQ, ORDER_ALARM

### 메시지 모델

- `OrderApiMessage`: LocalMessage → API 메시지로 변환
- `OrderCloseMessage`: 주문 종료 이벤트 전송
- `OrderCrudMessage`: API 메시지 + DTO 묶음
- `OrderLocalMessage`: 로컬 이벤트, validation 포함

----------------------------------------------------------------------------------------------------

## 5) Redis (제공 코드)

- `RedisInfraConfig`: Lettuce 기반 ConnectionFactory + RedisTemplate + RedisRepository 조건부 등록
- `RedisProperties`: `spring.redis.*` 프로퍼티 매핑
- `RedisKeyManager`: 표준 키 패턴 + TTL 관리 (login token, order lock 등)
- `RedisSerializerFactory`: Jackson2 기반 JsonSerializer 생성

----------------------------------------------------------------------------------------------------

## 6) Lock (제공 코드)

### 어노테이션

- `@DistributedLock` : 기존 트랜잭션 참여(REQUIRED)
- `@DistributedLockT`: 새로운 트랜잭션 시작(REQUIRES_NEW)

### Aspect

- `DistributedLockAspect`: AOP 기반 진입점, KeyGenerator/Executor/TxOperator 조합

### 구현체

- KeyGenerator: SHA256, Simple(리터럴 평가), SpEL
- LockExecutor: NamedLockExecutor(MySQL GET_LOCK), RedissonLockExecutor
- TransactionalOperator: 트랜잭션 래핑 유틸

----------------------------------------------------------------------------------------------------

## 7) TSID (제공 코드)

- `TsidConfig`: tsid.enabled=true 조건부, EC2 meta/hostname 기반 nodeId 산출
- `TsidFactoryHolder`: 정적 접근
- `CustomTsid`: Hibernate 통합

----------------------------------------------------------------------------------------------------

## 8) 설정 샘플 (application.yml)

    jpa:
      enabled: true

    dynamodb:
      enabled: true
      endpoint: http://localhost:4566
      region: ap-northeast-2
      access-key: test
      secret-key: test
      table-name: order_projection

    redis:
      enabled: true
      uri: redis://127.0.0.1:6379
      database: 0
      pool-max-active: 64
      pool-max-idle: 32
      pool-min-idle: 8
      pool-max-wait: 2000

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

    crypto:
      enabled: true
      props:
        seed: true
    encrypt:
      aes128: { key: "BASE64_KEY_128" }
      aes256: { key: "BASE64_KEY_256" }
      aesgcm: { key: "BASE64_KEY_GCM" }
      hmac:   { key: "BASE64_KEY_HMAC" }

    aws:
      secrets-manager:
        enabled: true
        region: ap-northeast-2
        secret-name: myapp/secret-keyset
        refresh-interval-millis: 300000
        fail-fast: true
        scheduler-enabled: false

    tsid:
      enabled: true
      node-bits: 10
      prefer-ec2-meta: false
      zone-id: Asia/Seoul

----------------------------------------------------------------------------------------------------

## 9) 테스트 전략 (요약)

- **Application**
    - 매퍼 변환/DTO 일관성 검증
- **Messaging**
    - 메시지 생성/변환/validation 검증
- **Redis**
    - Repository API, KeyManager TTL, Serializer round-trip 검증
- **Lock**
    - NamedLock: GET_LOCK/RELEASE_LOCK 정상 획득/해제
    - Redisson: tryLock/해제 동작 검증
    - Aspect: AOP 프록시 경유 확인
- **TSID**
    - NodeId 계산, zone fallback, EC2 meta 비활성화 시 랜덤 처리

----------------------------------------------------------------------------------------------------

## 10) 한 줄 요약

- Application: 주문 DTO/매퍼만 포함, 도메인 보호
- Infra: Messaging/Redis/Lock/TSID 등 제공 코드로 조립
- 원칙: 조건부 구성(@ConditionalOnProperty) + AOP/팩토리/유틸로 환경 독립성 확보
