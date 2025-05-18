# order-core:application:messaging 패키지 구조 및 역할 정리

본 디렉토리는 `order-core` 모듈의 메시징 기능을 담당하는 구성 요소를 정의하며, 메시지 전송, DLQ(Died Letter Queue), 메시지 카테고리, 메시지 객체 모델 등으로 구성된다.  
MSA 환경에서 Kafka, Redis, MQ 기반 메시징 시스템을 연계할 수 있도록 설계되어 있으며, 메시지의 발신/수신/변환/분류/에러 처리 등의 역할을 수행한다.

---

## 📁 common

### 📁 code

- 공통 메시징에서 사용하는 `Enum` 정의 위치
- 예: `MessageMethodType`, `CodeEnum` 등 메시지 행위, 코드 분류 기준 정의

### 📁 message

- 메시지의 기본 클래스 정의 위치
- `DlqMessage` 등 공통 메시지 베이스 클래스 포함
- 모든 서비스 메시지는 이 클래스를 상속받아 사용

---

## 📁 order

### 📁 code

#### DlqOrderType.java

- `DlqType` 인터페이스를 구현한 `Order` 서비스 전용 DLQ 유형 Enum
- 실패한 메시지를 DLQ로 라우팅할 때 사용되는 구분값
- 값 목록:
    - ORDER_LOCAL
    - ORDER_API
    - ORDER_CRUD
    - ORDER_REMOTE

> **용도:** 메시지 전송 실패 시, 해당 메시지를 어떤 DLQ 채널로 보낼지 명시

#### MessageCategory.java

- `CodeEnum` 인터페이스를 구현한 메시지 분류용 Enum
- 전체 메시지 흐름 혹은 구독 처리 시 메시지 카테고리를 식별하는 데 사용
- 값 목록:
    - ORDER_LOCAL, ORDER_API, ORDER_CRUD, ORDER_REMOTE, ORDER_DLQ, ORDER_ALARM

> **용도:** Kafka Topic, 로그 필터링, 모니터링 등에서 메시지 범주 구분

---

### 📁 message

#### OrderLocalMessage.java

- 메시지 생성 시 가장 최초에 생성되는 로컬 메시지
- 이후 다른 메시지(`OrderApiMessage`, `OrderCrudMessage`)로 변환되어 사용됨
- 필드:
    - id: 메시지 대상 ID
    - methodType: 생성/수정/삭제 등
    - publishedTimestamp: 메시지 최초 발행 시각

> **용도:** 내부 이벤트 발생의 시작점. 유효성 검사 및 후속 메시지로 변환

#### OrderApiMessage.java

- 외부 API 또는 서비스로 전송되는 메시지 포맷
- `OrderLocalMessage`에서 변환됨
- DLQ 유형: ORDER_API

> **용도:** 외부 API 연동 이벤트 전파

#### OrderCloseMessage.java

- 주문 마감(close) 이벤트 전용 메시지
- 주문 ID와 동작 타입을 포함
- DLQ 유형: ORDER_REMOTE

> **용도:** 주문 마감 이벤트를 외부 시스템 또는 다른 컨텍스트로 전달

#### OrderCrudMessage.java

- CRUD 이벤트 처리 메시지
- 내부 DTO와 메시지 행위 정보 포함
- DLQ 유형: ORDER_CRUD

> **용도:** 주문 생성/수정/삭제 시 관련 서비스로 전파

---

## ✅ 메시지 흐름 요약

    OrderLocalMessage (발생 원본)
        ↓ toMessage()
    OrderApiMessage / OrderCloseMessage 등 (API / Remote 송신용)
        ↓ toMessage()
    OrderCrudMessage (CRUD 동작 대상 전달)

---

## 📌 설계 의도 및 확장성 가이드

- **Enum 분리 관리**: 메시지 타입별 명확한 책임과 경계 유지
- **단일 책임 메시지 클래스**: 유지보수성과 테스트 용이성 향상
- **유효성 검사 내장**: OrderLocalMessage 생성 시 필수값 검증
- **DlqMessage 상속 구조**: 일관된 메시지 처리 흐름 확보

---

## 📦 확장 고려 사항

1. **Kafka Topic 매핑 명시**
    - 메시지 타입 → Topic 이름 명확화
    - 예: `ORDER_API → topic.order.api`

2. **DLQ 메시지 복구 처리**
    - `DlqType` 기반 메시지 모니터링 및 재처리 설계 필요

3. **MessageCategory 기반 메트릭 분류**
    - Prometheus, ELK 등 연동 시 카테고리별 집계 가능

---

## 📚 구조 요약

| 영역             | 설명                                                    |
|------------------|---------------------------------------------------------|
| `code`           | 메시지 분류, DLQ 타입, 카테고리 정의                     |
| `message`        | 각 메시지 페이로드 클래스 정의                           |
| `common.message` | 메시지 베이스 클래스(DlqMessage)                         |
| `common.code`    | 공통 메시지 동작(Enum), 코드 인터페이스 정의            |

> 패키지 분리는 명확한 책임 경계를 유지하며, 확장성·유지보수성을 고려한 구조입니다.
