# 🧰 order-worker 서비스 README (Kafka 워커 · 구성/확장/운영 가이드)

Spring Boot 기반 **Kafka 워커**입니다.  
Local → Api → Crud → Remote로 이어지는 메시지 흐름을 **리스너(Listener)**, **파사드(Facade)**, **서비스(Service)** 로 분리했고, 공통 오류 처리(DLQ), S3 로그 동기화, 스케줄링(ThreadPoolTaskScheduler), SmartLifecycle 기반 기동/종료 훅을 포함합니다.  
설정은 모두 **YAML 중심**이며, 토픽명은 `MessageCategory` 기반으로 `KafkaTopicProperties` 에서 타입 세이프하게 주입합니다.  
또한 **MDC(traceId/orderId) → Kafka 헤더/역방향 복원**이 자동으로 동작하여 **엔드-투-엔드 추적성**을 보장합니다.

---

## 1) 전체 구조

| 레이어 | 주요 클래스 | 핵심 역할 |
|---|---|---|
| 부트스트랩/조립 | OrderWorkerApplication, OrderWorkerConfig | 앱 구동, 코어·클라이언트 모듈 Import, 워커 패키지 스캔, ObjectMapper 기본 제공 |
| 설정 | CustomSchedulerConfig, KafkaListenerTopicConfig | 스케줄러 스레드풀, MessageCategory → 토픽명 Bean 주입 |
| **카프카(MDC)** | **KafkaMdcInterceptorConfig** | **컨슈머 인터셉터(Record/Batch)로 헤더 또는 payload.id → MDC(traceId/orderId) 복원/강제 세팅** |
| 리스너 | OrderLocalMessageListenerImpl, OrderApiMessageListenerImpl, OrderCrudMessageListenerImpl | Kafka 수신, 수동 Ack, 오류 로그 |
| 파사드 | OrderLocalMessageFacadeImpl, OrderApiMessageFacadeImpl, OrderCrudMessageFacadeImpl | 메시지 검증·변환·오케스트레이션·DLQ 분기 |
| 서비스 | KafkaProducerServiceImpl, OrderWebClientServiceImpl, OrderCrudServiceImpl, OrderServiceImpl | Kafka 발행, WebClient 연동, DB 벌크, 메서드 타입 분기 |
| S3 동기화 | S3LogSyncServiceImpl, ApplicationStartupHandlerImpl, ApplicationShutdownHandlerImpl, S3LogSyncSchedulerImpl | Pod 로그 S3 업로드, 기동/종료/주기 처리 (!local 프로파일) |
| 예외/코드 | WorkerExceptionCode, DatabaseExecuteException | 표준 오류 코드, 실패 시나리오 표현 |

메시지 카테고리:
- ORDER_LOCAL
- ORDER_API
- ORDER_CRUD
- ORDER_REMOTE
- ORDER_DLQ

> **MDC 트레이싱 한눈에 보기**
> - **프로듀서측(API 계열 모듈)**: `order-api-common` 의 `CommonKafkaProducerAutoConfiguration` 이 `MdcToHeaderProducerInterceptor` 를 자동 주입 → **MDC(traceId/orderId) → Kafka 헤더** 주입
> - **컨슈머측(워커)**: 본 모듈의 `KafkaMdcInterceptorConfig` 가 `RecordInterceptor/BatchInterceptor` 를 모든 리스너 컨테이너 팩토리에 적용 → **Kafka 헤더 또는 ORDER_API payload.id → MDC 복원/강제 세팅**

---

## 2) 동작 흐름(요약)

    KafkaListener (Local/API/CRUD)
      → Facade (검증/변환/오케스트레이션)
         → Service (외부콜/DB/발행)
            → 실패 시 즉시 DLQ 전송

원칙:
- Listener: try-catch-logging-finally-acknowledge (수동 Ack로 at-least-once)
- Facade: 예외 시 즉시 DLQ 전송 후 재던짐
- Service: 도메인/외부 시스템 예외 로깅 후 상위 전파
- **MDC 보장**: 수신 직전 `Record/BatchInterceptor` 가 **traceId/orderId** 를 MDC에 세팅 → 파사드/서비스 로그에 동일 추적키 노출

---

## 3) 구성/조립

