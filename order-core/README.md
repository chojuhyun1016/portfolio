# 📦 order-core

order-core 모듈은 주문 도메인의 **핵심 비즈니스 로직과 애플리케이션 규칙**을 담는 중심 레이어입니다.  
DDD와 Clean Architecture 원칙을 기반으로 **Application / Domain Port / Infra Adapter** 를 명확히 분리하여  
기술 변경과 운영 환경에 독립적인 구조를 제공합니다.

---

## 🎯 목적 (Purpose)

- 주문 도메인의 핵심 규칙과 유스케이스 보호
- 인프라(JPA, DynamoDB, Redis, Kafka, Crypto 등)로부터 비즈니스 로직 분리
- 설정 기반 조립을 통한 환경 독립성 및 Fail-fast 설계
- 멀티 스토리지, 이벤트 기반 아키텍처를 안정적으로 지원

---

## 🧩 아키텍처 개념 (Concept)

의존 방향 요약

application → domain ports ← infra adapters → external systems

- Application 은 오직 Port(인터페이스)에만 의존
- Infra 는 Port 를 구현하는 Adapter 역할
- Domain 개념은 외부 스키마/기술에 노출되지 않음

---

## 🗂️ 패키지 구조 개요 (High-level Structure)

- application  
  유스케이스 계층  
  Command / Query / Sync / View DTO  
  Application Service, Cache 조립, MapStruct Mapper

- infra  
  기술 구현 계층 (Adapters + Config)  
  JPA, JDBC, DynamoDB, Redis, Lock, Crypto, Secrets, TSID  
  모든 인프라는 조건부(@Conditional…)로 조립

- support  
  애플리케이션/인프라 공통 기술 지원  
  시간 처리(TimeMapper, TimeProvider), 매핑 설정(AppMappingConfig)

---

## ⚙️ 설계 원칙 (Principles)

- 도메인 보호  
  Domain 은 Port 만 소유하고 구현은 Infra 에 위임

- 경계 보호  
  외부 시스템 스키마는 직접 노출 금지 (필요 시 Adapter/ACL에서 변환)

- 애그리거트 우선  
  저장소 구현은 aggregate 단위로 구성

- 설정 기반 조립  
  컴포넌트 스캔 최소화, @Bean + @Conditional 조합

- Fail-fast  
  필수 설정 누락 시 가능한 빠르게 실패

---

## 🔑 핵심 요약

- order-core 는 시스템의 **비즈니스 중심축**이다
- 기술은 교체 가능하지만 도메인 규칙은 보호된다
- 멀티 스토리지와 이벤트 드리븐 환경을 전제로 설계되었다
- 운영 환경(Local, Dev, Prod)에 따라 **동일 코드, 다른 조립**을 지
