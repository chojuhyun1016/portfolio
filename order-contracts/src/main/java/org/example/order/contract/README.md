# 📦 order-contracts 모듈 개요 (Contract / Wire Schema Layer)

`order-contracts` 모듈은 **서비스 경계를 넘는 데이터 구조(계약, Contract)** 만을 정의하는 전용 계층입니다.  
HTTP API, Kafka 메시지, DLQ, 모니터링 등 **외부로 노출되거나 서비스 간 전달되는 스키마**를 단일 진실(Single Source of Truth)로 관리합니다.

- ✅ 포함: **Request/Response DTO**, **Event/Payload DTO**, **DLQ Envelope**, **Monitoring Wire Schema**, **공통 Operation/Error 타입**
- ❌ 제외: **비즈니스 로직**, **서비스/레포지토리**, **프레임워크 의존(Spring/JPA/Kafka Client)**, **내부 엔티티/도메인 모델**

> 핵심 원칙
> - “무엇을 주고받는지”만 정의한다 (How는 상위/하위 모듈 책임)
> - 후방 호환성 우선 (필드 추가는 가능, 의미 변경/삭제는 엄격 제한)
> - 원시/문자열 중심 (프레임워크/내부 타입 의존 최소화)
> - 내부 DTO/도메인과 **완전 분리**

---

## 📂 패키지 구조

~~~
org.example.order.contract
 ├─ order
 │   ├─ http
 │   │   ├─ request
 │   │   ├─ response
 │   │   └─ type
 │   └─ messaging
 │       ├─ dlq
 │       ├─ event
 │       ├─ payload
 │       └─ type
 └─ shared
     ├─ error
     ├─ monitoring
     │   ├─ ctx
     │   ├─ msg
     │   └─ type
     └─ op
~~~

---

## 1) order.http.request — HTTP 요청 계약

**목적**
- 외부 클라이언트 → API 요청 바디 스키마 정의
- 검증/처리 로직 없이 **필드 구조만 고정**

**구성**
- `OrderIdRequest(Long orderId)`
    - 주문 단건 조회 요청 DTO
    - 예: `{ "orderId": 12345 }`
- `OrderPublishRequest(Long orderId, MethodType methodType)`
    - 주문 이벤트 발행 요청 DTO
    - 예: `{ "orderId": 12345, "methodType": "CREATE" }`

**특징**
- record 기반의 불변 DTO
- 필드명 자체가 계약이며, validation 정책은 API/Service 계층에서 수행

---

## 2) order.http.response — HTTP 응답 계약

**목적**
- 외부 노출용 읽기 모델(View) 제공
- 내부 엔티티/도메인 구조 은닉 (외부 계약 안정성 확보)

**구성**
- `OrderDetail(...)`
    - 외부 노출용 주문 상세 View
    - `publishedDatetime` 은 **문자열 포맷 반환 권장**  
      (예: `"yyyy-MM-dd HH:mm:ss"`)
- `OrderGetResponse(OrderDetail order)`
    - 응답 래퍼: `{ "order": { ... } }` 형태를 표준화

**특징**
- “외부 응답 스키마”를 고정하여 내부 변경 파급을 차단

---

## 3) order.http.type — HTTP 계약 전용 타입

**목적**
- HTTP 바디에서 사용하는 “동작 타입”을 계약으로 고정

**구성**
- `MethodType`
    - `CREATE`, `UPDATE`, `DELETE`

**원칙**
- HTTP 계약 용도에 한정
- 메시징의 `Operation` 과 의미는 유사하지만 **계층 분리(독립 진화)**

---

## 4) order.messaging.type — 메시지 카테고리(계약)

**목적**
- 서비스 간 메시지의 성격(카테고리)을 단일 Enum으로 식별

**구성**
- `MessageOrderType`
    - `ORDER_LOCAL`, `ORDER_API`, `ORDER_CRUD`, `ORDER_REMOTE`, `ORDER_DLQ`, `ORDER_ALARM`

**원칙**
- 서비스 간 합의된 카테고리 값이며, 라우팅/필터링/관측에 사용

---

## 5) order.messaging.payload — 메시지 Payload(계약)

**목적**
- 이벤트 본문에 실리는 주문 데이터 스냅샷 정의
- 내부 엔티티와 유사해도 “계약 관점 DTO”로 분리

**구성**
- `OrderPayload(...)`
    - 주문 식별자/사용자/금액/상태/버전 등 핵심 데이터
    - 생성/수정 메타(사용자/유형/일시) 포함
    - 날짜 필드: `@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")` 로 계약 포맷을 명시

**특징**
- 이벤트 종류에 따라 일부 필드는 null 허용(선택 채움)
- 시간은 혼재 가능:
    - `createdDatetime`, `modifiedDatetime`: LocalDateTime(문자열 포맷)
    - `publishedTimestamp`: epoch millis(Long)

---

## 6) order.messaging.event — 이벤트 메시지 계약

### 6.1 OrderLocalMessage (Contract)

**목적**
- Producer/Consumer 공용 로컬 메시지 스키마
- 후방 호환을 위해 `operation` 을 **String 유지**

**구성**
- `id` (필수, 양수)
- `operation` (필수, String; 예: "CREATE"/"UPDATE"/"DELETE")
- `orderType` (필수, `MessageOrderType`)
- `publishedTimestamp` (필수, epoch millis)

