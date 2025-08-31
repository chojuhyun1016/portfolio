# 📦 order-core — 통합 분석( application + infra )

----------------------------------------------------------------------------------------------------

## 1) 최상위 개요(DDD + Clean Architecture)

    의존 흐름(요지)
      [application] → [domain ports + mapper] ←implements— [infra adapters] —talks-to→ [DB/Kafka/Redis/External]

    모듈 루트
      org.example.order.core
      ├─ application/     ← 유스케이스, DTO, 매퍼, 이벤트/리스너, 스케줄러, 예외
      └─ infra/           ← persistence(저장/조회 구현), messaging, acl, jpa, dynamo, redis, lock, crypto, common, config

    핵심 원칙
      - 도메인 보호: Domain 은 Port(인터페이스)만 소유, 구현은 Infra Adapter
      - 경계 보호: 외부/타 컨텍스트 스키마는 Domain 에 직접 노출 금지 → infra.acl 로 흡수/번역(Gateway/Translator/외부 DTO)
      - 애그리거트 우선: 저장/조회 어댑터는 persistence/<aggregate>/<tech> 구조
      - 설정 기반 조립: @ConditionalOnProperty + @Import 로 모듈 토글/배선

----------------------------------------------------------------------------------------------------

## 2) application 계층 — 디렉터리 지도(고정폭 트리)

    org.example.order.core.application
    ├─ common/
    │   ├─ adapter/         ← 외부 연동 인터페이스(애플리케이션 관점)
    │   ├─ dto/
    │   │   ├─ command/     ← 명령 요청 DTO
    │   │   ├─ incoming/    ← 외부 유입 DTO(Kafka/Webhook)
    │   │   ├─ internal/    ← 내부 전달 DTO(Local* 등)
    │   │   ├─ model/       ← 단순 VO/레코드
    │   │   ├─ outgoing/    ← 외부 송신 DTO
    │   │   ├─ query/       ← 조회 전용 DTO(Projection)
    │   │   └─ response/    ← API 응답 DTO
    │   ├─ event/           ← @EventListener, @TransactionalEventListener
    │   ├─ exception/       ← 애플리케이션 전용 예외
    │   ├─ listener/        ← Kafka/MQ/스케줄 리스너
    │   ├─ mapper/          ← Application ↔ Domain 변환(도메인 직접 노출 금지)
    │   ├─ scheduler/       ← 배치/주기 작업
    │   └─ service/         ← 유스케이스/핸들러
    └─ order/
        ├─ adapter/
        ├─ dto/ (incoming|command|internal|model|outgoing|query|response)
        ├─ event/
        ├─ exception/
        ├─ listener/
        ├─ mapper/
        ├─ scheduler/
        └─ service/

- DTO/매퍼 핵심
  - LocalOrderDto(내부 전달 표준) ↔ OrderEntity(도메인)
  - OrderApiOutgoingDto → (toMessage) → OrderCloseMessage(메시징 전송 DTO)
  - OrderEntityDto: 도메인 엔티티의 애플리케이션용 래퍼(직접 노출 금지)

- 흐름 예시(Command → Domain → Messaging)
  LocalOrderDto
  → (OrderMapper.toEntity) → OrderEntity
  → 도메인 서비스/포트 호출(저장/상태변경)
  → (mapper) → OrderApiOutgoingDto
  → toMessage() → OrderCloseMessage → producer.send()

----------------------------------------------------------------------------------------------------

## 3) infra 계층 — 디렉터리 지도(고정폭 트리)

    org.example.order.core.infra
    ├─ persistence/                 ← 저장/조회 어댑터(애그리거트 우선 → 기술 하위)
    │   ├─ order/
    │   │   ├─ jpa/
    │   │   │   ├─ adapter/         (SpringDataOrderJpaRepository)
    │   │   │   └─ impl/            (OrderRepositoryJpaImpl, OrderQueryRepositoryJpaImpl)
    │   │   ├─ jdbc/
    │   │   │   └─ impl/            (OrderCommandRepositoryJdbcImpl)    ← 현 구조가 order/impl 인 경우, jdbc/impl 로 이관 권장
    │   │   ├─ dynamo/
    │   │   │   └─ impl/            (OrderDynamoRepositoryImpl)
    │   │   └─ redis/
    │   │       ├─ RedisRepository.java
    │   │       └─ impl/            (RedisRepositoryImpl)
    │   └─ payment/                 (대칭 구조: jpa|jdbc|dynamo|redis 하위 구성)
    ├─ messaging/                   ← 브로커 설정, 프로듀서/컨슈머, 전송 DTO
    │   ├─ config/
    │   ├─ common/                  (DLQ/헤더/키 전략/재시도)
    │   └─ order/
    │       ├─ producer/
    │       ├─ consumer/
    │       └─ message/             (OrderCloseMessage 등)
    ├─ acl/                         ← Anti-Corruption Layer(외부/타 컨텍스트 번역)
    │   ├─ member/
    │   │   ├─ MemberClient / MemberDto / MemberTranslator
    │   │   └─ MemberGatewayHttp (implements MemberGateway)
    │   └─ payment/
    │       ├─ PaymentClient / PaymentDto / PaymentTranslator
    │       └─ PaymentGatewayHttp (implements PaymentGateway)
    ├─ jpa/                         ← JPA/QueryDSL 설정/유틸
    │   ├─ config/                  (JpaInfraConfig: jpa.enabled)
    │   └─ querydsl/                (QuerydslUtils, WhereClauseBuilder 등)
    ├─ dynamo/                      ← DynamoDB 설정/유틸
    │   ├─ config/                  (DynamoInfraConfig: dynamodb.enabled)
    │   └─ props/                   (DynamoDbProperties)
    ├─ redis/                       ← Redis 설정/유틸
    ├─ lock/                        ← NamedLock + RedissonLock
    ├─ crypto/                      ← 암호화/키관리(algorithm/contract/util/config/...)
    ├─ common/                      ← idgen(TSID), secrets, aop 등
    └─ config/                      ← 글로벌 오토컨피그 허브(선택)

