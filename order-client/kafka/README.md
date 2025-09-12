# 📦 order-client.kafka 모듈

---

## 1) 모듈 개요 (현재 코드 기준)

Spring Boot + Spring for Apache Kafka 기반의 **Producer/Consumer 표준 모듈**입니다.  
최신 구조는 **설정 기반(@Bean) + `@Import` 조립**과 **조건부 빈 등록**으로, 필요할 때만 켜지도록 설계되었습니다.  
또한 **MDC(Mapped Diagnostic Context)** 지원을 통해 **traceId/userId 등 진단 컨텍스트를 Kafka 헤더로 전파·복원**하여 로그 상관관계를 유지합니다.

| 구성요소 | 역할 | 핵심 포인트(코드 반영) |
|---|---|---|
| `KafkaModuleConfig` | 모듈 통합 Import | Producer/Consumer Config를 **한 번에 로드**, 각 Config는 `@ConditionalOnProperty`로 개별 ON/OFF |
| `KafkaProducerConfig` | `ProducerFactory` / `KafkaTemplate` | `kafka.producer.enabled=true`일 때만 활성, **JsonSerializer(ObjectMapper)**, **LZ4**, `batch.size=65536` |
| `KafkaConsumerConfig` | 단건/배치 `ConcurrentKafkaListenerContainerFactory` | `kafka.consumer.enabled=true`일 때만 활성, **`MANUAL_IMMEDIATE` ack**, 기본 **재시도 없음** |
| `KafkaProducerCluster` | 전송 서비스 | `@ConditionalOnBean(KafkaTemplate)` → Producer 켜졌을 때만 등록, **SmartLifecycle**(우선 시작/최후 종료), `sendMessage(data, topic)` |
| `KafkaProducerProperties` | 프로듀서 설정 | `kafka.producer.enabled`, `bootstrap-servers`(필수, 검증) |
| `KafkaConsumerProperties` | 컨슈머 설정 | `kafka.consumer.enabled`, `bootstrap-servers`(필수, 검증), `option.*`(poll/페치/커밋) |
| `KafkaSSLProperties` | SSL/SASL 공통 | `kafka.ssl.enabled=true`일 때만 보안 설정 주입 |
| `KafkaTopicProperties` + `KafkaTopicEntry` | 토픽 매핑 | `kafka.topic` 리스트 바인딩, `getName(category[, region])` 제공(미매핑 시 Fail-fast) |
| **MDC 지원** | 진단 컨텍스트 전파 | **옵션**: `kafka.mdc.enabled=true`일 때, Producer는 MDC를 헤더에 **주입**, Consumer는 수신 시 **복원 후 처리/정리** |
| **테스트** | IT/단위 검증 | `EmbeddedKafka` 통합 검증(`KafkaTemplate`→Raw `KafkaConsumer` 수신), enabled/disabled 조건부 빈 생성 테스트 |

> 패키지 예시:  
> `org.example.order.client.kafka.config.*` / `.config.producer` / `.config.consumer` / `.config.properties` / `.service`

---

## 2) 설정 (application.yml / profile)

### 2.1 최소/공통 (코드 반영)

보안(옵션) — MSK IAM/SASL 등

    kafka:
      ssl:
        enabled: false                 # 기본 false, 필요 시 true
        security-protocol: SASL_SSL
        sasl-mechanism: AWS_MSK_IAM
        sasl-jaas-config: software.amazon.msk.auth.iam.IAMLoginModule required;
        sasl-client-callback-handler-class: software.amazon.msk.auth.iam.IAMClientCallbackHandler

프로듀서

    kafka:
      producer:
        enabled: true                  # ✅ 켜면 ProducerFactory/KafkaTemplate 생성
        bootstrap-servers: localhost:9092

컨슈머

    kafka:
      consumer:
        enabled: true                  # ✅ 켜면 ListenerContainerFactory 생성(@EnableKafka)
        bootstrap-servers: localhost:9092
        option:
          max-fail-count: 1
          max-poll-records: 1000
          fetch-max-wait-ms: 500
          fetch-max-bytes: 52428800        # 50MiB
          max-poll-interval-ms: 300000     # 5분
          idle-between-polls: 0
          auto-offset-reset: earliest
          enable-auto-commit: false        # MANUAL_IMMEDIATE 정책과 일치

서비스별 토픽 매핑(선택)

    kafka:
      topic:
        - category: order-local
          name: "beta-order-local"
        - category: order-api
          name: "beta-order-api"

MDC 전파 옵션(선택)

    kafka:
      mdc:
        enabled: true                  # ✅ MDC 헤더 전파/복원 On/Off
        keys:                          # 전파할 MDC 키 화이트리스트(미설정 시 전체 전파)
          - traceId
          - userId
          - sessionId

