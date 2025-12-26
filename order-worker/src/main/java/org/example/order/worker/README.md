# 🧰 order-worker 서비스 README (Kafka 워커 · 구성/확장/운영 가이드)

Spring Boot 기반 **Kafka 워커**입니다.  
Local → Api → Crud → Remote로 이어지는 메시지 흐름을 **리스너(Listener)**, **파사드(Facade)**, **서비스(Service)** 로 분리했고, 공통 오류 처리(DLQ), S3 로그 동기화, 스케줄링(ThreadPoolTaskScheduler), SmartLifecycle 기반 기동/종료 훅을 포함합니다.  
설정은 모두 **YAML 중심**이며, 토픽명은 `MessageOrderType/MessageCategory` 기반으로 `KafkaTopicProperties` 에서 타입 세이프하게 주입합니다.  
또한 **MDC(traceId/orderId) ↔ Kafka 헤더/역방향 복원**이 자동으로 동작하여 **엔드-투-엔드 추적성**을 보장합니다.

---

## 변경 요약(본 문서 반영)

- **OrderWorkerConfig 정리**: 외부 모듈 패키지 직접 스캔 제거. **S3/Kafka/Cache/Application(Web) 오토컨피그를 @ImportAutoConfiguration 라인업에서 명시**합니다.
- **Kafka MDC 단건/배치 대응**: `order-client:kafka` 모듈에서 제공하는
  - `MdcHeadersProducerInterceptor` (Producer, MDC → Kafka 헤더 주입)
  - `MdcRecordInterceptor` / `MdcBatchInterceptor` (Consumer, Kafka 헤더 → MDC 복원)  
    를 컨테이너 팩토리에 적용하여 레코드/배치 모두 MDC 일관성을 보장합니다.
- **Crypto 키 선택/적용 표준화**: `AppCryptoKeyProperties(app.crypto.keys)` 기반 `CryptoKeySelectionApplier`가 `SecretsKeyClient`로 키 선택을 적용하고, `EncryptorFactory`의 Encryptor/Signer에 키를 주입합니다. 알고리즘 문자열은 워커 레벨에서 정규화합니다.
- **Lifecycle 기반 기동/종료 훅**: `ApplicationStartupHandlerImpl/ShutdownHandlerImpl`가 **S3 로그 업로드 + Secrets/스케줄/클라이언트 정리**까지 포함합니다.

---

## 1) 전체 구조

| 레이어 | 주요 클래스 | 핵심 역할 |
|---|---|---|
| 부트스트랩/조립 | OrderWorkerApplication, **OrderWorkerConfig** | 앱 구동, 코어·클라이언트 모듈 Import, 워커 패키지 스캔, ObjectMapper 기본 제공 |
| 설정 | AsyncConfig, CustomSchedulerConfig, KafkaListenerTopicConfig, KafkaTopicsConfig(local) | 비동기/스케줄 MDC 전파, 토픽명 Bean 주입, 로컬 토픽 자동생성/기동 후 보장 |
| **카프카(MDC)** | (**order-client:kafka**) MdcHeadersProducerInterceptor / MdcRecordInterceptor / MdcBatchInterceptor | **Producer: MDC → Kafka 헤더 주입**, **Consumer: Kafka 헤더 → MDC 복원(단건/배치)** |
| 리스너 | OrderLocalMessageListenerImpl, OrderApiMessageListenerImpl, OrderCrudMessageListenerImpl | Kafka 수신, 수동 Ack, 오류 로그, ConsumerEnvelope 래핑 |
| 파사드 | OrderLocalMessageFacadeImpl, OrderApiMessageFacadeImpl, OrderCrudMessageFacadeImpl | 메시지 검증·변환·오케스트레이션·DLQ 분기 |
| 서비스 | KafkaProducerServiceImpl, WebClientServiceImpl, OrderCrudMessageServiceImpl, OrderServiceImpl | Kafka 발행, Web 연동, DB 벌크/JPA 증폭, 메서드 타입 분기 |
| Crypto/Secrets | CryptoKeySelectionApplier, CryptoKeyRefreshListener, AppCryptoKeyProperties | 키 선택/적용, 리프레시 시 재적용, 설정 바인딩 |
| S3 동기화 | S3LogSyncServiceImpl, ApplicationStartupHandlerImpl, ApplicationShutdownHandlerImpl | Pod 로그 S3 업로드, 기동/종료 처리 (aws.s3.enabled=true 조건) |
| 예외/코드 | WorkerExceptionCode, DatabaseExecuteException | 표준 오류 코드, 실패 시나리오 표현 |

