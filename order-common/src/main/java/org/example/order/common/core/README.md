# 📁 order-common/core 디렉토리 구조 설명

`core` 디렉토리는 **비즈니스 도메인이나 인프라에 의존하지 않는 순수 핵심 코드**를 담는 영역입니다.  
모든 서비스(api / worker / batch 등)에서 **동일한 의미와 규칙으로 재사용**될 수 있도록 설계되었으며,  
프레임워크(Spring, Kafka 등)에 대한 직접 의존을 갖지 않는 것을 원칙으로 합니다.

---

## 📂 context

- **목적**: 호출자/실행 컨텍스트에 대한 표준 모델 제공
- **구성 요소**:
  - `AccessUserContext`
    - `ThreadLocal` 기반 사용자 컨텍스트 저장소
    - 요청 스레드 내에서 호출자 정보 안전하게 조회/설정/해제
  - `AccessUserInfo`
    - 게이트웨이/헤더 기반 사용자 정보 표준 모델
    - `userId`, `loginId`, `userType`, `roles`, `groups`
    - roles / groups 는 CSV 문자열 유지 + List getter 제공
- **책임**:
  - 인증/인가 프레임워크에 종속되지 않는 **공통 호출자 컨텍스트 모델** 제공
  - 웹/메시징/배치 환경 모두에서 동일한 방식으로 접근 가능
  - ThreadLocal lifecycle 명확화(clear 제공)

---

## 📂 code

- **목적**: 시스템 전역에서 사용하는 코드/Enum 표준 정의
- **구성**:
  - ### type
    - `CodeEnum`
      - 모든 코드 Enum의 공통 인터페이스
      - `getText()`, `getCode()` 표준화
    - 코드 Enum
      - `RegionCode`
      - `ContinentCode`
      - `CurrencyType`
      - `ZoneCode`
  - ### dto
    - `CodeEnumDto`
      - `CodeEnum` → API/계약 전송용 DTO
      - `text / code` 구조로 단순화
- **책임**:
  - 코드 값의 **의미(text)** 와 **식별(code)** 분리
  - DB / API / 메시지 어디서든 동일한 코드 체계 유지
  - Enum → DTO 변환의 단일 진입점 제공

---

## 📂 constant

- **목적**: 전역 상수 정의
- **구성 요소**:
  - `HttpConstant`
    - 공통 HTTP 헤더 키 (`X-User-Id`, `X-Login-Id`, `X-Client-Id` 등)
  - `FileConstant`
    - 파일 확장자 등 단순 고정 값 (`.json`)
- **책임**:
  - 문자열 하드코딩 제거
  - 전역 상수의 중앙 집중 관리
  - 변경 시 파급 범위 최소화

---

## 📂 exception

- **목적**: 공통 예외/에러 처리 모델 정의
- **구성**:
  - ### code
    - `ExceptionCodeEnum`
      - 에러 코드 인터페이스
      - `code / msg / httpStatus`
    - `CommonExceptionCode`
      - 시스템 전역 공통 에러 코드 정의
  - ### core
    - `CommonException`
      - RuntimeException 기반 공통 예외
      - 에러 코드 + 커스텀 메시지 + cause 지원
  - ### message
    - `CustomErrorMessage`
      - 에러 메시지 전송용 모델
      - timestamp 포함
- **책임**:
  - 서비스 전반에서 **일관된 에러 코드/HTTP 상태 관리**
  - API / 배치 / 메시지 처리에서 동일한 예외 모델 사용
  - 로깅/응답/모니터링에서 공통 포맷 유지

---

## 📂 helper (core 하위 공통 유틸)

> `helper`는 구조상 `core` 성격의 코드로 분류되며,  
> 외부 라이브러리 의존 없이 **JDK 기반 공통 유틸**을 제공합니다.

- **구성 요소**:
  - 날짜/시간
    - `DateTimeFormat`
    - `DateTimeUtils`
  - 인코딩/압축
    - `Base64Utils`
    - `ZipBase64Utils`
  - 해싱
    - `SecureHashUtils`
  - 예외
    - `ExceptionUtils`
  - 시간대 매핑
    - `ZoneMapper`
- **책임**:
  - 반복 로직 제거
  - 포맷/변환 규칙의 단일 기준 제공
  - 테스트/운영 환경 모두에서 동일 동작 보장

---

## ✅ 정리

| 디렉토리  | 책임 요약 |
|-----------|------------|
| context   | 호출자/실행 컨텍스트(ThreadLocal) 표준 모델 |
| code      | 시스템 전역 코드/Enum 및 DTO 표준 |
| constant  | HTTP/파일 등 공통 상수 정의 |
| exception | 공통 예외, 에러 코드, 에러 메시지 모델 |
| helper    | 날짜/인코딩/해싱/시간대 등 범용 유틸 |

---

## 🔑 핵심 포인트

- `core`는 **비즈니스/인프라 무관**한 순수 영역
- Spring / Kafka / Web 의존 코드 없음
- Enum / Exception / Context / Util 의 **단일 기준(Single Source of Truth)** 제공
- 상위 모듈(api, worker, batch)은 `core`를 **그대로 신뢰하고 사용**하는 구조
