# order-core:application:messaging:order 패키지 구조 및 역할 정리

본 디렉토리는 `order-core` 모듈의 **주문(Order) 서비스 전용 메시징 기능**을 담당합니다.  
메시지 전송, DLQ(Died Letter Queue) 분류, 메시지 카테고리 정의, 메시지 객체 모델 등으로 구성되며,  
MSA 환경에서 Kafka 기반 이벤트 드리븐 아키텍처를 지원하도록 설계되었습니다.

---

## 📁 code

### DlqOrderType.java
- `DlqType` 인터페이스를 구현한 **Order 서비스 전용 DLQ Enum**
- 메시지가 실패했을 때 어떤 DLQ 채널로 라우팅할지 명시
- 값 목록:
    - ORDER_LOCAL
    - ORDER_API
    - ORDER_CRUD
    - ORDER_REMOTE

> 용도: 메시지 전송 실패 시, 각 메시지가 속한 DLQ 유형을 지정하여 재처리 경로를 분리

### MessageCategory.java
- `CodeEnum` 인터페이스를 구현한 **메시지 카테고리 Enum**
- 서비스 전반에서 메시지 흐름/토픽 구분을 위해 사용
- 값 목록:
    - ORDER_LOCAL
    - ORDER_API
    - ORDER_CRUD
    - ORDER_REMOTE
    - ORDER_DLQ
    - ORDER_ALARM

> 용도: Kafka Topic, 로그 필터링, 모니터링/메트릭 분류 등에 사용

---

## 📁 message

### OrderLocalMessage.java
- 메시지 플로우의 시작점(Local 이벤트)
- 필드:
    - id: 이벤트 대상 PK
    - methodType: 행위(CREATE/UPDATE/DELETE)
    - publishedTimestamp: 메시지 최초 발행 시각
- validation() 메서드를 통해 필수값 검증 수행

> 용도: 내부 도메인 이벤트가 최초 발생했음을 알리는 메시지

### OrderApiMessage.java
- OrderLocalMessage → API 전달 포맷으로 변환
- 필드: id, methodType, publishedTimestamp
- 변환 생성자 및 toMessage() 팩토리 메서드 제공
- DLQ 유형: ORDER_API

> 용도: 외부 API/다른 서비스로 이벤트를 전파

### OrderCrudMessage.java
- CRUD 처리 레이어에서 사용되는 메시지
- 필드:
    - methodType: 행위
    - dto: OrderDto (주문 도메인 데이터)
- OrderApiMessage + OrderDto 기반으로 생성
- 테스트 전용 팩토리(test())와 생성자는 Deprecated 처리
- DLQ 유형: ORDER_CRUD

> 용도: 주문 생성/수정/삭제 이벤트를 내부 다른 서비스로 전파

### OrderCloseMessage.java
- 주문 마감(Remote) 이벤트 전용 메시지
- 필드:
    - orderId
    - methodType
- toMessage() 팩토리 제공
- DlqOrderType.ORDER_REMOTE 고정
- 기본 생성자는 PRIVATE 접근 제한으로 무분별한 사용 방지

> 용도: 주문 종료 이벤트를 원격 시스템/외부 컨텍스트로 전달

---

## ✅ 메시지 흐름 요약

OrderLocalMessage (로컬 이벤트 발생)
↓ toMessage()
OrderApiMessage (API 전송용)
↓ toMessage()
OrderCrudMessage (CRUD 반영/내부 전달)
↓
OrderCloseMessage (외부 시스템 전달)

---

## 📌 설계 의도 및 확장성 가이드

- Enum 분리 관리: 메시지 타입별 경계와 책임을 명확히 분리
- 단일 책임 원칙: 각 메시지는 특정 계층/용도에만 집중
- 유효성 검사 내장: LocalMessage 단계에서 필수값 검증 수행
- DlqMessage 상속: DLQ 대응 및 일관된 메시지 처리 플로우 보장

---

## 📦 확장 고려 사항

1. Kafka Topic 매핑
    - MessageCategory → Kafka Topic 명시적 매핑 필요
    - 예: ORDER_API → topic.order.api

2. DLQ 재처리 전략
    - DlqType 기반 모니터링/재처리 파이프라인 설계
    - 운영 시 DLQ 적재 → 알람 → 재처리 → 복구 흐름 지원

3. 카테고리 기반 모니터링
    - MessageCategory 별 집계를 Prometheus/ELK 등과 연동 가능

---

## 📚 구조 요약

| 영역              | 설명                                         |
|-------------------|----------------------------------------------|
| code              | DLQ Enum, 메시지 카테고리 Enum 정의          |
| message           | Order 서비스 전용 메시지 클래스 정의          |
| common.message    | 모든 메시지의 상위 클래스(DlqMessage)        |
| common.code       | 공통 메시지 동작(Enum), 코드 인터페이스 정의 |

> 패키지 구조는 이벤트 플로우 단계별 메시지 책임 분리와 DLQ 기반 안정성 확보를 목표로 합니다.
