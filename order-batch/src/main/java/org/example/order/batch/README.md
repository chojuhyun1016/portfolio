# 🧰 order-batch 서비스 README (Spring Batch · DLQ 재처리 · S3 업로드 · Secrets/Crypto 키 시딩 · 운영 가이드)

Spring Boot 기반 배치 모듈입니다.  
Kafka DLQ(Dead Letter) 재처리, S3 로그 동기화(시작/종료 훅), Secrets/Crypto 키 시딩(AES128/AES256/AESGCM/HMAC_SHA256)까지 운영에 필요한 경로를 포함합니다.  
실행 후 종료되는 단발성 배치이며, **잡 상태 → 프로세스 종료코드 매핑(성공=0, 그 외=1)** 을 지원합니다.

---

## 1) 모듈 조립/오토구성

- **핵심 오토구성:** `OrderBatchConfig` (현재 코드 반영)
  - Import: `OrderCoreConfig`, `WebAutoConfiguration`, `TsidInfraConfig`
  - ImportAutoConfiguration: `S3AutoConfiguration`, `KafkaAutoConfiguration`, `CacheAutoConfiguration`, `ApplicationAutoConfiguration`
  - ComponentScan: `config/crypto/facade/job/lifecycle/service`
  - Properties: `BatchProperties`, `AppCryptoKeyProperties`
  - ObjectMapper 기본 빈 제공(`ObjectMapperFactory`) — `@ConditionalOnMissingBean`

~~~java
@Configuration
@Import({
        OrderCoreConfig.class,
        WebAutoConfiguration.class,
        TsidInfraConfig.class
})
@ImportAutoConfiguration({
        S3AutoConfiguration.class,
        KafkaAutoConfiguration.class,
        CacheAutoConfiguration.class,
        ApplicationAutoConfiguration.class
})
@ComponentScan(basePackages = {
        "org.example.order.batch.config",
        "org.example.order.batch.crypto",
        "org.example.order.batch.facade",
        "org.example.order.batch.job",
        "org.example.order.batch.lifecycle",
        "org.example.order.batch.service"
})
@EnableConfigurationProperties({
        BatchProperties.class,
        AppCryptoKeyProperties.class
})
@RequiredArgsConstructor
public class OrderBatchConfig {

    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper objectMapper() {
        return ObjectMapperFactory.defaultObjectMapper();
    }
}
~~~

- **배치 앱 엔트리:** `OrderBatchApplication`
  - `WebApplicationType.NONE`, 실행 후 `SpringApplication.exit(ctx)` → `System.exit(exitCode)`
  - JVM 기본 타임존: `UTC`

~~~java
@SpringBootApplication
@Import({
        OrderBatchConfig.class,
        FlywayDevLocalStrategy.class,
        OrderCoreConfig.class
})
public class OrderBatchApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(OrderBatchApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);

        ConfigurableApplicationContext ctx = app.run(args);

        int exitCode = SpringApplication.exit(ctx);

        System.exit(exitCode);
    }

    @PostConstruct
    void setTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
~~~

---

## 2) 잡 구성 (DLQ 재처리)

- 구성 클래스: `job.deadletter.OrderDeadLetterJobConfig`
  - Job 이름: `ORDER_DEAD_LETTER_JOB`
  - `RunIdIncrementer` 적용(재실행 충돌 방지), `preventRestart()`
  - Step: `ORDER_DEAD_LETTER_JOB.retry`
  - Tasklet: `facade.retry()` 수행(예외 로깅 후 전파)

~~~java
@Configuration
@RequiredArgsConstructor
@Slf4j
public class OrderDeadLetterJobConfig {

    private final OrderDeadLetterFacade facade;

    public static final String JOB_NAME = "ORDER_DEAD_LETTER_JOB";

    @Bean(name = JOB_NAME)
    public Job job(JobRepository jobRepository, Step orderDeadLetterStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(orderDeadLetterStep)
                .preventRestart()
                .build();
    }

    @Bean
    @JobScope
    public Step orderDeadLetterStep(JobRepository jobRepository,
                                    Tasklet orderDeadLetterTasklet,
                                    PlatformTransactionManager tx) {
        return new StepBuilder(JOB_NAME + ".retry", jobRepository)
                .tasklet(orderDeadLetterTasklet, tx)
                .build();
    }

