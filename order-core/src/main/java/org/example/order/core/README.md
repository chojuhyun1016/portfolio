# 📦 order-core — 통합 분석 (application + infra)

----------------------------------------------------------------------------------------------------

## 1) 최상위 개요 (DDD + Clean Architecture)

의존 흐름(요지)

    [application] → [domain ports + mapper] ←implements— [infra adapters] —talks-to→ [DB/Dynamo/Redis/External]

루트 모듈(패키지)

    org.example.order.core
    ├─ application/     ← 유스케이스/앱 서비스/DTO/Mapper (도메인 포트 사용)
    ├─ infra/           ← 기술 구현(Adapters) + 조건부 구성(Config)
    └─ support/         ← 매핑/시간 등 공통 기술 지원(도메인·인프라 중립)

핵심 원칙

- 도메인 보호: Domain 은 Port(인터페이스)만 소유, 구현은 Infra Adapter
- 경계 보호: 외부/타 컨텍스트 스키마는 Domain 에 직접 노출 금지 → (필요 시) infra.acl 에서 번역/격리
- 애그리거트 우선: 저장/조회 어댑터는 `infra.persistence/<aggregate>/<tech>` 구조를 우선
- 설정 기반 조립: 인프라 빈은 컴포넌트 스캔이 아니라 `@Bean + @Conditional...` 조합으로 등록
- Fail-fast: 필수 설정(예: region/endpoint/secret-name 등) 누락 시 가능한 빠르게 실패하도록 설계

----------------------------------------------------------------------------------------------------

## 2) application 계층 — 현재 구조(현행 코드 스냅샷 반영)

루트

    org.example.order.core.application

주요 구성(ORDER 애그리거트 중심)

    org.example.order.core.application
    ├─ order/
    │  ├─ dto/
    │  │  ├─ command/        LocalOrderCommand, OrderCommand
    │  │  ├─ query/          LocalOrderQuery, OrderQuery
    │  │  ├─ sync/           LocalOrderSync, OrderSync
    │  │  └─ view/           LocalOrderView, OrderView
    │  ├─ cache/             OrderCacheService, OrderCacheWriteService, OrderCacheAssembler
    │  ├─ config/            ApplicationAutoConfiguration, OrderCacheConfig, OrderMapperConfig
    │  └─ mapper/            LocalOrderMapper(MapStruct), OrderMapper(MapStruct), OrderCacheViewMapper(MapStruct/Bean)
    └─ (기타 애그리거트 확장 시 동일 패턴)

요약(의도)

- Command/Query/Sync/View 를 분리하여 목적을 명확히 함
  - Command: “무엇을 처리할지” (Operation 포함)
  - Query: “무엇을 조회할지”
  - Sync: 이벤트/동기화/전파 관점의 표준 형태(with* 메서드로 메타 보강)
  - View: 조회 응답(읽기 모델)
- Mapper 는 MapStruct 기반으로 **정합성(누락 필드 컴파일 에러) + 시간 변환 표준화(TimeMapper/TimeProvider)** 를 추구

핵심 토글

- `order.application.enabled` (default true)
  - ApplicationAutoConfiguration 활성/비활성 제어
- 캐시 관련은 별도의 조건(`order.cache.enabled`, `order.cache.redis.enabled`, Repository 존재 등)으로 조립

----------------------------------------------------------------------------------------------------

## 3) support 계층 — 매핑/시간 공통(추가된 현행 코드 반영)

루트

    org.example.order.core.support

구성(현행)

    org.example.order.core.support
    └─ mapping/
       ├─ config/            AppMappingConfig (MapStruct 전역 설정)
       ├─ TimeMapper         (LocalDateTime ↔ epoch millis, @Named)
       └─ TimeProvider       (Clock 주입 기반 now 제공)

요약

- `AppMappingConfig`
  - componentModel=spring
  - unmappedTargetPolicy=ERROR (누락 필드 컴파일 에러)
  - injectionStrategy=CONSTRUCTOR
  - uses = {TimeMapper, TimeProvider}
- `TimeProvider` 는 Clock 기반이므로 테스트에서 `Clock.fixed(...)` 로 결정성 확보

----------------------------------------------------------------------------------------------------

## 4) infra 계층 — 디렉터리(현행 코드/스냅샷 반영)

루트

    org.example.order.core.infra

