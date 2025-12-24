# org.example.order.core.infra.persistence

--------------------------------------------------------------------------------

## 1) 목적과 스코프

- 스코프
  - `org.example.order.core.infra.persistence` 하위의 **order 애그리거트**
  - 동일한 규칙으로 **payment 애그리거트 확장 시 기준 구조**까지 포함
- 역할
  - 도메인 레이어에 정의된 **포트(Repository 인터페이스)** 를
    실제 저장 기술(**JPA, JDBC, DynamoDB, Redis**)로 구현하는 **인프라 어댑터 계층**
- 핵심 원칙
  - 도메인은 기술을 모른다  
    → 인프라는 **도메인 포트**만 구현한다
  - 인프라 구현체는 **컴포넌트 스캔 금지**
    → **설정 클래스(@Configuration + @Bean)** 에서만 **조건부 등록**
  - 구조 규칙
    - 애그리거트 우선
    - 기술은 하위 분기
    - 형태: `persistence/<aggregate>/<tech>/...`

--------------------------------------------------------------------------------

## 2) 현재 코드 기준 구조 (ORDER 도메인)

디렉터리 트리 요약

    org.example.order.core.infra.persistence.order
    ├─ dynamo/
    │  └─ impl/
    │     └─ OrderDynamoRepositoryImpl.java
    ├─ jdbc/
    │  └─ impl/
    │     ├─ OrderCommandRepositoryJdbcImpl.java
    │     └─ LocalOrderCommandRepositoryJdbcImpl.java
    ├─ jpa/
    │  └─ impl/
    │     ├─ OrderQueryRepositoryJpaImpl.java
    │     ├─ OrderRepositoryJpaImpl.java
    │     ├─ LocalOrderQueryRepositoryJpaImpl.java
    │     └─ LocalOrderRepositoryJpaImpl.java
    └─ redis/
       ├─ RedisRepository.java
       └─ impl/
          └─ RedisRepositoryImpl.java

--------------------------------------------------------------------------------

## 3) 기술별 구현 상세 (Order / LocalOrder 공통 규칙)

### 3.1 DynamoDB (읽기 모델 / 프로젝션 전용)

구현체
- OrderDynamoRepositoryImpl
- AWS SDK v2 **Enhanced Client** 기반

핵심 설계 규칙
- 테이블은 **복합키(PK + SK)** 구조
- 단건 조회라도 **getItem 사용 금지**
  - PK만 알고 있는 접근 패턴이므로
  - 항상 Query + limit(1) 사용
- 정렬 키(SK)는 orderNumber

조회 전략
- 기본: Base Table Query
- userId 조건:
  - GSI(userIdIndexName) 존재 시 → GSI Query
  - 실패 시
    - allowScanFallback=false → 빈 결과
    - allowScanFallback=true → Scan 후 필터링

옵션 기본값 (생성자 기준)
- defaultQueryLimit = 1000
- consistentReadGet = false
- allowScanFallback = false

삭제 규칙
- deleteById(id) : **명시적으로 금지**
- deleteByIdAndOrderNumber(id, orderNumber) : 유일한 단건 삭제
- deleteAllByPartition(id)
  - Query로 PK 전체 조회
  - BatchWriteItemEnhanced (25개 단위)
  - 미처리 항목은 지수 백오프(max 5회) 재시도

이 계층의 책임
- 순수 Infra
- 비즈니스 판단 로직 없음
- 트랜잭션 개념 없음

--------------------------------------------------------------------------------

### 3.2 JDBC (고성능 Command 전용)

구현체
- OrderCommandRepositoryJdbcImpl
- LocalOrderCommandRepositoryJdbcImpl

공통 특징
- JdbcTemplate 기반
- 대량 insert / update 전용
- JPA EntityManager 사용하지 않음

Insert 전략
- insert ignore
- PK(id)는 사전에 할당되어 있어야 함
- 중복 충돌은 조용히 무시

Update 전략
- 조건
  - where order_id = ?
  - and published_datetime <= ?
- 의미
  - 이벤트 정렬 보장
  - 늦게 도착한 이벤트가 최신 상태를 덮어쓰지 않도록 방어
- version = version + 1

배치 처리
- 기본 batchChunkSize = 10,000
- OrderBatchOptions로 런타임 조정 가능

--------------------------------------------------------------------------------

### 3.3 JPA / QueryDSL (정합성 중심)

Query 계열
- OrderQueryRepositoryJpaImpl
- LocalOrderQueryRepositoryJpaImpl

특징
- JPAQueryFactory + QueryDSL
- Entity 직접 반환 금지
- OrderView로 프로젝션

Command / Aggregate 저장
- OrderRepositoryJpaImpl
- LocalOrderRepositoryJpaImpl