    /**
     * DLQ 재처리 Tasklet
     * - 현재는 파라미터를 참조하지 않으므로 스코프는 기본.
     * - 만약 JobParametersAccessor 등을 주입해서 파라미터 사용 시 @StepScope 권장.
     */
    @Bean
    public Tasklet orderDeadLetterTasklet() {
        return (contribution, chunkContext) -> {
            log.info("OrderDeadLetterJob start");

            try {
                facade.retry();
            } catch (Exception e) {
                log.error("dead-letter job failed", e);
                throw e;
            }

            return RepeatStatus.FINISHED;
        };
    }
}
~~~

- **대안 Tasklet(파라미터 필요 시):** `job.tasklet.OrderDeadLetterRetryTasklet` (`@StepScope`)
  - 현재 코드에는 `@StepScope`가 적용되어 있으며, 필요 시 `JobParametersAccessor`를 주입해 파라미터 기반 실행으로 확장합니다.

~~~java
@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class OrderDeadLetterRetryTasklet implements Tasklet {

    private final OrderDeadLetterFacade facade;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("OrderDeadLetterJob start");

        facade.retry();

        return RepeatStatus.FINISHED;
    }
}
~~~

---

## 3) Kafka 토픽/컨슈머/프로듀서

### 3.1 토픽 이름 빈 (운영용 이름 매핑)

- `KafkaListenerTopicConfig` — `MessageOrderType` 기반 이름 주입

~~~java
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaListenerTopicConfig {

    @Bean
    public String orderLocalTopic(KafkaTopicProperties p) {
        return p.getName(MessageOrderType.ORDER_LOCAL);
    }

    @Bean
    public String orderApiTopic(KafkaTopicProperties p) {
        return p.getName(MessageOrderType.ORDER_API);
    }

    @Bean
    public String orderCrudTopic(KafkaTopicProperties p) {
        return p.getName(MessageOrderType.ORDER_CRUD);
    }

    @Bean
    public String orderRemoteTopic(KafkaTopicProperties p) {
        return p.getName(MessageOrderType.ORDER_REMOTE);
    }

    @Bean
    public String orderDlqTopic(KafkaTopicProperties p) {
        return p.getName(MessageOrderType.ORDER_DLQ);
    }

    @Bean
    public String orderAlarmTopic(KafkaTopicProperties p) {
        return p.getName(MessageOrderType.ORDER_ALARM);
    }
}
~~~

### 3.2 로컬 프로필 토픽 자동 생성/보장

- `KafkaTopicsConfig` (`@Profile("local")`)
  - `KafkaAdmin` 경로 + `ApplicationReadyEvent` 이후 `AdminClient`로 최종 보장(`app.kafka.ensure-at-startup=true`)
  - 브로커 준비 대기/재시도 포함
  - 기본 로컬 토픽: `local-order-*`

~~~java
@Configuration
@Profile("local")
@ConditionalOnProperty(prefix = "app.kafka", name = "auto-create-topics", havingValue = "true", matchIfMissing = true)
public class KafkaTopicsConfig {
    // ... (현재 코드 그대로)
}
~~~

### 3.3 DLQ 전용 ConsumerFactory (DeadLetter<?> 역직렬화)

- `KafkaDeadLetterConsumerConfig`
  - value: `DeadLetter<?>`, `JsonDeserializer`(ignoreTypeHeaders, trustedPackages 동적)
  - 활성 프로필 `local/test` → `addTrustedPackages("*")`, 그 외 운영 패키지 화이트리스트 사용
  - 주의: 현재 구현은 `System.getProperty("spring.profiles.active", "local")` 값을 사용합니다.

~~~java
@Configuration
public class KafkaDeadLetterConsumerConfig {

    private static final String CONTRACT_PACKAGE_PREFIX = "org.example.order.contract.*";