**특징**
- Jackson 역직렬화를 안정화하기 위해 `@JsonCreator` + `@JsonProperty` 기반 생성자 명시
- `validation()` 제공:
    - 필수값/양수 여부
    - `operation` 값이 `Operation` enum으로 변환 가능한지 검증(공백 제거 + 대문자 normalize)

> 계약 계층에서의 validation은 “정책”이 아니라 “방어적 유효성” 수준으로 제한하는 것을 권장합니다.

### 6.2 OrderApiMessage (라우팅 계약)

**목적**
- Local 메시지를 API 경유/전달할 때 사용하는 라우팅 메시지

**특징**
- `fromLocal(OrderLocalMessage local)` 제공:
    - `local.operation`(String) → `Operation` 안전 변환(공백 제거, 대문자 변환)
    - 실패 시 허용 값 목록 포함한 `IllegalArgumentException`

### 6.3 OrderCrudMessage (CRUD 계약)

**목적**
- CRUD 처리 consumer로 전달하는 표준 이벤트 형태

**구성**
- `operation` (`Operation`)
- `orderType` (`MessageOrderType.ORDER_CRUD`)
- `payload` (`OrderPayload`)

**특징**
- `of(Operation methodType, OrderPayload payload)` 팩토리로 타입 고정

### 6.4 OrderCloseMessage (Remote Close 계약)

**목적**
- 외부/원격 시스템에 주문 종료/닫기 이벤트 전달

**특징**
- `of(Long orderId, Operation operation)` 제공
- `orderType` 은 `MessageOrderType.ORDER_REMOTE` 로 고정

---

## 7) order.messaging.dlq — DLQ(Dead Letter) 계약

**목적**
- 처리 실패 메시지를 표준 Envelope로 전달
- 런타임 정책(재시도 횟수, 실패 카운트 등)은 계약에서 제외

**구성**
- `DeadLetter<T>`
    - `type` : 서비스 간 합의된 카테고리 문자열
    - `error` : `ErrorDetail`
    - `payload` : 원본 메시지(또는 요약본)

**특징**
- `of(Enum<?> type, ErrorDetail error, T payload)` → `type.name()` 으로 문자열화
- `of(String type, ErrorDetail error, T payload)` → 문자열 직접 지정

---

## 8) shared.error — ErrorDetail(계약)

**목적**
- 내부 예외/프레임워크에 의존하지 않는 “와이어 레벨 에러” 표현

**구성**
- `ErrorDetail`
    - `code` : 표준 에러 코드(예: "ORDER-001")
    - `message` : 에러 메시지
    - `exception` : 예외 클래스명(문자열)
    - `occurredAtMs` : 발생 시각(epoch millis)
    - `meta` : 부가 컨텍스트(Map) — 선택
    - `stackTrace` : 스택트레이스 — 선택(필요 시 truncate 권장)

**원칙**
- 보안/크기 이슈 고려 → stackTrace/meta 는 선택적으로 사용

---

## 9) shared.op — Operation(계약)

**목적**
- 메시징 계약에서 사용하는 “동작” 표준 enum

**구성**
- `Operation`
    - `CREATE`, `UPDATE`, `DELETE`

**원칙**
- 메시징 계약 전용
- String 기반 operation(`OrderLocalMessage.operation`)을 수신 측에서 `Operation`으로 변환하는 구조를 지원

---

## 10) shared.monitoring — 모니터링(계약)

### 10.1 monitoring.ctx — MonitoringContext

**목적**
- 모니터링 메시지에 들어가는 고정 식별자(회사/시스템/도메인)

**구성**
- `MonitoringContext`
    - `COMPANY("PORTFOLIO")`
    - `SYSTEM("EXAMPLE")`
    - `DOMAIN("ORDER")`

**원칙**
- 계약 관점에서 중요한 것은 “텍스트 값(text())”

### 10.2 monitoring.type — MonitoringSeverity / MonitoringType

- `MonitoringType`
    - `NORMAL(1)`, `ERROR(2)`
- `MonitoringSeverity`
    - `LEVEL_1(1)` ~ `LEVEL_5(5)`
    - `ofLevel(int)` 제공(없으면 null)

### 10.3 monitoring.msg — MonitoringMessage

**목적**
- 외부 모니터링 시스템으로 전송되는 최종 와이어 스키마

**구성**
- `MonitoringMessage`
    - `type` : `MonitoringType.code()`
    - `level` : `MonitoringSeverity.level()`
    - `company/system/domain` : `MonitoringContext.*.text()`
    - `message` : 에러/상태 메시지(문자열)

**원칙**
- 내부 공통 예외/enum 의존 없이 “원시 필드”만 보유

---

## ✅ 계약 계층 사용 가이드(권장)

- HTTP 경계:
    - Request/Response 는 `order.http.*` 만 사용
    - 내부 모델/DTO로의 매핑은 API 계층에서 수행
- 메시지 경계:
    - 이벤트/페이로드는 `order.messaging.*` 만 사용
    - 내부 처리용 DTO/도메인으로의 매핑은 Worker/Batch/Application 계층에서 수행
- 오류/모니터링:
    - 외부로 내보내는 에러는 `ErrorDetail` 로 표준화
    - 모니터링은 `MonitoringMessage` 로 표준화

---

## 🔑 마지막 한 줄 요약

`order-contracts` 는 서비스 경계를 넘는 **“데이터 계약”만**을 정의하여,  
내부 구현 변경과 무관하게 **통신 스키마의 안정성과 일관성**을 보장하는 모듈입니다.
