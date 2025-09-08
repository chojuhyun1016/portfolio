# org.example.order.core.infra.persistence

--------------------------------------------------------------------------------

## 1) 목적과 스코프

- 스코프: `org.example.order.core.infra.persistence` 하위 구현(주문 **order** 도메인)과 동일한 규칙으로 **payment** 도메인 추가 시 구조 예시까지 포함
- 역할: 도메인 레이어의 **포트(인터페이스)** 를 실제 기술(**JPA, JDBC, DynamoDB, Redis**)로 구현하는 **인프라 어댑터** 모음
- 원칙:
  - 도메인은 기술을 모른다 → 인프라는 **도메인 포트**를 구현한다
  - 인프라 구현은 **컴포넌트 스캔 없이 설정 클래스(@Bean)** 에서만 **조건부**로 등록한다
  - **애그리거트-우선, 기술-하위**: `persistence/<aggregate>/<tech>/...`

--------------------------------------------------------------------------------

## 2) 현재 코드 기준 구조(ORDER 도메인)와 파일별 역할

디렉터리 트리 요약

    org.example.order.core.infra.persistence.order
    ├─ dynamo/
    │  └─ impl/
    │     └─ OrderDynamoRepositoryImpl.java
    ├─ impl/
    │  └─ OrderCommandRepositoryJdbcImpl.java
    ├─ jpa/
    │  └─ impl/
    │     ├─ OrderQueryRepositoryJpaImpl.java
    │     └─ OrderRepositoryJpaImpl.java
    └─ redis/
       ├─ RedisRepository.java
       └─ impl/
          └─ RedisRepositoryImpl.java

클래스 역할 매핑

- **Dynamo (읽기모델, 특수조회, 프로젝션)**
  - `OrderDynamoRepositoryImpl` (implements `org.example.order.domain.order.repository.OrderDynamoRepository`)
    - **AWS SDK v2 Enhanced Client** 사용
    - 제공 기능: `save`, `findById`, `findAll`, `findByUserId`, `deleteById`
    - **GSI(userId 인덱스) 우선 조회**, 실패 시 **scan fallback**(옵션)
    - **옵션**: `limit`(기본 1000), `consistentReadGet`(기본 false), `allowScanFallback`(기본 true)
    - 등록 조건: `DynamoDbEnhancedClient` 빈 존재(+ 테이블명 등 환경 설정)

- **JDBC (고성능 명령, 벌크 처리)**
  - `OrderCommandRepositoryJdbcImpl` (implements `OrderCommandRepository`)
    - `JdbcTemplate` + `TsidFactory`로 **대량 insert/update**
    - **`insert ignore`**, `version = version + 1` 전략 반영
    - 등록 조건: `JdbcTemplate`, `TsidFactory` 빈 존재

- **JPA/QueryDSL (정합성 높은 읽기, 기본 저장)**
  - `OrderQueryRepositoryJpaImpl` (implements `OrderQueryRepository`)
    - `JPAQueryFactory` + QueryDSL로 조회 결과를 `OrderView`로 **프로젝션**
  - `OrderRepositoryJpaImpl` (implements `OrderRepository`)
    - **Spring Data 위임 제거** → **`EntityManager` + QueryDSL**로 단일 레이어 구현
    - `findById`: `em.find`
    - `save`: `persist/merge` 분기
    - `deleteByOrderIdIn`: `queryFactory.delete(...).where(in(...)).execute()`

- **Redis (범용 캐시/자료구조 유틸)**
  - `RedisRepository` (인프라 내부 공통 인터페이스)
  - `RedisRepositoryImpl` (`RedisTemplate` 기반 구현)
    - Value, Hash, List(블로킹/논블로킹), Set, ZSet, TTL/Keys, Transaction 등 **광범위 연산 지원**
    - 성격상 **도메인 포트 구현체라기보다 인프라 공통 유틸**에 가까움

- **공통 보조**
  - `BooleanToYNConverter`: `Boolean` ↔ `"Y"/"N"` 매핑 컨버터(autoApply=false)
  - QueryDSL 유틸
    - `QuerydslUtils.page(...)` : **별도 count 쿼리로 total 계산**(Querydsl 5.x `fetchCount` 제거 흐름 반영)
    - `QuerydslUtils.stream(...)` : 하이버네이트 `getResultStream()` 사용
    - `WhereClauseBuilder` / `LazyBooleanExpression`: **가독성 높은 동적 where** 헬퍼

--------------------------------------------------------------------------------

## 3) 설계 원칙(ORDER와 PAYMENT 공통 적용)

- **애그리거트 우선, 기술 하위**
  - 위치: `infra/persistence/<aggregate>/<tech>/...`
  - 예: `infra/persistence/order/jpa/impl/...`, `infra/persistence/order/dynamo/impl/...`