    @Bean
    @Qualifier("deadLetterConsumerFactory")
    public ConsumerFactory<String, DeadLetter<?>> deadLetterConsumerFactory() {

        Map<String, Object> props = new HashMap<>(kafkaProperties.buildConsumerProperties(null));

        StringDeserializer keyDeserializer = new StringDeserializer();
        JsonDeserializer<DeadLetter<?>> valueDeserializer =
                new JsonDeserializer<>(DeadLetter.class, objectMapper);

        valueDeserializer.ignoreTypeHeaders();
        valueDeserializer.setUseTypeMapperForKey(false);

        String activeProfile = System.getProperty("spring.profiles.active", "local");

        if ("local".equals(activeProfile) || "test".equals(activeProfile)) {
            valueDeserializer.addTrustedPackages("*");
        } else {
            valueDeserializer.addTrustedPackages(CONTRACT_PACKAGE_PREFIX);
        }

        return new DefaultKafkaConsumerFactory<>(props, keyDeserializer, valueDeserializer);
    }
}
~~~

### 3.4 DLQ 파사드 — 멀티 파티션 안전 커밋 / 최종 종료 보강

- `facade.retry.impl.OrderDeadLetterFacadeImpl`
  - `@Qualifier("deadLetterConsumerFactory")` 사용
  - 모든 파티션 assign → 커밋 오프셋 있으면 seek, 없으면 beginning
  - 한 번 `poll(Duration.ofSeconds(2))`로 들어온 레코드만 처리
  - 파티션별 처리 후 **(마지막 offset + 1) 커밋**
  - `finally`에서 누적된 오프셋을 한 번 더 `commitSync` 시도(실패해도 로깅만)
  - `consumer.close(Duration)`로 종료 타임아웃 명시
  - 헤더 정규화(`RETRY_COUNT_HEADER = x-retry-count`), 타입 안전 파싱(`MessageOrderType`)

