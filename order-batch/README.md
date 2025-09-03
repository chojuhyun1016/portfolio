# 🧰 order-batch 서비스 README (Spring Batch · DLQ 재처리 · S3 업로드 · 구성/확장/운영 가이드)

Spring Boot 기반 **배치 모듈**입니다.  
Kafka DLQ(Dead Letter Queue) 재처리 잡과 S3 업로드 유틸을 제공하며, 코어/클라이언트 모듈은 **설정(@Import)** 으로 조립하고, 배치 모듈 내부만 **컴포넌트 스캔**합니다.  
설정은 **YAML 중심**, 토픽명은 `MessageCategory` → `KafkaTopicProperties` 로 타입 세이프하게 주입합니다.

---

## 1) 전체 구조

| 레이어 | 주요 패키지/클래스 | 핵심 역할 |
|---|---|---|
| 조립/부트스트랩 | org.example.order.batch.config.OrderBatchConfig | 코어/클라이언트 모듈 Import, 배치 패키지 스캔, BatchProperties 바인딩, ObjectMapper 기본 빈 |
| 잡/스텝/태스크릿 | org.example.order.batch.job.OrderDeadLetterJob | DLQ 재처리 Job/Step/Tasklet 정의 |
| 파사드 | org.example.order.batch.facade.retry.impl.OrderDeadLetterFacadeImpl | Kafka DLQ 파티션 수동 소비, 오프셋 관리, 재처리 위임 |
| 서비스(공통) | FileServiceImpl, KafkaProducerServiceImpl, S3ServiceImpl | 파일→S3 업로드, 카프카 발행(DLQ/Discard 포함), S3 래퍼 |
| 서비스(DLQ) | OrderDeadLetterServiceImpl | DLQ 메시지 타입 분기(LOCAL/API/CRUD), 실패 횟수 기준 discard 판단 |
| 예외/코드 | BatchExceptionCode | 배치 전용 표준 오류 코드 |

메시지 카테고리(일부): ORDER_LOCAL, ORDER_API, ORDER_CRUD, ORDER_DLQ, ORDER_ALARM  
DLQ 재처리 타입: DlqOrderType.ORDER_LOCAL, ORDER_API, ORDER_CRUD

---

## 2) 동작 흐름(요약)

    OrderDeadLetterJob (Tasklet)
      → OrderDeadLetterFacadeImpl.retry()
         → Kafka Consumer (DLQ 토픽 파티션 0 수동 할당, 오프셋 지정)
            → poll → record.value() 전달
               → OrderDeadLetterServiceImpl.retry(...)
                  → DlqOrderType 에 따라 Producer로 재발행(sendToLocal/Api/Crud)
                  → 실패 누적이 임계값 초과 시 discard 토픽으로 전송

원칙:
- 파사드: 컨슈머 생성/오프셋 결정/루프 처리/커밋 — 핵심 경로만 수행
- 서비스: 타입 안전 변환(ObjectMapperUtils), 실패 회수 증가, Discard 임계값 체크
- 프로듀서: DLQ/Discard/일반 토픽 발행 공통 send(...) 경로 유지

---

## 3) 조립/구성

### 3.1 OrderBatchConfig

    @Configuration
    @Import({
            OrderCoreConfig.class,     // 코어 인프라(도메인/JPA/락/레디스 등)
            KafkaModuleConfig.class,   // Kafka 클라이언트 모듈
            S3ModuleConfig.class       // S3 클라이언트 모듈
    })
    @EnableConfigurationProperties(BatchProperties.class)
    @ComponentScan(basePackages = {
            "org.example.order.batch.config",
            "org.example.order.batch.application",
            "org.example.order.batch.facade",
            "org.example.order.batch.job",
            "org.example.order.batch.service"
    })
    public class OrderBatchConfig {

        @Bean
        @ConditionalOnMissingBean(ObjectMapper.class)
        ObjectMapper objectMapper() {
            return ObjectMapperFactory.defaultObjectMapper();
        }
    }

- 외부 모듈은 @Import 로만 조립하고, **외부 패키지는 스캔하지 않음**
- BatchProperties 바인딩 활성화
- ObjectMapper 는 외부 제공 시 중복 생성 방지