하위 구조(요약)

    org.example.order.core.infra
    ├─ common/
    │  ├─ aop/                AopConfig (@EnableAspectJAutoProxy proxyTargetClass=true)
    │  ├─ idgen/tsid/          TsidInfraConfig, TsidConfig, TsidProperties, TsidFactoryHolder, CustomTsidGenerator, FallbackIdGeneratorConfig
    │  └─ secrets/             SecretsInfraConfig, SecretsLoader, SecretsKeyResolver, SecretsKeyClient, SecretsManagerProperties
    ├─ crypto/                 CryptoInfraConfig, EncryptorFactory, EncryptProperties, Algorithm/Engine/Exception/Seed
    ├─ dynamodb/               DynamoInfraConfig, DynamoDbProperties, DynamoQuerySupport, (local) DynamoMigrationAutoConfiguration
    ├─ jpa/
    │  ├─ config/              JpaInfraConfig (+ 하위 Order/LocalOrder Query/Repo/Command Infra Config)
    │  ├─ converter/           BooleanToYNConverter (autoApply=true)
    │  └─ querydsl/            QuerydslUtils, WhereClauseBuilder, LazyBooleanExpression
    ├─ persistence/
    │  └─ order/
    │     ├─ dynamo/impl/      OrderDynamoRepositoryImpl (Enhanced Client)
    │     ├─ jpa/impl/         OrderRepositoryJpaImpl, OrderQueryRepositoryJpaImpl (QueryDSL + EM)
    │     ├─ jdbc/             OrderCommandRepositoryJdbcImpl (JdbcTemplate 벌크)
    │     └─ redis/            RedisRepository, RedisRepositoryImpl (범용 인프라 유틸)
    └─ lock/
       ├─ annotation/          DistributedLock, DistributedLockT
       ├─ aspect/              DistributedLockAspect
       ├─ config/              LockInfraConfig (단일 조립)
       ├─ exception/           LockAcquisitionException
       ├─ factory/             LockExecutorFactory, LockKeyGeneratorFactory
       ├─ key/                 LockKeyGenerator
       │  └─ impl/             SHA256LockKeyGenerator, SimpleLockKeyGenerator, SpelLockKeyGenerator
       ├─ lock/                LockExecutor, LockCallback
       │  └─ impl/             NamedLockExecutor, RedissonLockExecutor
       ├─ props/               NamedLockProperties, RedissonLockProperties
       └─ support/             TransactionalOperator

구조 의도

- “기술별 모듈” (jpa/dynamodb/crypto/lock/secrets/tsid) 과
- “애그리거트 우선 어댑터” (infra.persistence.order.*) 를 분리
- 기술 모듈은 클라이언트/공통 유틸/조건부 구성을 제공
- persistence 는 도메인 포트 구현체(adapters)를 애그리거트 단위로 배치

----------------------------------------------------------------------------------------------------

## 5) Persistence(ORDER) — 저장/조회 어댑터(현행)

### 5.1 JPA/QueryDSL

- `OrderRepositoryJpaImpl` (implements domain `OrderRepository`)
  - `EntityManager.find/persist/merge` + QueryDSL 삭제
  - Spring Data Repository 인터페이스 사용 없이 “직접 구현”으로 단일 레이어 유지
- `OrderQueryRepositoryJpaImpl` (implements `OrderQueryRepository`)
  - QueryDSL projection 으로 `OrderView` 반환
  - updateByOrderId 등 “정합성/일관성 우선” 성격의 쿼리 제공

### 5.2 JDBC 벌크(명령)

- `OrderCommandRepositoryJdbcImpl` (implements `OrderCommandRepository`)
  - insert ignore, 조건부 update, version=version+1 등 “대량 처리 최적화”
  - 청크 단위 batchUpdate (기본/옵션으로 조정)

### 5.3 DynamoDB(읽기 모델/프로젝션)

- `OrderDynamoRepositoryImpl` (implements `OrderDynamoRepository`)
  - AWS SDK v2 Enhanced Client
  - PK+SK 기반 모델에서 deleteById 제한(복합 키 특성)
  - userIdIndexName(GSI) 존재 시 Query 우선, 옵션으로 scan fallback
  - batch delete(25개 단위) + backoff + unprocessed 처리 로그

### 5.4 Redis(범용 인프라 유틸)