~~~java
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDeadLetterFacadeImpl implements OrderDeadLetterFacade {

    private final OrderDeadLetterService orderDeadLetterService;

    @Qualifier("deadLetterConsumerFactory")
    private final ConsumerFactory<String, DeadLetter<?>> deadLetterConsumerFactory;

    private final KafkaTopicProperties kafkaTopicProperties;

    private static final String DEAD_LETTER_GROUP_ID = "group-order-dead-letter";
    private static final String CLIENT_SUFFIX = "dlt-client";
    private static final String RETRY_COUNT_HEADER = "x-retry-count";

    @Override
    public void retry() {
        final String topic = kafkaTopicProperties.getName(MessageOrderType.ORDER_DLQ);

        Consumer<String, DeadLetter<?>> consumer = null;
        final Map<TopicPartition, OffsetAndMetadata> lastProcessedToCommit = new HashMap<>();

        try {
            consumer = deadLetterConsumerFactory.createConsumer(DEAD_LETTER_GROUP_ID, CLIENT_SUFFIX);

            List<PartitionInfo> infos = consumer.partitionsFor(topic);

            if (infos == null || infos.isEmpty()) {
                log.info("DLQ topic has no partitions: {}", topic);
                return;
            }

            // 모든 파티션 assign
            List<TopicPartition> partitions = new ArrayList<>();

            for (PartitionInfo pi : infos) {
                partitions.add(new TopicPartition(topic, pi.partition()));
            }

            consumer.assign(partitions);

            // 커밋된 오프셋 기준으로 seek (없으면 earliest)
            Map<TopicPartition, OffsetAndMetadata> committed = consumer.committed(new HashSet<>(partitions));

            for (TopicPartition tp : partitions) {
                OffsetAndMetadata meta = committed != null ? committed.get(tp) : null;

                if (meta == null) {
                    consumer.seekToBeginning(Collections.singleton(tp));
                } else {
                    consumer.seek(tp, meta.offset());
                }
            }

            // 한 번만 폴링해서 있는 레코드 처리
            ConsumerRecords<String, DeadLetter<?>> records = consumer.poll(Duration.ofSeconds(2));

            if (records == null || records.isEmpty()) {
                log.info("DLQ empty (no records polled)");
                return;
            }

            int processed = 0;

            // 파티션별로 모아서 커밋
            for (TopicPartition tp : records.partitions()) {
                List<ConsumerRecord<String, DeadLetter<?>>> list = records.records(tp);
                long lastOffset = -1L;

                for (ConsumerRecord<String, DeadLetter<?>> r : list) {
                    try {
                        processOne(consumer, r);
                        processed++;
                        lastOffset = r.offset();
                    } catch (Exception e) {
                        log.error("dead-letter retry failed at tp={}, offset={}", tp, r.offset(), e);
                    }
                }

                // 해당 파티션에서 처리한 마지막 오프셋 + 1 로 커밋
                if (lastOffset >= 0) {
                    OffsetAndMetadata oam = new OffsetAndMetadata(lastOffset + 1);
                    lastProcessedToCommit.put(tp, oam);
                    consumer.commitSync(Collections.singletonMap(tp, oam));

                    log.info("DLQ commit tp={}, lastOffsetCommitted={}", tp, lastOffset + 1);
                }
            }

            log.info("DLQ processed count={}", processed);

        } catch (Exception e) {
            log.error("dead-letter facade error", e);
            throw new CommonException(BatchExceptionCode.POLLING_FAILED);
        } finally {
            // 지금까지 누적된 마지막 오프셋(+1)을 한 번 더 동기 커밋(실패해도 로깅만)
            if (consumer != null && !lastProcessedToCommit.isEmpty()) {
                try {
                    consumer.commitSync(lastProcessedToCommit);
                    log.info("DLQ final commitSync done for {} partition(s).", lastProcessedToCommit.size());
                } catch (Exception ex) {
                    log.warn("DLQ final commitSync failed (ignored): {}", ex.toString());
                }
            }

            // 타임아웃 있는 종료로 네트워크/코디네이터 정리 기회 제공
            if (consumer != null) {
                try {
                    consumer.close(Duration.ofSeconds(5));
                } catch (Exception ex) {
                    log.warn("DLQ consumer close failed (ignored): {}", ex.toString());
                }
            }
        }
    }

    protected void processOne(Consumer<String, DeadLetter<?>> consumer,
                              ConsumerRecord<String, DeadLetter<?>> record) {
        Map<String, String> headers = extractHeaders(record.headers());
        normalizeRetryCount(headers);

        final DeadLetter<?> dlq = record.value();
        final MessageOrderType type = resolveTypeSafely(dlq.type());

        String orderId = resolveOrderId(headers);

        log.info("DLQ record tp={}-{}, offset={}, key={}, type={}, orderId={}, headers={}",
                record.topic(), record.partition(), record.offset(), record.key(), type, orderId, headers);

        switch (type) {
            case ORDER_LOCAL -> orderDeadLetterService.retryLocal(dlq, headers);
            case ORDER_API -> orderDeadLetterService.retryApi(dlq, headers);
            case ORDER_CRUD -> orderDeadLetterService.retryCrud(dlq, headers);
            default -> throw new CommonException(BatchExceptionCode.UNSUPPORTED_DLQ_TYPE);
        }
    }

    // extractHeaders / normalizeRetryCount / resolveOrderId / resolveTypeSafely : 현재 코드 동일
}
~~~

### 3.5 프로듀서 서비스

- `service.common.impl.KafkaProducerServiceImpl`
  - `KafkaProducerCluster` 이용
  - 토픽명: `KafkaTopicProperties.getName(MessageOrderType.*.name())` (현재 코드 기준)
  - `sendToLocal/Api/Crud(+headers)`, `sendToDlq(헤더 포함 오버로드)`, `sendToDiscard`(ALARM 토픽)
  - `ErrorDetail` 생성 시 스택 제한/NULL 세이프 처리

~~~java
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties({KafkaTopicProperties.class})
public class KafkaProducerServiceImpl implements KafkaProducerService {
    // ... (현재 코드 그대로)
}
~~~

---

## 4) DLQ 재처리 서비스 로직