OrderWorkerApplication

    @SpringBootApplication
    @EnableScheduling
    @Import(OrderWorkerConfig.class)
    public class OrderWorkerApplication {
        public static void main(String[] args) {
            SpringApplication.run(OrderWorkerApplication.class);
        }

        @PostConstruct
        private void setTimeZone() {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        }
    }

OrderWorkerConfig

    @Configuration
    @Import({
        OrderCoreConfig.class,         // 코어 인프라(도메인/JPA/락/레디스 등)
        KafkaModuleConfig.class,       // Kafka 클라이언트 모듈
        S3ModuleConfig.class,          // S3 클라이언트 모듈
        WebClientModuleConfig.class    // WebClient 모듈
    })
    @ComponentScan(basePackages = {
        "org.example.order.worker.config",
        "org.example.order.worker.service",
        "org.example.order.worker.facade",
        "org.example.order.worker.controller",
        "org.example.order.worker.listener",
        "org.example.order.worker.lifecycle"
    })
    public class OrderWorkerConfig {
        @Bean
        @ConditionalOnMissingBean(ObjectMapper.class)
        ObjectMapper objectMapper() {
            return ObjectMapperFactory.defaultObjectMapper();
        }
    }

KafkaListenerTopicConfig (MessageCategory → 토픽명 Bean)

    @Configuration
    public class KafkaListenerTopicConfig {
        @Bean public String orderLocalTopic (KafkaTopicProperties p){ return p.getName(MessageCategory.ORDER_LOCAL); }
        @Bean public String orderApiTopic   (KafkaTopicProperties p){ return p.getName(MessageCategory.ORDER_API); }
        @Bean public String orderCrudTopic  (KafkaTopicProperties p){ return p.getName(MessageCategory.ORDER_CRUD); }
        @Bean public String orderRemoteTopic(KafkaTopicProperties p){ return p.getName(MessageCategory.ORDER_REMOTE); }
    }

CustomSchedulerConfig (스케줄링 스레드풀)

    @Configuration
    @EnableAsync
    @EnableScheduling
    public class CustomSchedulerConfig {
        @Bean
        public ThreadPoolTaskScheduler threadPoolTaskScheduler() {
            var s = new ThreadPoolTaskScheduler();
            s.setPoolSize(2);
            s.setWaitForTasksToCompleteOnShutdown(true);
            s.setRemoveOnCancelPolicy(true);
            s.initialize();
            return s;
        }
        @Bean
        public SchedulingConfigurer schedulingConfigurer() {
            return taskRegistrar -> taskRegistrar.setScheduler(threadPoolTaskScheduler());
        }
    }

**KafkaMdcInterceptorConfig (컨슈머측 MDC 복원/강제 세팅)**

- 위치: `org.example.order.worker.config.KafkaMdcInterceptorConfig`
- 역할: 모든 `ConcurrentKafkaListenerContainerFactory` 에 아래 인터셉터를 부착
  - **RecordInterceptor**
    - `ORDER_API` 토픽이면 **payload(JSON)의 `id` 값을 읽어 `MDC["traceId"]`/`MDC["orderId"]` 강제 세팅**
    - 그 외 토픽은 **Kafka 헤더 `traceId`를 MDC로 복원**(payload 파싱 실패 시에도 헤더 fallback)
  - **BatchInterceptor**
    - 배치(예: CRUD)에서 **첫 레코드 헤더의 `traceId`를 MDC로 복원**
- 버전 호환: 일부 Spring Kafka 버전에는 `getRecordInterceptor/getBatchInterceptor` 게터가 없어 **존재 여부 확인 없이 `set*Interceptor` 를 직접 적용**해도 무해

---

## 4) 리스너 → 파사드 → 서비스

4.1 Local → Api

Listener

    @KafkaListener(topics = "#{@orderLocalTopic}", groupId = "order-order-local", concurrency = "2")
    public void orderLocal(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            facade.sendOrderApiTopic(record);
        } catch (Exception e) {
            log.error("error : order-local", e);
        } finally {
            ack.acknowledge();
        }
    }

Facade

    public void sendOrderApiTopic(ConsumerRecord<String,Object> record) {
        OrderLocalMessage message = ObjectMapperUtils.valueToObject(record.value(), OrderLocalMessage.class);
        message.validation();
        kafkaProducerService.sendToOrderApi(OrderApiMessage.toMessage(message));
    }