- **도메인 포트 ↔ 인프라 구현 매핑**
  - 포트 예시: `OrderRepository`, `OrderQueryRepository`, `OrderCommandRepository`, `OrderDynamoRepository`
  - 구현 예시: `OrderRepositoryJpaImpl`, `OrderQueryRepositoryJpaImpl`, `OrderCommandRepositoryJdbcImpl`, `OrderDynamoRepositoryImpl`
  - 선택 가이드: 구현 접미사를 **Adapter**로 통일하는 네이밍(예: `...JpaAdapter`)도 선택지

- **설정은 각 기술 config에, 어댑터 생성은 조건부 빈으로**
  - JPA: 상위 `JpaInfraConfig`에서 `EntityManager`, `JPAQueryFactory` 등 기술 클라이언트 빈 구성
  - Dynamo: 별도 `DynamoInfraConfig`에서 `DynamoDbClient`, `DynamoDbEnhancedClient` 구성
  - Redis: `RedisTemplate` 등은 전용 Config에서 구성
  - 각 어댑터는 **관련 클라이언트 빈 존재** 및 **기능 스위치 ON** 시에만 `@Bean` 등록

--------------------------------------------------------------------------------

## 4) PAYMENT 도메인 추가 시 권장 구조

디렉터리 트리 예시

    org.example.order.core.infra.persistence.payment
    ├─ jpa/
    │  └─ impl/
    │     ├─ PaymentQueryRepositoryJpaImpl.java           (implements PaymentQueryRepository)
    │     └─ PaymentRepositoryJpaImpl.java                (implements PaymentRepository)
    ├─ jdbc/
    │  └─ PaymentCommandRepositoryJdbcImpl.java           (implements PaymentCommandRepository)
    ├─ dynamo/
    │  └─ impl/
    │     └─ PaymentDynamoRepositoryImpl.java             (implements PaymentDynamoRepository)
    └─ redis/
       ├─ RedisRepository.java                            (선택: 공통 유틸로 승격 권장)
       └─ impl/
          └─ RedisRepositoryImpl.java

주의
- 위 구조는 **payment가 내부 애그리거트**일 때의 예시
- **외부 컨텍스트(연동)** 라면 `persistence`가 아닌 `infra/acl/payment` 아래에서 **Gateway/DTO/Translator**로 통신

--------------------------------------------------------------------------------

## 5) 설정(application.yml, 현재 코드 조건과 호환)

현재 코드에서 사용되는 전역 스위치는 **`jpa.enabled`** 이다. Dynamo는 테이블명 등 별도 프라퍼티를 사용한다(프로젝트 규칙에 맞게 prefix 구성).

    jpa:
      enabled: true

    dynamodb:
      enabled: true
      endpoint: http://localhost:4566
      region: ap-northeast-2
      access-key: test
      secret-key: test
      table-name: order_projection
      # 선택: userId 조회용 GSI 이름(있으면 우선 Query, 실패 시 scan fallback)
      user-id-index-name: idx_user_id

    # payment용 Dynamo 별도 구성(모듈에서 prefix를 분리하려면 코드에서 별도 Properties로 주입)
    dynamodb-payment:
      enabled: false
      table-name: payment_projection

설명
- `jpa.enabled=true`일 때 상위 `JpaInfraConfig`가 활성화되고, `EntityManager` 존재 시 `JPAQueryFactory`가 등록되며, 조건을 만족하면 JPA 기반 어댑터 빈이 생성된다
- `dynamodb.enabled=true`이고 `table-name`이 존재하면 `OrderDynamoRepositoryImpl` 빈 등록 구성 가능(설정 클래스에서 조건부 등록)

--------------------------------------------------------------------------------

## 6) 조건부 빈 구성(Java, 현재 코드 스타일과 호환되는 스케치)

**JPA 설정과 ORDER 어댑터 조립 예시**

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(EntityManager.class)
    public JPAQueryFactory jpaQueryFactory(EntityManager em) {
        // 템플릿 지정 없이 기본 JPQL 템플릿 사용
        return new JPAQueryFactory(em);
    }

    @Bean
    @ConditionalOnMissingBean(OrderQueryRepository.class)
    @ConditionalOnBean(JPAQueryFactory.class)
    public OrderQueryRepository orderQueryRepositoryJpa(JPAQueryFactory queryFactory) {
        return new OrderQueryRepositoryJpaImpl(queryFactory);
    }

    @Bean
    @ConditionalOnMissingBean(OrderRepository.class)
    @ConditionalOnBean({JPAQueryFactory.class, EntityManager.class})
    public OrderRepository orderRepositoryJpa(JPAQueryFactory queryFactory, EntityManager em) {
        // Spring Data 위임 제거: EM + QueryDSL 직접 구현 사용
        return new OrderRepositoryJpaImpl(queryFactory, em);
    }

    @Bean
    @ConditionalOnMissingBean(OrderCommandRepository.class)
    @ConditionalOnBean({JdbcTemplate.class, TsidFactory.class})
    public OrderCommandRepository orderCommandRepositoryJdbc(JdbcTemplate jdbc, TsidFactory tsid) {
        return new OrderCommandRepositoryJdbcImpl(jdbc, tsid);
    }

