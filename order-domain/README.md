# 📦 order-domain

`order-domain` 모듈은 주문 시스템의 **순수 도메인 레이어**입니다.  
인프라(DB/Kafka/Web/S3)나 유스케이스(Application Service)와 분리된 **도메인 중심 모델(DDD)** 을 제공하며,  
상위 모듈(`order-core`, `api`, `batch`, `worker`)이 이 도메인을 조립·사용합니다.

---

## 🎯 목적 (Purpose)

- 주문 도메인의 **핵심 모델**을 단일 기준으로 제공
- 기술 구현(저장소/클라이언트)과 분리하여 **재사용성/독립성** 확보
- 도메인 규칙(VO/Enum/제약)을 통해 **일관성** 유지

---

## 🧩 포함/비포함 (Boundary)

- 포함
    - Domain Model / Entity / Value Object / Enum(Type)
    - Repository Port(인터페이스)
    - 공통 감사/버전 베이스 엔티티

- 비포함
    - DB 접근 구현(JPA/QueryDSL/JDBC/Dynamo Enhanced Client 구현체)
    - Kafka/Web/S3 등 인프라 모듈
    - Application Service(UseCase), Facade, Controller, Contract DTO

---

## 🗂️ 구조 개요 (High-level Structure)

    org.example.order.domain
    ├─ common/
    │  ├─ entity/           VersionEntity (감사 + @Version)
    │  └─ id/               IdGenerator (식별자 생성 Port)
    └─ order/
       ├─ entity/           LocalOrderEntity, OrderEntity, OrderDynamoEntity
       ├─ model/            OrderView, OrderUpdate, Options(Batch/Dynamo)
       ├─ repository/       *Repository / *QueryRepository / *CommandRepository (Ports)
       ├─ type/             OrderStatus
       └─ value/            OrderNumber, UserId

---

## 🔑 핵심 요약

- 도메인은 **구현을 모른다**: 저장/조회/암호화/전송 구현은 외부로 위임한다.
- Repository 는 **Port(인터페이스)** 로만 정의하고, 구현은 `order-core.infra` 등에서 제공한다.
- 시간/표현 차이(RDB datetime vs Dynamo epoch millis, Boolean vs Y/N)는 **Infra/Adapter에서 변환**한다.

---
