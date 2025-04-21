# order-core

order-core 모듈은 주문 도메인의 핵심 비즈니스 로직을 담당하는 모듈입니다  
MSA와 DDD 아키텍처 기반으로 구성되었으며, 다음과 같은 세 계층으로 나뉘어 있습니다

- application 계층  유스케이스, 명령, 쿼리, 이벤트 처리
- domain 계층       비즈니스 도메인 모델과 규칙 정의
- infra 계층         외부 시스템과의 연결 구현 (JPA, Redis, DynamoDB, Lock 등)

## 모듈 계층별 설명

### application

외부 요청에 대해 도메인 로직을 호출하거나 이벤트를 처리하는 계층입니다

- application/common/error
  공통 예외 정의 및 핸들링 로직

- application/common/event
  내부 이벤트 처리 및 전파 구조 정의

- application/common/external
  외부 시스템 연동 어댑터 정의

- application/order/command
  주문 관련 유스케이스 명령 처리

- application/order/event
  주문 관련 이벤트 정의 및 핸들링
    - message 디렉토리 내에서 이벤트 분류별 메시지 구성

- application/order/query
  주문 관련 쿼리 요청 처리
    - Dto 응답 구성

- application/order/vo
  서비스 단에서 사용하는 응답 및 전달용 VO 객체

### domain

비즈니스 중심의 도메인 모델이 정의되는 계층입니다  
JPA 엔티티, Enum, VO, Converter 등 불변 규칙을 중심으로 구성됩니다

- domain/common/entity
    - VersionEntity  JPA 공통 엔티티 부모 클래스

- domain/order/entity
    - OrderEntity  주문 도메인 모델

- domain/order/enums
    - OrderStatus  주문 상태 Enum

- domain/order/vo
    - OrderNumber, UserId  값 객체로서 식별자나 주문번호 등의 타입 정의

- domain/order/converter
    - JPA용 주문 상태 Enum 컨버터

### infra

도메인 혹은 애플리케이션 계층과 외부 인프라 간의 연결을 담당하는 계층입니다

- infra/config
    - OrderCoreConfig  전체 설정 컴포넌트

- infra/common
    - aop  공통 AOP 설정 (예 logging 등)
    - idgen  ID 생성 전략 (TSID, TABLE 전략 등)

- infra/crypto
    - 암복호화, 해싱, 서명 엔진 및 팩토리
    - 각종 예외, 설정, 유틸 포함

- infra/dynamo
    - DynamoDB 연동 설정 및 Repository 구현
    - query 지원 유틸 포함

- infra/jpa
    - JPA QueryDSL 설정 및 WhereClauseBuilder 유틸
    - Repository, CustomQuery 구현

- infra/lock
    - Redisson, NamedLock 기반 분산 락 처리
    - LockKey 전략 및 어노테이션 기반 락 처리

- infra/redis
    - Redis 연결 설정, Repository, Key 지원 유틸

---

## 대표 기능 요약

- 주문 도메인 중심의 유스케이스 실행 및 이벤트 처리
- 도메인 모델 기반의 VO, Entity, Enum 구성
- TSID, TABLE 기반의 ID 자동 생성 전략
- QueryDSL 기반 복잡한 JPA 쿼리 지원
- Redis, DynamoDB 등 다양한 스토리지 연동
- AOP, 분산락, 암호화 등 공통 인프라 기능 포함

---

## 참고

이 모듈은 서비스 간 의존성 없이 독립적으로 동작하도록 설계되어 있으며  
다른 모듈(api-service, batch-service 등)에서 core 모듈의 도메인/유스케이스를 활용합니다
