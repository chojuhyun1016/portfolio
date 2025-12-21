# 📁 order-common 모듈 디렉토리 구조 설명

`order-common` 모듈은 전역적으로 사용되는 **공통 기능, 유틸리티, 상수, 예외, JSON 직렬화/역직렬화, 로깅/트레이싱(Web + MDC)** 과 같은  
교차 관심사(AOP) 기능을 제공합니다.  
비즈니스 도메인이나 특정 인프라에 종속되지 않으며, 모든 서브 모듈(api, batch, worker 등)에서 **동일한 동작과 규칙**을 보장하도록 설계되었습니다.

---

## 📂 core

- **목적**: 전 모듈에서 공유하는 핵심 타입과 규칙 정의
- **예시**:
  - 코드 표준: `CodeEnum`, `CodeEnumDto`
  - 코드 Enum: `RegionCode`, `ContinentCode`, `CurrencyType`, `ZoneCode`
  - 예외: `CommonException`, `CommonExceptionCode`, `ExceptionCodeEnum`
  - 인증/호출자 컨텍스트: `AccessUserInfo`, `AccessUserContext`
  - 상수: `HttpConstant`, `FileConstant`
- **책임**:
  - 비즈니스와 무관한 **공통 개념의 단일 기준 제공**
  - Enum 기반 코드 체계 및 전역 예외 코드 관리
  - 사용자/시스템 호출자 컨텍스트의 표준 모델 제공

---

## 📂 config

- **목적**: 과거 구조 호환을 위한 최소 설정 영역
- **예시**:
  - `OrderCommonConfig` (deprecated / no-op)
- **책임**:
  - 레거시 모듈과의 호환성 유지
  - 신규 코드에서는 **직접 사용하지 않으며**, 오토컨피그 사용을 강제
- **비고**:
  - 신규 기능 및 빈 정의는 이 패키지에 추가하지 않음

---

## 📂 support

- **목적**: 외부 라이브러리 및 프레임워크 기반 공통 기능 제공
- **구성**:
  - ### logging
    - `@Correlate` 애노테이션
    - `CorrelationAspect` (SpEL 기반 도메인 키 추출, MDC 주입/복원)
    - `TraceIdTurboFilter` (traceId UUID 자동 보장)
    - `MdcPropagation` (비동기/콜백 MDC 전파)
    - `PathValueExtractor` (프레임워크 비의존 경로 추출)
  - ### json
    - `ObjectMapperFactory`
      - 직렬화 포맷 고정
      - 역직렬화 관대화(LocalDateTime 다중 포맷 허용)
    - `ObjectMapperUtils`
  - ### jpa
    - `BooleanToYNConverter` (Boolean ↔ 'Y'/'N')
  - ### messaging
    - `ConsumerEnvelope`
    - `ConsumerMessage`
- **책임**:
  - Jackson 기반 JSON 직렬화/역직렬화 표준화
  - AOP 기반 MDC(traceId, 도메인 키) 전파
  - JPA 공통 컨버터 제공
  - Kafka 메시지 메타 래핑(컨슈머 내부 전달용)

---

## 📂 autoconfigure

- **목적**: Spring Boot AutoConfiguration 제공
- **예시**:
  - `LoggingAutoConfiguration`
    - MDC `TaskDecorator` 자동 등록
    - `CorrelationAspect` 자동 등록
    - AspectJ AOP 활성화
  - `WebAutoConfiguration`
    - `CorrelationIdFilter` 자동 등록
    - 필터 최상위 우선순위 적용
- **특징**:
  - `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 기반 자동 로딩
  - 애플리케이션 코드에서 `@Import`, `@ComponentScan` 불필요
  - 중복 빈 등록 방지(`@ConditionalOnMissingBean`)

---

## 📂 web

- **목적**: 웹 계층 공통 처리
- **예시**:
  - 응답 표준: `ApiResponse<T>`, `ResponseMeta`
  - 필터: `CorrelationIdFilter`
- **책임**:
  - API 응답 포맷 표준화
  - 요청 단위 상관관계 ID 관리
    - `MDC["requestId"]`
    - `MDC["traceId"]` (없을 경우 requestId로 브리지)
  - 응답 헤더에 `X-Request-Id` 반환

---

## 📂 security

- **목적**: 내부/게이트웨이 전용 보안 필터 제공
- **예시**:
  - `GatewayOnlyFilter`
- **책임**:
  - 지정된 헤더/시크릿 기반 요청 검증
  - 내부 API 보호 및 외부 직접 접근 차단
  - 게이트웨이 환경 강제

---

## 📂 helper

- **목적**: 범용 경량 유틸리티 제공
- **예시**:
  - 날짜/시간: `DateTimeUtils`, `DateTimeFormat`
  - 인코딩/압축: `Base64Utils`, `ZipBase64Utils`
  - 해싱: `SecureHashUtils`
  - 예외 탐색: `ExceptionUtils`
  - 시간대 매핑: `ZoneMapper`
- **책임**:
  - 반복 로직 제거
  - JDK 표준 API를 안전하게 래핑
  - 외부 라이브러리 의존 최소화

---

## 📂 messaging (Kafka 공통)

- **목적**: Kafka 메시징 공통 유틸 제공
- **예시**:
  - `KafkaHeaderUtils`
    - Headers → `Map<String,String>` 변환
    - retry-count 추출(Headers / Map 양쪽 지원)
- **책임**:
  - Kafka 헤더 접근 표준화
  - 재시도 메타 정보 처리 일관성 유지
- **비고**:
  - 프로듀서/컨슈머 인터셉터 구현은 각 모듈(api/worker) 책임
  - `order-common`은 **유틸과 표준 모델만 제공**

---

## ✅ 정리

| 디렉토리        | 설명 |
|-----------------|------|
| `core`          | 공통 Enum, 예외, 컨텍스트, 상수 |
| `config`        | 레거시 호환용 최소 설정 |
| `support`       | JSON/JPA/Logging/MDC/AOP 핵심 기능 |
| `autoconfigure` | Boot AutoConfiguration (Aspect, Filter, TaskDecorator) |
| `web`           | API 응답 표준, 요청 필터 |
| `security`      | 게이트웨이 전용 보안 필터 |
| `helper`        | 날짜/암호화/Base64/GZIP 등 범용 유틸 |
| `messaging`     | Kafka 헤더/메시지 공통 유틸 |

---

## 🔑 핵심 포인트

- `support` → **순수 기능 코드** (유틸, 애노테이션, Aspect, 컨버터)
- `autoconfigure` → **자동 설정 진입점** (빈 등록, 연결)
- `config` → **레거시 호환용 no-op**
- `web` → **표준 웹 요청/응답 + 상관관계 ID 관리**
- `security` → **내부 API 보호 전용**
- `messaging` → **Kafka 공통 유틸 (정책·전송 로직은 각 모듈 책임)**
- 전체 모듈은 **비즈니스 무관**, 모든 서비스에서 동일한 규칙과 동작을 보장