메시지 유형/흐름(논리):
- ORDER_LOCAL → ORDER_API → ORDER_CRUD → ORDER_REMOTE
- 실패/예외: ORDER_DLQ

> **MDC 트레이싱 한눈에 보기**
> - 프로듀서측: `MdcHeadersProducerInterceptor` 가 **MDC(traceId/orderId) → Kafka 헤더** 주입
> - 컨슈머측(워커): `MdcRecordInterceptor`/`MdcBatchInterceptor` 가 **Kafka 헤더 → MDC 복원**
> - 워커 전반(Async/Scheduler): `AsyncConfig`/`CustomSchedulerConfig` 로 **스레드 경계에서도 MDC 유지**

---

## 2) 동작 흐름(요약)

    KafkaListener (Local/API/CRUD)
      → Facade (검증/변환/오케스트레이션)
         → Service (외부콜/DB/발행)
            → 실패 시 즉시 DLQ 전송

원칙:
- Listener: try-catch-logging-finally-acknowledge (수동 Ack로 at-least-once)
- Facade: 예외 시 즉시 DLQ 전송 후 상위 전파(필요 시 원본 헤더 유지)
- Service: 도메인/외부 시스템 예외 로깅 후 상위 전파
- **MDC 보장**: 수신 직전 Interceptor가 **traceId/orderId** 를 MDC에 세팅 → 파사드/서비스 로그에 동일 추적키 노출

---

## 3) 구성/조립

### 3.1 OrderWorkerApplication

    @SpringBootApplication
    @Import(OrderWorkerConfig.class)
    public class OrderWorkerApplication {

        public static void main(String[] args) {
            SpringApplication.run(OrderWorkerApplication.class, args);
        }

        @PostConstruct
        private void setTimeZone() {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        }
    }

### 3.2 OrderWorkerConfig  (오토컨피그 라인업 명시)

- 코어/웹/TSID를 명시적으로 Import
- S3/Kafka/Cache/Application(코어 어플리케이션) 오토컨피그를 ImportAutoConfiguration으로 라인업
- 워커 패키지만 ComponentScan


    @Configuration
    @Import({
    OrderCoreConfig.class,
    WebAutoConfiguration.class,
    TsidInfraConfig.class
    })
    @ImportAutoConfiguration({
    S3AutoConfiguration.class,
    KafkaAutoConfiguration.class,
    RedisCacheAutoConfiguration.class,
    ApplicationAutoConfiguration.class
    })
    @ComponentScan(basePackages = {
    "org.example.order.worker.config",
    "org.example.order.worker.service",
    "org.example.order.worker.facade",
    "org.example.order.worker.controller",
    "org.example.order.worker.listener",
    "org.example.order.worker.lifecycle",
    "org.example.order.worker.crypto"
    })
    public class OrderWorkerConfig {
  
          @Bean
          @ConditionalOnMissingBean(ObjectMapper.class)
          ObjectMapper objectMapper() {
              return ObjectMapperFactory.defaultObjectMapper();
          }
    }

### 3.3 KafkaListenerTopicConfig (MessageOrderType/Category → 토픽명 Bean)

- 비로컬 환경: `KafkaTopicProperties` 기반으로 Message 타입별 토픽명 Bean 제공


    @Configuration
    public class KafkaListenerTopicConfig {
        @Bean public String orderLocalTopic(KafkaTopicProperties p)  { return p.getName(MessageOrderType.ORDER_LOCAL);  }
        @Bean public String orderApiTopic(KafkaTopicProperties p)    { return p.getName(MessageOrderType.ORDER_API);    }
        @Bean public String orderCrudTopic(KafkaTopicProperties p)   { return p.getName(MessageOrderType.ORDER_CRUD);   }
        @Bean public String orderRemoteTopic(KafkaTopicProperties p) { return p.getName(MessageOrderType.ORDER_REMOTE); }
        @Bean public String orderDlqTopic(KafkaTopicProperties p)    { return p.getName(MessageOrderType.ORDER_DLQ);    }
    }

