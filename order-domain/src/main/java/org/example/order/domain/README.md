# 📦 order-domain

`order-domain` 모듈은 주문 시스템 전반에서 재사용되는 **도메인 중심 모델**(DDD)을 정의합니다.  
인프라/유스케이스/전송(HTTP/Kafka)과 분리된 **순수 도메인 레이어**를 목표로 하며, `order-core`, `api`, `batch`, `worker` 등 상위 모듈이 이 도메인을 조립·사용합니다.

- **경계**: Domain Model / Entity / Value Object / Enum(Type) / Repository Port(인터페이스)까지만 포함
- **비포함**: DB 접근 구현(JPA/QueryDSL/JDBC/Dynamo Enhanced Client 구현), Kafka/Web/S3 인프라, 애플리케이션 서비스(UseCase)

---

## ✅ 현재 코드 기준 “도메인 핵심 포인트”

### 1) 공통 감사/버전 엔티티: `VersionEntity` (JPA `@MappedSuperclass`)
- 생성/수정자 + 생성/수정일시 + 낙관적 락(`@Version`)을 제공하는 공용 베이스 엔티티
- `@PrePersist` / `@PreUpdate`에서 `AccessUserContext.getAccessUser()` 기반으로 감사 필드 자동 채움
- **주의 포인트(현재 코드 그대로)**:
  - `@PrePersist`에서 `createdUserId / createdUserType / createdDatetime / modifiedDatetime`만 세팅
  - `@PrePersist`에서 `modifiedUserId / modifiedUserType`은 세팅하지 않음(초기 수정자 미설정)
  - 컬럼 `columnDefinition`에 `bigint` + COMMENT가 혼용되어 있으며,
    `createdUserType / modifiedUserType`은 `String`인데 `bigint`로 기재되어 있음(스키마/DDL 정책에 맞게 점검 필요)

### 2) 도메인 포트: `IdGenerator`
- 도메인 계층에서 “식별자 생성”에 대한 의존을 인터페이스로만 보유
- 구현(TSID 등)은 infra에서 제공

### 3) 주문 JPA 엔티티: `LocalOrderEntity`, `OrderEntity`
- 테이블: `local_order`, `order` (백틱 테이블명)
- 주요 필드:
  - `id`(PK), `order_id`(unique), `order_number`, `user_id`, `user_number`, `order_price`
  - `delete_yn`은 `Boolean`이지만 DB는 `varchar(1) not null` 정의 (Y/N 대신 Boolean을 쓰는 형태)
  - `published_datetime` 존재
  - 감사 필드(`created_*`, `modified_*`)와 `version` 직접 보유
- `@PrePersist`에서 방어 로직:
  - `version == null`이면 `0L`
  - `deleteYn == null`이면 `FALSE`
- `updateAll(...)`로 “전체 갱신”을 수행(여러 레이어에서 맵핑/동기화 시 활용)

### 4) 주문 DynamoDB 엔티티: `OrderDynamoEntity` (AWS SDK v2 Enhanced Client)
- `@DynamoDbBean`
- PK/SortKey:
  - PartitionKey: `id` (String)
  - SortKey: `orderNumber` (String)
- 주요 필드:
  - `orderPriceEnc`(암호화된 금액), `deleteYn`(Y/N String), `publishedTimestamp`(epoch millis)
  - 감사 필드 포함

### 5) 옵션/모델 레이어
- `OrderBatchOptions`: JDBC 배치 튜닝을 위한 옵션(청크 크기/SQL 힌트)
- `OrderDynamoQueryOptions`: Dynamo 조회 가드(결과 limit, consistentRead, scan fallback 허용 등)
- `OrderUpdate`: 도메인 전용 커맨드(레코드)
- `OrderView`: 도메인 전용 조회 결과(레코드)

### 6) 리포지토리 포트(인터페이스)
- LocalOrder:
  - `LocalOrderRepository` (기본 CRUD 성격)
  - `LocalOrderQueryRepository` (projection 조회/단건 update)
  - `LocalOrderCommandRepository` (bulk insert/update)
- Order:
  - `OrderRepository`
  - `OrderQueryRepository`
  - `OrderCommandRepository`
- Dynamo:
  - `OrderDynamoRepository` (save/find/query/delete + 옵션 오버로드 default 제공)

### 7) Enum / VO
- `OrderStatus`: 코드/설명 + `fromCode(String)` (잘못된 코드면 `IllegalArgumentException`)
- `OrderNumber`: nonblank 보장 + `masked()` 제공(마지막 4자리 제외 마스킹)
- `UserId`: 0 이상 보장 + `isSystemUser()`(0L)