설계 의도
- Spring Data JPA 미사용
- EntityManager 직접 사용
  - persist / merge 명시적 분기
- QueryDSL delete / update 직접 호출

--------------------------------------------------------------------------------

### 3.4 Redis (공통 인프라 유틸)

구성 위치
- infra.persistence.order.redis

성격
- 특정 도메인 포트 구현체 아님
- **인프라 공통 유틸 계층**

제공 기능
- Value / Hash / List / Set / ZSet
- Blocking / Non-blocking List
- TTL / SCAN
- Transaction 지원

권장 사용 방식
- 도메인 캐시 직접 연결 금지
- 예:
  - OrderCachePort (도메인)
  - OrderCacheRedisAdapter (infra)
- 키 네임스페이스 / TTL / 직렬화 정책을 Adapter에서 통제

--------------------------------------------------------------------------------

## 4) 공통 인프라 보조 구성

### 4.1 BooleanToYNConverter

역할
- Boolean ↔ "Y" / "N"

특징
- autoApply = true
- 모든 Boolean 필드에 전역 적용
- 엔티티에 @Convert 명시 불필요

위치
- infra.jpa.converter
- 도메인이 인프라를 참조하지 않도록 분리

### 4.2 QueryDSL 유틸

구성요소
- QuerydslUtils
  - page()
    - QueryDSL 5.x fetchCount 제거 대응
    - count 쿼리 별도 실행
  - stream()
    - Hibernate getResultStream 사용
- WhereClauseBuilder
- LazyBooleanExpression

효과
- null / 조건부 where 절 가독성 향상
- 동적 쿼리 안정성 확보

--------------------------------------------------------------------------------

## 5) PAYMENT 도메인 확장 기준 구조

    org.example.order.core.infra.persistence.payment
    ├─ jpa/
    │  └─ impl/
    │     ├─ PaymentQueryRepositoryJpaImpl.java
    │     └─ PaymentRepositoryJpaImpl.java
    ├─ jdbc/
    │  └─ impl/
    │     └─ PaymentCommandRepositoryJdbcImpl.java
    ├─ dynamo/
    │  └─ impl/
    │     └─ PaymentDynamoRepositoryImpl.java
    └─ redis/
       └─ (공통 RedisRepository 재사용 또는 Adapter 래핑)

주의
- payment가 내부 애그리거트인 경우에만 persistence 하위
- 외부 결제 연동일 경우
  - infra/acl/payment
  - Gateway / DTO / Translator 패턴 사용

--------------------------------------------------------------------------------

## 6) 설정(application.yml 기준)

전역 JPA 스위치

    jpa:
      enabled: true

Order DynamoDB

    dynamodb:
      enabled: true
      endpoint: http://localhost:4566
      region: ap-northeast-2
      table-name: order_projection
      user-id-index-name: idx_user_id

Payment 분리 시 예시

    dynamodb-payment:
      enabled: false
      table-name: payment_projection

--------------------------------------------------------------------------------

## 7) 애플리케이션 레이어 사용 시나리오

A) 대량 적재
- JDBC Command Repository 사용
- 트랜잭션은 서비스 레이어에서 관리

B) 단건 조회 / 정합성
- JPA Query Repository 사용

C) 빠른 조회 / 목록
- Dynamo Repository 사용

D) 캐시
- RedisRepository 직접 사용 금지
- 반드시 도메인 Port + Adapter 경유

--------------------------------------------------------------------------------

## 8) 확장 패턴

- 신규 애그리거트(invoice, shipment)
  - persistence/<aggregate>/<tech>
- 신규 기술(elastic, mongo)
  - persistence/<aggregate>/<new-tech>
- 구현체 명명
  - Impl → Adapter 전환 권장

--------------------------------------------------------------------------------

## 9) 성능 및 운영 가이드

- JDBC
  - rewriteBatchedStatements=true 권장
  - batch size는 DB 상황에 따라 조정
- Dynamo
  - Scan은 최후의 수단
  - GSI 중심 설계
- Redis
  - keys 금지
  - SCAN + pipelining 사용

--------------------------------------------------------------------------------

## 10) 테스트 전략

- 도메인 서비스
  - 포트 Mock / Stub
- 인프라
  - Testcontainers (MySQL, LocalStack, Redis)
- 구성
  - ConditionalOnProperty / ConditionalOnBean 조합 테스트

--------------------------------------------------------------------------------

## 11) 결론

- persistence 계층은
  - 기술 교체
  - 확장
  - 운영 분리를 가능하게 하는 핵심 인프라 계층이다
- 현재 구조는
  - Order / LocalOrder 이중 모델
  - JDBC + JPA + Dynamo 역할 분리
  - 대규모 트래픽 대응을 전제로 한 설계다
- payment 역시 동일한 원칙으로 대칭 확장한다

--------------------------------------------------------------------------------