### 3.4 AsyncConfig / CustomSchedulerConfig (MDC 전파)

- `AsyncConfig`: `ThreadPoolTaskExecutor` + MDC TaskDecorator (core=8/max=32/queue=1000)
- `CustomSchedulerConfig`: `ThreadPoolTaskScheduler` 확장으로 모든 schedule* Runnable을 MDC 데코레이트 (pool=2, removeOnCancel=true, waitOnShutdown=true)

---

## 4) 리스너 → 파사드 → 서비스

### 4.1 Local → Api

Listener (단건)


    @KafkaListener(
        topics = "#{@orderLocalTopic}",
        groupId = "group-order-local",
        concurrency = "2"
    )
    public void orderLocal(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            facade.handle(record);
        } catch (Exception e) {
            log.error("error : order-local", e);
        } finally {
            ack.acknowledge();
        }
    }

Facade (LOCAL → API 발행)

    public void handle(ConsumerRecord<String, Object> record) {
        var dto = mapper.toDto(record);     // contract → internal
        dto.validateStrict();               // op/필수값 검증(엄격)
        producer.sendToOrderApi(dto);       // 정상: 원본 헤더 변경 없음(ProducerInterceptor가 MDC 헤더 주입)
    }

> **MDC 포인트**: Consumer Interceptor가 이미 `MDC["traceId"]`, `MDC["orderId"]` 를 복원했으므로 이후 로그는 동일 traceId로 이어집니다.

---

### 4.2 Api → Crud

Listener (단건)

    @KafkaListener(
        topics = "#{@orderApiTopic}",
        groupId = "group-order-api",
        concurrency = "2"
    )
    public void orderApi(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            facade.handle(record);
        } catch (Exception e) {
            log.error("error : order-api", e);
        } finally {
            ack.acknowledge();
        }
    }

Facade (API 조회 → CRUD 발행, 실패 시 DLQ)

    public void handle(ConsumerRecord<String, Object> record) {
        var dto = mapper.toDto(record);

        try {
            // order-api-master로 조회 (POST /api/v1/local-orders/query)
            var sync = webClientService.fetchLocalOrderSync(dto.getOrderId());

            // CRUD 메시지 생성/발행
            var crud = assembler.toCrud(dto, sync);
            producer.sendToOrderCrud(crud);
        } catch (Exception e) {
            producer.sendToDlq(dto, e); // DLQ: DeadLetter + ErrorDetail(스택 4000자) + 원본헤더 복원
            throw e;
        }

        // NOTE(현 스냅샷): 테스트용 RuntimeException 강제 throw 코드가 존재할 수 있으므로 운영 반영 전 제거/가드 필요
    }

---

### 4.3 Crud → DB/Remote

Listener (배치)

    @KafkaListener(
        topics = "#{@orderCrudTopic}",
        groupId = "group-order-crud",
        containerFactory = "kafkaBatchListenerContainerFactory",
        concurrency = "10"
    )
    public void executeOrderCrud(List<ConsumerRecord<String, Object>> records, Acknowledgment ack) {
        try {
            facade.handle(records);
        } catch (Exception e) {
            log.error("error : order-crud", e);
        } finally {
            ack.acknowledge();
        }
    }

Facade (배치 유효/무효 분리, op별 그룹핑, DLQ 정책)

    public void handle(List<ConsumerRecord<String, Object>> records) {
        if (records == null || records.isEmpty()) return;

        var dtos = records.stream()
            .map(mapper::toDto)
            .toList();

        var split = validator.splitValidInvalid(dtos);

        // 무효 메시지는 즉시 DLQ (개별)
        split.invalid().forEach(m -> producer.sendToDlq(m, m.invalidReasonAsException()));

        // 유효 메시지 op별 그룹 처리
        var grouped = grouper.groupByOperation(split.valid());

        grouped.forEach((op, group) -> {
            try {
                orderService.execute(op, group); // REQUIRES_NEW
                group.forEach(success -> producer.sendToOrderRemote(success.toCloseMessage()));
            } catch (Exception e) {
                producer.sendToDlq(group, e); // 그룹 단위 DLQ
            }
        });
    }