**Dynamo 설정과 ORDER 어댑터 조립 예시**

    @Bean
    @ConditionalOnBean(DynamoDbClient.class)
    @ConditionalOnMissingBean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient client) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(client).build();
    }

    @Bean
    @ConditionalOnBean(DynamoDbEnhancedClient.class)
    @ConditionalOnProperty(prefix = "dynamodb", name = "table-name")
    @ConditionalOnMissingBean
    public OrderDynamoRepository orderDynamoRepository(
            DynamoDbEnhancedClient enhancedClient,
            DynamoDbProperties props
    ) {
        // GSI 이름/옵션 주입 가능한 오버로드 사용
        return new OrderDynamoRepositoryImpl(
            enhancedClient,
            props.getTableName(),
            props.getUserIdIndexName(), // null이면 scan fallback 가능
            props.getDefaultQueryLimit(), // null이면 기본 1000
            props.isConsistentReadGet(),  // 기본 false
            props.isAllowScanFallback()   // 기본 true
        );
    }

payment 도메인에도 동일 패턴을 대칭 적용하며, 필요 시 payment 전용 프로퍼티 prefix를 분리할 수 있다.

--------------------------------------------------------------------------------

## 7) 상황별 사용법(애플리케이션 레이어 관점)

**A) 대량 신규 주문 적재(고성능)**
- 전제: `JdbcTemplate`, `TsidFactory` 구성, `OrderCommandRepositoryJdbcImpl` 등록
- 사용: `OrderCommandRepository.bulkInsert(entities)`
- 이점: `insert ignore`로 중복 방어, **TSID**로 키 생성 일관성 확보

  @Service
  @RequiredArgsConstructor
  public class OrderIngestionService {
  private final OrderCommandRepository orderCommandRepository;

        @Transactional
        public void ingest(List<OrderEntity> entities) {
            orderCommandRepository.bulkInsert(entities);
        }
  }

**B) 정합성 높은 조회(단건 상세)**
- 전제: `JPAQueryFactory` 구성, `OrderQueryRepositoryJpaImpl` 등록
- 사용: `OrderQueryRepository.fetchByOrderId(orderId)`
- 이점: QueryDSL로 투명한 프로젝션 및 조건 조합

  @Service
  @RequiredArgsConstructor
  public class OrderQueryService {
  private final OrderQueryRepository orderQueryRepository;

        @Transactional(readOnly = true)
        public OrderView view(Long orderId) {
            return orderQueryRepository.fetchByOrderId(orderId).orElse(null);
        }
  }

**C) 읽기모델 기반의 빠른 목록/특수 조회(Dynamo)**
- 전제: `DynamoDbEnhancedClient` 구성, `dynamodb.table-name` 설정
- 사용: `OrderDynamoRepository.findById`, `findAll`, `findByUserId`
- 이점: 분리된 프로젝션 테이블로 빠른 접근, **GSI 우선 / scan fallback** 옵션

  @Service
  @RequiredArgsConstructor
  public class OrderProjectionService {
  private final OrderDynamoRepository orderDynamoRepository;

        @Transactional(readOnly = true)
        public Optional<OrderDynamoEntity> byId(String id) {
            return orderDynamoRepository.findById(id);
        }
  }

**D) Redis 범용 유틸 사용(키-값, 자료구조, 트랜잭션)**
- 전제: `RedisTemplate` 구성, `RedisRepositoryImpl` 등록
- 사용: `set/get`, `hash`, `list`(블로킹/논블로킹), `set`, `zset`, `expire`, `transaction` 등
- 권장: 실제 도메인 캐시가 필요하면 **별도의 Port(예: `OrderCachePort`)**를 정의하고 해당 Port를 구현하는 Adapter에서 `RedisRepository`를 감싸 **키 네임스페이스, TTL, 직렬화**를 일관되게 관리

  @Service
  @RequiredArgsConstructor
  public class OrderCacheService {
  private final RedisRepository redis;

        public void putOrderView(String key, Object view, long ttlSec) {
            redis.set(key, view, ttlSec);
        }

        public Object getOrderView(String key) {
            return redis.get(key);
        }
  }