---

## 🗂️ 디렉토리 구조 (현행화된 기준)

현재 제공된 코드 스냅샷 기준으로 `order-domain`은 크게 다음처럼 이해하면 됩니다.

- `common`
  - `entity`
    - `VersionEntity` (공용 감사 + 낙관적 락 베이스)
  - `id`
    - `IdGenerator` (식별자 생성 포트)
- `order`
  - `entity`
    - `LocalOrderEntity` (JPA)
    - `OrderEntity` (JPA)
    - `OrderDynamoEntity` (DynamoDB Enhanced Client)
  - `model`
    - `OrderBatchOptions`
    - `OrderDynamoQueryOptions`
    - `OrderUpdate`
    - `OrderView`
  - `repository`
    - `LocalOrderCommandRepository`
    - `LocalOrderQueryRepository`
    - `LocalOrderRepository`
    - `OrderCommandRepository`
    - `OrderQueryRepository`
    - `OrderRepository`
    - `OrderDynamoRepository`
  - `type`
    - `OrderStatus`
  - `value`
    - `OrderNumber`
    - `UserId`

---

# 📁 order-domain 디렉토리 구조 설명

`order-domain`은 주문(Order) 도메인과 관련된 핵심 비즈니스 모델을 담고 있는 모듈입니다.  
DDD 관점에서 도메인 모델, 엔티티, 값 객체(VO), 타입(Enum), 그리고 리포지토리 포트(인터페이스)가 위치합니다.

---

## 📂 common

- **목적**: 여러 도메인 간 공유되는 도메인 수준의 공통 구성 요소 제공
- **현재 코드 기준 구성**
  - `common/entity`
    - `VersionEntity`
      - JPA `@MappedSuperclass`
      - 감사 필드(생성/수정자, 생성/수정일시)
      - 낙관적 락 버전(`@Version`)
      - `AccessUserContext` 기반 자동 주입(`@PrePersist`, `@PreUpdate`)
  - `common/id`
    - `IdGenerator`
      - 도메인 계층의 식별자 생성 포트(구현은 infra에서 제공)

- **책임**
  - 도메인 간 재사용 가능한 기반 추상화 제공
  - 감사/버전/식별자 등 공통 관심사를 도메인 레벨에서 표준화

---

## 📂 order

- **목적**: 주문(Order) 도메인의 핵심 모델/포트 정의
- **현재 코드 기준 구성**
  - `order/entity`
    - `LocalOrderEntity` (JPA)
    - `OrderEntity` (JPA)
    - `OrderDynamoEntity` (DynamoDB Enhanced Client)
  - `order/model`
    - `OrderBatchOptions` (JDBC 배치 옵션)
    - `OrderDynamoQueryOptions` (Dynamo 조회 옵션)
    - `OrderUpdate` (도메인 커맨드 레코드)
    - `OrderView` (도메인 조회 레코드)
  - `order/repository`
    - (JPA 성격) Repository/QueryRepository/CommandRepository 포트
    - (Dynamo 성격) `OrderDynamoRepository` 포트
  - `order/type`
    - `OrderStatus` (코드/설명 + fromCode)
  - `order/value`
    - `OrderNumber` (VO)
    - `UserId` (VO)

- **책임**
  - 주문 관련 핵심 데이터 구조와 제약(VO/Enum)을 도메인 레벨에서 캡슐화
  - 저장소 접근은 “포트”로만 정의하고 구현은 외부로 위임

---

## ✅ 디렉토리 요약

| 디렉토리 | 책임 요약 |
|----------|------------|
| common   | 도메인 간 공유되는 공통 엔티티/추상화(감사/버전/식별자 포트) |
| order    | 주문 도메인 전용 모델(엔티티/VO/Enum) + 저장소 포트 정의 |

---

# 📁 order-domain/common 디렉토리 구조 설명

`common` 디렉토리는 `order-domain` 내부에서 여러 하위 도메인에서 재사용될 수 있는 공통 컴포넌트들을 포함합니다.  
엔티티, 값 객체, 코드, 예외, 이벤트 등 DDD 기반의 핵심 구성 요소들이 중심이 됩니다.

> 아래 구조는 “권장 분류”도 포함합니다.  
> (현재 코드 스냅샷에는 일부만 존재하며, 존재하는 항목은 ✅로 표시)

---