- `service.retry.impl.OrderDeadLetterServiceImpl`
  - 입력: `DeadLetter<?>` (JsonDeserializer 경유)
  - payload를 안전 변환(Map/JsonNode → DTO)
  - 현재 재시도 카운트 `current` 계산(메타/헤더 후보키의 “유효 숫자 최대값”)
    - 메타 우선 키: `retryCount`(`PRIMARY_RETRY_KEY`)
    - 헤더 후보 키: `x-retry-count`, `retry-count`, `x_delivery_attempts`, `deliveryAttempts` 등
  - 임계치 비교는 **증가 전(`current`)** 으로 수행
    - `current >= MAX` → ALARM 토픽으로 폐기(`sendToDiscard`)
    - `current <  MAX` → 재전송, 이때만 `next=current+1` 로 메타/헤더 **동시 반영**
  - 타입별 임계치(현재 코드 하드코딩)
    - `LOCAL_MAX_RETRY = 5`
    - `API_MAX_RETRY   = 3`
    - `CRUD_MAX_RETRY  = 5`
  - 공통 보조기: `Bumped<T>`(증가된 DeadLetter + 헤더 맵)

~~~java
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDeadLetterServiceImpl implements OrderDeadLetterService {
    // retryLocal / retryApi / retryCrud — 현재 코드와 동일한 정책 구현
}
~~~

---

## 5) Secrets/Crypto 키 시딩

### 5.1 앱 프로퍼티 바인딩

- `config.properties.AppCryptoKeyProperties`
  - `prefix=app.crypto`, `keys[logical-name].{alias, encryptor, kid|version}`
  - `kid`가 있으면 `version`보다 우선
  - encryptor 문자열은 사람 친화적(`AES-GCM`, `aes_256` 등)도 허용 (정규화 로직은 Applier에 존재)

~~~java
@Getter
@Setter
@ConfigurationProperties(prefix = "app.crypto")
public class AppCryptoKeyProperties {

    private Map<String, Alias> keys = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class Alias {
        private String alias;      // Secrets alias
        private String encryptor;  // "AES128" | "AES256" | "AESGCM"
        private String kid;        // 우선(지정되면 version보다 우위)
        private Integer version;   // kid가 없을 때만 사용
    }
}
~~~

### 5.2 키 선택/시딩 적용기

- `crypto.selection.CryptoKeySelectionApplier`
  - `normalizeAlgorithm()`: 하이픈/언더스코어/공백/슬래시/점 제거 후 대문자 → 내부 enum(`CryptoAlgorithmType`)로 매핑  
    (`AESGCM`/`AES256`/`AES128`/`SHA256`/`SHA512`/`HMAC_SHA256`)
  - `secrets.applySelection(alias, version, kid, allowLatest)`
  - `secrets.getKey(alias)` 가져와 **표준 Base64 문자열**로 Encryptor/Signer에 시딩
  - 기본 정책: **자동 최신 금지(allowLatest=false)**, 운영 승인 시 수동 승격

~~~java
@Slf4j
@Component
@RequiredArgsConstructor
public class CryptoKeySelectionApplier {

    private final AppCryptoKeyProperties props;
    private final SecretsKeyClient secrets;
    private final EncryptorFactory factory;

    public void applyAll(boolean allowLatest) {
        for (Map.Entry<String, AppCryptoKeyProperties.Alias> e : props.getKeys().entrySet()) {
            String logicalName = e.getKey();
            AppCryptoKeyProperties.Alias a = e.getValue();
            applyOne(logicalName, a, allowLatest);
        }
    }

    private void applyOne(String logicalName, AppCryptoKeyProperties.Alias a, boolean allowLatest) {
        String alias = a.getAlias();
        String encStr = a.getEncryptor();
        String kid = a.getKid();
        Integer version = a.getVersion();

        final CryptoAlgorithmType type;

        try {
            type = normalizeAlgorithm(encStr);
        } catch (IllegalArgumentException ex) {
            log.error("[CryptoKeySelection] logicalName={} 지원하지 않는 encryptor={}", logicalName, encStr, ex);
            return;
        }

        boolean selected = secrets.applySelection(alias, version, kid, allowLatest);

        if (!selected) {
            log.warn("[CryptoKeySelection] logicalName={} alias={} 선택 실패(kid/version/최신 불가) -> 스킵",
                    logicalName, alias);
            return;
        }

        final byte[] keyBytes;

        try {
            keyBytes = secrets.getKey(alias);
        } catch (IllegalStateException ise) {
            log.warn("[CryptoKeySelection] logicalName={} alias={} 현재 키 없음 -> 스킵", logicalName, alias);
            return;
        }

        switch (type) {
            case AES128, AES256, AESGCM -> {
                Encryptor encryptor = factory.getEncryptor(type);
                encryptor.setKey(Base64Utils.encode(keyBytes));
                log.info("[CryptoKeySelection] logicalName={} alias={} type={} 키 시딩 완료", logicalName, alias, type);
            }
            case HMAC_SHA256 -> {
                Signer signer = factory.getSigner(type);
                signer.setKey(Base64Utils.encode(keyBytes));
                log.info("[CryptoKeySelection] logicalName={} alias={} type={} 키 시딩 완료", logicalName, alias, type);
            }
            case SHA256, SHA512, BCRYPT, ARGON2 -> {
                log.info("[CryptoKeySelection] logicalName={} alias={} type={} 해시/단방향 -> 시딩 스킵",
                        logicalName, alias, type);
            }
        }
    }