**E) payment 도메인 사용 시**
- 내부 애그리거트라면 order와 동일 구조로 `persistence/payment/<tech>` 하위에 구현을 추가
- 외부 연동이라면 `persistence`가 아닌 `infra/acl/payment`에 **Gateway/DTO/Translator**로 구현

--------------------------------------------------------------------------------

## 8) 확장 사용법과 구조 패턴

- **새로운 애그리거트 추가(예: invoice)**
  - 디렉터리: `infra/persistence/invoice/{jpa|jdbc|dynamo|redis}`
  - 도메인 포트 인터페이스만 정의하면, 실제 기술 선택은 **설정 스위치**로 제어 가능

- **새로운 기술 추가(예: ElasticSearch)**
  - 디렉터리: `infra/persistence/<aggregate>/elastic`
  - 설정: `infra/elastic/config/ElasticInfraConfig`에 클라이언트 빈 구성
  - 어댑터: 포트 구현체를 `elastic` 하위에 배치

- **선택적 리네이밍 가이드**
  - 구현체 접미사 `Impl` → `Adapter` 통일 **권장**
  - 예: `OrderRepositoryJpaImpl` → `OrderRepositoryJpaAdapter`
  - 명확성: **도메인 Repository(포트)** vs **인프라 Adapter(구현)** 구분 강화

--------------------------------------------------------------------------------

## 9) 성능과 운영 팁

- **JDBC 벌크**
  - 드라이버 URL에 `rewriteBatchedStatements=true` 권장
  - `insert ignore`는 충돌을 조용히 무시하므로 **감사 로깅**이나 **ON DUPLICATE KEY UPDATE** 전략 옵션화 고려
  - **배치 청크 크기**는 운영 환경에 맞게 조정(기본 10,000)

- **QueryDSL 카운트**
  - 최신 QueryDSL에서는 `fetchCount()` 제거 추세
  - **별도 count 쿼리**로 total 계산(현재 유틸은 count 전용 select 수행) → **content 쿼리와 분리** 권장

- **대형 IN 절 삭제**
  - `deleteByOrderIdIn`과 같은 IN 절은 매우 클 때 SQL 길이/계획 이슈
  - **배치 분할**(예: 1k 단위) 또는 **임시 테이블 조인** 전략을 옵션으로 제공

- **Dynamo 스캔**
  - `scan`은 비용/한도(1MB)/성능 이슈 → **PK/GSI + Query 중심**으로 스키마 설계
  - `allowScanFallback`은 운영에서 **주의 깊게 사용**(기본 true)

- **Redis**
  - **키 네임스페이스, TTL, 직렬화 정책**을 전용 어댑터에서 통일 관리
  - 대량 명령은 `executePipelined`로 **네트워크 왕복 감소**
  - `keys` 대신 `SCAN` 사용(구현 이미 `SCAN` 기반)

- **Boolean Y/N 매핑**
  - `@Converter(autoApply = false)`이므로 엔티티 필드에 `@Convert(converter = BooleanToYNConverter.class)` **명시 적용**

--------------------------------------------------------------------------------

## 10) 테스트 전략

- **단위 테스트**
  - 도메인 서비스는 **포트 스텁/목**을 주입해 기술 의존성 제거

- **인프라 테스트**
  - Testcontainers(LocalStack, MySQL, Redis 등)로 어댑터 단위 검증
  - `jpa.enabled`, Dynamo 테이블/인덱스, Redis 직렬화 등 **스위치 조합** 점검

- **계약/구성 테스트**
  - `@ConditionalOnProperty`, `@ConditionalOnBean` 조건을 가진 Config들의 **조합 테스트**로 오작동 예방

- **QueryDSL 삭제/업데이트 테스트**
  - Mockito로 `JPADeleteClause` 상호작용 검증 시 **과도한 검증**으로 실패가 잦음
  - **실제 `JPAQueryFactory` + H2/Testcontainers** 기반 슬라이스/통합 테스트 권장

--------------------------------------------------------------------------------

## 11) 결론

- `persistence` 아래에 **애그리거트 우선(order, payment)** → **기술 하위(jpa, jdbc, dynamo, redis)** 구조로 인프라 구현을 배치하면, **도메인(포트)** 와 **인프라(구현)** 의 경계가 명확하고 교체/확장이 수월하다
- 현재 코드의 **전역 토글(`jpa.enabled`)**, Dynamo 테이블/GSI 설정, `EntityManager` + `JPAQueryFactory` 직등록, Spring Data 의존 제거 등 **현행 구현 변화**를 반영하여 조건부 등록을 구성한다
- payment 도메인도 내부 애그리거트라면 완전히 **대칭 구조**로 추가하고, 외부 컨텍스트라면 `persistence`가 아닌 `acl` 패키지에 **Gateway/DTO/Translator**로 구현한다
