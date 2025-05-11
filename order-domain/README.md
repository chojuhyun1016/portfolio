# 📦 order-domain

`order-domain` 모듈은 주문 시스템 전반에서 사용되는 **도메인 중심 모델**을 정의합니다.  
도메인의 비즈니스 규칙을 **DDD 원칙**에 따라 설계하며, 다른 모듈(`core`, `api`, `batch`, `worker`)에서 재사용되도록 경량화되고 독립적인 구조로 구성되어 있습니다.

모듈은 다음과 같이 두 개의 상위 도메인 디렉토리로 구성됩니다:

- `common`: 시스템 전역에서 재사용되는 공통 도메인 객체
- `order`: 주문 도메인의 핵심 모델 정의

---

## 🧠 도메인 구성 디렉토리 설명

### 📂 common

범 도메인에서 공통으로 사용하는 VO, 코드, 예외, 이벤트 등 불변적 요소로 구성되어 있으며, 다음과 같은 디렉토리로 구성됩니다:

- **repository**  
  공통 저장소 인터페이스 정의 (예: Marker Interface 또는 공통 로직)

- **entity**  
  전 도메인 공용으로 사용할 수 있는 JPA 엔티티 또는 베이스 엔티티 정의  
  예: `BaseTimeEntity`, `VersionEntity`

- **value**  
  재사용 가능한 값 객체(Value Object) 정의  
  예: `UserId`, `Money`, `Name`, `PhoneNumber` 등

- **code**  
  여러 도메인에서 사용하는 공통 Code 또는 Enum 정의  
  예: `RegionCode`, `ContinentCode`, `CurrencyCode`

- **type**  
  ID, 상태, 코드 등 명시적 타입 모델 정의  
  예: `BranchId`, `OrderNumber`, `TsidId` 등

- **model**  
  일반적인 구조의 공용 도메인 모델 정의  
  예: 마이크로서비스 간 공유되는 구조체 또는 도메인 객체

- **exception**  
  도메인 공통 예외 클래스 정의  
  예: `DomainException`, `InvalidCodeException`

- **event**  
  공용 도메인 이벤트 객체 정의  
  예: `DomainEvent`, `UserRegisteredEvent`

---

### 📂 order

주문 도메인의 핵심 도메인 객체, Enum, VO, Converter 등을 정의합니다.

- **entity**  
  주문 관련 JPA 엔티티 정의  
  예: `OrderEntity`, `OrderItemEntity`, `PaymentEntity`

- **vo**  
  주문 도메인 내부에서 사용하는 값 객체(Value Object) 정의  
  예: `OrderNumber`, `ReceiverInfo`, `ShippingAddress`

- **code**  
  주문에 특화된 코드 정의  
  예: `OrderStatus`, `PaymentMethod`, `DeliveryType`

- **type**  
  주문 관련 식별자 또는 명시적 타입 정의  
  예: `OrderId`, `OrderItemId`

- **model**  
  주문 도메인에서 사용하는 구조적 모델 정의  
  예: `OrderSummary`, `OrderDetail`

- **exception**  
  주문 도메인 내에서 발생하는 예외 정의  
  예: `OrderNotFoundException`, `InvalidOrderStateException`

- **event**  
  주문과 관련된 도메인 이벤트 정의  
  예: `OrderPlacedEvent`, `OrderCancelledEvent`

---

## ✅ 대표 기능 요약

- 공통 및 주문 도메인 중심의 VO, Entity, Enum, Event 정의
- ID, 코드, 식별자 등을 명시적으로 관리하는 타입 기반 모델
- JPA 기반 도메인 객체에 대한 Converter 및 Annotation 구분 구조
- 다수의 도메인에서 재사용 가능한 구조를 통해 높은 응집도와 낮은 결합도 유지
- DDD 설계 원칙에 따라 불변성, 책임 분리, 이벤트 중심 설계 적용

---

## 🔗 참고

- 본 모듈은 **비즈니스 도메인 순수 모델**만을 포함하며,  
  인프라나 유스케이스 로직은 `order-core`, `api`, `batch` 모듈로 분리됨
- 모든 모델은 불변성을 유지하며, 생성자를 통한 명시적 초기화를 지향함
- 공통 구조는 공통 레이어에 위치하여, 도메인 간 중복을 최소화하고 재사용성을 높임
