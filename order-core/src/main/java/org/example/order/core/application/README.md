# 📦 order-core:application 패키지 구조 및 책임 정리

본 문서는 `order-core:application` 계층의 전체 디렉토리 및 DTO, Mapper 구성 요소에 대해 상세히 설명하고, 각 디렉토리의 목적 및 포함되어야 할 파일의 종류를 정의합니다.  
구조는 DDD와 MSA 원칙에 따라 Application Layer의 **Command / Query / Internal / Outgoing** 역할을 분리하고 있으며,  
도메인 계층과의 **변환 책임을 명확히 분리**하는 `Mapper`도 존재합니다.

---

## 📁 common

### 📁 adapter
- 외부 시스템과 연계되는 어댑터 정의 (예: REST Client, 외부 API Adapter)
- Kafka, Redis 등 메시징 시스템과의 인터페이스 구현 포함 가능

### 📁 dto
> 공통 계층에서 사용되는 DTO를 유형별로 정리

#### ▸ command
- 외부로부터 입력받아 내부 처리에 전달되는 요청 명세 DTO
- ex) 등록/수정 등의 명령 목적 객체

#### ▸ incoming
- 외부 시스템으로부터 유입되는 데이터 구조
- Kafka, Webhook, MQ 등 메시지 수신 시 사용되는 구조

#### ▸ internal
- 내부 로직 간 데이터 전달 시 사용하는 DTO
- Application ↔ Domain 또는 Application ↔ Messaging 간 변환 목적

#### ▸ model
- 간단한 구조의 Java 객체나 VO 클래스 정의 위치
- 상태 보존, 값 비교에 사용하는 불변 구조 중심

#### ▸ outgoing
- 외부로 전송되는 메시지, 응답 전용 객체
- Kafka 메시지 송신, 외부 API 응답 시 사용

#### ▸ query
- 조회 목적의 데이터 구조
- 보통 `record`로 정의하여 읽기 전용 용도로 사용
- 응답 최적화 및 Projection 구조를 갖음

#### ▸ response
- REST API 또는 서비스 응답용 DTO
- 클라이언트 응답 페이로드 정의

---

### 📁 event
- 도메인 이벤트를 Application 레벨에서 수신/처리하기 위한 클래스
- 예: `@EventListener`, `@TransactionalEventListener` 등 핸들러 위치

### 📁 exception
- Application 계층에서 발생하는 커스텀 예외 정의
- ex) `OrderNotFoundException`, `InvalidOrderStateException`

### 📁 listener
- 메시지 리스너 또는 스케줄 기반 이벤트 처리기 위치
- KafkaConsumer, RedisListener, ApplicationListener 등 포함

### 📁 mapper
- Application ↔ Domain 객체 간 변환 책임 수행
- Domain 객체는 DTO를 모르기 때문에 반드시 Application에서 수행해야 함

### 📁 scheduler
- 주기적으로 동작하는 Scheduled Task 정의
- ex) 주문 자동 처리, 상태 검증, 외부 동기화 작업 등

### 📁 service
- Application 서비스 계층 구현체
- UseCase 또는 CommandHandler 역할

---

## 📁 order

### 📁 adapter
- 주문 도메인과 외부 인터페이스 연결 역할
- ex) 주문 관련 API Client, MQ 발행자, 외부 시스템 통합

### 📁 dto

#### ▸ internal

##### LocalOrderDto.java
- 주문 커맨드용 내부 DTO
- JSON 직렬화 대상이며 메시지 또는 DB Entity와의 매핑에 사용
- 실패 여부 플래그, 타임스탬프 관리 기능 내장

##### OrderDto.java
- `LocalOrderDto`를 래핑하여 고수준 의미를 부여한 객체
- 정적 팩토리 메서드를 통해 `OrderEntityDto`, `LocalOrderDto`로부터 생성 가능

