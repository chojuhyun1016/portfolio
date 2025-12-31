# 🧰 order-worker (Kafka Worker)

`order-worker`는 **Kafka 기반 비동기 워커**로, 주문 이벤트를 수신·변환·처리·전파하는 실행 모듈입니다.  
메시지 흐름(Local → Api → Crud → Remote)을 **Listener → Facade → Service**로 분리하고, **DLQ**, **MDC(traceId/orderId) 추적**, **Secrets/Crypto 키 적용**, **S3 로그 동기화**, **SmartLifecycle 기동/종료 훅**을 포함합니다.

---

## ✅ 역할/경계

- **역할**: Kafka 토픽에서 이벤트를 소비하여 DB/외부 연동 및 후속 토픽으로 재발행
- **핵심**: at-least-once(수동 Ack) + 일관된 DLQ + 엔드-투-엔드 MDC 추적
- **비포함**: HTTP Controller 중심의 API 제공(워커는 실행/처리 모듈), 도메인 모델 정의(도메인은 별도 모듈)

---

## 🧭 메시지 파이프라인(요약)

    ORDER_LOCAL → ORDER_API → ORDER_CRUD → ORDER_REMOTE
                    └─────────────── 실패/예외 → ORDER_DLQ

- **Producer**: MDC → Kafka Header 주입 (Producer Interceptor)
- **Consumer**: Kafka Header → MDC 복원 (Record/Batch Interceptor)
- **Thread 경계**: Async/Scheduler에서도 MDC 유지 (TaskDecorator)

---

## 🗂️ 구성(간단 구조)

- **Bootstrap/조립**
    - `OrderWorkerApplication` (UTC timezone)
    - `OrderWorkerConfig` (Core/Web/TSID Import + S3/Kafka/Cache/Application AutoConfig 라인업)
- **Kafka**
    - Topic Name 주입: `KafkaListenerTopicConfig` (`KafkaTopicProperties` 기반)
    - Local 환경 토픽: `KafkaTopicsConfig` (auto-create/ensure)
- **Processing**
    - Listeners: `OrderLocalMessageListenerImpl`, `OrderApiMessageListenerImpl`, `OrderCrudMessageListenerImpl(batch)`
    - Facades: `OrderLocalMessageFacadeImpl`, `OrderApiMessageFacadeImpl`, `OrderCrudMessageFacadeImpl`
    - Services: `KafkaProducerServiceImpl`, `WebClientServiceImpl`, `OrderServiceImpl(REQUIRES_NEW)`, `OrderCrudServiceImpl`
- **Ops/Infra**
    - Crypto/Secrets: `AppCryptoKeyProperties`, `CryptoKeySelectionApplier`, `CryptoKeyRefreshListener`
    - S3 Log Sync: `S3LogSyncServiceImpl` + `ApplicationStartupHandlerImpl/ShutdownHandlerImpl`
    - Error: `WorkerExceptionCode`, `DatabaseExecuteException`

---

## ⚙️ 최소 설정 포인트(요약)

- Kafka: `kafka.consumer.trusted-packages` **필수(fail-fast)**
- DLQ: 실패 메시지는 `ORDER_DLQ`로 표준 Envelope 전송
- Crypto: `app.crypto.keys` 기반으로 Secrets 선택/적용(리프레시 시 재적용)
- S3: `aws.s3.enabled=true`일 때만 로그 업로드 및 종료 정리 수행

---

## 🔑 한 줄 요약

**order-worker는 “Listener → Facade → Service” 구조로 Kafka 이벤트를 처리하고, MDC 추적·DLQ·Crypto/Secrets·S3 로그 동기화를 포함해 운영 친화적으로 확장 가능한 주문 워커 모듈입니다.**
