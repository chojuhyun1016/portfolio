# Application Layer 구조 안내

아래는 application 패키지의 하위 구성입니다.  
MSA + 클린 코드 아키텍처 기준에 따라 역할 중심으로 분리되어 있으며, 개발 시 아래 위치 기준에 맞게 클래스를 추가해주세요.

---

## common

- error  
  공통 예외 정의 및 예외 응답 포맷 등 처리

- event  
  시스템 전역에서 사용되는 이벤트 정의

- external  
  외부 연동 (예: Slack, Webhook 등) 관련 DTO

- validation  
  공통 유효성 검사용 어노테이션, Validator 등

---

## order

도메인 `Order`와 관련된 애플리케이션 로직 전용 계층입니다.

- command  
  쓰기 (등록, 수정 등) 요청을 처리하기 위한 Command 객체

- query  
  읽기 (조회 등) 요청 결과 전달을 위한 DTO 또는 Projection

- response  
  컨트롤러의 응답 결과로 반환할 Response 전용 DTO

- vo  
  내부 값 전달을 위한 VO (Value Object), 도메인 VO 아님

- mapper  
  도메인 Entity ↔ DTO 간 매핑 정의

- event  
  Order 도메인의 이벤트 중심 구조

    - dto  
      이벤트 핸들링에 필요한 데이터 구조 정의

    - handler  
      Kafka 소비자 등 이벤트 처리 핸들러 구현체 위치

    - message  
      발행/소비되는 Event 객체 정의
      예: OrderApiEvent, OrderCrudEvent, OrderLocalEvent, OrderRemoteEvent

    - OrderRemoteMessageDto.java  
      외부 메시지 처리에 사용되는 DTO (예: Kafka 전송용)

---

## 참고

- 모든 `Dto`, `Command`, `Response`, `Vo`, `Event` 등은 각각의 목적에 따라 정해진 위치에 배치합니다.
- 공통/범용적인 로직은 `common`에, 도메인 종속적인 로직은 `order` 하위에 위치시킵니다.
- 컨트롤러는 이 레이어와 직접적으로 연결됩니다. 도메인 레이어나 인프라 레이어와는 간접적으로 연결되어야 합니다.