- `RedisRepository` / `RedisRepositoryImpl`
  - Value/Hash/List/Set/ZSet/TTL/SCAN/Transaction 등 범용 연산 제공
  - 성격: “도메인 포트 구현”이라기보다 “인프라 공용 유틸”
  - 실제 도메인 캐시는 별도 Port(예: OrderCacheRepository)를 두고 이를 감싸는 형태를 권장

----------------------------------------------------------------------------------------------------

## 6) Lock — 분산락(현행 코드 기준으로 현행화)

### 6.1 어노테이션(2종)

- `@DistributedLock`
  - 기존 트랜잭션 참여(REQUIRED)로 임계구역 실행
- `@DistributedLockT`
  - 새 트랜잭션(REQUIRES_NEW)로 임계구역 실행

파라미터

- key: 락 키 표현식(문자열)
- type: 실행기 이름 (예: `namedLock`, `redissonLock`)
- keyStrategy: 키 생성기 이름 (기본 `sha256`, 옵션 `spell`, `simple`)
- waitTime / leaseTime: 호출 단위 override (0 또는 미지정 시 properties 기본값 사용)

> 주의: keyStrategy 코멘트에 `spell` 로 표기되어 있으며, 실제 Bean 이름도 `spell` 로 등록됨.

### 6.2 Aspect 동작 흐름

    Caller (@DistributedLock / @DistributedLockT)
     └─ DistributedLockAspect
         1) annotation 파라미터 추출(key, type, keyStrategy, waitTime, leaseTime)
         2) LockKeyGeneratorFactory.getGenerator(keyStrategy) → 키 생성
         3) LockExecutorFactory.getExecutor(type) → 실행기 선택
         4) executor.execute(key, wait, lease, callback)
               └─ callback: TransactionalOperator.runWithExistingTransaction / runWithNewTransaction

- `@DistributedLock`  → transactionalOperator.runWithExistingTransaction(...) (REQUIRED)
- `@DistributedLockT` → transactionalOperator.runWithNewTransaction(...) (REQUIRES_NEW)

### 6.3 실행기(2종)

- NamedLockExecutor (DB)
  - MySQL/MariaDB `GET_LOCK/RELEASE_LOCK`
  - GET_LOCK 은 내부적으로 짧은 timeout(초)로 반복 시도
  - retryInterval 기반 재시도, 최종 실패 시 LockAcquisitionException
  - DataSourceUtils 로 커넥션 획득/반납

- RedissonLockExecutor (Redis)
  - `RLock.tryLock(...)` 기반
  - wait/lease/retryInterval 조합으로 반복 시도
  - 인터럽트 발생 시 interrupt flag 복원 후 예외 전파

### 6.4 LockInfraConfig(단일 조립)

- 컴포넌트 스캔이 아니라 `@Bean` 조립
- 전역 lock.enabled 스위치가 아니라 **실행기별 스위치**로 제어(현행 코드)
  - `lock.named.enabled=true` + DataSource 존재 → namedLock 등록
  - `lock.redisson.enabled=true` + RedissonClient 존재 → redissonLock 등록
  - `lock.redisson.enabled=true` + Redisson 클래스 존재 + RedissonClient 미존재 → RedissonClient 직접 생성

> 정리: “Aspect/Factory/TxOperator” 는 등록되지만, 실행기/클라이언트는 조건에 따라 부분적으로 등록된다.
> (운영 정책에 따라 Aspect 자체도 별도 스위치로 감싸고 싶다면 확장 포인트로 고려 가능)

----------------------------------------------------------------------------------------------------

## 7) Crypto + Secrets + TSID (현행 스냅샷 반영 요약)

### 7.1 Crypto

- `crypto.enabled=true` 조건부 활성
- AES128/AES256/AES-GCM + 해시/서명(HMAC 등) + Seed 옵션
- 키 주입은 Properties 또는 Secrets 로딩과 결합하여 운영에서 교체 가능
- 포맷/엔진 분리로 알고리즘 추가에 유리

### 7.2 Secrets(AWS Secrets Manager)

- `aws.secrets-manager.enabled=true` 조건부 활성
- SecretsLoader 가 ApplicationReadyEvent 시점에 refreshOnce
- localstack 인 경우 secret missing 시 `{}` bootstrap 지원(옵션/조건)
- 스냅샷/포인터 모델로 resolver 가 키 선택/조회
- 스케줄 갱신은 `scheduler-enabled=true` 일 때만 TaskScheduler로 fixedDelay 등록

### 7.3 TSID

