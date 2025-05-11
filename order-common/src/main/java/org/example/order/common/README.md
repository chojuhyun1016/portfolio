# 📁 order-common 모듈 디렉토리 구조 설명

`order-common` 모듈은 전역적으로 사용되는 공통 기능, 유틸리티, 상수, 예외 및 JSON 직렬화/역직렬화 등의 공통 구성 요소를 제공하는 모듈입니다.  
이 모듈은 다른 비즈니스 도메인이나 외부 인프라에 의존하지 않고, 모든 서브 모듈에서 안전하게 사용할 수 있도록 설계되었습니다.

---

## 📂 core

- **목적**: 공통 코드, Enum, 상수, 예외, 인증 정보 등 핵심 개념을 정의
- **내용 예시**:
    - `CodeEnum`, `ExceptionCodeEnum`, `CommonException`
    - `AccessUserInfo`, `AccessUserManager` (인증 컨텍스트)
    - 날짜 포맷, HTTP 상수, 기본 메시지 구조
- **책임**:
    - 모든 서비스에서 공통으로 사용하는 비즈니스 독립적인 핵심 코드
    - Enum 기반의 코드 관리, 전역 예외 코드 정의 등

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

- **목적**: 외부 라이브러리 기반의 공통 유틸 또는 기술 지원 코드
- **내용 예시**:
    - `ObjectMapperFactory`, `ObjectMapperUtils`
    - `CodeEnumJsonConverter` (Jackson 직렬화/역직렬화)
- **책임**:
    - Jackson 기반 JSON 처리 및 Enum 변환 지원
    - 라이브러리에 종속된 기술 유틸을 공통 계층으로 분리하여 제공

---

## 📂 web

- **목적**: 웹 계층에서 공통적으로 사용하는 응답 구조, 필터, 메시지 등
- **내용 예시**:
    - `ApiResponse`, `ResponseMeta`, `CustomErrorMessage`
- **책임**:
    - API 응답 일관성 확보
    - 예외 또는 정상 응답을 위한 통합된 응답 포맷 제공

---

## 📂 helper

- **목적**: 경량화된 범용 유틸리티 클래스 제공
- **내용 예시**:
    - 날짜 유틸 (`DateTimeUtils`), 해싱 유틸 (`SecureHashUtils`)
    - Base64, GZIP 압축, JSON 변환 등의 기능
- **책임**:
    - 공통적이고 반복적인 로직을 정리하여 코드 간결성 확보
    - 외부 라이브러리를 직접 사용하지 않고 래핑하여 안정성 향상

---

## ✅ 정리

| 디렉토리 | 설명 |
|----------|------|
| `core`   | Enum, 공통 예외, 상수, 인증 컨텍스트 등 핵심 타입 정의 |
| `config` | 외부 설정 객체화 (`@ConfigurationProperties`) |
| `support`| Jackson 등 기술 기반 공통 처리 지원 유틸 |
| `web`    | API 응답 포맷, 메시지 구조 등 웹 계층 공통 처리 |
| `helper` | Base64, 날짜 등 범용 유틸리티 헬퍼 클래스 모음 |

---

이 구조는 모든 모듈(api, batch, worker 등)에서 공통적으로 참조 가능하며, 외부 의존성과 도메인 종속성 없이 안전하고 재사용 가능한 설계를 지향합니다.