> **MDC 포인트**: 이 시점에서 `KafkaMdcInterceptorConfig` 가 이미 `MDC["traceId"]` 를 복원했으므로, 이후 파사드/서비스/프로듀서 로그에서 같은 traceId가 유지됩니다.

4.2 Api → Crud

Listener

    @KafkaListener(topics = "#{@orderApiTopic}", groupId = "order-order-api", concurrency = "2")
    public void orderApi(ConsumerRecord<String, Object> record, Acknowledgment ack) {
        try {
            facade.requestApi(record.value());
        } catch (Exception e) {
            log.error("error : order-api", e);
        } finally {
            ack.acknowledge();
        }
    }

Facade

    @Transactional
    public void requestApi(Object record) {
        OrderApiMessage msg = null;
        try {
            msg = ObjectMapperUtils.valueToObject(record, OrderApiMessage.class);
            OrderDto dto = webClientService.findOrderListByOrderId(msg.getId());
            dto.updatePublishedTimestamp(msg.getPublishedTimestamp());
            kafkaProducerService.sendToOrderCrud(OrderCrudMessage.toMessage(msg, dto));
        } catch (Exception e) {
            kafkaProducerService.sendToDlq(msg, e);
            throw e;
        }
    }

4.3 Crud → DB/Remote

Listener (배치)

    @KafkaListener(
        topics = "#{@orderCrudTopic}",
        groupId = "order-order-crud",
        containerFactory = "kafkaBatchListenerContainerFactory",
        concurrency = "10"
    )
    public void executeOrderCrud(List<ConsumerRecord<String, Object>> records, Acknowledgment ack) {
        try {
            facade.executeOrderCrud(records);
        } catch (Exception e) {
            log.error("error : order-crud", e);
        } finally {
            ack.acknowledge();
        }
    }

Facade (그룹 처리 + DLQ 정책)

    @Transactional
    public void executeOrderCrud(List<ConsumerRecord<String, Object>> records) {
        if (ObjectUtils.isEmpty(records)) return;

        List<OrderCrudMessage> all = records.stream()
            .map(ConsumerRecord::value)
            .map(v -> ObjectMapperUtils.valueToObject(v, OrderCrudMessage.class))
            .toList();

        List<OrderCrudMessage> failureList = new ArrayList<>();

        try {
            var grouped = groupingMessages(all); // methodType 별 그룹
            grouped.forEach((type, group) -> {
                try {
                    orderService.execute(type, group);
                    for (OrderCrudMessage m : group) {
                        var order = m.getDto().getOrder();
                        if (order.getFailure()) {
                            failureList.add(m);
                        } else {
                            var close = OrderCloseMessage.toMessage(order.getOrderId(), type);
                            kafkaProducerService.sendToOrderRemote(close);
                        }
                    }
                } catch (Exception e) {
                    kafkaProducerService.sendToDlq(group, e); // 그룹 단위 DLQ
                }
            });

            if (!failureList.isEmpty()) throw new DatabaseExecuteException(WorkerExceptionCode.MESSAGE_UPDATE_FAILED);
        } catch (DatabaseExecuteException e) {
            kafkaProducerService.sendToDlq(failureList, e); // 실패 목록 DLQ
            throw e;
        } catch (Exception e) {
            kafkaProducerService.sendToDlq(all, e); // 전체 목록 DLQ
            throw e;
        }
    }

Service (분기/벌크)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(MessageMethodType methodType, List<OrderCrudMessage> messages) {
        List<OrderDto> dtoList = messages.stream().map(OrderCrudMessage::getDto).toList();
        switch (methodType) {
            case POST   -> orderCrudService.bulkInsert(dtoList.stream().map(OrderDto::getOrder).toList());
            case PUT    -> orderCrudService.bulkUpdate(dtoList.stream().map(OrderDto::getOrder).toList());
            case DELETE -> orderCrudService.deleteAll(messages.stream().map(m -> m.getDto().getOrder()).toList());
        }
    }

