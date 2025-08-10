# 📦 order-web

`order-web` 모듈은 **웹 애플리케이션에서 발생하는 주문 요청을 처리**하는 API 서비스입니다.  
사용자 또는 외부 시스템에서 전달되는 HTTP 요청을 수신하고, 내부 서비스 로직 및 Kafka 프로듀서를 통해 다른 시스템과 연동합니다.

---

## 🧭 모듈 목적 및 주요 기능

- 웹에서 발생하는 주문 생성/처리 API 제공
- 요청/응답 DTO 변환 및 검증
- Kafka 메시지 발행을 통한 비동기 처리
- 공통 예외 처리(WebApiExceptionHandler)로 일관된 에러 응답 제공

---

## 📁 주요 패키지 구조

| 패키지 | 설명 |
|--------|------|
| config | WebMvc, 보안, 로깅 등 web 모듈 전용 설정 |
| controller | API 엔드포인트 정의 |
| facade | 비즈니스 로직을 캡슐화하여 서비스와 컨트롤러 간 연결 |
| service | 핵심 도메인 서비스 로직 구현 |
| mapper | DTO ↔ 엔티티 변환 |
| dto | 요청(Request), 응답(Response) 데이터 구조 정의 |
| exception | 전역 예외 처리 및 예외 클래스 정의 |

---

## ⚙ 주요 클래스

### 1. **OrderApiConfig**
- web 모듈에서 필요한 API 설정 클래스
- Bean 등록, 모듈 간 의존성 주입 등 환경 설정 담당

### 2. **OrderController**
- `/orders` 관련 REST API 제공
- 요청 DTO → 비즈니스 로직 호출 → 응답 DTO 반환
- 예:
    - `POST /orders/local` : 로컬 주문 생성 API

### 3. **OrderFacade / OrderFacadeImpl**
- Controller와 Service 간 중간 계층 역할
- 서비스 호출 순서, 트랜잭션 경계, 예외 처리 흐름 관리

### 4. **OrderServiceImpl**
- 주문 생성 및 처리 핵심 로직 구현
- KafkaProducerService를 통해 비동기 메시지 발행

### 5. **KafkaProducerService / KafkaProducerServiceImpl**
- Kafka 메시지 전송 담당
- 주문 이벤트를 특정 토픽으로 발행하여 다른 서비스로 전달

### 6. **OrderRequest / OrderResponse**
- API 요청/응답 DTO
- JSON 직렬화/역직렬화를 위한 필드와 어노테이션 정의

### 7. **OrderResponseMapper**
- Entity ↔ DTO 변환 로직
- MapStruct 기반 매퍼 또는 수동 매핑 로직 구현

### 8. **WebApiExceptionHandler**
- 전역 예외 처리(@RestControllerAdvice)
- 모든 컨트롤러의 예외를 포착하여 표준화된 응답 포맷 반환

---

## 📜 환경 설정 (application-*.yml)
- `application-local.yml`, `application-dev.yml`, `application-beta.yml`, `application-prod.yml` 등 환경별 설정 파일
- API Key, Kafka 설정, 로깅 레벨, 외부 연동 URL 등을 환경별로 분리 관리

---

## 🏗 권장 구성 방식
- **공통 설정**: common 모듈에서 AutoConfiguration으로 제공
- **web 모듈**: API 엔드포인트와 비즈니스 로직만 유지
- **환경 변수**: application.yml에서 환경별 값만 오버라이딩
- 장점:
    - 중복 설정 제거
    - 서비스 모듈은 기능 구현에만 집중
    - 환경 전환 용이성 향상

---

## ✅ 요약

| 항목 | 내용 |
|------|------|
| 목적 | 웹 요청 처리 및 비즈니스 로직 실행 |
| 특징 | Controller-Facade-Service 계층 구조, Kafka 연동, 전역 예외 처리 |
| 설정 | 환경별 application.yml, common 모듈 AutoConfiguration |
| 이점 | API 요청 처리 단순화, 유지보수성 향상, 공통 설정 재사용 |