## 📂 repository (권장)
- **목적**: 공통 도메인에서 사용할 수 있는 리포지토리 인터페이스 계층(예: Marker, 공통 읽기/쓰기 규약)
- **현재 코드**: (스냅샷 범위에는 미포함)

---

## 📂 entity ✅
- **목적**: 공통적으로 사용되는 JPA 기반 엔티티/베이스 엔티티 정의
- **현재 코드**
  - `VersionEntity`
    - 감사 필드 + 낙관적 락 버전 제공
    - `@PrePersist` / `@PreUpdate`로 `AccessUserContext`에서 사용자 정보를 읽어 자동 주입

---

## 📂 value (권장)
- **목적**: 범 도메인에서 재사용 가능한 불변 객체(Value Object)
- **현재 코드**: (스냅샷 범위에는 미포함)

---

## 📂 code (권장)
- **목적**: 도메인 전반에서 공통적으로 쓰이는 Enum 기반 코드 정의(지역/통화/시간대 등)
- **현재 코드**: (스냅샷 범위에는 미포함)

---

## 📂 type (권장)
- **목적**: 코드/식별자 등의 명시적 타입 모델(예: CodeEnum, 타입 그룹 분류)
- **현재 코드**: (스냅샷 범위에는 미포함)

---

## 📂 model (권장)
- **목적**: 여러 도메인에서 공유 가능한 단순 모델(구조체/설정 모델 등)
- **현재 코드**: (스냅샷 범위에는 미포함)

---

## 📂 exception (권장)
- **목적**: 도메인 공통 예외 정의(코드 기반, 정책 기반)
- **현재 코드**: (스냅샷 범위에는 미포함)

---

## 📂 event (권장)
- **목적**: 도메인 이벤트 정의 및 공유(도메인 이벤트 기반 아키텍처)
- **현재 코드**: (스냅샷 범위에는 미포함)

---

## ✅ common 디렉토리 요약

| 디렉토리    | 설명 |
|-------------|------|
| repository  | 공통 리포지토리 인터페이스(권장) |
| entity      | 공통 엔티티 정의(✅: VersionEntity) |
| value       | 공통 VO 정의(권장) |
| code        | Enum 기반 공통 코드(권장) |
| type        | 타입/코드 추상화(권장) |
| model       | 공통 전달 객체/구조체(권장) |
| exception   | 공통 예외(권장) |
| event       | 도메인 이벤트(권장) |

---

## 🧩 현재 코드 상세 (스냅샷 기반)

### 1) `VersionEntity` 상세
- 필드
  - createdUserId (Long)
  - createdUserType (String)
  - createdDatetime (LocalDateTime)
  - modifiedUserId (Long)
  - modifiedUserType (String)
  - modifiedDatetime (LocalDateTime)
  - version (Long, `@Version`)
- 라이프사이클
  - `@PrePersist`
    - createdUserId/userType 세팅
    - createdDatetime 세팅
    - modifiedDatetime만 세팅(초기 modifiedUserId/Type은 미설정)
  - `@PreUpdate`
    - modifiedUserId/userType/datetime 세팅

---

# 🧾 핵심 인터페이스/모델 요약 (현재 코드 기준)

## 1) Repository Ports (Domain Interfaces)

### LocalOrder (RDB/JPA/JDBC 혼합 구현을 위한 포트 분리)
- `LocalOrderRepository`
  - findById(id)
  - save(entity)
  - deleteByOrderIdIn(orderIds)
- `LocalOrderQueryRepository`
  - fetchByOrderId(orderId) -> Optional<OrderView>
  - updateByOrderId(...) -> int
- `LocalOrderCommandRepository`
  - bulkInsert(entities)
  - bulkUpdate(syncList)
  - options 오버로드 default 제공(`OrderBatchOptions`)

### Order (RDB/JPA/JDBC 혼합 구현을 위한 포트 분리)
- `OrderRepository`
  - findById(id)
  - save(entity)
  - deleteByOrderIdIn(orderId list)
- `OrderQueryRepository`
  - fetchByOrderId(orderId) -> Optional<OrderView>
  - updateByOrderId(...) -> int
- `OrderCommandRepository`
  - bulkInsert(entities)
  - bulkUpdate(syncList)
  - options 오버로드 default 제공(`OrderBatchOptions`)

### Dynamo
- `OrderDynamoRepository`
  - save(entity)
  - findById(id)
  - findAll()
  - findByUserId(userId)
  - deleteById(id)
  - deleteByIdAndOrderNumber(id, orderNumber)
  - deleteAllByPartition(id)
  - 옵션 오버로드 default 제공(`OrderDynamoQueryOptions`)

