# order-api/common 모듈 README

order-api 모듈의 `common` 디렉토리는 공통적으로 사용되는 **설정, 예외 처리, 인증, 포맷 변환** 등을 담당합니다.  
이 모듈은 Spring Boot의 **AutoConfiguration**과 `@ConfigurationProperties`를 활용하여,  
다른 모듈에서 최소한의 설정만으로 동일한 동작을 사용할 수 있도록 설계되었습니다.

---

## 모듈 설계 철학
1. 공통 모듈은 AutoConfiguration + @ConfigurationProperties 기본값으로 "행동과 디폴트"만 제공합니다.  
   각 서비스 모듈은 필요한 경우 application.yml 등을 통해 오버라이딩합니다.
2. 서비스 모듈에서의 설정은 최소화하고, 공통 모듈은 재사용성과 확장성을 보장합니다.
3. 글로벌 예외 처리, 포맷 바인딩, 로깅, 시큐리티 설정 등을 표준화하여 모든 모듈에서 동일한 방식으로 동작하게 합니다.

---

## 주요 기능
- **전역 예외 처리**: API 응답 포맷 통일
- **인증 상수 및 권한 Enum 관리**
- **WebMvc 공통 설정**: 메시지 컨버터, 바인딩, 인터셉터
- **포맷 바인딩 지원**: Enum, DateTime, Jackson 설정
- **로깅 필터**: MDC 기반 Correlation ID 자동 처리
- **보안 설정**: 서버 간 API Key 인증 구조 지원

---

## 디렉토리 구조

### advice
- 목적: 전역 예외 처리 핸들러 정의
- 핵심 클래스: `GlobalExceptionHandler`
- 설명:
  - `@RestControllerAdvice`로 모든 API 예외를 일관 처리
  - `CommonException` 등 비즈니스 예외와 시스템 예외 분리 처리
  - 실패 응답은 `ApiResponse.error()` 형태로 반환

### auth
- 목적: API 인증 및 클라이언트 권한 상수 관리
- 주요 클래스:
  - `ApiInfraProperties`: API Key 및 인프라 관련 프로퍼티 매핑
- 설명:
  - 서버 간 통신 시 API Key 인증 구조 제공
  - Spring Security 또는 커스텀 필터에서 사용 가능

### config
- 목적: Spring WebMvc, Logging, Security 공통 설정
- 핵심 클래스:
  - `CommonWebAutoConfiguration`
  - `CommonLoggingAutoConfiguration`
  - `CommonSecurityAutoConfiguration`
  - `CommonFormatAutoConfiguration`
- 설명:
  - WebMvc 설정 (바인더, 인터셉터, 메시지 컨버터)
  - Correlation ID 기반 로깅 필터 등록
  - Security 기본 정책 및 필터 설정
  - Jackson, 포맷 바인딩 등록

### filter
- 목적: 요청별 Correlation ID 로깅
- 핵심 클래스: `MdcCorrelationFilter`
- 설명:
  - 요청 시 MDC에 Trace ID, Span ID, Correlation ID 저장
  - 로그 추적 가능하게 지원
  - 응답 후 MDC 정리

### binder
- 목적: 컨트롤러 파라미터 바인딩 지원
- 핵심 클래스:
  - `DateTimeBinder`: 문자열 → LocalDateTime 변환
  - `EnumBinder`: 문자열 → Enum 변환
- 설명:
  - @RequestParam, @PathVariable, @RequestBody에 자동 적용

### support
- 목적: 포맷 변환 및 ObjectMapper 설정
- 핵심 클래스: `FormatConfig`
- 설명:
  - Custom Converter, ConverterFactory 등록
  - Jackson 직렬화/역직렬화 전략 통합

### config.mvc
- 목적: MVC 전역 설정
- 핵심 클래스: `WebMvcCommonConfig`
- 설명:
  - 리소스 매핑, 메시지 컨버터, 포맷터, 리졸버 등록

---

## 사용 방법

1. **common 모듈을 dependency로 추가**
    ```gradle
    implementation project(":order-api:common")
    ```

2. **서비스 모듈에서 application.yml 설정**  
   공통 모듈은 기본 동작을 제공하며, 서비스 모듈에서 필요한 경우 오버라이드합니다.
    ```yaml
    spring:
      application:
        name: order-api

    api:
      infra:
        api-key: example-key
        allowed-ips:
          - 127.0.0.1
          - 192.168.1.0/24

    logging:
      level:
        root: INFO
        org.springframework.web: DEBUG
    ```

3. **AutoConfiguration 동작 방식**
- Spring Boot가 자동으로 `Common*AutoConfiguration` 클래스들을 로드
- `@ConfigurationProperties`를 통해 설정 값 주입
- 각 서비스 모듈에서 최소한의 설정으로 통합된 환경 제공

---

## 요약
| 디렉토리 | 책임 |
|----------|------|
| advice | API 전역 예외 처리 |
| auth | API 인증 관련 상수/프로퍼티 |
| config | WebMvc, Logging, Security, Format 공통 설정 |
| filter | 요청별 MDC 로깅 필터 |
| binder | DateTime/Enum 바인딩 |
| support | Jackson 및 포맷 설정 |
| config.mvc | MVC 전역 설정 |

---

## 권장 구성 방식
- 공통 모듈: AutoConfiguration + @ConfigurationProperties 기본값 제공
- 서비스 모듈: application.yml로 환경별 값만 오버라이딩
- 장점: 공통 로직 유지보수 용이, 서비스 모듈의 설정 최소화, 재사용성 극대화
