# order-api 개요

order-api 는 **주문 도메인의 HTTP API 계층**을 담당하는 상위 모듈이며,  
역할과 책임에 따라 공통 규약, 쓰기(Command), 조회(Query) 레이어로 분리되어 구성됩니다.  
모든 모듈은 **Spring Boot AutoConfiguration 기반**으로 동작하며, 규약은 공유하되 책임은 명확히 분리됩니다.

---

## 모듈 구성

order-api
* order-api-common
* order-api-master
* order-api-web

---

## order-api-common
Web 공통 규약 레이어

의미
* order-api 전반에서 사용하는 **Web 표준 규약 모듈**
* 단독 실행되지 않으며, 다른 API 모듈에 의해 자동 적용됨

역할
* 전역 예외 처리 규칙 정의
* Controller 파라미터 바인딩 규칙 정의
* Enum, DateTime 처리 방식 표준화

특징
* AutoConfiguration 전제
* Fail-fast 중심 설계
* 공통 규칙만 제공하고 비즈니스 로직은 포함하지 않음

---

## order-api-master
Command 및 메시지 발행 레이어

의미
* 주문 **쓰기(Command)** 요청의 진입점 API
* HTTP 요청을 Kafka 메시지로 변환하여 발행

역할
* 주문 생성, 수정, 삭제 요청 수신
* Request를 Command로 변환
* Kafka Producer를 통한 이벤트 발행
* 보조적으로 단건 조회 API 제공

구조 개념
* Controller → Facade → Service → Kafka Producer

특징
* 게이트웨이 종결형 최소 보안
* Correlation 기반 MDC(traceId, orderId) 적용
* 비동기 및 스케줄 경계에서도 MDC 전파 보장

---

## order-api-web
조회 전용 Query 레이어

의미
* 주문 **단건 조회(Query)** 전용 Web Adapter
* 쓰기 및 이벤트 발행 책임 없음

역할
* MySQL, DynamoDB, Redis 기반 조회 API 제공
* Application View를 API Response DTO로 변환
* 표준 응답 포맷(ApiResponse) 반환

구조 개념
* Controller → Facade → Service → Repository

특징
* Read Only 전용
* 도메인 엔티티 직접 노출 금지
* MapStruct 기반 DTO 매핑
* REST Docs 기반 문서화 파이프라인 포함

---

## 모듈 간 책임 분리 개념

공통 규약은 order-api-common 이 제공하며  
order-api-master 는 쓰기와 이벤트 발행을 담당하고  
order-api-web 은 조회만 담당합니다.

각 모듈은 상호 책임을 침범하지 않으며,  
공통 규약 위에서 독립적으로 확장될 수 있도록 설계되었습니다.

---

## 한 줄 요약

order-api 는  
공통 Web 규약(Common), 쓰기 API(Master), 조회 API(Web)를 분리한 구조로  
확장성, 안정성, 추적성을 동시에 보장하는 주문 API 계층입니다.