### 3.2 배치 잡/스텝/태스크릿

    @Configuration
    @RequiredArgsConstructor
    @Slf4j
    public class OrderDeadLetterJob {

        private final OrderDeadLetterFacade facade;
        public static final String JOB_NAME = "ORDER_DEAD_LETTER_JOB";

        // 잡 정의
        @Bean(name = JOB_NAME)
        public Job job(JobRepository jobRepository, Step orderDeadLetterStep) {
            return new JobBuilder(JOB_NAME, jobRepository)
                    .start(orderDeadLetterStep)
                    .preventRestart()
                    .build();
        }

        // 스텝 정의
        @Bean
        @JobScope
        public Step orderDeadLetterStep(JobRepository jobRepository,
                                        Tasklet orderDeadLetterTasklet,
                                        PlatformTransactionManager platformTransactionManager) {
            return new StepBuilder(String.format("%s.%s", JOB_NAME, "retry"), jobRepository)
                    .tasklet(orderDeadLetterTasklet, platformTransactionManager)
                    .build();
        }

        // 태스크릿: DLQ 재처리 실행
        @Bean
        @JobScope
        public Tasklet orderDeadLetterTasklet() {
            return (contribution, chunkContext) -> {
                log.info("OrderDeadLetterJob start");
                facade.retry();
                return RepeatStatus.FINISHED;
            };
        }
    }

---

## 4) Kafka DLQ 재처리

### 4.1 파사드: OrderDeadLetterFacadeImpl (핵심 단계)

- DLQ 토픽 조회
- 컨슈머 생성 (그룹/클라이언트 suffix)
- 파티션 0 수동 할당
- 시작 오프셋 결정(커밋 없으면 beginning)
- 전체 메시지 수 계산(end-offset - position)
- 루프: poll → 서비스에 위임 → commitSync

  @RequiredArgsConstructor
  @Slf4j
  @Component
  public class OrderDeadLetterFacadeImpl implements OrderDeadLetterFacade {

        private final OrderDeadLetterService orderDeadLetterService;
        private final ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory;
        private final KafkaTopicProperties kafkaTopicProperties;

        private static final String DEAD_LETTER_GROUP_ID = "order-order-dead-letter";
        private static final String CLIENT_SUFFIX = "dlt-client";

        @Override
        public void retry() {
            try {
                // DLQ 토픽
                String topic = kafkaTopicProperties.getName(MessageCategory.ORDER_DLQ);

                // Consumer 생성
                ConsumerFactory<String, String> factory =
                        (ConsumerFactory<String, String>) kafkaListenerContainerFactory.getConsumerFactory();
                Consumer<String, String> consumer = factory.createConsumer(DEAD_LETTER_GROUP_ID, CLIENT_SUFFIX);

                // 파티션 할당
                TopicPartition partition = new TopicPartition(topic, 0);
                Set<TopicPartition> partitions = Collections.singleton(partition);
                consumer.assign(partitions);

                // 시작 offset 지정
                Map<TopicPartition, OffsetAndMetadata> committedOffsets = consumer.committed(partitions);
                if (committedOffsets.get(partition) == null) {
                    consumer.seekToBeginning(partitions);
                } else {
                    consumer.seek(partition, committedOffsets.get(partition).offset());
                }

                // 전체 메시지 수
                long endOffset = consumer.endOffsets(partitions).get(partition);
                long currentOffset = consumer.position(partition);
                long messageCount = endOffset - currentOffset;
                long consumedCount = 0L;

                log.debug("number of messages : {}", messageCount);

                // 메시지 처리 루프
                while (consumedCount < messageCount) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(2000));
                    if (records.count() == 0) {
                        throw new CommonException(BatchExceptionCode.POLLING_FAILED);
                    }

                    for (ConsumerRecord<String, String> record : records.records(topic)) {
                        orderDeadLetterService.retry(record.value());
                    }

                    consumedCount += records.count();
                    consumer.commitSync();
                }

                consumer.close();
            } catch (Exception e) {
                log.error("error : order dead letter retry failed", e);
                throw e;
            }
        }
  }