##### OrderEntityDto.java
- 도메인 엔티티(OrderEntity)를 감싸는 Application 계층용 DTO
- 도메인 객체를 직접 노출하지 않고 래핑하여 전달

#### ▸ outgoing

##### OrderApiOutgoingDto.java
- 외부 API 또는 메시지 시스템으로 송신되는 DTO
- `OrderCloseMessage`로 변환하는 기능 포함

#### ▸ query

##### OrderDetailQueryDto.java
- 주문 단건 조회 시 반환되는 구조체
- `OrderEntity → QueryDto` 변환 메서드 포함

##### OrderResultQueryDto.java
- 주문 상세 결과를 래핑하는 DTO
- 복합 응답을 구성할 때 사용
- `OrderEntityDto → ResultQueryDto` 정적 메서드 포함

#### ▸ response
- 주문 관련 API의 응답 구조를 정의하는 위치
- ex) 주문 생성 결과, 상태 변경 결과 등

---

### 📁 event
- 주문 도메인의 이벤트 핸들러 정의
- 예: 주문 생성됨, 주문 마감됨 등 도메인 이벤트 수신자

### 📁 exception
- 주문 전용 커스텀 예외
- 예: `OrderNotFoundException`, `InvalidOrderNumberException`

### 📁 listener
- 주문 관련 Kafka, MQ 메시지 리스너 구현
- 예: KafkaListener로 주문 이벤트 수신 처리

### 📁 mapper

##### OrderMapper.java
- `LocalOrderDto ↔ OrderEntity` 간 양방향 변환
- `OrderUpdate` 커맨드 객체로의 변환도 담당
- 리스트 변환 메서드 제공
- DateTime 변환 유틸과 연계

### 📁 scheduler
- 주문 도메인 관련 주기성 작업 정의
- 예: 일별 주문 마감, 배치 처리

### 📁 service
- 주문 도메인에 대한 Application 서비스 구현체 위치
- 도메인 서비스 호출, 트랜잭션 처리, 메시지 발행 포함

---

## ✅ 구조 설계 의도

- **계층 분리**: DTO의 목적(command, query, outgoing 등)을 명확히 하여 SRP 유지
- **매핑 명확화**: 도메인 모델을 외부에 직접 노출하지 않고 Mapper로 캡슐화
- **확장 고려**: 외부 연동, 이벤트 처리, 응답 구조 확장을 고려한 디렉토리 설계

---

## 🧭 예시 흐름 (Command → Domain → Outgoing)

    LocalOrderDto
        ↓ OrderMapper.toEntity
    OrderEntity
        ↓ 처리 및 저장 후
    OrderApiOutgoingDto
        ↓ toMessage()
    OrderCloseMessage (Kafka 전송)

---

## 📚 정리 요약

| 디렉토리          | 역할 및 포함 내용                                                         |
|-------------------|---------------------------------------------------------------------------|
| adapter           | 외부 시스템과의 통합 처리 (REST, MQ, Kafka 등)                            |
| dto.command       | 외부 요청 처리용 명령 DTO                                                 |
| dto.internal      | 내부 서비스 간 데이터 전달 구조체 (`LocalOrderDto`, `OrderDto`)           |
| dto.query         | 조회 응답용 DTO (`OrderDetailQueryDto`, `OrderResultQueryDto`)            |
| dto.outgoing      | 외부 메시지 송신용 DTO (`OrderApiOutgoingDto`)                            |
| event             | 도메인 이벤트 리스너 및 정의                                              |
| exception         | 주문 전용 예외 처리                                                       |
| listener          | 메시징 시스템(Kafka 등) 이벤트 수신 처리기                                |
| mapper            | DTO ↔ Domain 변환 (`OrderMapper`)                                         |
| scheduler         | 주문 관련 스케줄링 처리                                                   |
| service           | 주문 Application 서비스 구현체                                            |

> 이 구조는 명확한 책임 분리와 유지보수성을 목표로 하며, 메시징 및 API 통합을 고려한 MSA 기반 설계입니다.
