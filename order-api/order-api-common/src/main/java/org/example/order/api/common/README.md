# 📁 order-api/common 디렉토리 구조 설명

`common` 디렉토리는 `order-api` 모듈에서 공통적으로 사용되는 설정, 예외 처리, 인증 및 바인딩 기능을 담당합니다.  
인터셉터, 리졸버, 포맷 바인딩, WebMvc 설정 등의 API 전역 기능이 이 디렉토리에 포함됩니다.

---

## 📂 advice

- **목적**: 전역 예외 처리 핸들러 정의
- **핵심 클래스**: `ApiExceptionHandler`
- **설명**:
    - `@RestControllerAdvice`를 사용하여 API 예외를 일관되게 처리
    - `CommonException`, `Exception` 등 다양한 예외 처리
    - 실패 응답은 `ApiResponse.error()` 형태로 반환

---

## 📂 auth

- **목적**: API 인증 및 클라이언트 권한 상수 관리
- **주요 클래스**:
    - `AuthConstant`: 인증 관련 상수 정의 (`x-api-key` 등)
    - `ClientRole`: 인증된 클라이언트 권한 Enum (`ROLE_CLIENT`)
- **설명**:
    - 인증 관련 상수는 공통 상수 클래스로 분리 관리
    - Spring Security 또는 인터셉터 권한 부여 시 사용

---

## 📂 config

- **목적**: Spring WebMvc 전역 설정
- **핵심 클래스**: `WebMvcConfig`
- **설명**:
    - 정적 리소스 매핑 (`/static`, `/public` 등)
    - 인터셉터(`AccessUserInterceptor`) 등록
    - 컨트롤러 파라미터 리졸버(`AccessUserArgumentResolver`) 등록
    - Jackson 기반 메시지 컨버터 및 `ObjectMapper` 구성
    - 커스텀 `ConverterFactory` (예: `StringToEnum`) 등록

---

## 📂 interceptor

- **목적**: 요청 헤더 기반 사용자 정보 추출
- **핵심 클래스**: `AccessUserInterceptor`
- **설명**:
    - 요청 헤더에서 사용자 정보(`userId`, `roles` 등) 추출
    - `AccessUserContext`에 저장하여 전역 컨텍스트로 사용
    - 요청 종료 후 `clear()` 처리

---

## 📂 resolver

- **목적**: 사용자 정보 자동 주입을 위한 리졸버 제공
- **핵심 클래스**: `AccessUserArgumentResolver`
- **설명**:
    - 컨트롤러 메서드에 `AccessUserInfo` 타입 파라미터 바인딩
    - 내부적으로 `AccessUserContext`에서 사용자 정보 꺼내 바인딩

---

## 📂 support

- **목적**: 포맷 변환 및 Jackson 설정 모듈화
- **핵심 클래스**:
    - `EnumBinder`: `String → Enum<T>` 변환 처리
    - `DateTimeBinder`: `String → LocalDateTime` 변환 처리
    - `FormatConfig`: 위의 포맷 바인딩 클래스와 ObjectMapper 구성
- **설명**:
    - 커스텀 Converter, ConverterFactory 등록
    - `@RequestParam`, `@PathVariable`, `@RequestBody` 바인딩 지원
    - `FormatResourceFactory`를 통해 바인딩 전략을 중앙 집중화

---

## ✅ 디렉토리 책임 요약

| 디렉토리     | 책임 요약 |
|--------------|------------|
| advice       | API 전역 예외 처리 |
| auth         | 인증 관련 상수 및 권한 Enum 정의 |
| config       | WebMvc 설정 및 메시지 컨버터 구성 |
| interceptor  | 사용자 정보 추출 및 컨텍스트 저장 |
| resolver     | 사용자 정보 자동 바인딩 지원 |
| support      | 포맷 바인딩 및 Jackson Mapper 설정 지원 |