> **보안 설정 주입 조건**: `kafka.ssl.enabled=true`일 때에만 Producer/Consumer 공통 클라이언트 설정에 SECURITY_PROTOCOL / SASL 값들이 적용됩니다.  
> **프로듀서 직렬화**: 코드 상 **JsonSerializer 고정** + **공통 ObjectMapper** 주입(도메인 객체 전송 표준화).  
> **압축/배치**: `LZ4` + `batch.size=65536`(64KiB) 기본 적용.  
> **MDC 전파**: `kafka.mdc.enabled=true` 시, **Producer→헤더 주입 / Consumer→MDC 복원**이 자동 적용됩니다.

---

## 3) 빠른 시작 (가장 중요한 사용법)

### 3.1 Producer — 메시지 전송 (서비스에서 간단 사용, MDC 자동 주입)

    @Service
    @RequiredArgsConstructor
    public class OrderEventPublisher {
      private final KafkaProducerCluster producer; // SmartLifecycle, KafkaTemplate 래핑

      public void publishOrderCreated(OrderCreatedEvent evt, String topic) {
        // topic: KafkaTopicProperties.getName(MessageCategory.ORDER_LOCAL) 등으로 주입 권장
        // MDC(enabled)일 경우 traceId/userId 등이 헤더에 자동 포함됨
        producer.sendMessage(evt, topic);
      }
    }

- `KafkaProducerCluster#sendMessage(Object data, String topic)`:
    - `MessageBuilder`로 payload + `KafkaHeaders.TOPIC` 설정 후 `KafkaTemplate.send()` 호출
    - **MDC 활성 시** 현재 Thread MDC를 헤더로 복사(화이트리스트 적용 가능)
    - 반환된 `CompletableFuture`에 **성공/실패 콜백**(오프셋/에러 로그)
- **Lifecycle**: 애플리케이션 시작 시 **가장 먼저 시작**, 종료 시 **flush 후 안전 종료**

### 3.2 Consumer — 리스너 작성 (MANUAL_IMMEDIATE ack, MDC 자동 복원)

    @Component
    public class OrderEventListener {

      @KafkaListener(
          topics = "#{@kafkaTopicProperties.getName(T(org.example.order.core.messaging.order.code.MessageCategory).ORDER_LOCAL)}",
          groupId = "order-service",
          containerFactory = "kafkaListenerContainerFactory"  // 단건 리스너
      )
      public void onMessage(org.apache.kafka.clients.consumer.ConsumerRecord<String, String> rec,
                            org.springframework.kafka.support.Acknowledgment ack) {
        try {
          // JSON → DTO 매핑 필요 시 ObjectMapper 사용 (현재 ConsumerFactory는 StringDeserializer)
          // MDC(enabled)일 경우 인터셉터가 헤더→MDC 복원 (traceId/userId 등)
          process(rec.value());
          ack.acknowledge(); // ✅ MANUAL_IMMEDIATE : 호출 시 즉시 커밋
        } catch (Exception e) {
          // 기본 에러핸들러는 "재시도 없음" — 정책 필요 시 교체 가능
          throw e;
        } finally {
          // 인터셉터가 정리하지만, Listener 레벨에서도 누수 방지 차원에서 clear 가능
          // MDC.clear();
        }
      }
    }

- 컨테이너 팩토리:
    - `kafkaListenerContainerFactory()` : **단건** 리스너, `MANUAL_IMMEDIATE`
    - `kafkaBatchListenerContainerFactory()` : **배치** 리스너, `MAX_POLL_RECORDS` 등 옵션 반영 + `MANUAL_IMMEDIATE`
- 주의: `enable-auto-commit=false`일 때 **반드시 `ack.acknowledge()` 호출**로 커밋 제어

---

## 4) 동작 흐름

    kafka.producer.enabled=true
      └─ KafkaProducerConfig
           ├─ ProducerFactory<String,Object>  (JsonSerializer + LZ4 + batch.size)
           ├─ KafkaTemplate<String,Object>
           └─ (옵션) MDCProducerInterceptor / 메시지 빌더 훅 → MDC 헤더 주입

    kafka.consumer.enabled=true
      └─ KafkaConsumerConfig (@EnableKafka)
           ├─ ConcurrentKafkaListenerContainerFactory (단건)  [Ack=MANUAL_IMMEDIATE]
           ├─ ConcurrentKafkaListenerContainerFactory (배치)  [Ack=MANUAL_IMMEDIATE, Batch 옵션]
           ├─ DefaultErrorHandler(FixedBackOff 0,0)  // 기본 재시도 없음
           └─ (옵션) MDCConsumerInterceptor → 헤더→MDC 복원 → 처리 후 MDC 정리

