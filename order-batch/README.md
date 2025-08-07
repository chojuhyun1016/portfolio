# 📦 order-batch

`order-batch` 모듈은 주문 시스템의 **정기적 및 비정기적 배치 작업**을 처리하는 전용 컴포넌트입니다.  
Kafka DLQ 메시지 재처리, 주기적인 정산 로직, 리포트 생성 및 외부 시스템 동기화 등  
**시간 기반 작업 및 대용량 비즈니스 데이터를 안정적으로 처리**하는 데 중점을 둡니다.

이 모듈은 `order-core`, `order-client` 등 내부 도메인 모듈과 연동되며,  
**운영 안정성, 재처리 내구성, 인프라 연계 유연성**을 고려해 설계되었습니다.

## 🧭 주요 기능

- Kafka DLQ 메시지 재처리
  - 메시지 타입별 재처리 (예: ORDER_LOCAL, ORDER_API, ORDER_CRUD)
  - 실패 횟수 초과 시 discard 처리 및 알림 메시지 전송
- 정기 배치 작업
  - Quartz 또는 Spring Scheduler를 통한 예약 작업 실행
  - 배치 간 중복 실행 방지를 위한 Named Lock 기반 동기화
- 파일/로그 업로드 및 외부 연동
  - S3 로그 업로드
  - 외부 시스템과의 데이터 동기화
- 인프라 내결함성 대응
  - Fallback, Retry, DeadLetterQueue 처리 전략 구현
  - 장애 대응을 위한 재전송 및 Alert 메시지 분기

## 📁 패키지 구조

org.example.order.batch  
├── service  
│   ├── retry         : DLQ 메시지 재처리  
│   ├── synchronize   : 외부 시스템 동기화, S3 로그 업로드  
│   └── common        : Kafka 프로듀서, 유틸리티  
└── config  
├── lock          : Lock 기반 설정  
├── scheduler     : 배치 Job 등록 설정  
└── kafka         : Kafka Consumer/Producer 설정

### 🔹 service.retry

- Kafka DLQ 메시지를 유형별로 분기하여 재처리
- 실패 횟수 초과 여부 판단 후 discard 또는 재전송 수행
- 유효하지 않은 메시지는 Alert 토픽으로 분리

### 🔹 service.synchronize

- 외부 시스템과의 주기적인 동기화 처리
- 예: S3 로그 업로드, 리포트 전송, 외부 정산 시스템과의 연계

### 🔹 config

- Kafka 설정, Scheduler 설정, Lock 설정 등 배치 전용 설정 집합
- DB 기반 Lock (`GET_LOCK`), Redis Lock, Scheduler 설정 클래스 포함

## 🔗 설계 원칙

| 항목 | 설명 |
|------|------|
| MSA 독립성 | 배치 모듈은 독립된 Spring Boot 애플리케이션으로 구성되며 최소한의 모듈 의존성만 유지 |
| 역할 분리 | 실시간 트랜잭션 처리와 비동기/배치 처리를 명확히 분리 |
| 재시도/장애 대응 | DLQ 재처리, 실패 알림, fallback 전략을 통한 복원력 확보 |
| 중복 실행 방지 | Lock 기반 실행 제어 (MySQL GET_LOCK, Redis 분산락 등) |
| 운영 모니터링 고려 | discard 메시지, 재처리 결과, 장애 시나리오에 대한 Alert 메시지 분리 처리 지원 |

## ✅ 결론

`order-batch`는 주문 시스템의 **비동기 및 예약 처리 요구사항을 책임지는 배치 처리 전용 모듈**입니다.  
Kafka 기반 DLQ 재처리, 정기적 리포트 및 정산 실행, 외부 연계 작업을 모듈화하여  
**운영의 안정성과 유지보수성을 확보**하며, 실시간 트랜잭션과 배치 프로세스 간의 **명확한 책임 분리**를 실현합니다.  
이를 통해 MSA 기반의 복잡한 시스템 내에서도 효율적이고 확장 가능한 배치 처리 구조를 보장합니다.