    private static CryptoAlgorithmType normalizeAlgorithm(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("encryptor is null");
        }

        String t = raw.trim().toUpperCase()
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "")
                .replace("/", "")
                .replace(".", "");

        Map<String, String> alias = new HashMap<>();
        alias.put("AESGCM", "AESGCM");
        alias.put("AES256", "AES256");
        alias.put("AES128", "AES128");
        alias.put("SHA256", "SHA256");
        alias.put("SHA512", "SHA512");
        alias.put("HMACSHA256", "HMAC_SHA256");

        String canonical = alias.getOrDefault(t, t);

        return CryptoAlgorithmType.valueOf(canonical);
    }
}
~~~

### 5.3 Secrets 로드 리스너

- `lifecycle.crypto.listener.CryptoKeyRefreshListener`
  - `@ConditionalOnBean(SecretsLoader, CryptoKeySelectionApplier)`
  - `onSecretKeyRefreshed` → `applier.applyAll(false)` (자동 반영 금지)

~~~java
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean({SecretsLoader.class, CryptoKeySelectionApplier.class})
public class CryptoKeyRefreshListener implements SecretKeyRefreshListener {

    private final CryptoKeySelectionApplier applier;

    @Override
    public void onSecretKeyRefreshed() {
        applier.applyAll(false);

        log.info("[Secrets] 키 리프레시 이벤트 수신(자동 적용 금지). 운영 승인 시 별도 경로에서 applyAll(true) 호출 권장.");
    }
}
~~~

---

## 6) S3 로그 동기화(시작/종료 훅)

### 6.1 시작 훅

- `lifecycle.handler.ApplicationStartupHandlerImpl`
  - 프로필: `local/dev/beta/prod`
  - 조건: `aws.s3.enabled=true` AND `@ConditionalOnBean(S3LogSyncService)`
  - 로그 디렉터리 준비(없으면 생성, 디렉터리 아님이면 스킵)
  - 기존 파일 1회 업로드(`S3LogSyncService.syncFileToS3`)
  - 키 시딩은 **SecretsLoader 초기 로드 이벤트(SecretKeyRefreshListener)** 경유로 수행됨(자동 최신 금지)
  - **SecretsLoader schedule 즉시 취소**(startup-only load)

~~~java
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(S3Properties.class)
@Profile({"local", "dev", "beta", "prod"})
@ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true")
@ConditionalOnBean(S3LogSyncService.class)
public class ApplicationStartupHandlerImpl implements ApplicationStartupHandler, SmartLifecycle {
    // ... (현재 코드 그대로)
}
~~~

### 6.2 종료 훅

- `lifecycle.handler.ApplicationShutdownHandlerImpl`
  - 프로필: `local/dev/beta/prod`
  - 조건: `aws.s3.enabled=true`
  - 종료 시점에 로그 디렉터리 파일들 업로드(성공/실패 카운트)
  - 이후 Secrets/Crypto 정리 순서(빈 있을 때만 수행)
    - `SecretsLoader.cancelSchedule()`
    - `SecretsManagerClient.close()`
    - `SecretsKeyResolver.wipeAll()`

~~~java
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(S3Properties.class)
@Profile({"local", "dev", "beta", "prod"})
@ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true")
public class ApplicationShutdownHandlerImpl implements ApplicationShutdownHandler, SmartLifecycle {
    // ... (현재 코드 그대로)
}
~~~

### 6.3 S3 동기화 서비스