---

## 2) Entity Models

### `LocalOrderEntity` / `OrderEntity` (JPA)
- 공통 구조
  - `id` (PK)
  - `orderId` (unique)
  - `orderNumber`
  - `userId`, `userNumber`
  - `orderPrice`
  - `deleteYn` (Boolean, DB는 varchar(1) not null)
  - `publishedDatetime`
  - `created*`, `modified*`
  - `version` (`@Version`)
- 방어 로직
  - `@PrePersist`: version null -> 0L, deleteYn null -> FALSE
- 갱신
  - `updateAll(...)`: 전체 필드 갱신용(동기화/배치에서 유용)

### `OrderDynamoEntity` (DynamoDB Enhanced Client)
- Key
  - PK: `id` (String)
  - SK: `orderNumber` (String)
- 주요 필드
  - orderPriceEnc (암호화)
  - deleteYn (Y/N String)
  - publishedTimestamp (epoch millis)
  - created/modified 감사 필드

---

## 3) Domain Models

### `OrderBatchOptions`
- 목적: JDBC Bulk 작업 시 구현체가 참고할 튜닝 힌트
- 필드
  - batchChunkSize(Integer)
  - sqlHint(String)

### `OrderDynamoQueryOptions`
- 목적: Dynamo 조회 정책을 표준화(운영 가드)
- 필드
  - limit(Integer)
  - consistentRead(Boolean)
  - allowScanFallback(Boolean)
  - startKey(String)

### `OrderUpdate` (record)
- 목적: 도메인 전용 동기화/업데이트 커맨드 모델
- 필드: user/order/price/deleteYn + created/modified/publishedDatetime 등

### `OrderView` (record)
- 목적: 도메인 전용 조회 결과 모델(외부 레이어 DTO와 분리)
- 필드: orderId, orderNumber, userId, userNumber, orderPrice

---

## 4) Types / Values

### `OrderStatus`
- 코드/설명:
  - CREATED("C"), PAID("P"), SHIPPED("S"), COMPLETED("D"), CANCELLED("X")
- `fromCode(code)` 제공(유효하지 않으면 IllegalArgumentException)

### `OrderNumber`
- 불변 VO
- 생성 시 nonblank 보장
- `masked()` 제공(마지막 4자리 제외 마스킹)

### `UserId`
- 불변 VO
- 생성 시 0 이상 보장
- `isSystemUser()` (0L)

---

# 🧪 (권장) 도메인 레이어 테스트 가이드

> 도메인 레이어는 “인프라 없는 순수 테스트”가 가능해야 합니다.  
> 현재 코드 대부분은 단순 모델이므로 다음이 핵심입니다.

- VO/Enum 검증
  - OrderNumber: blank/null이면 예외
  - UserId: 음수/null이면 예외
  - OrderStatus.fromCode: 잘못된 코드는 예외
- Entity 방어 로직
  - LocalOrderEntity/OrderEntity: prePersist 시 version/deleteYn이 null이어도 보정되는지
- Repository Port 계약
  - 도메인 테스트에서는 포트 인터페이스 자체를 Mock으로 두고 상위 모듈에서 계약 테스트를 수행

---

# ⚙️ 운영/설계 메모 (현재 코드 기반, 체크포인트)

- `deleteYn`의 표현
  - RDB(JPA): Boolean + columnDefinition varchar(1)
  - Dynamo: String "Y"/"N"
  - 구현체(Infra)에서 변환 정책을 반드시 표준화해야 함
- `publishedDatetime`(RDB) vs `publishedTimestamp`(Dynamo)
  - 시간 표현이 다르므로 변환 규칙(UTC/zone, millis 기준)을 통일해야 함
- 감사 필드 정책
  - `VersionEntity`는 공통 베이스로 유용하지만,
    현재 `createdUserType`의 columnDefinition(bigint) 등 스키마 정책과 충돌 여지가 있음
- Domain은 “구현을 모른다”
  - JPA/QueryDSL/JDBC/Dynamo 구현은 반드시 외부 모듈(Infra/Client)로 분리 유지

---

## ✅ 마지막 한 줄 요약
**“`order-domain`은 주문 시스템의 핵심 모델(엔티티/VO/Enum/포트)을 최소 단위로 제공하고, 구현은 외부로 밀어내어 재사용성과 독립성을 확보한다.”**
