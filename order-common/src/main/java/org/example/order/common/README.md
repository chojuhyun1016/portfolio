# 📁 order-common 모듈 디렉토리 구조 설명

`order-common` 모듈은 전역적으로 사용되는 공통 기능, 유틸리티, 상수, 예외, JSON 직렬화/역직렬화,  
그리고 **로깅/트레이싱(Web + MDC)** 과 같은 교차 관심사(AOP) 기능을 제공합니다.  
이 모듈은 다른 비즈니스 도메인이나 외부 인프라에 의존하지 않고, 모든 서브 모듈에서 안전하게 사용할 수 있도록 설계되었습니다.

---

## 📂 core

- **목적**: 공통 코드, Enum, 상수, 예외, 인증 정보 등 핵심 개념 정의
- **내용 예시**:
  - `CodeEnum`, `ExceptionCodeEnum`, `CommonException`
  - `AccessUserInfo`, `AccessUserManager` (인증 컨텍스트)
  - 날짜 포맷, HTTP 상수, 기본 메시지 구조
- **책임**:
  - 모든 서비스에서 공통으로 사용하는 비즈니스 독립적인 핵심 코드
  - Enum 기반 코드 관리, 전역 예외 코드 정의 등

---

## 📂 config

- **목적**: 외부 설정을 객체로 바인딩하기 위한 구조
- **내용 예시**:
  - `@ConfigurationProperties`를 사용하는 설정 클래스
  - 예: S3, KMS, Redis 등 설정 값에 대한 타입 안전한 매핑
- **책임**:
  - `application.yml` 또는 환경변수 기반 설정을 코드로 전달

---

## 📂 support

- **목적**: 외부 라이브러리 기반 공통 유틸 또는 기술 지원 코드
- **내용 예시**:
  - JSON: `ObjectMapperFactory`, `ObjectMapperUtils`, `CodeEnumJsonConverter`
  - JPA: `BooleanToYNConverter`
  - Logging: `@Correlate` 애노테이션, `CorrelationAspect`
- **책임**:
  - Jackson 기반 JSON 처리 및 Enum 변환 지원
  - JPA AttributeConverter 등 ORM 유틸
  - AOP 기반 MDC(traceId, domain key) 전파

---

## 📂 autoconfigure

- **목적**: Spring Boot 오토컨피그 클래스 제공
- **내용 예시**:
  - `LoggingSupportAutoConfiguration`
  - `WebCommonAutoConfiguration`
- **책임**:
  - `TaskDecorator`/`CorrelationAspect` 자동 등록
  - `CorrelationIdFilter` 자동 등록
- **특징**:
  - `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`를 통해 자동 인식
  - 애플리케이션에서 `@ComponentScan`/직접 등록 필요 없음

---

## 📂 web

- **목적**: 웹 계층 공통 응답 구조, 필터, 메시지 처리
- **내용 예시**:
  - `ApiResponse<T>`, `ResponseMeta`
  - `AccessUserArgumentResolver`
  - `CorrelationIdFilter`
- **책임**:
  - API 응답 표준화
  - 게이트웨이 헤더 기반 사용자 컨텍스트 주입
  - 요청 단위 상관관계 ID 브리지(MDC["requestId"], MDC["traceId"])

---

## 📂 helper

- **목적**: 경량 범용 유틸리티 제공
- **내용 예시**:
  - 날짜 유틸 (`DateTimeUtils`)
  - 해싱 유틸 (`SecureHashUtils`)
  - Base64, GZIP, JSON 변환
- **책임**:
  - 반복 로직 최소화
  - 외부 라이브러리 직접 호출 대신 안전 래핑

---

## ✅ 정리

| 디렉토리 | 설명 |
|----------|------|
| `core`   | Enum, 공통 예외, 상수, 인증 컨텍스트 등 핵심 타입 |
| `config` | 외부 설정 바인딩(`@ConfigurationProperties`) |
| `support`| JSON/JPA/Logging 유틸, AOP 애노테이션/Aspect |
| `autoconfigure` | Boot 자동 구성 (필터, Aspect, TaskDecorator) |
| `web`    | API 응답 표준, 필터, ArgumentResolver 등 |
| `helper` | 날짜/암호화/Base64/GZIP 등 범용 유틸 |

---

## 🔑 핵심 포인트

- `support`는 **순수 기능 코드** (유틸, 애노테이션, Aspect)
- `autoconfigure`는 **자동 설정 코드** (빈 등록, 필터/AOP 연결)
- `web`은 **표준 웹 응답/필터 구조**
- 모두 **비즈니스 무관** → 모든 서비스 모듈(api, batch, worker 등)에서 안전하게 재사용 가능
- 