### 4.2 서비스: OrderDeadLetterServiceImpl

핵심 포인트:
- 원본 문자열에서 DlqOrderType 필드만 추출 → 타입 분기
- 각 타입의 메시지 클래스로 역직렬화 → 실패 회수 증가
- maxFailCount 초과 시 discard, 아니면 재발행

  @RequiredArgsConstructor
  @Slf4j
  @Service
  @EnableConfigurationProperties({KafkaConsumerProperties.class})
  public class OrderDeadLetterServiceImpl implements OrderDeadLetterService {

        private final KafkaProducerService kafkaProducerService;
        private final KafkaConsumerProperties kafkaConsumerProperties;

        // DLQ 메시지 유형별 재처리
        @Override
        public void retry(Object message) {
            DlqOrderType type = ObjectMapperUtils.getFieldValueFromString(message.toString(), "type", DlqOrderType.class);
            log.info("DLQ 처리 시작 - Type: {}", type);

            switch (type) {
                case ORDER_LOCAL -> retryMessage(message, OrderLocalMessage.class, kafkaProducerService::sendToLocal);
                case ORDER_API   -> retryMessage(message, OrderApiMessage.class, kafkaProducerService::sendToOrderApi);
                case ORDER_CRUD  -> retryMessage(message, OrderCrudMessage.class, kafkaProducerService::sendToOrderCrud);
                default -> throw new CommonException(BatchExceptionCode.UNSUPPORTED_DLQ_TYPE);
            }
        }

        // 개별 메시지 재처리 로직
        private <T extends DlqMessage> void retryMessage(Object rawMessage, Class<T> clazz, java.util.function.Consumer<T> retrySender) {
            T dlqMessage = ObjectMapperUtils.valueToObject(rawMessage, clazz);
            dlqMessage.increaseFailedCount();

            if (shouldDiscard(dlqMessage)) {
                kafkaProducerService.sendToDiscard(dlqMessage);
            } else {
                retrySender.accept(dlqMessage);
            }
        }

        // 최대 실패 횟수 초과 시 discard
        private <T extends DlqMessage> boolean shouldDiscard(T message) {
            return message.discard(kafkaConsumerProperties.getOption().getMaxFailCount());
        }
  }

---

## 5) 공통 서비스

### 5.1 KafkaProducerServiceImpl (요약)

- 카테고리별 일반 발행: sendToLocal, sendToOrderApi, sendToOrderCrud
- DLQ 일괄/단건 발행: sendToDlq(List<T>, e), sendToDlq(T, e) — 예외를 메시지에 주입 후 발행
- Discard 발행: sendToDiscard(T) — 모니터링 메시지로 ALARM 토픽 전송
- 토픽명은 KafkaTopicProperties.getName(MessageCategory) 로 안전 주입, 공통 send(Object, topic) 경로 사용

  @Slf4j
  @Component
  @RequiredArgsConstructor
  @EnableConfigurationProperties({KafkaTopicProperties.class})
  public class KafkaProducerServiceImpl implements KafkaProducerService {

        private final KafkaProducerCluster cluster;
        private final KafkaTopicProperties kafkaTopicProperties;

        // 로컬 메시지 전송
        @Override
        public void sendToLocal(OrderLocalMessage message) {
            send(message, kafkaTopicProperties.getName(MessageCategory.ORDER_LOCAL));
        }

        // API 메시지 전송
        @Override
        public void sendToOrderApi(OrderApiMessage message) {
            send(message, kafkaTopicProperties.getName(MessageCategory.ORDER_API));
        }

        // CRUD 메시지 전송
        @Override
        public void sendToOrderCrud(OrderCrudMessage message) {
            send(message, kafkaTopicProperties.getName(MessageCategory.ORDER_CRUD));
        }

        // DLQ 메시지 일괄 전송
        @Override
        public <T extends DlqMessage> void sendToDlq(List<T> messages, Exception currentException) {
            if (ObjectUtils.isEmpty(messages)) {
                return;
            }
            for (T message : messages) {
                sendToDlq(message, currentException);
            }
        }

        // 폐기(discard) 토픽으로 전송
        @Override
        public <T extends DlqMessage> void sendToDiscard(T message) {
            log.info("Sending message to discard topic");
            send(MonitoringMessage.toMessage(
                            MonitoringType.ERROR,
                            MonitoringLevelCode.LEVEL_3,
                            message),
                    kafkaTopicProperties.getName(MessageCategory.ORDER_ALARM));
        }

        // DLQ 토픽으로 전송
        @Override
        public <T extends DlqMessage> void sendToDlq(T message, Exception currentException) {
            if (ObjectUtils.isEmpty(message)) {
                return;
            }
            try {
                if (currentException instanceof CommonException commonException) {
                    message.fail(CustomErrorMessage.toMessage(commonException.getCode(), commonException));
                } else {
                    message.fail(CustomErrorMessage.toMessage(currentException));
                }
                log.info("Sending message to dead letter topic");
                send(message, kafkaTopicProperties.getName(MessageCategory.ORDER_DLQ));
            } catch (Exception e) {
                log.error("error : send message to order-dead-letter failed : {}", message);
                log.error(e.getMessage(), e);
            }
        }

        // 메시지 전송 공통 처리
        private void send(Object message, String topic) {
            cluster.sendMessage(message, topic);
        }
  }

