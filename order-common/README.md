# order-common

order-common 모듈은 전체 서비스에서 공통적으로 사용되는 순수 유틸리티, 코드 Enum, 예외, 메시지, 날짜 포맷, 암복호화 유틸 등을 모아놓은 공통 의존 모듈입니다

모든 서비스 모듈은 이 공통 모듈에 의존하며, 이 모듈은 다른 하위 도메인이나 비즈니스 로직에 직접 의존하지 않습니다

---

## 모듈 성격 및 주요 기능

- 공통 예외, 응답 구조, 메시지 포맷 제공
- Enum 기반 코드 처리 및 직렬화 기능
- 공통 날짜 유틸 및 포맷 정의
- Base64, GZIP, SHA256 등 인코딩 유틸
- 공통 ObjectMapper 설정
- 시스템 유저, 접근 유저 정보 처리

---

## 패키지 구조 및 설명

- application
    - dto
      CodeEnumDto  Enum의 코드와 텍스트를 담는 공통 Dto
    - message
      CustomErrorMessage  예외 발생 시 사용자 응답용 메시지 구조

- auth
  AccessUserInfo  사용자 인증 정보 레코드
  AccessUserManager  ThreadLocal 기반 유저 컨텍스트 관리

- code
  CodeEnum  공통 코드 인터페이스
  ExceptionCodeEnum  공통 예외 코드 인터페이스
  CommonExceptionCode  서버 전역에서 사용되는 예외 코드 정의
  기타 Enum들  DLQ, 메시지 채널, 방식, 모니터링 타입 등

- constant
  DateTimeConstant  시간대 및 표준 시간 상수
  FileConstant  파일 관련 상수
  HttpConstant  HTTP 헤더 및 키 상수

- event
  DlqMessage  DLQ 실패 메시지 구조
  MonitoringMessage  모니터링 전송 메시지 구조

- exception
  CommonException  공통 런타임 예외

- format
  DefaultDateTimeFormat  날짜 포맷 패턴 모음

- jackson
    - config
      CommonObjectMapperFactory  날짜 Enum 포함 ObjectMapper 설정
    - converter
      CodeEnumJsonConverter  Enum을 Json으로 직렬화 및 역직렬화

- utils
    - datetime
      DateTimeUtils  날짜 변환 유틸, 마이크로초 처리 포함
    - encode
      Base64Utils  Base64 표준 및 URL-safe 인코딩 지원
      ZipBase64Utils  GZIP 압축 후 Base64 인코딩 지원
    - exception
      ExceptionUtils  SQLException 추출 유틸
    - hash
      SecureHashUtils  MD5, SHA256 해싱 유틸
    - jackson
      ObjectMapperUtils  Json 직렬화 및 필드 추출 유틸

- web
  ApiResponse  공통 응답 구조
  ResponseMeta  응답 메타 정보 코드, 메시지, 시간

---

## 대표적인 사용 예시

- Enum을 Json으로 직렬화할 때 CodeEnum 인터페이스 기반으로 text code 동시 전달
- 사용자 인증 정보는 AccessUserManager로 ThreadLocal 방식 처리
- 날짜 변환은 DateTimeUtils의 startOfDay endOfMonth 등으로 통일
- 공통 예외는 CommonException 또는 CommonExceptionCode 사용

---

## 의존성 성격

- 이 모듈은 순수 Java 및 Jackson, Spring 의존만 가지며 비즈니스 로직이나 외부 API와는 무관합니다
- core, api, batch, worker 등 모든 모듈에서 import 하여 사용됩니다