- `tsid.enabled=true` 조건부로 TSID factory 구성
- nodeId 계산(EC2 meta/hostname hash XOR, 실패 시 랜덤)
- Hibernate @CustomTsid 로 IdentifierGenerator 연동
- IdGenerator(domain port) 로도 제공되어 서비스/리포지토리에서 공통 사용

----------------------------------------------------------------------------------------------------

## 8) 설정 샘플 (application.yml) — “현행 코드” 기준 예시

### 8.1 JPA

    jpa:
      enabled: true

### 8.2 DynamoDB

    dynamodb:
      enabled: true
      endpoint: http://localhost:4566
      region: ap-northeast-2
      access-key: test
      secret-key: test
      table-name: order_projection
      # (선택) userIdIndexName / allowScanFallback 등은 프로젝트 Properties에 맞춰 추가

### 8.3 Lock (현행: 실행기별 스위치 + Redisson은 host/port)

    lock:
      named:
        enabled: true
        wait-time: 3000
        retry-interval: 150

      redisson:
        enabled: true
        host: 127.0.0.1
        port: 6379
        password:
        database: 0
        wait-time: 3000
        lease-time: 10000
        retry-interval: 150

> RedissonClient 는 `lock.redisson.host/port` 기반으로 `redis://host:port` 를 내부에서 구성한다.
> (TLS가 필요하면 normalize 로직 확장 또는 rediss 지원을 위한 별도 설정 확장이 필요)

### 8.4 Crypto

    crypto:
      enabled: true
      props:
        seed: true

    encrypt:
      aes128: { key: "BASE64_KEY_128" }
      aes256: { key: "BASE64_KEY_256" }
      aesgcm: { key: "BASE64_KEY_GCM" }
      hmac:   { key: "BASE64_KEY_HMAC" }

### 8.5 Secrets Manager

    aws:
      secrets-manager:
        enabled: true
        region: ap-northeast-2
        secret-name: myapp/secret-keyset
        refresh-interval-millis: 300000
        fail-fast: true
        scheduler-enabled: false

### 8.6 TSID

    tsid:
      enabled: true
      node-bits: 10
      prefer-ec2-meta: false
      zone-id: Asia/Seoul

----------------------------------------------------------------------------------------------------

## 9) 테스트 전략 (현행 구성에 맞춘 권장)

- **Application**
  - MapStruct 매핑 누락 방지(컴파일 단계) + 변환 결과 스냅샷 테스트
  - TimeProvider(Clock) 고정으로 시간 의존 로직 결정성 확보

- **Persistence**
  - JDBC 벌크: Testcontainers(MySQL) 기반 insert/update/버전 증가 검증
  - JPA/QueryDSL: 슬라이스 또는 통합 테스트로 projection/updateByOrderId 검증
  - Dynamo: LocalStack + 테이블/인덱스 구성 후 Query/ScanFallback/배치삭제 검증
  - RedisRepository: 직렬화 round-trip + SCAN 동작 검증(대량 키에서 keys 금지 확인)

- **Lock**
  - NamedLock: GET_LOCK/RELEASE_LOCK 정상 획득/해제, 경합/재시도/타임아웃 검증
  - Redisson: tryLock/해제/인터럽트 처리 검증
  - Aspect: 프록시 경유(스프링 AOP) + @DistributedLock / @DistributedLockT 전파 차이 검증

- **Secrets**
  - localstack endpoint + secret bootstrap/refreshOnce/fail-fast 조건 분기 테스트
  - resolver selection(kid/version) 및 wipeAll 동작 검증(메타만 로깅)

- **TSID**
  - nodeId 계산 경로(EC2 meta off/on, hostname hash, fallback random) 검증
  - Hibernate generator(@CustomTsid) 연동 smoke test

----------------------------------------------------------------------------------------------------

## 10) 한 줄 요약(현행)

- Application: Command/Query/Sync/View + MapStruct 기반 변환/캐시 조립(조건부)
- Support: AppMappingConfig + TimeMapper/TimeProvider로 “시간/매핑 표준”을 중앙화
- Infra: JPA/Dynamo/JDBC/Redis/Lock/Crypto/Secrets/TSID 를 조건부 구성으로 조립
- 원칙: “도메인 포트 중심 + 인프라 어댑터 구현 + 설정 기반 토글 + fail-fast” 로 환경 독립성과 운영 안정성을 확보