Service (REQUIRES_NEW + JDBC bulk + JPA 증폭 + afterCommit 후속 처리)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(Operation op, List<OrderCrudDto> dtos) {
        switch (op) {
            case CREATE -> crudService.bulkInsert(dtos);
            case UPDATE -> crudService.bulkUpdate(dtos);
            case DELETE -> crudService.bulkDelete(dtos);
            default -> throw new WorkerException(WorkerExceptionCode.UNSUPPORTED_EVENT_CATEGORY);
        }
    }

- 커밋 이후(afterCommit):
  - Dynamo upsert/delete
  - Cache upsert/evict
- Dynamo 저장 시 `orderPriceEnc` 는 `AesGcmEncryptor` 로 처리

---

## 5) Crypto / Secrets (키 선택/적용)

- 설정: `app.crypto.keys` (logicalName → {alias, encryptor(AES128/256/GCM), kid 우선, version})
- 적용: `CryptoKeySelectionApplier`
  - `SecretsKeyClient.applySelection(alias, version, kid, allowLatest)`
  - 선택된 현재 키를 Base64로 `EncryptorFactory`의 Encryptor/Signer에 `setKey`
  - 알고리즘 문자열 정규화(예: AES-GCM ↔ AESGCM 등)
- 리프레시: `CryptoKeyRefreshListener`가 secrets 갱신 이벤트 수신 시 `applyAll(false)`로 재적용(자동 승격 금지)

---

## 6) S3 로그 동기화

활성 조건:
- `aws.s3.enabled=true` 일 때만 활성
- Startup/Shutdown 핸들러도 동일 조건 하에서 동작

핵심 동작:
- `S3LogSyncServiceImpl`
  - region 필수 (fail-fast)
  - 파일명에 `HOSTNAME` 포함 파일만 업로드 (자신 Pod 로그만)
  - 로컬 파일 MD5 vs S3 ETag 비교 → 동일하면 업로드 스킵
  - `.upload/*.snapshot` 로 업로드 스냅샷 관리

Lifecycle:
- `ApplicationStartupHandlerImpl` (SmartLifecycle phase MIN_VALUE)
  - start 시 로그 디렉토리 준비 + 초기 업로드
- `ApplicationShutdownHandlerImpl` (SmartLifecycle phase MIN_VALUE)
  - stop 시 최종 업로드 후
    - `SecretsLoader.cancelSchedule()`
    - `SecretsManagerClient.close()`
    - `SecretsKeyResolver.wipeAll()`

---

## 7) 설정(YAML) 샘플 (현행 스냅샷 기준)

### 7.1 Kafka (order-client:kafka 프로퍼티 기준)

    kafka:
      consumer:
        enabled: true
        bootstrap-servers: localhost:9092
        trusted-packages: "org.example.order"   # 필수(없으면 fail-fast)
        auto-offset-reset: earliest
        enable-auto-commit: false
        max-poll-records: 500
        idle-between-polls: 0
      producer:
        enabled: true
        bootstrap-servers: localhost:9092

      topic:
        list:
          - category: ORDER_LOCAL
            name: local-order-topic
          - category: ORDER_API
            name: order-api-topic
          - category: ORDER_CRUD
            name: order-crud-topic
          - category: ORDER_REMOTE
            name: order-remote-topic
          - category: ORDER_DLQ
            name: order-dlq-topic

### 7.2 S3 (order-client:s3 프로퍼티 기준)

    aws:
      region: ap-northeast-2
      endpoint: ""                # LocalStack 사용 시 지정
      credential:
        enabled: true
        accessKey: test
        secretKey: test
      s3:
        enabled: true
        bucket: corp-logs
        defaultFolder: order-worker
        autoCreate: false
        createPrefixPlaceholder: true

### 7.3 WebClient (order-client:web 프로퍼티 기준)

    web:
      enabled: true
      timeout:
        connectMs: 3000
        readMs: 10000
      codec:
        maxBytes: 2097152
      client:
        clientId: order-worker
        url:
          order: "http://order-api-master:8080/api/v1/local-orders/query"

### 7.4 Crypto (워커 적용 키 선택)

    crypto:
      enabled: true

    app:
      crypto:
        keys:
          orderPrice:
            alias: order-price-key
            encryptor: AESGCM
            kid: "kid-001"
            # version: "v1"

---

## 8) 확장 가이드

