# Domain Layer 구조 안내

이 디렉토리는 도메인의 핵심 비즈니스 모델을 정의하는 레이어입니다.  
Entity, Value Object, Enum, Converter 등을 통해 도메인 규칙과 불변성을 표현합니다.  
MSA + 클린 코드 아키텍처 기준에 따라 도메인별 분리 및 공통 요소 분리 방식을 따릅니다.

---

## 공통 디렉토리 (common)

- converter  
  여러 도메인에서 재사용 가능한 공통 JPA AttributeConverter 정의

- entity  
  모든 도메인에서 상속 가능한 공통 엔티티 속성 정의
  예: VersionEntity (등록자/수정자/버전 필드 포함)

- enums  
  공통적으로 사용되는 Enum 정의

- vo  
  모든 도메인에서 재사용 가능한 Value Object 정의

---

## 주문 도메인 디렉토리 (order)

- converter  
  Order 도메인 전용 JPA AttributeConverter 정의
  예: OrderStatusConverter

- entity  
  Order 도메인의 핵심 Entity 정의
  예: OrderEntity

- enums  
  Order 도메인 전용 Enum 정의
  예: OrderStatus

- vo  
  Order 도메인 전용 Value Object 정의
  예: OrderNumber, UserId

---

## 작성 가이드

- 도메인별 디렉토리는 `order`, `member`, `product` 등 기능 중심으로 명명합니다.
- 공통으로 사용되는 코드는 `common` 디렉토리에 위치시킵니다.
- VO, Enum, Converter 등은 의미 단위로 명확히 분리해 관리합니다.
- 각 도메인의 Entity는 해당 도메인 내에서만 직접 접근되도록 설계합니다.

---

이 구조는 도메인 확장, 변경에 유연하며 유지보수에 효과적입니다.  
도메인 간 관심사 분리와 재사용성 확보를 위한 기준으로 활용하세요.