### 5.2 FileServiceImpl / S3ServiceImpl (요약)

    @Slf4j
    @Service
    @RequiredArgsConstructor
    public class FileServiceImpl implements FileService {

        private final S3Service s3Service;

        // 객체를 파일로 변환 후 S3 업로드
        @Override
        public void upload(String fileName, String suffix, Object object) {
            try {
                File file = convert(suffix, object);
                s3Service.upload(fileName, file);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        // 객체 → JSON 직렬화 → 임시 파일 변환
        private File convert(String suffix, Object object) throws IOException {
            File tempFile = File.createTempFile("tmp", suffix);
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                ObjectMapperUtils.writeValue(fos, object);
            }
            return tempFile;
        }
    }

    @Slf4j
    @Service
    @RequiredArgsConstructor
    @EnableConfigurationProperties(S3Properties.class)
    public class S3ServiceImpl implements S3Service {

        private final S3Client s3Client;
        private final S3Properties s3Properties;

        // 파일 업로드
        @Override
        public void upload(String fileName, File file) {
            try {
                s3Client.putObject(
                        s3Properties.getS3().getBucket(),
                        String.format("%s/%s", s3Properties.getS3().getDefaultFolder(), fileName),
                        file
                );
            } catch (Exception e) {
                log.error("error : fail to upload file", e);
                log.error(e.getMessage(), e);
                throw new CommonException(CommonExceptionCode.UPLOAD_FILE_TO_S3_ERROR);
            }
        }

        // 파일 읽기
        @Override
        public S3Object read(String key) {
            return s3Client.getObject(s3Properties.getS3().getBucket(), key);
        }
    }

---

## 6) 설정(YAML) — prod 프로필

현재 코드(S3Properties/S3ClientConfig/OrderBatchConfig)에 정확히 맞춘 샘플:

    spring:
      config:
        activate:
          on-profile: prod
        import:
          - application-core-prod.yml
          - application-kafka-prod.yml

      batch:
        job:
          name: ${JOB_NAME:NONE}          # 실행할 배치 잡 이름 (기본 NONE → 자동 실행 안 함)
          enabled: true                   # 애플리케이션 기동 시 Job 실행 여부
        jdbc:
          initialize-schema: always       # 배치 메타테이블 자동 생성 (RDS 초기가동/로컬 포함)

    aws:
      endpoint: ${AWS_ENDPOINT:}          # LocalStack 등 프록시 사용 시 지정, 실AWS면 빈 값
      region: ${AWS_REGION:ap-northeast-2}

      credential:
        enabled: false                    # true → access/secret 사용, false → 기본 자격증명 체인(IAM Role 등)
        accessKey: ${AWS_ACCESS_KEY:}
        secretKey: ${AWS_SECRET_KEY:}

      s3:
        enabled: true
        bucket: ${AWS_S3_BUCKET:my-bucket}
        default-folder: ${AWS_S3_DEFAULT_FOLDER:tmp}

주의:
- aws.s3.enabled=true 여야 AmazonS3/S3Client 빈 생성(@ConditionalOnProperty)
- endpoint 미지정 시 region 필수
- credential.enabled=true 시 accessKey/secretKey 필수

---

## 7) 빌드/런타임

### 7.1 빌드(버전 카탈로그 일원화)
- 모든 의존성은 gradle/libs.versions.toml 의 alias(libs.*) 로 관리
- 루트 build.gradle 의 통합 테스트 소스셋/태스크 규약을 그대로 상속
- order-batch 모듈은 라이브러리/배치 구성 유지(부트 플러그인은 루트에서 통합 관리)

### 7.2 실행
- 기본적으로 Job은 기동 시 자동 실행(enabled: true)
- 어떤 Job을 실행할지 JOB_NAME 로 지정
- 예) DLQ 재처리 잡만 실행

  java -DJOB_NAME=ORDER_DEAD_LETTER_JOB -jar order-batch.jar --spring.profiles.active=prod