KafkaProducerService (DLQ 오버로드)

    public <T extends DlqMessage> void sendToDlq(List<T> messages, Exception e) {
        if (ObjectUtils.isEmpty(messages)) return;
        for (T m : messages) sendToDlq(m, e);
    }
    public <T extends DlqMessage> void sendToDlq(T message, Exception e) {
        if (ObjectUtils.isEmpty(message)) return;
        if (e instanceof CommonException ce) message.fail(CustomErrorMessage.toMessage(ce.getCode(), ce));
        else message.fail(CustomErrorMessage.toMessage(e));
        send(message, kafkaTopicProperties.getName(MessageCategory.ORDER_DLQ));
    }

---

## 5) S3 로그 동기화

컴포넌트 활성 조건: 프로파일 `!local`

- S3LogSyncServiceImpl
  - HOSTNAME 환경변수를 파일명에 포함한 로그만 업로드 (자신 Pod 로그만)
  - 로컬 파일 MD5 vs S3 객체 MD5 비교 → 동일 시 스킵
  - S3 404(NoSuchKey)는 빈 체크섬으로 처리하여 업로드 유도
- ApplicationStartupHandlerImpl (SmartLifecycle)
  - start 시 onStartup → 로그 폴더 순회 업로드
- ApplicationShutdownHandlerImpl (SmartLifecycle)
  - stop 시 onShutdown → 동일 처리
- S3LogSyncSchedulerImpl
  - fixedDelay 10초, initialDelay 10초

---

## 6) 설정(YAML) 샘플

6.1 공통

    spring:
      application:
        name: order-worker
      jackson:
        timezone: UTC
      kafka:
        bootstrap-servers: localhost:9092
        consumer:
          group-id: order-worker
          auto-offset-reset: earliest
          enable-auto-commit: false
          key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
          value-deserializer: org.apache.kafka.common.serialization.StringDeserializer
        listener:
          ack-mode: manual

    logging:
      level:
        org.example.order: INFO

    client:
      web:
        url:
          base: http://localhost:8080
          order: /api/orders/{id}
        client-id: order-worker

    s3:
      bucket: my-bucket
      default-folder: logs/order-worker

    worker:
      log:
        directory: logs

    kafka:
      topics:
        ORDER_LOCAL: order.local
        ORDER_API:   order.api
        ORDER_CRUD:  order.crud
        ORDER_REMOTE: order.remote
        ORDER_DLQ:   order.dlq

6.2 로컬

    spring:
      profiles:
        active: local
    logging:
      file:
        path: logs

6.3 스테이징/프로덕션

    spring:
      kafka:
        bootstrap-servers: PLAINTEXT://kafka-broker:9092

    client:
      web:
        url:
          base: https://api.example.com
          order: /api/orders/{id}
        client-id: order-worker

    s3:
      bucket: corp-prod-logs
      default-folder: order-worker

---

## 7) 확장 가이드

7.1 카테고리/토픽 추가
- MessageCategory에 새 항목 추가
- KafkaTopicProperties에 매핑 추가
- KafkaListenerTopicConfig에 String Bean 추가
- 새 Listener/Facade/Service 구현
  - Listener: @KafkaListener(topics = "#{@newTopic}", groupId, concurrency 지정)
  - Facade: 변환·검증·DLQ 정책 재사용
  - Service: 도메인 로직 캡슐화

7.2 DLQ 정책 커스터마이징
- KafkaProducerServiceImpl의 단건/목록 오버로드 유지
- 환경별로 ORDER_DLQ 토픽명 분리 시 KafkaTopicProperties를 프로파일별로 오버라이드

7.3 성능/동시성
- Listener concurrency를 트래픽에 맞게 조정
- CustomSchedulerConfig 풀 사이즈 조정
- OrderServiceImpl는 REQUIRES_NEW로 상위 트랜잭션과 격리 커밋

7.4 ObjectMapper 교체
- OrderWorkerConfig의 @ConditionalOnMissingBean 덕에 외부에서 Bean 제공 시 자동 교체

7.5 WebClient 타임아웃/리트라이
- WebClientModuleConfig에서 공통 정책 정의
- Facade 레벨에서 실패 시 즉시 DLQ 전환 유지

---

## 8) 테스트 가이드

8.1 단위 테스트
- Facade/Service를 Mockito로 검증
- DTO 세터가 없으면 팩토리 메서드 또는 mock getter 스텁 사용
- DLQ 오버로드 모호성은 구체 타입 매처(any(DlqMessage.class) / anyList())로 분리

8.2 임베디드 Kafka 통합 테스트(권장)

