# 📦 order-core

`order-core` 모듈은 **주문 도메인**의 핵심 비즈니스 로직을 담당하는 중심 모듈입니다.  
**MSA**와 **DDD** 아키텍처 기반으로 설계되었으며, 다음 세 계층으로 구성됩니다:

- `application` 계층: 유스케이스, 명령(Command), 쿼리(Query), 이벤트 처리
- `domain` 계층: 도메인 모델, VO, Enum, Converter 등 비즈니스 규칙 정의
- `infra` 계층: 외부 인프라(JPA, Redis, DynamoDB, 분산 락 등) 연동 구현

---

## 🔧 모듈 계층별 설명

### 📂 application

비즈니스 유스케이스의 진입점으로, 외부 요청을 도메인 계층과 연결하거나 이벤트를 처리합니다.

#### `application/order`
- **dto**: 주문 요청/응답을 위한 DTO 정의
- **listener**: 주문 관련 Kafka/DLQ 등 외부 이벤트 수신 처리
- **mapper**: DTO ↔ Entity 간 변환 처리
- **scheduler**: 주문 도메인 배치성 예약 처리
- **adapter**: 외부 서비스 연동을 위한 인터페이스
- **service**: 주문 유스케이스 중심의 명령/쿼리 처리 서비스
- **exception**: 주문 도메인에 특화된 예외 정의
- **event**: 주문 도메인 내부 이벤트 정의 및 발행 처리

#### `application/common`
- 공통 로직을 유스케이스 관점에서 재사용 가능하도록 모듈화
- **dto, listener, mapper, scheduler, adapter, service, exception, event**: 각각의 목적은 `order`와 동일하되 도메인 간 공용 로직으로 활용

---

### 🧠 domain

불변성과 규칙 중심의 비즈니스 모델을 정의합니다.

#### `domain/order`
- **entity**: 주문 핵심 도메인 객체 (`OrderEntity`, `OrderItemEntity` 등)
- **enums**: `OrderStatus`, `PaymentType` 등 상태/타입 정의
- **vo**: 값 객체 (예: `OrderNumber`, `UserId`, `Money` 등)
- **converter**: JPA Enum ↔ DB 값 변환기 (`@Converter` 기반)

#### `domain/common`
- 여러 도메인 간 공유 가능한 공통 규칙과 객체 정의
- **repository**: 공통 조회/저장 인터페이스
- **entity**: 공통 부모 클래스 (`BaseTimeEntity`, `VersionEntity` 등)
- **code/type**: Enum 외 코드 및 식별자 타입
- **value/model**: VO 또는 공용 도메인 모델
- **exception**: 시스템 전반에서 발생 가능한 예외 정의
- **event**: 시스템 전역 이벤트 정의 및 전파 구조

---

### 🧩 infra

도메인/애플리케이션 계층과 실제 인프라스트럭처(DB, Redis 등) 간 연결을 담당합니다.

#### `infra/crypto`
- **contract, util, config, algorithm, constant, exception, factory**  
  암호화, 복호화, 서명, 해싱 관련 모듈

#### `infra/config`
- 전체 모듈 설정 관련 클래스 정의

#### `infra/security`
- **jwt, oauth2, gateway**  
  인증/인가 관련 설정 및 유틸

#### `infra/redis`
- **config, repository, support**  
  RedisTemplate 설정, 키 전략, TTL 관리 등 Redis 연동 로직

#### `infra/lock`
- **config, lock, annotation, key, support, aspect, exception, factory**  
  Redisson, NamedLock 기반 분산락 처리 및 AOP 확장

#### `infra/jpa`
- **repository, config, querydsl**  
  JPA 기본 Repository, QueryDSL 설정 및 WhereClauseBuilder 등

#### `infra/dynamo`
- **repository, config, support**  
  AWS DynamoDB 연동 설정 및 EnhancedClient 기반 구현

#### `infra/common`
- **idgen**: TSID 등 고유 ID 생성 전략
- **secrets**: KMS 연동 등 보안 비밀 관리
- **aop**: 공통 AOP 로직 (예: 로깅, 트랜잭션 트레이싱 등)

---

## 🔄 messaging

비동기 메시징 구조를 도메인별로 정의한 서브모듈

#### `messaging/order`
- **code**: 주문 메시지 타입, DLQ 타입 등의 상수 정의
- **message**: 주문 도메인 관련 Kafka 메시지, DLQ 메시지 포맷

#### `messaging/common`
- **code**: 전역 메시지 수준, 타입 코드 정의
- **message**: 공통 메시지 구조 (예: `DlqMessage`, `MonitoringMessage` 등)

---

## ✅ 대표 기능 요약

- 주문 도메인 중심의 유스케이스 실행 및 이벤트 처리
- 도메인 모델 기반의 VO, Entity, Enum 구성
- TSID, TABLE 기반의 ID 생성 전략
- QueryDSL 기반 복합 JPA 쿼리 처리
- Redis, DynamoDB 등 다양한 스토리지 연동
- 암호화/복호화, AOP, 분산 락 등 공통 인프라 처리
- Kafka 기반 메시징, DLQ, 모니터링 이벤트 처리 구조 내장

---

## 🔗 참고

- 본 모듈은 독립적으로 동작하도록 설계되었으며,
  `api-service`, `batch-service`, `worker-service` 등에서 core의 유스케이스와 도메인을 직접 참조함
- 외부 시스템과의 결합은 `adapter`를 통해 decouple 되며, 테스트 용이성과 유지보수성을 확보함
