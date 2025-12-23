# 📦 order-client.kafka 모듈

---

## 1) 모듈 개요 (현행 코드 기준)

Spring Boot + Spring for Apache Kafka 기반의 **Producer / Consumer 표준 인프라 모듈**이다.  
본 모듈은 **AutoConfiguration + @Import 조합**을 통해 구성되며, 각 기능은 **프로퍼티 기반 ConditionalOnProperty**로 명확히 ON/OFF 된다.

또한 **MDC(Mapped Diagnostic Context)** 를 Kafka Header로 전파/복원하여 `traceId`, `orderId` 기반의 **로그 상관관계(Log Correlation)** 를 보장한다.

| 구성요소 | 역할 | 핵심 포인트 (코드 기준) |
|---|---|---|
| `KafkaAutoConfiguration` | 모듈 진입 AutoConfig | Consumer / Producer / Topic Config를 한 번에 Import |
| `KafkaProducerConfig` | Producer 설정 | `kafka.producer.enabled=true` 시 활성, JsonSerializer 고정, LZ4, batch.size=64KiB, ProducerInterceptor 등록 |
| `KafkaConsumerConfig` | Consumer 설정 | `kafka.consumer.enabled=true` 시 활성, 단건/배치 Factory 분리, MANUAL_IMMEDIATE ack, 재시도 없음 |
| `KafkaProducerCluster` | 전송 서비스 | `producer.enabled=true` + `KafkaTemplate` 존재 시에만 등록, SmartLifecycle(phase=MIN_VALUE), stop()에서 flush |
| `KafkaProducerProperties` | Producer 프로퍼티 | enabled, bootstrap-servers(@NotBlank) |
| `KafkaConsumerProperties` | Consumer 프로퍼티 | enabled, bootstrap-servers(@NotBlank), trusted-packages(필수), option.* |
| `KafkaSSLProperties` | SSL/SASL 공통 | enabled=true 인 경우에만 security.protocol / sasl.* 주입 |
| `KafkaTopicProperties` + `KafkaTopicEntry` | 토픽 매핑 | kafka.topic 리스트 바인딩, category(+region) → topic name, 미존재 시 CommonException(UNKNOWN_SERVER_ERROR) |
| `MdcHeadersProducerInterceptor` | Producer MDC 전파 | MDC(traceId/orderId) → Kafka Header 주입, 기존 동일 헤더 제거 후 재주입, 예외 무시 |
| `MdcRecordInterceptor` | Consumer 단건 MDC 복원 | Header(traceId/orderId) → MDC 복원, value==null 시 헤더 덤프 + springDeserializerException* 복원 로그 |
| `MdcBatchInterceptor` | Consumer 배치 MDC 복원 | 첫 레코드 기준 MDC 복원, 각 레코드 value==null 시 헤더 덤프 + springDeserializerException* 복원 로그 |

패키지 구조 예시
- `org.example.order.client.kafka.autoconfig`
- `org.example.order.client.kafka.config.consumer`
- `org.example.order.client.kafka.config.producer`
- `org.example.order.client.kafka.config.topic`
- `org.example.order.client.kafka.config.properties`
- `org.example.order.client.kafka.interceptor`
- `org.example.order.client.kafka.service`

---

## 2) 설정 (application.yml)

아래 예시는 **현행 코드에 존재하는 프로퍼티만** 반영했다.

### 2.1 SSL / SASL (옵션)

YAML 예시:

    kafka:
      ssl:
        enabled: false
        security-protocol: SASL_SSL
        sasl-mechanism: AWS_MSK_IAM
        sasl-jaas-config: software.amazon.msk.auth.iam.IAMLoginModule required;
        sasl-client-callback-handler-class: software.amazon.msk.auth.iam.IAMClientCallbackHandler

- `kafka.ssl.enabled=true` 일 때만 Producer / Consumer 공통 설정에 아래 항목이 반영된다.
  - `security.protocol`
  - `sasl.mechanism`
  - `sasl.jaas.config`
  - `sasl.client.callback.handler.class`