EmbeddedKafka 베이스

    @EmbeddedKafka(
      topics = {"ORDER_LOCAL"},
      partitions = 1,
      brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"}
    )
    public abstract class EmbeddedKafkaITBase {
        @DynamicPropertySource
        static void kafkaProps(DynamicPropertyRegistry r) {
            r.add("spring.kafka.bootstrap-servers",
                  () -> System.getProperty("spring.embedded.kafka.brokers"));
            r.add("spring.kafka.producer.key-serializer",
                  () -> "org.apache.kafka.common.serialization.StringSerializer");
            r.add("spring.kafka.producer.value-serializer",
                  () -> "org.apache.kafka.common.serialization.StringSerializer");
        }
        @AfterAll static void tearDown() {}
    }

Boot 파일(테스트 전용, 외부 의존 스캔 차단)

    @SpringBootConfiguration
    @EnableAutoConfiguration(excludeName = {
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration",
        "org.redisson.spring.starter.RedissonAutoConfigurationV2",
        "org.redisson.spring.starter.RedissonReactiveAutoConfigurationV2"
    })
    public class IntegrationBoot { }

카프카 IT 예시

    @SpringBootTest(classes = IntegrationBoot.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
    @DirtiesContext
    class KafkaProducerServiceEmbeddedKafkaIT extends EmbeddedKafkaITBase {

        @Autowired KafkaTemplate<String,String> kafkaTemplate;

        @Test
        void 메시지_발행_수신() {
            String topic = "ORDER_LOCAL";
            String key = "k1";
            String payload = "hello-order";

            kafkaTemplate.send(topic, key, payload);
            kafkaTemplate.flush();

            Map<String,Object> props = new HashMap<>();
            props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getProperty("spring.embedded.kafka.brokers"));
            props.put(ConsumerConfig.GROUP_ID_CONFIG, "it-consumer-group");
            props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
            props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

            try (var consumer = new DefaultKafkaConsumerFactory<String,String>(props).createConsumer()) {
                consumer.subscribe(List.of(topic));
                var records = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10));
                assertThat(records.isEmpty()).isFalse();
            }
        }
    }

> 주의: 통합 테스트에서 컴포넌트 스캔을 크게 열면 `KafkaProducerCluster` 와 같은 외부 빈을 요구해 `NoSuchBeanDefinitionException` 이 발생할 수 있습니다. 위처럼 **오토컨피그만 사용하고 컴포넌트 스캔을 비우는 Boot** 구성으로 해결하세요.

---

## 9) 운영 팁

- Ack 및 재처리: 수동 Ack로 at-least-once. 멱등 처리(업서트/키) 권장.
- 모니터링: DLQ 토픽, 실패율, 처리 지연, 스케줄러 상태 점검.
- 로그: 성공/실패 이벤트 구조적 로깅. S3 업로드 키 로그로 추적성 확보.
- 프로파일: `!local` 에서만 S3 동기화/라이프사이클 훅 활성.
- **MDC 일관성**: API 계열 모듈의 프로듀서 인터셉터(`MdcToHeaderProducerInterceptor`)와 워커의 컨슈머 인터셉터(`KafkaMdcInterceptorConfig`)가 **쌍으로 동작**해야 가장 깔끔한 추적 값을 남깁니다.

---

## 10) 예외 코드

- WorkerExceptionCode: EMPTY_PAYLOAD, EMPTY_MESSAGE, MESSAGE_TRANSMISSION_FAILED, MESSAGE_POLLING_FAILED, MESSAGE_GROUPING_FAILED, MESSAGE_UPDATE_FAILED, POLLING_FAILED, UNSUPPORTED_EVENT_CATEGORY, NOT_FOUND_LOCAL_RESOURCE
- DatabaseExecuteException: CommonException 상속, 실패 목록 누적 후 시그널링

---

## 11) 한 줄 요약

카테고리 기반 토픽 주입, 파사드 중심 오류 격리, 일관된 DLQ, S3 로그 동기화에 더해  
**프로듀서/컨슈머 인터셉터로 강화된 MDC 추적성**까지 갖춘 **Kafka 워커**입니다.  
YAML 설정만으로 환경 전환이 가능하며, Listener · Facade · Service 레이어로 안전하게 확장하세요.
