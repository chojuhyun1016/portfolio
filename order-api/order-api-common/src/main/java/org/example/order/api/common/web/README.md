# order-api/common 모듈 README

`order-api-common` 모듈은 order-api 계열 서비스 전반에서 공통으로 사용되는 **Web 설정, 예외 처리, 바인딩 규칙**을 제공하는 모듈입니다.  
Spring Boot의 **AutoConfiguration**을 전제로 설계되었으며, 각 서비스 모듈은 별도 설정 없이도
동일한 Web 동작 규약을 사용할 수 있도록 구성되어 있습니다.

---

## 모듈 설계 철학

1. **공통 모듈은 “규약과 기본 동작”만 제공**
  - AutoConfiguration을 통해 자동 적용
  - 서비스 모듈에서는 필요 시 application.yml 수준에서만 오버라이드

2. **Web 계층의 동작을 전면 표준화**
  - 예외 응답 포맷
  - 파라미터 바인딩 규칙
  - Enum / DateTime 처리 방식

3. **명시적·Fail-fast 설계**
  - 암묵적 기본값 최소화
  - Date/Time 포맷은 반드시 주입된 Formatter만 사용
  - 잘못된 요청은 즉시 INVALID_REQUEST 로 귀결

---

## 주요 기능

- **전역 예외 처리**
  - API 응답 포맷 일관화
  - 비즈니스 예외 / 요청 오류 / 시스템 오류 명확 분리

- **WebMvc 공통 설정**
  - Enum 바인딩 규칙
  - Date / Time 파라미터 바인딩 규칙

- **포맷 바인딩 지원**
  - String → Enum
  - String → LocalDate / LocalTime / LocalDateTime / YearMonth

---

## 디렉토리 구조

### advice

- 목적: 전역 예외 처리
- 핵심 클래스: `GlobalExceptionHandler`

설명:
- `@RestControllerAdvice` 기반 전역 예외 처리
- 우선순위: `@Order(Ordered.LOWEST_PRECEDENCE)`
- 처리 규칙:
  - `CommonException`  
    → `ApiResponse.error(e)`
  - 요청/역직렬화/검증 오류  
    (`MethodArgumentNotValidException`, `BindException`,
    `ConstraintViolationException`, `HttpMessageNotReadableException`)  
    → `CommonExceptionCode.INVALID_REQUEST`
  - 그 외 모든 예외  
    → `CommonExceptionCode.UNKNOWN_SERVER_ERROR`

로깅 정책:
- 비즈니스/요청 오류: `warn`
- 시스템 예외: `error`

---

### binder

- 목적: Controller 파라미터 바인딩 규칙 제공
- 특징: **암묵적 기본 포맷 사용 금지**

#### DateTimeBinder

- 문자열을 날짜/시간 타입으로 변환하는 컨버터 제공
- 반드시 외부에서 주입된 `DateTimeFormatter` 사용

지원 타입:
- `String → LocalDate`
- `String → LocalTime`
- `String → LocalDateTime`
- `String → YearMonth`

특징:
- 기본 포맷 없음
- 포맷 불일치 시 즉시 예외 발생 (Fail-fast)

#### EnumBinder

- 범용 Enum 바인딩 ConverterFactory
- 처리 규칙:
  - 앞뒤 공백 제거
  - 빈 문자열 → `null`
  - 대소문자 무시 (`toUpperCase()`)

예:
- `"foo"` / `" FOO "` / `"Foo"` → `FOO`

---

### config.mvc

- 목적: WebMvc 공통 설정
- 핵심 클래스: `WebMvcCommonConfig`

설명:
- `WebMvcConfigurer` 구현
- AutoConfiguration 에서 빈으로 생성되어 적용
- 직접 `@Configuration` 또는 Component Scan 대상 아님

등록 항목:
- `DateTimeBinder`의 Converter 들
- `EnumBinder` ConverterFactory

적용 범위:
- `@RequestParam`
- `@PathVariable`
- `@RequestBody` 내부 바인딩

---

## AutoConfiguration 적용 방식

- 본 모듈은 단독으로 동작하지 않음
- 상위 모듈의 AutoConfiguration 에서 아래 구성 요소들이 빈으로 생성됨

구성 흐름:
1. DateTimeFormatter 들을 정의
2. `DateTimeBinder`, `EnumBinder` 빈 생성
3. `WebMvcCommonConfig` 생성
4. Spring MVC FormatterRegistry 에 자동 등록

서비스 모듈은 **아무 설정도 하지 않아도 즉시 동일한 Web 바인딩/예외 처리 규약을 사용**

---

## 사용 방법

### 1) 의존성 추가

```gradle
implementation project(":order-api:order-api-common")