- **SSL/SASL**: `kafka.ssl.enabled=true` → `security.protocol`, `sasl.*` 속성들을 Producer/Consumer 공통 설정에 주입
- **Topic 매핑**: `KafkaTopicProperties`에서 `MessageCategory`(+선택 `RegionCode`) → 토픽명 조회(Fail-fast)

---

## 5) 프로퍼티 상세

### 5.1 Producer
- `kafka.producer.enabled` (boolean) : **ON/OFF 스위치**
- `kafka.producer.bootstrap-servers` (string) : **필수** (검증 애노테이션 적용)

### 5.2 Consumer
- `kafka.consumer.enabled` (boolean) : **ON/OFF 스위치**
- `kafka.consumer.bootstrap-servers` (string) : **필수** (검증 애노테이션 적용)
- `kafka.consumer.option.*` : poll/페치/오프셋/유휴시간 등 세부 튜닝 파라미터

### 5.3 SSL/SASL
- `kafka.ssl.enabled` (boolean) : 보안 설정 사용 여부
- `kafka.ssl.security-protocol` / `kafka.ssl.sasl-mechanism` / `kafka.ssl.sasl-jaas-config` / `kafka.ssl.sasl-client-callback-handler-class`

### 5.4 Topic
- `kafka.topic` (list of `KafkaTopicEntry`) : `category`, `regionCode(옵션)`, `name`
- 조회:
    - `getName(MessageCategory category)`
    - `getName(MessageCategory category, RegionCode regionCode)`

### 5.5 MDC (옵션)
- `kafka.mdc.enabled` (boolean) : **MDC 전파/복원 스위치**
- `kafka.mdc.keys` (list) : **전파할 MDC 키 화이트리스트**(미설정 시 전체 전파)

---

## 6) 테스트 가이드 (코드 반영 해설)

### 6.1 EmbeddedKafka 통합 테스트 — `KafkaProducerIT`

- `@EmbeddedKafka`로 **브로커 기동**(테스트 토픽 1개)
- `KafkaTemplate`으로 **메시지 전송** → **Raw `KafkaConsumer`**로 폴링 수신 검증
- JsonSerializer 특성: 문자열 payload가 **JSON 문자열(따옴표 포함)**로 전송될 수 있어, 수신값 비교 시 `"value"` 도 허용

핵심 포인트:

    String value = "hello-kafka-" + UUID.randomUUID();
    String jsonEncodedValue = "\"" + value + "\""; // JsonSerializer일 경우 수신 값

    // 매칭 조건: value 또는 jsonEncodedValue
    if (key.equals(recKey) && (value.equals(recVal) || jsonEncodedValue.equals(recVal))) {
        matched = true;
    }

### 6.2 조건부 빈 비활성화 테스트 — `KafkaProducerConfigDisabledTest`
- `kafka.producer.enabled=false` ⇒ **KafkaTemplate 빈 미생성** (`NoSuchBeanDefinitionException` 검증)

### 6.3 조건부 빈 활성화 테스트 — `KafkaProducerConfigEnabledTest`
- `kafka.producer.enabled=true` + 더미 `bootstrap-servers` ⇒ **KafkaTemplate 빈 생성** (실 브로커 연결 없이)

### 6.4 MDC 전파/복원 테스트 (선택)
- Producer 전송 전 `MDC.put("traceId", "...")` 설정 → Consumer 수신 시 헤더에 동일 값 존재 및 로그 상 포함 확인
- Listener 처리 종료 후 **MDC 정리**(메모리 누수 방지) 검증

---

## 7) 운영 팁 & 권장 설정

- **Idempotent/acks/retries**: 필요 시 Producer에 `enable.idempotence=true`, `acks=all`, `retries` 등 추가(현재 기본은 LZ4+batch).
- **Key 설계**: 순서/파티션 지역성이 필요한 이벤트는 **비즈니스 키**(예: `orderId`) 사용.
- **DLT/재시도 정책**: 기본 에러핸들러는 **재시도 없음**. 필요 시 `DefaultErrorHandler` + DLT 리커버러로 교체.
- **보안(MSK/IAM)**: 운영은 `kafka.ssl.enabled=true` 프로파일, 로컬/테스트는 `false`로 간단 유지.
- **관측성**: `KafkaProducerCluster` 전송 로그(토픽/오프셋/실패), 컨슈머 Lag/오프셋 커밋 지표 모니터링.
- **MDC 성능/안전**: 키 수 최소화(화이트리스트 사용), 바이너리/대형 값 금지, 처리 후 **MDC.clear()** 습관화.

---