---

### 2.2 Producer

YAML 예시:

    kafka:
      producer:
        enabled: true
        bootstrap-servers: localhost:9092

현행 코드에서 Producer 기본값(하드코딩 적용):
- VALUE Serializer: `JsonSerializer` (ObjectMapperFactory.defaultObjectMapper() 고정)
- 압축: `LZ4`
- `batch.size=65536` (64KiB)
- ProducerInterceptor: `MdcHeadersProducerInterceptor`

---

### 2.3 Consumer

YAML 예시:

    kafka:
      consumer:
        enabled: true
        bootstrap-servers: localhost:9092
        trusted-packages: "org.example.order.*,org.example.common.*"
        option:
          max-fail-count: 1
          max-poll-records: 1000
          fetch-max-wait-ms: 500
          fetch-max-bytes: 52428800
          max-poll-interval-ms: 300000
          idle-between-polls: 0
          auto-offset-reset: earliest
          enable-auto-commit: false

현행 코드 기준 동작:
- Container AckMode: `MANUAL_IMMEDIATE`
- ErrorHandler: `DefaultErrorHandler(new FixedBackOff(0L, 0L))` → **재시도 없음**
- ConsumerFactory:
  - KEY/VALUE 모두 `ErrorHandlingDeserializer`
  - delegate: KEY=`StringDeserializer`, VALUE=`JsonDeserializer`
  - `JsonDeserializer.TRUSTED_PACKAGES`는 `kafka.consumer.trusted-packages`에서 주입 (미설정 시 fail-fast)

---

### 2.4 Topic 매핑

YAML 예시:

    kafka:
      topic:
        - category: order-local
          name: beta-order-local
        - category: order-api
          name: beta-order-api

토픽 조회:
- `KafkaTopicProperties#getName(String category)` (대소문자 무시)
- `KafkaTopicProperties#getName(String category, RegionCode regionCode)`
- Enum 오버로드 지원: `getName(Enum<?>)`, `getName(Enum<?>, RegionCode)` (Enum.name() 사용)

---

## 3) 빠른 시작 (가장 중요한 사용법)

### 3.1 Producer — 메시지 전송 (서비스 코드)

현행 KafkaProducerCluster API:
- `sendMessage(Object data, String topic)`
- `sendMessage(Object data, String topic, Map<String, String> originalHeaders)`

예시(기본 전송):

    @Service
    @RequiredArgsConstructor
    public class OrderEventPublisher {

        private final KafkaProducerCluster producer;

        public void publish(Object event, String topic) {
            producer.sendMessage(event, topic);
        }
    }

전송 흐름(현행 코드):
- `MessageBuilder.withPayload(data).setHeader(KafkaHeaders.TOPIC, topic).build()`
- `KafkaTemplate.send(message)` → `CompletableFuture<SendResult<...>>`
- 완료 콜백은 `MdcPropagation.wrap(...)` 로 MDC 유지 후 로그 출력
- 내부 로그:
  - DEBUG: payload JSON 문자열 출력(성능/민감정보 고려 필요)
  - INFO: payloadType 출력

MDC 헤더 주입(현행 코드):
- ProducerFactory에 `MdcHeadersProducerInterceptor` 등록
- MDC의 `traceId`, `orderId`를 Kafka Header에 자동 주입(동일 키가 있으면 제거 후 재주입)
- 인터셉터에서 예외가 발생해도 전송이 막히지 않도록 예외 무시

---

### 3.2 Consumer — 리스너 작성 (단건)

단건 컨테이너 팩토리:
- Bean name: `kafkaListenerContainerFactory`
- RecordInterceptor: `MdcRecordInterceptor`

예시:

    @Component
    public class OrderEventListener {

        @KafkaListener(
            topics = "#{@kafkaTopicProperties.getName('order-local')}",
            groupId = "order-service",
            containerFactory = "kafkaListenerCon