- `service.synchronize.impl.S3LogSyncServiceImpl` (`@ConditionalOnProperty aws.s3.enabled=true`)
  - `@PostConstruct`: AmazonS3 존재 검증, region 필수, 버킷 생성(autoCreate), prefix placeholder(.keep) 생성 옵션
  - `syncFileToS3`: 파일을 `.upload/*.snapshot`으로 복제 후 **MD5 → ETag 비교**, 같으면 업로드 스킵
  - `HOSTNAME` 포함 파일만 업로드(다중 인스턴스 구분)

~~~java
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true")
public class S3LogSyncServiceImpl implements S3LogSyncService {
    // ... (현재 코드 그대로)
}
~~~

### 6.4 비로컬 환경 수동 실행 파사드

- `facade.synchronize.impl.S3LogSyncFacadeImpl` (`@Profile("!local")`)
  - 지정 로그 디렉터리 전체를 순회하여 `syncFileToS3` 위임

~~~java
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(S3Properties.class)
@Profile({"!local"})
public class S3LogSyncFacadeImpl implements S3LogSyncFacade {
    // ... (현재 코드 그대로)
}
~~~

---

## 7) 비동기 MDC 전파 / 배치 종료코드

### 7.1 Async MDC 전파

- **AsyncConfig** (`@EnableAsync`)
  - `ThreadPoolTaskExecutor(8/32/1000)` + `TaskDecorator`로 MDC 컨텍스트 복제/복원

~~~java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(8);
        ex.setMaxPoolSize(32);
        ex.setQueueCapacity(1000);
        ex.setThreadNamePrefix("async-");
        ex.setTaskDecorator(mdcTaskDecorator());
        ex.initialize();

        return ex;
    }

    @Bean
    public TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> context = MDC.getCopyOfContextMap();

            return () -> {
                Map<String, String> prev = MDC.getCopyOfContextMap();

                if (context != null) {
                    MDC.setContextMap(context);
                } else {
                    MDC.clear();
                }

                try {
                    runnable.run();
                } finally {
                    if (prev != null) {
                        MDC.setContextMap(prev);
                    } else {
                        MDC.clear();
                    }
                }
            };
        };
    }
}
~~~

### 7.2 잡 상태 → 프로세스 종료코드 매핑

- **BatchExitCodeConfig**
  - `JobExecutionListenerSupport.afterJob` → `COMPLETED=0`, 그 외 `=1`을 `AtomicInteger`에 기록
  - `ExitCodeGenerator` → `SpringApplication.exit(ctx)` 시 읽혀서 프로세스 종료코드 결정

~~~java
@Configuration
public class BatchExitCodeConfig {

    @Bean
    public AtomicInteger batchExitCodeHolder() {
        return new AtomicInteger(0);
    }

    @Bean
    public JobExecutionListenerSupport jobExitCodeListener(AtomicInteger batchExitCodeHolder) {
        return new JobExecutionListenerSupport() {
            @Override
            public void afterJob(JobExecution jobExecution) {
                BatchStatus status = jobExecution.getStatus();

                int code = (status == BatchStatus.COMPLETED) ? 0 : 1;

                batchExitCodeHolder.set(code);
            }
        };
    }

    @Bean
    public ExitCodeGenerator batchExitCodeGenerator(AtomicInteger batchExitCodeHolder) {
        return batchExitCodeHolder::get;
    }
}
~~~

---

## 8) 예외 코드

- **BatchExceptionCode**
  - `EMPTY_MESSAGE(6001)`
  - `MESSAGE_TRANSMISSION_FAILED(6002)`
  - `POLLING_FAILED(6003)`
  - `UNSUPPORTED_EVENT_CATEGORY(6004)`
  - `UNSUPPORTED_DLQ_TYPE(6005)`

