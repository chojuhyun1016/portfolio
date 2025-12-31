# 📦 order-contracts

`order-contracts` 모듈은 서비스 경계를 넘는 **데이터 계약(Contract / Wire Schema)** 만을 정의하는 전용 레이어입니다.  
HTTP API / Kafka 메시지 / DLQ / 모니터링 등 **외부로 노출되거나 서비스 간 전달되는 스키마**를 단일 기준(SSOT)으로 관리합니다.

---

## 🎯 목적 (Purpose)

- 서비스 간 “무엇을 주고받는지”를 **단일 진실(Single Source of Truth)** 로 고정
- 내부 구현 변경과 무관하게 **통신 스키마 안정성** 확보
- 후방 호환성 우선(필드 추가는 상대적으로 안전, 의미 변경/삭제는 엄격 제한)

---

## 🧩 포함/비포함 (Boundary)

- 포함
    - HTTP Request/Response DTO
    - Event/Payload DTO
    - DLQ Envelope / ErrorDetail
    - Monitoring Wire Schema
    - 공통 Operation/Type(라우팅/분류용)

- 비포함
    - 비즈니스 로직(처리/규칙)
    - 서비스/레포지토리/유스케이스
    - 프레임워크 의존(Spring/JPA/Kafka Client 등)
    - 내부 엔티티/도메인 모델

---

## 🗂️ 구조 개요 (High-level Structure)

~~~
org.example.order.contract
 ├─ order
 │   ├─ http
 │   │   ├─ request          (외부 요청 스키마)
 │   │   ├─ response         (외부 응답 스키마)
 │   │   └─ type             (HTTP 계약 전용 타입)
 │   └─ messaging
 │       ├─ event            (서비스 간 이벤트 스키마)
 │       ├─ payload          (이벤트 본문 스키마)
 │       ├─ dlq              (DeadLetter Envelope)
 │       └─ type             (메시지 분류/라우팅 타입)
 └─ shared
     ├─ op                   (Operation 등 공통 동작 타입)
     ├─ error                (ErrorDetail 등 와이어 에러 표현)
     └─ monitoring
         ├─ ctx              (회사/시스템/도메인 고정 텍스트)
         ├─ msg              (MonitoringMessage)
         └─ type             (MonitoringType/Severity)
~~~

---

## 🔑 핵심 규칙 (Rules)

- 계약은 **원시/문자열 중심**으로 유지(내부 타입/프레임워크 의존 최소화)
- 내부 DTO/도메인과 **완전 분리**
- 계약 계층의 검증은 “정책”이 아니라 **방어적 유효성 수준**으로 제한(필수값/범위 등)

---

## ✅ 마지막 한 줄 요약

**“`order-contracts`는 외부/서비스 간에 오가는 스키마만 고정하여, 내부 구현 변화로부터 통신 계약을 보호하는 모듈이다.”**
