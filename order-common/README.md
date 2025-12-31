# 📦 order-common

order-common 모듈은 모든 서비스(api, web, worker, batch 등)에서 공통으로 사용되는  
**비즈니스 비의존 공통 레이어(Common Layer)** 입니다.

도메인 로직이나 인프라 구현을 포함하지 않으며,  
전사 서비스가 동일한 규칙과 동작을 갖도록 **표준·규약·유틸리티**를 제공합니다.

---

## 🎯 목적 (Purpose)

- 전역 공통 규칙의 **단일 기준(Single Source of Truth)** 제공
- 로깅, 트레이싱, JSON, 예외, 응답 포맷 등 **교차 관심사 분리**
- 각 서비스가 구현에 집중할 수 있도록 **반복 코드 제거**
- Spring Boot 환경에서 **자동 구성(AutoConfiguration)** 기반 사용

---

## 🧩 성격 (Characteristics)

- 비즈니스 도메인 무관
- 인프라 직접 의존 없음
- 모든 모듈에서 동일하게 동작
- Import/Scan 없이 **자동 적용 가능**
- 정책 제공, 구현 강제 없음

---

## 🗂️ 구성 개요 (High-level Structure)

- core  
  공통 Enum, 예외, 상수, 호출자 컨텍스트 등 핵심 개념 정의

- support  
  로깅, MDC, AOP, JSON, JPA 컨버터 등 기능성 코드

- autoconfigure  
  Spring Boot AutoConfiguration 진입점  
  필터, Aspect, TaskDecorator 자동 등록

- web  
  API 응답 표준과 요청 상관관계 ID 관리

- security  
  게이트웨이 전용 보안 필터 (내부 API 보호)

- helper  
  날짜, 인코딩, 해싱, 압축 등 범용 경량 유틸

- messaging  
  Kafka 메시지/헤더 공통 유틸리티

---

## ⚙️ 사용 방식 (How to Use)

- 애플리케이션에서 별도 설정 없이 의존성 추가만으로 사용
- Filter, Aspect, MDC 전파, ObjectMapper 등은 자동 적용
- 필요 시 일부 빈은 @ConditionalOnMissingBean 으로 교체 가능

---

## 🔑 핵심 요약

- order-common은 기능이 아니라 **규칙과 표준을 제공**한다
- 모든 서비스는 이 모듈을 통해 **같은 방식으로 로그, 예외, 응답을 처리**한다
- 비즈니스와 인프라를 깨끗하게 분리하기 위한 **기반 레이어**이다