- 포트 ↔ 어댑터 매핑(주요)
  - OrderRepository            ↔  OrderRepositoryJpaImpl
  - OrderQueryRepository       ↔  OrderQueryRepositoryJpaImpl
  - OrderCommandRepository     ↔  OrderCommandRepositoryJdbcImpl
  - OrderDynamoRepository      ↔  OrderDynamoRepositoryImpl
  - (도메인 캐시 필요 시) CachePort ↔ 전용 CacheAdapter(내부에서 RedisRepositoryImpl 사용)

- ACL 사용 규칙
  - 도메인/애플리케이션은 외부/타 컨텍스트를 직접 참조 금지
  - 항상 PaymentGateway/MemberGateway 등 도메인 Port 에만 의존
  - 외부 스키마 변경/하위호환/타임아웃/리트라이/서킷/폴백은 ACL 어댑터에서 캡슐화

----------------------------------------------------------------------------------------------------

## 4) 설정 샘플(application.yml · 4-space 블록)

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

    core:
      infra:
        persistence:
          order:
            jpa:    { enabled: true }
            jdbc:   { enabled: true }
            dynamo: { enabled: true,  table-name: order_projection }
            redis:  { enabled: false }
          payment:
            jpa:    { enabled: true }
            jdbc:   { enabled: false }
            dynamo: { enabled: false, table-name: payment_projection }
            redis:  { enabled: true }
        messaging:
          kafka:   { enabled: true }
        acl:
          payment:
            enabled: true
            base-url: https://payment.api
            timeout-ms: 3000

----------------------------------------------------------------------------------------------------

## 5) 상황별 사용 스니펫(4-space 코드 블록)

A) 대량 적재/명령(JDBC)

        @Service
        @RequiredArgsConstructor
        public class OrderIngestionService {
            private final OrderCommandRepository orderCommandRepository;
            @Transactional
            public void ingest(List<OrderEntity> entities) {
                orderCommandRepository.bulkInsert(entities); // JdbcTemplate + TSID
            }
        }

B) 정합성 조회(JPA/QueryDSL)

        @Service
        @RequiredArgsConstructor
        public class OrderQueryService {
            private final OrderQueryRepository orderQueryRepository;
            @Transactional(readOnly = true)
            public OrderView view(Long orderId) {
                return orderQueryRepository.fetchByOrderId(orderId); // Projection
            }
        }

C) 읽기모델/특수조회(Dynamo)

        @Service
        @RequiredArgsConstructor
        public class OrderProjectionService {
            private final OrderDynamoRepository orderDynamoRepository;
            @Transactional(readOnly = true)
            public Optional<OrderDynamoEntity> byId(String id) {
                return orderDynamoRepository.findById(id);
            }
        }

D) 캐시 활용(Redis 유틸 → 전용 Adapter로 감싸 권장)

        @Service
        @RequiredArgsConstructor
        public class OrderCacheService {
            private final RedisRepository redis;
            public void putOrderView(String key, Object view, long ttlSec) { redis.set(key, view, ttlSec); }
            public Object getOrderView(String key) { return redis.get(key); }
        }

E) 메시징 발행(도메인 이벤트 → 전송 DTO)

        @Service
        @RequiredArgsConstructor
        public class OrderClosePublisher {
            private final OrderCloseProducer producer;
            public void publish(Long orderId) {
                var msg = OrderCloseMessage.toMessage(orderId, MessageMethodType.CLOSE);
                producer.send(msg);
            }
        }

F) 외부 연동(반드시 ACL 경유)

        @Service
        @RequiredArgsConstructor
        public class PaymentAppService {
            private final PaymentGateway paymentGateway; // 도메인 out-port
            public PaymentStatus ensureAuthorized(PaymentId pid) {
                return paymentGateway.fetchStatus(pid);   // infra.acl.payment.PaymentGatewayHttp
            }
        }

----------------------------------------------------------------------------------------------------

## 6) 운영/테스트 팁(요약)

- 단위 테스트: Domain/Application 은 Port 스텁/목 사용(기술 의존 제거)
- 인프라 검증: Testcontainers(LocalStack/RDB/Redis)로 어댑터 동작 확인
- 구성 테스트: ConditionalOnProperty 조합별 Context 로딩 검증
- JDBC 벌크: insert ignore 는 충돌을 조용히 흡수 → on duplicate key update/감사로그 권장
- QueryDSL: fetchCount 비권장 → 별도 카운트 쿼리 유틸 도입
- Dynamo: Scan 남용 지양, PK/GSI + Query 우선
- Redis: 키 네임스페이스/TTL/직렬화 정책은 전용 CacheAdapter에 캡슐화
- Messaging: 파티셔닝/순서보장, DLQ 관측, idempotency 키 설계
- ACL: 타임아웃/리트라이/서킷/폴백 수치 명시, 외부 오류를 도메인 예외/상태로 변환

----------------------------------------------------------------------------------------------------

## 7) 한 줄 요약

- Application: 유스케이스 중심, DTO/매퍼로 도메인 보호, 외부는 **항상 ACL 경유**
- Infra: 저장/조회 구현은 **persistence** 로 통합(애그리거트 우선 → 기술 하위)
- Messaging: infra.messaging 으로 이동, 프로듀서/컨슈머/전송 DTO 일원화
- 설정: 현행 키(jpa.enabled, dynamodb.*, redis.enabled, lock.*) 유지 + 선택적 세분 토글로 점진 확장