## 8) 확장/개선 제안 (선택)

- **Producer 옵션 보강**: `linger.ms`, `buffer.memory`, `delivery.timeout.ms`, `request.timeout.ms`, `retries`, `max.in.flight.requests.per.connection` 등 yml 노출.
- **Consumer 재시도/복구**: `DefaultErrorHandler` + DeadLetterPublishingRecoverer, 재시도/스킵 분리 정책화.
- **Value SerDe 다양화**: Consumer에 `JsonDeserializer`(타입 바인딩), Producer에 `Headers` 기반 타입 힌트.
- **토픽 네임스페이스 표준**: `<bounded-context>.<event-name>` (`order.local`, `order.api` 등) + 지역코드 파티셔닝.
- **테스트 유틸**: 임시 토픽/그룹 ID 헬퍼, Awaitility 기반 대기 헬퍼.
- **MDC 전파 표준화**: 키 네이밍 컨벤션(`traceId`, `spanId`, `userId`) 및 보안 민감정보 필터링 정책.

---

## 9) 핵심 코드 스니펫(반영 확인)

### 9.1 Producer 설정 요지 (`KafkaProducerConfig`)

    configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, CompressionType.LZ4.name);
    configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 65_536);
    if (sslProperties.isEnabled()) {
        // SECURITY_PROTOCOL / SASL_* 주입
    }
    // (옵션) MDCProducerInterceptor 등록 또는 KafkaProducerCluster 내부에서 헤더 주입 로직 사용
    DefaultKafkaProducerFactory<String, Object> factory =
        new DefaultKafkaProducerFactory<>(configProps);
    factory.setValueSerializer(new JsonSerializer<>(ObjectMapperFactory.defaultObjectMapper()));
    return new KafkaTemplate<>(factory);

### 9.2 Consumer 설정 요지 (`KafkaConsumerConfig`)

    factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(0L, 0L))); // 재시도 없음
    ContainerProperties cp = factory.getContainerProperties();
    cp.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE); // 수동 커밋
    // 배치 팩토리에서는 MAX_POLL_RECORDS/FETCH_* 등 option 반영 + setBatchListener(true)
    // (옵션) MDCConsumerInterceptor를 컨테이너 팩토리에 등록하여 헤더→MDC 복원

### 9.3 전송 서비스 요지 (`KafkaProducerCluster`) — MDC 헤더 주입 예시

    Message<Object> message = MessageBuilder.withPayload(data)
            .setHeader(KafkaHeaders.TOPIC, topic)
            .build();

    // (옵션) MDC enabled & keys 화이트리스트 적용 시
    Map<String, String> ctx = MDC.getCopyOfContextMap();
    if (ctx != null) {
        Set<String> allow = mdcProps.getKeysOrNull(); // null이면 전체
        ctx.forEach((k, v) -> {
            if (allow == null || allow.contains(k)) {
                message.getHeaders().add(k, v == null ? new byte[0] : v.getBytes(StandardCharsets.UTF_8));
            }
        });
    }

    kafkaTemplate.send(message).whenComplete((result, ex) -> {
        if (ex == null) {
            log.info("Sending kafka message - topic: {}, offset: {}, traceId: {}",
                     topic,
                     result.getRecordMetadata().offset(),
                     MDC.get("traceId"));
        } else {
            log.error("error : Sending kafka message failed - topic: {}, message: {}",
                      topic, ex.getMessage(), ex);
        }
    });

### 9.4 Consumer 수신 시 MDC 복원 예시 (`MDCConsumerInterceptor` 혹은 Listener 시작부)

    // 헤더 → MDC 복원
    for (Header h : record.headers()) {
        MDC.put(h.key(), h.value() == null ? null : new String(h.value(), StandardCharsets.UTF_8));
    }
    try {
        // 실제 처리
        process(record.value());
        ack.acknowledge();
    } finally {
        MDC.clear(); // 누수 방지
    }

### 9.5 EmbeddedKafka IT 요지 (`KafkaProducerIT`)

    kafkaTemplate.send(TOPIC, key, value).join();
    try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
        consumer.subscribe(Collections.singletonList(TOPIC));
        // 폴링하며 value 또는 "value"(Json 문자열) 매칭 확인
    }

---

## 10) 마지막 한 줄 요약
**“yml 스위치로 Producer/Consumer를 명확히 제어하고, Producer는 `KafkaProducerCluster.sendMessage()`—Consumer는 `MANUAL_IMMEDIATE ack`—로 일관 사용.”**  
JsonSerializer + LZ4 + 보안 옵션 + **MDC 전파/복원**까지 **표준화된 설정/코드 경로**로 안정·가시성 있게 운영합니다.