~~~java
@Getter
public enum BatchExceptionCode implements ExceptionCodeEnum {
    EMPTY_MESSAGE(6001, "Message is empty", HttpStatus.BAD_REQUEST),
    MESSAGE_TRANSMISSION_FAILED(6002, "Message transmission failed", HttpStatus.INTERNAL_SERVER_ERROR),
    POLLING_FAILED(6003, "Message polling failed", HttpStatus.INTERNAL_SERVER_ERROR),
    UNSUPPORTED_EVENT_CATEGORY(6004, "Unsupported event category", HttpStatus.INTERNAL_SERVER_ERROR),
    UNSUPPORTED_DLQ_TYPE(6005, "Dlq Type is not unregistered", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String msg;
    private final HttpStatus httpStatus;

    BatchExceptionCode(int code, String msg, HttpStatus httpStatus) {
        this.code = code;
        this.msg = msg;
        this.httpStatus = httpStatus;
    }
}
~~~

---

## 9) 설정(YAML) 예시 (prod)

- S3/Secrets/Kafka 자동구성에 맞춘 샘플(핵심만)
- **주의:** Kafka 토픽 이름은 `KafkaTopicProperties`에 위임됩니다(프로듀서/토픽 빈에서 MessageOrderType 기반으로 조회).

~~~yaml
spring:
  config:
    activate:
      on-profile: prod
    import:
      - application-core-prod.yml
      - application-kafka-prod.yml

  batch:
    job:
      name: ${JOB_NAME:NONE}          # 실행할 배치 잡 이름 (NONE이면 자동 실행 안 함)
      enabled: true
    jdbc:
      initialize-schema: always       # 배치 메타테이블 자동 생성

aws:
  endpoint: ${AWS_ENDPOINT:}          # LocalStack 등 사용 시 지정, 실AWS면 빈 값
  region: ${AWS_REGION:ap-northeast-2}
  s3:
    enabled: true
    bucket: ${AWS_S3_BUCKET:my-bucket}
    default-folder: ${AWS_S3_DEFAULT_FOLDER:logs}
    auto-create: true
    create-prefix-placeholder: true

app:
  crypto:
    keys:
      orderAesGcm:
        alias: "order.aesgcm"
        encryptor: "AESGCM"
        kid: "key-2025-09-27"
      userPhoneAes256:
        alias: "user.phone.aes256"
        encryptor: "AES256"
        version: 2
~~~

- 참고
  - `aws.s3.enabled=true` 여야 AmazonS3 빈 생성(S3AutoConfiguration 의존)
  - endpoint 미지정 시 region 필수 (S3LogSyncServiceImpl @PostConstruct에서 fail-fast)
  - 로컬 토픽 자동 생성은 `@Profile("local")` + `app.kafka.auto-create-topics` 조건에 따릅니다.

---

## 10) 실행/종료

- 특정 잡만 실행(예: **DLQ 재처리**)

~~~bash
java -DJOB_NAME=ORDER_DEAD_LETTER_JOB -jar order-batch.jar --spring.profiles.active=prod
~~~

- 종료코드
  - `COMPLETED` → **0**
  - `FAILED` 등 → **1**
  - CI/CD/스케줄러에서 “성공/실패 분기”에 바로 활용 가능

---

## 11) 테스트 가이드

### 11.1 단위 테스트

- `OrderDeadLetterServiceImpl`
  - current 계산(메타/헤더), 임계치 분기, bump 동작 검증
  - off-by-one 방지(증가 전 current로 비교) 검증
- `CryptoKeySelectionApplier`
  - normalizeAlgorithm 매핑, applySelection 실패/성공 경로, 시딩 대상 분기(Encryptor/Signer/Hash) 검증
- `S3LogSyncServiceImpl`
  - ETag=MD5 동일 시 스킵, 스냅샷 삭제 보장, HOSTNAME 필터 동작 검증

### 11.2 통합 테스트

- EmbeddedKafka
  - DLQ에 `DeadLetter<?>` 적재 → 잡 실행 → 재발행(LOCAL/API/CRUD) 또는 폐기(ALARM) 토픽 수신 검증
- 로컬 프로필
  - `KafkaTopicsConfig`에서 `app.kafka.ensure-at-startup=true` 로 토픽 보장 검증(브로커 준비 대기 포함)

---

## 12) 한 줄 요약

운영 친화적인 **단발성 배치 플랫폼**:  
DLQ 재처리(DeadLetter<?> 직접 역직렬화), S3 로그 업로드(시작/종료 훅), Secrets/Crypto 키 시딩(자동 최신 금지), 종료코드 매핑까지 **현업 즉시 적용 가능한** 구성을 제공합니다.