### 8.1 메시지/토픽 추가
- `MessageOrderType`(또는 Category)에 새 항목 추가
- `KafkaTopicProperties(kafka.topic.list)`에 category/name 추가
- 워커에 신규 Listener/Facade/Service 구현
  - Listener: @KafkaListener(topics = "#{@newTopic}", groupId, concurrency 지정)
  - Facade: 검증·변환·DLQ 정책 재사용
  - Service: 도메인 로직 캡슐화

### 8.2 DLQ 정책 커스터마이징
- `KafkaProducerServiceImpl`에서
  - DLQ 전송 시 `DeadLetter` 생성 + `ErrorDetail(스택 4000자 제한)` 구성
  - 원본 헤더 복원 전송(추적키 유지)
- 환경별 DLQ 분리 시 `KafkaTopicProperties` 엔트리를 프로파일로 분기

### 8.3 성능/동시성
- CRUD 배치 리스너 `concurrency=10` 기준으로 트래픽에 맞춰 조정
- `OrderServiceImpl`는 `REQUIRES_NEW`로 상위 트랜잭션과 격리 커밋
- JDBC bulk chunking/옵션은 코어 레벨 설정/옵션으로 조정

### 8.4 WebClient 타임아웃/최대 바이트
- `web.timeout.*`, `web.codec.maxBytes` 조정
- 실패는 Facade 레벨에서 즉시 DLQ 전환 유지

### 8.5 Crypto 키 롤링
- Secrets 갱신 → `CryptoKeyRefreshListener` → `CryptoKeySelectionApplier.applyAll(false)`
- 자동 승격(allowLatest=true)은 운영 정책으로 통제(기본은 false)

---

## 9) 테스트 가이드

### 9.1 단위 테스트
- Facade/Service를 Mockito로 검증
- DLQ 오버로드는 구체 타입 매처(any(), anyList())를 분리해 모호성 제거
- WebClientService는 WebService를 Mock으로 주입해 `ApiResponse.data` 변환/예외 케이스 검증

### 9.2 통합 테스트(권장)
- Kafka는 Embedded Kafka 사용
- 단, `order-client:kafka` 오토컨피그가 사용되므로 `kafka.consumer.trusted-packages` 등 필수 프로퍼티를 반드시 설정
- 외부 의존(S3/Redis/Dynamo 등)은 프로파일/오토컨피그 exclude로 차단하거나 TestContainer로 대체

---

## 10) 운영 팁

- Ack 및 재처리: 수동 Ack로 at-least-once. 멱등 처리(업서트/키/버전) 권장.
- 모니터링: DLQ 토픽 적재량, 처리 지연, 배치 실패율, 컨슈머 lag, 스케줄러 상태 점검.
- 로그: 구조적 로깅 + traceId/orderId 기준으로 전 구간 추적.
- 프로파일: local에서는 토픽 자동 생성/보장(`KafkaTopicsConfig`)을 사용하고, 운영에서는 프로퍼티 기반 토픽 매핑(`KafkaTopicProperties`) 사용을 권장.
- **MDC 일관성**: Producer 인터셉터(MDC→Header) + Consumer 인터셉터(Header→MDC)가 쌍으로 동작해야 가장 깔끔한 추적 값을 남깁니다.

---

## 11) 예외 코드

- WorkerExceptionCode: EMPTY_PAYLOAD, EMPTY_MESSAGE, MESSAGE_TRANSMISSION_FAILED, MESSAGE_POLLING_FAILED, MESSAGE_GROUPING_FAILED, MESSAGE_UPDATE_FAILED, POLLING_FAILED, UNSUPPORTED_EVENT_CATEGORY, NOT_FOUND_LOCAL_RESOURCE 등
- DatabaseExecuteException: CommonException 상속, 실패 누적/시그널링

---

## 12) 한 줄 요약

카테고리 기반 토픽 주입, 파사드 중심 오류 격리, 일관된 DLQ, S3 로그 동기화에 더해  
**order-client:kafka가 제공하는 Producer/Consumer 인터셉터로 강화된 MDC 추적성**과  
**Secrets 기반 키 선택/적용 및 Lifecycle 정리까지 포함한 Kafka 워커**입니다.  
YAML 설정만으로 환경 전환이 가능하며, Listener · Facade · Service 레이어로 안전하게 확장하세요.
