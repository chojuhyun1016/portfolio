# 📁 order-common 모듈 디렉토리 구조 설명

`order-common` 모듈은 전역적으로 사용되는 **공통 기능, 유틸리티, 상수, 예외, JSON 직렬화/역직렬화, 로깅/트레이싱(Web + MDC)** 과 같은  
교차 관심사(AOP) 기능을 제공합니다.  
비즈니스 도메인이나 외부 인프라에 의존하지 않고, 모든 서브 모듈(api, batch, worker 등)에서 안전하게 사용할 수 있도록 설계되었습니다.

---

## 📂 core

- **목적**: 공통 코드, Enum, 상수, 예외, 인증 정보 등 핵심 개념 정의
- **예시**:
  - `CodeEnum`, `CommonException`, `CommonExceptionCode`
  - `AccessUserInfo` (인증 컨텍스트)
  - 날짜/시간 포맷, HTTP 상수
- **책임**:
  - 모든 서비스에서 공통적으로 사용하는 비즈니스 독립적인 핵심 코드 제공
  - Enum 기반 코드 관리, 전역 예외 코드 정의

---

## 📂 config

- **목적**: 공통 설정/레거시 호환을 위한 최소한의 빈 정의
- **예시**:
  - `OrderCommonConfig` (no-op, @Deprecated)  
    → 과거 구조 호환용. 실제 빈 등록은 `autoconfigure` 경유.
- **책임**:
  - 불필요한 `@ComponentScan`/`@EnableAutoConfiguration`을 제거
  - 새 코드에서는 **직접 사용하지 않고 오토컨피그만 사용**하도록 유도

---

## 📂 support

- **목적**: 외부 라이브러리/Jackson/JPA/로깅 기반 공통 유틸 코드
- **예시**:
  - JSON: `ObjectMapperFactory`, `ObjectMapperUtils`, `CodeEnumJsonConverter`
  - JPA: `BooleanToYNConverter`
  - Logging: `@Correlate` 애노테이션, `CorrelationAspect`
- **책임**:
  - Jackson 기반 직렬화/역직렬화 지원
  - Enum ↔ DTO 변환 처리
  - JPA Boolean ↔ "Y/N" 매핑
  - AOP 기반 MDC(traceId, domain key) 전파

---

## 📂 autoconfigure

- **목적**: Spring Boot AutoConfiguration 클래스
- **예시**:
  - `LoggingAutoConfiguration`  
    → `TaskDecorator` / `CorrelationAspect` 자동 등록
  - `WebAutoConfiguration`  
    → `CorrelationIdFilter` 자동 등록
- **특징**:
  - `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 경유 자동 인식
  - 애플리케이션은 `@Import`/`@ComponentScan` 필요 없음

---

## 📂 web

- **목적**: 웹 계층 공통 응답 및 요청 처리
- **예시**:
  - `ApiResponse<T>`, `ResponseMeta`
  - `AccessUserArgumentResolver`
  - `CorrelationIdFilter`
- **책임**:
  - API 응답 포맷 표준화
  - 게이트웨이 헤더 기반 사용자 정보 바인딩
  - 요청 단위 상관관계 ID 관리 (`MDC["requestId"]`, `MDC["traceId"]`)

---

## 📂 security

- **목적**: 보안/내부 게이트웨이 전용 필터 제공
- **예시**:
  - `GatewayOnlyFilter`  
    → 지정된 헤더/시크릿 값 검증, 화이트리스트 패턴 매칭 지원
- **책임**:
  - 내부 API 호출 보호 (잘못된 헤더 접근 차단)
  - 게이트웨이 환경에서만 요청이 통과되도록 강제

---

## 📂 helper

- **목적**: 경량 범용 유틸리티 제공
- **예시**:
  - 날짜 유틸 (`DateTimeUtils`)
  - 해싱 유틸 (`SecureHashUtils`)
  - Base64, GZIP 변환
- **책임**:
  - 반복되는 로직 최소화
  - 외부 라이브러리를 직접 호출하지 않고 안전하게 래핑

---

## 📂 kafka  _(신규 · 프로듀서 MDC 연계)_

- **목적**: Kafka 연동 시 **MDC → Kafka 헤더** 동기화(프로듀서 측)
- **예시**:
  - `MdcToHeaderProducerInterceptor`  
    → 프로듀서 발행 시 `MDC["traceId"]`, `MDC["orderId"]` 를 Kafka 헤더로 주입(기존 헤더가 있으면 덮어쓰기 정책)
- **책임**:
  - API 계열 모듈에서 메시지 발행 시, 별도 코드 변경 없이 **엔드-투-엔드 추적성** 확보
  - 적용 방식은 `order-api-common` 모듈의 `CommonKafkaProducerAutoConfiguration`(AutoConfiguration)이 부트 스트랩 시 자동 주입
    - 기존에 사용자가 정의한 `interceptor.classes` 가 있어도 **중복 없이 병합** 처리
- **비고(컨슈머 측)**:
  - 컨슈머에서는 **워커 모듈(order-worker)** 이 `RecordInterceptor`/`BatchInterceptor` 를 통해 헤더 → MDC 복원(또는 payload 기반 강제 세팅)을 담당  
    → 프로듀서/컨슈머 양단에서 MDC 전파가 보장되어 **테스트/운영 모두 동일한 트레이싱** 유지

---

## ✅ 정리

| 디렉토리     | 설명 |
|--------------|------|
| `core`       | Enum, 예외, 인증 컨텍스트 등 핵심 타입 |
| `config`     | 공통 설정(no-op, 레거시 호환) |
| `support`    | JSON/JPA/Logging 유틸, AOP 애노테이션/Aspect |
| `autoconfigure` | Boot AutoConfig (Filter, Aspect, TaskDecorator) |
| `web`        | API 응답 포맷, ArgumentResolver, 필터 |
| `security`   | 게이트웨이 전용 보안 필터 |
| `helper`     | 날짜/암호화/Base64/GZIP 등 범용 유틸 |
| `kafka`      | **프로듀서 MDC → Kafka 헤더 인터셉터**(엔드-투-엔드 추적 보조) |

---

## 🔑 핵심 포인트

- `support` → **순수 기능 코드** (유틸, 애노테이션, Aspect)
- `autoconfigure` → **자동 설정 코드** (빈 등록, 필터/AOP 연결)
- `config` → **레거시 호환용 최소 no-op 설정**
- `web` → **표준 웹 응답/필터 구조**
- `security` → **게이트웨이 내부 전용 보안 필터**
- `kafka` → **프로듀서 인터셉터로 MDC 동기화**, 수신측은 워커의 컨슈머 인터셉터로 복원
- 모두 **비즈니스 무관** → 모든 서비스 모듈(api, batch, worker 등)에서 안정적 재사용 가능