- 스키마 자동 초기화 비활성화: spring.batch.jdbc.initialize-schema=never

---

## 8) 확장 가이드

### 8.1 새로운 DLQ 타입 추가
1) DlqOrderType 에 항목 추가 (예: ORDER_REMOTE)
2) 해당 타입의 메시지 클래스 정의(예: OrderRemoteMessage)
3) OrderDeadLetterServiceImpl.retry 의 switch 에 분기 추가
4) KafkaProducerService 에 대응 전송 메서드/토픽 매핑 추가

### 8.2 멀티 파티션/멀티 토픽 재처리
- OrderDeadLetterFacadeImpl 에서 TopicPartition 목록 주입/조회 → 순회 또는 병렬화
- 커밋/에러 처리 정책을 파티션 단위로 격리

### 8.3 배치 잡 추가
- Job/Step/Tasklet 클래스를 job 패키지에 추가
- @Bean(name = "JOB_NAME") 로 Job 등록, JOB_NAME 으로 실행 제어
- 공통 파사드/서비스/프로듀서 재사용

### 8.4 S3 유틸 확장
- 멱등 키(날짜/호스트/파일명) 표준화
- 체크섬 비교 후 업로드 스킵
- 실패 재시도/백오프/서킷브레이커 적용

---

## 9) 테스트 전략

### 9.1 단위 테스트
- OrderDeadLetterServiceImpl 의 타입 분기/Discard 조건 검증
- KafkaProducerServiceImpl 의 DLQ/Discard 전송 경로 검증(Mockito)

### 9.2 통합 테스트(선호)
- EmbeddedKafka 로 DLQ 토픽에 페이로드 적재 → 잡 실행 → 재발행 토픽 수신 검증
- 배치 메타테이블 초기화가 필요한 경우 테스트 프로필에서 initialize-schema=always

유의:
- 통합 테스트에서 외부 모듈 스캔을 크게 열면 외부 빈 의존도가 커져 NoSuchBeanDefinitionException 이 발생할 수 있으니, **테스트 전용 부트 구성**으로 필요한 설정/빈만 로딩하세요.

---

## 10) 예외 코드

- BatchExceptionCode
  - EMPTY_MESSAGE(6001), MESSAGE_TRANSMISSION_FAILED(6002), POLLING_FAILED(6003), UNSUPPORTED_EVENT_CATEGORY(6004), UNSUPPORTED_DLQ_TYPE(6005)
- 의미
  - POLLING_FAILED: 컨슈머 poll 결과 0건 지속 등 비정상 상태
  - UNSUPPORTED_DLQ_TYPE: 새로운 타입 미등록 시 명확한 실패

---

## 11) 한 줄 요약

**단순하고 안전한 DLQ 재처리 배치**입니다.  
설정(@Import)으로 모듈을 조립하고, 배치 내부만 스캔하여 충돌을 줄였으며, S3/카프카 유틸을 재사용 가능한 서비스로 분리했습니다. 잡/스텝/태스크릿은 최소단위로 나눠 확장이 쉽습니다.
