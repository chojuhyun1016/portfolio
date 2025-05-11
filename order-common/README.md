# 🧱 order-common

`order-common` 모듈은 전체 시스템에서 공통적으로 사용되는 **순수 유틸리티, 코드 Enum, 공통 메시지, 예외, 날짜 포맷, 해싱/암호화 유틸** 등을 포함한 **비즈니스 무관 공통 모듈**입니다.  
모든 서비스 모듈(`core`, `api`, `batch`, `worker`)이 이 모듈에 의존하며, 반대로 이 모듈은 어떤 비즈니스 도메인에도 의존하지 않습니다.

---

## 🎯 모듈 목적 및 주요 기능

- 시스템 전역에서 재사용 가능한 순수 Java 기반 유틸리티 제공
- Enum 기반 코드 직렬화 및 변환 구조 표준화
- 공통 예외 코드와 런타임 예외 처리 체계 제공
- 인증 유저(ThreadLocal) 컨텍스트 제공
- 날짜/시간 유틸리티 및 포맷 통일
- SHA256, MD5, Base64, GZIP 등의 인코딩 및 해싱 유틸
- 표준 응답 구조 및 메시지 포맷 정의
- Jackson 기반 직렬화/역직렬화 지원 설정 포함

---

## 📂 패키지 구조 및 설명

### 🔹 `application`

- `dto`: 공통 응답용 DTO 정의
    - `CodeEnumDto`: 코드 Enum(text, code) 표준 전달 구조

- `message`: 사용자에게 전달할 메시지 포맷 정의
    - `CustomErrorMessage`: 에러 응답 구조 객체

---

### 🔹 `auth`

- `AccessUserInfo`: 인증된 사용자 정보 객체
- `AccessUserManager`: `ThreadLocal` 기반 사용자 컨텍스트 관리

---

### 🔹 `code`

- `CodeEnum`, `ExceptionCodeEnum`: Enum 직렬화용 인터페이스
- `CommonExceptionCode`: 시스템 공통 예외 코드 정의
- 기타 코드 Enum: `DlqType`, `MessageChannel`, `MonitoringType` 등

---

### 🔹 `constant`

- `DateTimeConstant`: 표준 시간대, 기본 날짜/시간 상수
- `FileConstant`: MIME, 확장자 등 파일 관련 상수
- `HttpConstant`: HTTP 헤더 키, 요청 상수 등

---

### 🔹 `event`

- `DlqMessage`: DLQ(Dead Letter Queue) 전송 실패 메시지
- `MonitoringMessage`: 시스템 모니터링용 메시지 구조

---

### 🔹 `exception`

- `CommonException`: 공통 런타임 예외 처리 클래스

---

### 🔹 `format`

- `DefaultDateTimeFormat`: 일관된 날짜 포맷 정의(`yyyy-MM-dd HH:mm:ss`, 등)

---

### 🔹 `jackson`

- `config/CommonObjectMapperFactory`: 날짜/Enum 포맷 포함 `ObjectMapper` 설정
- `converter/CodeEnumJsonConverter`: Enum 직렬화/역직렬화 처리기 (`code`, `text` 필드 자동 변환)

---

### 🔹 `utils`

- `datetime/DateTimeUtils`: 날짜 변환 및 포맷 유틸 (예: `startOfDay`, `endOfMonth`)
- `encode/Base64Utils`, `ZipBase64Utils`: 인코딩, 압축 후 인코딩 유틸
- `exception/ExceptionUtils`: DB 예외 추출, 상세 메시지 포맷
- `hash/SecureHashUtils`: `SHA-256`, `MD5`, `HMAC` 해싱 지원
- `jackson/ObjectMapperUtils`: JSON 직렬화/역직렬화 및 필드 추출 유틸

---

### 🔹 `web`

- `ApiResponse`: 표준 응답 데이터 구조 (`data`, `meta`)
- `ResponseMeta`: 응답의 코드/메시지/응답시간 포함

---

## 🛠 대표 사용 예시

| 상황                           | 사용 도구                                     |
|--------------------------------|-----------------------------------------------|
| Enum을 JSON 응답에 포함         | `CodeEnumJsonConverter`, `CodeEnumDto`        |
| 인증된 사용자 정보 접근         | `AccessUserManager.get()`                     |
| 날짜 처리 및 포맷 통일         | `DateTimeUtils.startOfDay()`, `endOfMonth()` |
| 예외 처리 표준화               | `CommonException`, `CommonExceptionCode`     |
| 응답 메시지 전송 포맷          | `CustomErrorMessage`, `ApiResponse`          |
| Json ObjectMapper 통합 설정     | `CommonObjectMapperFactory`                  |
| SHA256/MD5 해싱, Base64 인코딩 | `SecureHashUtils`, `Base64Utils`             |

---

## 🔗 의존성 및 설계 원칙

- **단방향 순수 의존 구조**  
  `order-common` 모듈은 **비즈니스 계층, 외부 시스템, DB**에 전혀 의존하지 않으며  
  전 모듈(`core`, `api`, `worker`, `batch`)에서 import 하여 사용

- **순수 유틸리티 중심 모듈**  
  모든 클래스는 상태를 가지지 않으며, 스프링 빈 없이도 사용 가능

- **표준화된 Enum/Exception/Response 구조**  
  시스템 전반에 걸쳐 일관성 있는 처리 체계 제공

---

## ✅ 결론

이 모듈은 **시스템 품질, 유지보수성, 통합성**을 보장하기 위한 **표준 기반 유틸리티 허브**입니다.  
비즈니스와 독립적으로 공통 관심사를 해결하고, 서비스 모듈 간 중복 구현을 방지하는 핵심 기반을 제공합니다.
