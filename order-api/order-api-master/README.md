# 📘 order-api-master 서비스 README (API 마스터 · 구성/확장/운영 가이드)

Spring Boot 기반 **주문 API 게이트웨이**입니다.  
HTTP 엔드포인트(`/order`)를 통해 외부 요청을 받고, 이를 **파사드(Facade) → 서비스(Service) → Kafka 프로듀서(Producer)** 계층으로 위임해 메시지를 발행합니다.  
전역 예외 처리, DTO 매핑, Kafka 토픽 발행 구조를 포함하며, `ObjectMapper`, `Validation`, `ExceptionAdvice` 를 통해 안정성을 확보합니다.

본 문서는 **설정(Setup) → 사용(Usage) → 개발(Dev) → 확장(Extend) → 테스트(Test) → REST Docs(Documentation) → 트러블슈팅(Troubleshooting) → 커맨드(Cheatsheet)** 순서로 정리되어 있습니다.

--------------------------------------------------------------------------------

## 1) 전체 구조

레이어 | 주요 클래스 | 핵심 역할
---|---|---
부트스트랩/조립 | `OrderApiMasterApplication`, `OrderApiMasterConfig` | 애플리케이션 구동, Core·Kafka 모듈 Import, ObjectMapper 제공
컨트롤러 | `OrderController` | `/order` POST API, 요청 DTO 검증, 응답 표준화
DTO | `LocalOrderRequest`, `LocalOrderResponse` | 요청/응답 구조 정의 (`MessageMethodType` 기반)
파사드 | `OrderFacade`, `OrderFacadeImpl` | Request → Command 매핑 후 Service 호출
매퍼 | `OrderRequestMapper` | API DTO → Application Command 변환
서비스 | `OrderService`, `OrderServiceImpl`, `KafkaProducerService`, `KafkaProducerServiceImpl` | Command 검증, Kafka 발행, 토픽 라우팅
예외/웹 | `MasterApiExceptionHandler` | API 모듈 전용 예외 로깅 및 표준 응답
MDC/Kafka | (자동) `MdcToHeaderProducerInterceptor` · `CommonKafkaProducerAutoConfiguration` | **Producer 발행 시 MDC(traceId/orderId) → Kafka 헤더 자동 주입**

메시지 메서드 타입:
- `POST`
- `PUT`
- `DELETE`

--------------------------------------------------------------------------------

## 2) 코드 개요

핵심 파일 요약

    // File: org/example/order/api/master/OrderApiMasterApplication.java
    @SpringBootApplication
    public class OrderApiMasterApplication {
        public static void main(String[] args) {
            SpringApplication.run(OrderApiMasterApplication.class, args);
        }
    }

    // File: org/example/order/api/master/config/OrderApiMasterConfig.java
    @Configuration
    @Import({
        OrderCoreConfig.class,   // 코어 인프라 (도메인/JPA/락/Redis 등)
        KafkaModuleConfig.class  // Kafka 클라이언트 모듈
    })
    @EnableConfigurationProperties(KafkaTopicProperties.class)
    @ComponentScan(basePackages = { "org.example.order.api.master" })
    @RequiredArgsConstructor
    public class OrderApiMasterConfig {
        @Bean
        @ConditionalOnMissingBean(ObjectMapper.class)
        ObjectMapper objectMapper() {
            return ObjectMapperFactory.defaultObjectMapper();
        }
    }

    // File: org/example/order/api/master/controller/order/OrderController.java
    @RestController
    @Validated
    @RequiredArgsConstructor
    @RequestMapping("/order")
    public class OrderController {
        private final OrderFacade facade;

        @PostMapping
        public ResponseEntity<ApiResponse<LocalOrderResponse>> sendOrderMasterMessage(
                @RequestBody @Valid LocalOrderRequest request
        ) {
            facade.sendOrderMessage(request);
            return ApiResponse.accepted(
                new LocalOrderResponse(request.orderId(), HttpStatus.ACCEPTED.name())
            );
        }
    }

    // File: org/example/order/api/master/dto/order/LocalOrderRequest.java
    public record LocalOrderRequest(
        @NotNull Long orderId,
        @NotNull MessageMethodType methodType
    ) { }

    // File: org/example/order/api/master/dto/order/LocalOrderResponse.java
    public record LocalOrderResponse(
        Long orderId,
        String status
    ) { }

    // File: org/example/order/api/master/facade/order/OrderFacade.java
    public interface OrderFacade {
        void sendOrderMessage(LocalOrderRequest request);
    }

    // File: org/example/order/api/master/facade/order/impl/OrderFacadeImpl.java
    @Component
    @RequiredArgsConstructor
    public class OrderFacadeImpl implements OrderFacade {
        private final OrderService orderService;
        private final OrderRequestMapper orderRequestMapper;

        @Override
        public void sendOrderMessage(LocalOrderRequest request) {
            var command = orderRequestMapper.toCommand(request);
            orderService.sendMessage(command);
        }
    }

    // File: org/example/order/api/master/mapper/order/OrderRequestMapper.java
    @Component
    public class OrderRequestMapper {
        public LocalOrderCommand toCommand(LocalOrderRequest req) {
            if (req == null) return null;
            return new LocalOrderCommand(req.orderId(), req.methodType());
        }
    }

    // File: org/example/order/api/master/service/common/KafkaProducerService.java
    public interface KafkaProducerService {
        void sendToOrder(OrderLocalMessage message);
    }

    // File: org/example/order/api/master/service/common/impl/KafkaProducerServiceImpl.java
    @Component
    @RequiredArgsConstructor
    public class KafkaProducerServiceImpl implements KafkaProducerService {
        private final KafkaProducerCluster cluster;
        private final KafkaTopicProperties kafkaTopicProperties;

        @Override
        public void sendToOrder(OrderLocalMessage message) {
            cluster.sendMessage(
                message,
                kafkaTopicProperties.getName(MessageCategory.ORDER_LOCAL)
            );
        }
    }

    // File: org/example/order/api/master/service/order/OrderService.java
    public interface OrderService {
        void sendMessage(LocalOrderCommand command);
    }

    // File: org/example/order/api/master/service/order/impl/OrderServiceImpl.java
    @Service
    @RequiredArgsConstructor
    public class OrderServiceImpl implements OrderService {
        private final KafkaProducerService kafkaProducerService;
        private final OrderMapper orderMapper;

        @Override
        public void sendMessage(LocalOrderCommand command) {
            final OrderLocalMessage message = orderMapper.toOrderLocalMessage(command);
            message.validation();
            kafkaProducerService.sendToOrder(message);
        }
    }

    // File: org/example/order/api/master/web/advice/MasterApiExceptionHandler.java
    @RestControllerAdvice(basePackages = "org.example.order.api.master")
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public class MasterApiExceptionHandler {
        @ExceptionHandler(CommonException.class)
        public ResponseEntity<ApiResponse<Void>> handleCommonForMaster(CommonException e) {
            return ApiResponse.error(e);
        }
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleUnknownForMaster(Exception e) {
            return ApiResponse.error(CommonExceptionCode.UNKNOWN_SERVER_ERROR);
        }
    }

> ⚙️ **MDC → Kafka 헤더 자동 주입(Producer)**  
> 이 모듈은 `order-common` 의 `MdcToHeaderProducerInterceptor` 와 `order-api-common` 의 `CommonKafkaProducerAutoConfiguration`(AutoConfiguration) 적용으로,  
> **Kafka Producer 발행 시 MDC의 `traceId`/`orderId`를 Kafka 헤더에 자동 주입**합니다(코드 수정 불필요).  
> 서비스 코드에서는 그대로 `KafkaProducerCluster#sendMessage(...)` 만 호출하면 됩니다.

--------------------------------------------------------------------------------

## 3) 설정(Setup)

### 3.1 Gradle 태스크(예시 · 멀티모듈 환경 기준)

- 전체 빌드 및 단위 테스트 실행  
  `./gradlew clean build test`

- 통합 테스트 전용 태스크가 정의된 경우(모듈 스크립트 설정에 따라 존재)  
  `./gradlew :order-api:order-api-master:integrationTest`

- REST Docs 전용 테스트 태스크가 정의된 경우(예: `rest`)  
  `./gradlew :order-api:order-api-master:rest`  
  → 프로젝트 `build.gradle` 에서 `@Tag("restdocs")` 테스트만 선택 실행 + 스니펫 생성

- Asciidoctor HTML 변환 (스니펫 포함)  
  `./gradlew :order-api:order-api-master:asciidoctor`

- 빌드 아티팩트 생성 (REST Docs 포함)  
  `./gradlew :order-api:order-api-master:build`

**산출물 경로(기본값)**
- 스니펫: `order-api-master/build/generated-snippets/`
- Asciidoctor HTML: `order-api-master/build/docs/asciidoc/`
- `bootJar` 포함 시 정적 리소스: `order-api-master/build/libs/` 내 JAR의 `/static/docs/` (스크립트 설정에 따름)

### 3.2 전체 환경설정(YAML) — local 프로파일 분할 포함

#### 3.2.1 `application.yml`

    spring:
      config:
        activate:
          on-profile: local
        import:
          - classpath:config/local/server.yml
          - classpath:config/local/datasource.yml
          - classpath:config/local/jpa.yml
          - classpath:config/local/flyway.yml
          - classpath:config/local/redis.yml
          - classpath:config/local/web.yml
          - classpath:config/local/aws.yml
          - classpath:config/local/dynamodb.yml
          - classpath:config/local/lock.yml
          - classpath:config/local/crypto.yml
          - classpath:config/local/kafka.yml
          - classpath:config/local/logging.yml

    server:
      port: 18081

#### 3.2.2 `config/local/aws.yml`

    aws:
      region: ap-northeast-2
      endpoint: http://localhost:4566

      credential:
        enabled: false
        access-key: local
        secret-key: local

      s3:
        enabled: false
        bucket: my-local-bucket
        default-folder: logs
        auto-create: true
        create-prefix-placeholder: true

      secrets-manager:
        enabled: false
        secret-name: myapp/secret-key
        scheduler-enabled: false
        refresh-interval-millis: 300000
        fail-fast: true

#### 3.2.3 `config/local/crypto.yml`

    crypto:
      enabled: false
      props:
        seed: false

    app:
      crypto:
        keys:
          orderAesGcm:
            alias: "order.aesgcm"
            encryptor: "AES-GCM"
            kid: "key-2025-09-27"   # Secrets JSON에서 kid=key-2025-09-27 사용
          orderAes256:
            alias: "order.aes256"
            encryptor: "AES-256"
            version: 3              # kid 대신 version=3 사용
          orderAes128:
            alias: "order.aes128"
            encryptor: "AES-128"
            version: 2              # 최신 버전 2 사용
          orderHmac:
            alias: "order.hmac"
            encryptor: "HMAC_SHA256"
            kid: "key-2025-01-10"   # 최신 kid로 핀

#### 3.2.4 `config/local/datasource.yml`

    spring:
      datasource:
        url: jdbc:mysql://localhost:3306/order_local?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false
        username: order
        password: order1234
        driver-class-name: com.mysql.cj.jdbc.Driver
        hikari:
          connection-timeout: 3000         # ms
          max-lifetime: 58000              # ms (테스트 환경 짧게)
          maximum-pool-size: 16
          auto-commit: false
          data-source-properties:
            connectTimeout: 3000           # ms
            socketTimeout: 60000           # ms
            useUnicode: true
            characterEncoding: utf-8
            rewriteBatchedStatements: true # JDBC 대량 insert 최적화

      sql:
        init:
          mode: never

#### 3.2.5 `config/local/dynamodb.yml`

    # DynamoDB 로컬 설정 (local 프로파일 전용)
    # - dynamodb.enabled=true 일 때만 모듈 동작
    # - endpoint 지정 시: LocalStack 등 로컬 엔드포인트 사용
    # - region: 수동/자동 모두에서 사용 가능. 로컬은 아무 리전이어도 무방
    # - accessKey/secretKey: LocalStack이면 dummy 값으로 사용해도 됨
    # - tableName: 사용할 테이블 이름 (존재하지 않으면 local 프로파일에서만 자동 생성)
    # - auto-create: 로컬 + true일 때만 테이블 생성/시드
    # - migration-location: 마이그레이션 리소스 위치(테이블 생성)
    # - seed-location: 시드 리소스 위치(데이터 생성)
    # - create-missing-gsi: 기존 테이블이 있을 경우, 누락된 GSI만 UpdateTable로 추가(LSI는 생성 시점에만 가능)
    # - 권장: 로컬에서는 안전 변경(누락 GSI 추가)만 자동 반영, 위험 변경(PK/LSI/GSI 키·프로젝션 변경)은 DRY-RUN 유지
    dynamodb:
      enabled: false
      endpoint: "http://localhost:4566"
      region: "ap-northeast-2"   # 로컬은 임의 리전 가능(예: us-east-1 / ap-northeast-2)
      access-key: "local"
      secret-key: "local"
      table-name: "order_dynamo"

      auto-create: false                          # 로컬에서만 테이블/인덱스 생성 + 시드
      migration-location: classpath:dynamodb/migration
      seed-location: classpath:dynamodb/seed
      create-missing-gsi: true                   # 누락된 GSI는 안전 변경으로 자동 추가

      schema-reconcile:
        enabled: true                            # 드리프트 감지/조정 사용(로컬 전용 권장)
        dry-run: true                            # 기본 DRY-RUN(위험 변경은 로그만 출력)
        allow-destructive: false                 # 파괴적 재생성(드롭&리크리에이트) 금지
        delete-extra-gsi: false                  # 마이그레이션에 없는 GSI 자동 삭제 금지(명시적 전환 권장)
        copy-data: false                         # 재생성 시 데이터 복사 비활성(실험 시에만 켜기)
        max-item-count: 10000                    # copy-data=true일 때 복사 허용 상한

      # ===== [선택] 로컬 실험용 토글(필요 시 주석 해제 후 사용) =====
      #  - 위험 변경을 실제 반영해야 하는 실험에서만 잠시 켜고, 테스트 후 반드시 되돌리세요.
      # schema-reconcile:
      #   enabled: true
      #   dry-run: false                          # 실제 변경 수행
      #   allow-destructive: true                 # 테이블 재빌드 허용(데이터 유실 주의)
      #   copy-data: true                         # 재빌드시 임시 테이블 경유 데이터 복사
      #   max-item-count: 100000                  # 복사 상한 증설(테스트 용량에 맞게)
      #   delete-extra-gsi: true                  # 여분 GSI 제거(실험에서만 권장)

#### 3.2.6 `config/local/flyway.yml`

    spring:
      flyway:
        enabled: true
        locations: classpath:db/migration
        baseline-on-migrate: true
        validate-on-migrate: true
        out-of-order: false
        connect-retries: 10
        clean-disabled: false   # 로컬만 허용 (운영/베타/개발은 true 권장)

#### 3.2.7 `config/local/jpa.yml`

    spring:
      jpa:
        hibernate:
          ddl-auto: none      # local
          # ddl-auto: validate  # dev, beta, prod
        open-in-view: false
        properties:
          hibernate:
            # Hibernate 6은 dialect 자동선택 가능(명시 제거 권장). 필요한 경우만 지정.
            # dialect: org.hibernate.dialect.MySQLDialect

            # 저장소/쿼리 성능 및 가독
            highlight_sql: true
            use_sql_comments: true
            show_sql: true

            # 배치/정렬 최적화
            default_batch_fetch_size: ${chunkSize:1000}
            jdbc.batch_size: ${chunkSize:1000}
            jdbc.batch_versioned_data: true
            order_inserts: true
            order_updates: true

            # 트랜잭션/타임존
            connection.provider_disables_autocommit: true
            jdbc.time_zone: UTC
            timezone.default_storage: NORMALIZE

            # 기타
            hbm2ddl.import_files_sql_extractor: org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor
            dialect.storage_engine: innodb

    jpa:
      enabled: true

#### 3.2.8 `config/local/kafka.yml`

    # ============================================================================
    # config/local/kafka.yml
    #  - 로컬 개발/테스트용 카프카 설정
    # ============================================================================

    spring:
      kafka:
        bootstrap-servers: 127.0.0.1:29092

        admin:
          auto-create: true

    # ------------------------------------------------------------------------
    # 애플리케이션 커스텀 토글
    # ------------------------------------------------------------------------
    app:
      kafka:
        auto-create-topics: true     # @Configuration 활성
        ensure-at-startup: true      # 기동 후 AdminClient 보장 활성

    kafka:
      # ------------------------------------------------------------------------
      # SSL/SASL : 로컬에선 일반적으로 필요 없음 → 끔
      # ------------------------------------------------------------------------
      ssl:
        enabled: false
        security-protocol: SASL_SSL
        sasl-mechanism: AWS_MSK_IAM
        sasl-jaas-config: software.amazon.msk.auth.iam.IAMLoginModule required;
        sasl-client-callback-handler-class: software.amazon.msk.auth.iam.IAMClientCallbackHandler

      # ------------------------------------------------------------------------
      # 프로듀서/컨슈머 부트스트랩(참고: spring.kafka.bootstrap-servers 사용)
      #  - 아래 값은 애플 내부 팩토리 바인딩용으로 남겨두되, 실제 공통 값과 동일하게 유지
      # ------------------------------------------------------------------------
      producer:
        enabled: true
        bootstrap-servers: 127.0.0.1:29092

      consumer:
        enabled: false
        bootstrap-servers: 127.0.0.1:29092
        trusted-packages: "org.example.order.*,org.example.common.*"
        option:
          max-fail-count: 1
          max-poll-records: 1000
          fetch-max-wait-ms: 500
          fetch-max-bytes: 52428800
          max-poll-interval-ms: 300000
          idle-between-polls: 0
          auto-offset-reset: "earliest"
          enable-auto-commit: false

      # ------------------------------------------------------------------------
      # 토픽 매핑
      # ------------------------------------------------------------------------
      topic:
        - category: ORDER_LOCAL
          name: "local-order-local"

#### 3.2.9 `config/local/lock.yml`

    spring:
      autoconfigure:
        exclude:
          - org.redisson.spring.starter.RedissonAutoConfigurationV2

    lock:
      redisson:
        enabled: false
        host: localhost
        port: 6379
        database: 10
        password: order1234
        wait-time: 3000
        lease-time: 10000
        retry-interval: 150

      named:
        enabled: false
        wait-time: 3000
        retry-interval: 150

#### 3.2.10 `config/local/logging.yml`  (요청하신 레벨 반영)

    logging:
      file:
        path: ./logs
      level:
        org.example: info
        org.hibernate.orm.jdbc.bind: info

#### 3.2.11 `config/local/redis.yml`

    spring:
      redis:
        # Redis 사용 여부 토글
        # true  → Redis 연결을 활성화 (host/port 반드시 설정 필요)
        # false → Redis 자동 구성 비활성화 (client-type=none 권장)
        enabled: false
        host: localhost
        port: 6379
        database: 0
        password: order1234
        trusted-package: org.example.order
        command-timeout-seconds: 3
        shutdown-timeout-seconds: 3
        pool-max-active: 32
        pool-max-idle: 16
        pool-min-idle: 8
        pool-max-wait: 2000

    management:
      health:
        redis:
          enabled: false

#### 3.2.12 `config/local/server.yml`

    server:
      shutdown: graceful

    spring:
      lifecycle:
        timeout-per-shutdown-phase: 30s

#### 3.2.13 `config/local/tsid.yml`

    tsid:
      enabled: false         # 반드시 true 여야 TsidFactory 빈 생성 + Holder 세팅
      node-bits: 10          # 0~1023
      zone-id: UTC           # 필요 시 조정 (미설정 시 시스템 기본)
      prefer-ec2-meta: false # 로컬에선 false 권장, EC2에선 true 권장

#### 3.2.14 `config/local/web.yml`

    spring:
      application:
        name: order-api-master

--------------------------------------------------------------------------------

## 4) 사용(Usage)

### 4.1 HTTP 요청 예시

- 엔드포인트: `POST /order`
- 요청(JSON)

        {
          "orderId": 1001,
          "methodType": "POST"
        }

- 응답(JSON · 표준 ApiResponse)

        {
          "data": {
            "orderId": 1001,
            "status": "ACCEPTED"
          },
          "success": true,
          "code": "SUCCESS",
          "message": null,
          "timestamp": "2025-09-04T02:34:56Z"
        }

### 4.2 메시지 전송 흐름

    Controller(LocalOrderRequest)
      → Facade(toCommand)
        → Service(OrderLocalMessage 변환/validation)
          → KafkaProducerService(ORDER_LOCAL 토픽으로 발행)

> 🧭 **MDC 흐름(요약)**  
> 컨트롤러~서비스 구간에서 설정된 MDC(`traceId`/`orderId`)는 **프로듀서 발행 시 자동으로 Kafka 헤더에 포함**됩니다.  
> 수신측(예: `order-worker`)에서는 Record/BatchInterceptor로 헤더를 읽어 MDC를 복원하여 **로그 상관관계를 유지**합니다.

--------------------------------------------------------------------------------

## 5) 개발(Dev)

### 5.1 DTO 확장

- `LocalOrderRequest` 에 필드 추가 시, `OrderRequestMapper` 에도 동일 필드 매핑을 반영하세요.
- `MessageMethodType`(POST, PUT, DELETE) 외 값은 허용되지 않습니다.

### 5.2 서비스 로직

- `OrderServiceImpl#sendMessage` 는 필수적으로 `message.validation()` 을 호출합니다.
- 검증 실패 시 도메인 예외를 던지고, `MasterApiExceptionHandler` 가 표준 응답으로 변환합니다.

### 5.3 Kafka 발행

- `KafkaProducerServiceImpl#sendToOrder` 는 `KafkaTopicProperties` 에 정의된 `MessageCategory.ORDER_LOCAL` 의 토픽명으로 발행합니다.
- 실제 발행은 `KafkaProducerCluster#sendMessage(Object, String)` 로 위임합니다.
- **MDC → 헤더 자동 주입**은 AutoConfiguration(`order-api-common`)과 공통 인터셉터(`order-common`)에 의해 적용됩니다.  
  서비스/파사드/컨트롤러 코드는 **별도 수정이 필요 없습니다.**

--------------------------------------------------------------------------------

## 6) 테스트(Test)

### 6.1 단위 테스트 가이드

- Controller: `@WebMvcTest(controllers = OrderController.class)` + 필요한 빈만 `@Import` 로 주입, `@MockBean` 으로 협력 객체 대체
- Facade/Service: 순수 JUnit + Mockito 로 검증
- 예시: `OrderServiceImplTest`
  - 메시지 변환, `validation()` 호출, `sendToOrder()` 가 각각 1회 호출되는지 검증

### 6.2 통합 테스트 가이드(외부 인프라 제외)

**문제 배경**  
로컬 통합 테스트에서 Redis/Redisson 또는 Security 오토컨피그가 개입되면, 실제 인프라 미기동 시 다음 오류가 발생할 수 있습니다.
- Redisson: `Unable to connect to Redis server: localhost:6379`
- Security(Management Security 포함): `HttpSecurity` 빈 미정의 등

**권장 해법**  
`@SpringBootTest` 를 사용하는 테스트 클래스에서 **테스트 컨텍스트 한정으로** 오토컨피그를 제외합니다.  
또한 Security 필터를 끄기 위해 `@AutoConfigureMockMvc(addFilters = false)` 를 사용합니다.  
Kafka 클러스터 의존 빈(`KafkaProducerCluster`)과 프로듀서 서비스(`KafkaProducerService`)는 `@MockBean` 으로 대체합니다.

- 예시: `src/integrationTest/java/org/example/order/api/master/http/OrderControllerHttpIT.java`

        @SpringBootTest(
            classes = OrderApiMasterApplication.class,
            webEnvironment = SpringBootTest.WebEnvironment.MOCK
        )
        @AutoConfigureMockMvc(addFilters = false)
        @TestPropertySource(properties = {
            "spring.main.web-application-type=servlet",
            // 테스트 컨텍스트에서만 외부 의존 오토컨피그 제외
            "spring.autoconfigure.exclude=" +
                    "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration," +
                    "org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration"
        })
        class OrderControllerHttpIT {

            @Autowired private MockMvc mvc;
            @Autowired private ObjectMapper om;

            @MockBean private KafkaProducerService kafkaProducerService;
            @MockBean private KafkaProducerCluster kafkaProducerCluster;

            @Test
            @DisplayName("HTTP 통합: /order 202 응답")
            void post_order_should_return_202() throws Exception {
                doNothing().when(kafkaProducerService).sendToOrder(any());
                var req = new LocalOrderRequest(999L, MessageMethodType.POST);

                mvc.perform(post("/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.data.orderId").value(999))
                    .andExpect(jsonPath("$.data.status").value("ACCEPTED"));
            }
        }

**포인트**
- Redisson(V2, Reactive 포함)을 “제외 대상으로 나열”하기보다, **RedisAutoConfiguration 두 가지만 제외**하면 대부분의 로컬-미기동 이슈를 해소할 수 있습니다.
- Security 자동설정들도 테스트 컨텍스트에서 제외하고, `addFilters=false` 로 MockMvc 보안 필터를 비활성화합니다(403 방지).
- 테스트에서 필요한 외부 빈은 반드시 `@MockBean` 으로 대체하세요.

### 6.3 임베디드 Kafka 통합 테스트

테스트 내에서 프로듀서/컨슈머를 **직접 생성**하여 라운드트립을 검증합니다.  
임베디드 브로커 주소는 `spring.embedded.kafka.brokers` 시스템 프로퍼티로 노출됩니다.

- 예시: `src/integrationTest/java/org/example/order/api/master/kafka/KafkaTemplateRoundTripIT.java`

        @SpringBootTest(
            classes = IntegrationBoot.class,
            webEnvironment = SpringBootTest.WebEnvironment.NONE,
            properties = "spring.main.web-application-type=none"
        )
        @TestPropertySource(properties = {
            "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
        })
        @DirtiesContext
        class KafkaTemplateRoundTripIT extends EmbeddedKafkaITBase {

            @Test
            @DisplayName("EmbeddedKafka: 수동 Producer 발행 → 수동 Consumer 수신")
            void ORDER_LOCAL_publish_and_consume() {
                String brokers = System.getProperty("spring.embedded.kafka.brokers");
                String topic   = "ORDER_LOCAL";
                String key     = "k1";
                String payload = "hello-order";

                Map<String, Object> prod = new HashMap<>();
                prod.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
                prod.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                prod.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
                var kt = new KafkaTemplate<>(new DefaultKafkaProducerFactory<String,String>(prod));
                kt.send(topic, key, payload);
                kt.flush();

                Map<String, Object> cons = new HashMap<>();
                cons.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
                cons.put(ConsumerConfig.GROUP_ID_CONFIG, "it-consumer-group");
                cons.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                cons.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
                cons.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
                cons.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

                try (var c = new DefaultKafkaConsumerFactory<String,String>(cons).createConsumer()) {
                    c.subscribe(java.util.List.of(topic));
                    var records = KafkaTestUtils.getRecords(c, Duration.ofSeconds(10));
                    assertThat(records.isEmpty()).isFalse();
                }
            }
        }

- 테스트용 Boot(외부 스캔을 막고, 필요한 오토컨피그만 허용)
  - 예: `IntegrationBoot` 클래스에서 **컴포넌트 스캔을 최소화**하고, Redis 등 불필요한 자동설정을 제외합니다.
  - `KafkaTemplateRoundTripIT` 예시는 실제 Bean 주입이 아닌 “수동 생성” 방식을 사용하므로, 스캔 부담이 적습니다.

--------------------------------------------------------------------------------

## 7) REST Docs(Documentation)

### 7.1 사전조건(Gradle 설정 체크 리스트)

- `order-api-master/build.gradle` 에 다음이 반영되어 있어야 합니다.
  - `ext { snippetsDir = file("build/generated-snippets") }`
  - `tasks.register("rest", Test) { ... systemProperty "org.springframework.restdocs.outputDir", snippetsDir ... }`
  - `asciidoctor { inputs.dir(snippetsDir) ... dependsOn tasks.named("rest") }`
  - `bootJar { dependsOn asciidoctor; from("$buildDir/docs/asciidoc/") { into "static/docs" } }`

### 7.2 스니펫 생성 절차

1) **REST Docs 전용 테스트**에 태그 부여  
   테스트 메서드 또는 클래스에 `@Tag("restdocs")` 를 추가하고, `MockMvc` 호출 결과에 `andDo(document(...))` 포함.

2) **스니펫만 실행/생성**  
   `./gradlew :order-api:order-api-master:rest`
- 실행 후 스니펫이 `order-api-master/build/generated-snippets/` 에 생성되어야 합니다.
- 생성 여부는 해당 디렉터리와 하위 폴더(예: `order-accepted/http-request.adoc`)로 확인합니다.

### 7.3 Asciidoctor 변환

- 커맨드:  
  `./gradlew :order-api:order-api-master:asciidoctor`
- 결과물:  
  `order-api-master/build/docs/asciidoc/index.html`  
  (문서에 스니펫 include 구문이 있다면, 위 7.2 과정을 선행해야 합니다.)

### 7.4 샘플 문서 스켈레톤

        = Order API Master REST Docs
        :toc: left
        :toclevels: 3
        :sectanchors:
        :source-highlighter: highlightjs
        :snippets: build/generated-snippets

        == POST /order → 202 ACCEPTED

        .Request
        include::{snippets}/order-accepted/http-request.adoc[]

        .Response
        include::{snippets}/order-accepted/http-response.adoc[]

> **FAQ (빌드 실패 흔한 원인)**
> - `403`(Forbidden) → 테스트에서 Security 필터가 켜져 있습니다. `@AutoConfigureMockMvc(addFilters = false)` 적용.
> - `metadata`·`success` 등 필드 미문서 → `responseFields(...)` 에 해당 필드를 추가(문서 스키마와 실제 응답이 일치해야 함).
> - 스니펫 폴더 없음 → `rest` 태스크 실행 선행 필요. `snippetsDir` 경로 오타 확인.

--------------------------------------------------------------------------------

## 8) 확장(Extend)

### 8.1 API 엔드포인트 추가
- 새로운 컨트롤러 추가 → 요청 DTO 정의 → 파사드/서비스에 흐름 연결
- 표준 응답(`ApiResponse`) 사용, 예외는 `MasterApiExceptionHandler` 에 위임

### 8.2 메시지/토픽 확장
- `MessageCategory` 에 새 항목 추가
- `KafkaTopicProperties` 에 매핑 추가
- `KafkaProducerServiceImpl` 에 라우팅 메서드 추가

### 8.3 ObjectMapper 커스터마이즈
- 외부에서 `ObjectMapper` Bean 을 제공하면 `@ConditionalOnMissingBean` 에 의해 기본 매퍼가 대체됩니다.

--------------------------------------------------------------------------------

## 9) 운영 가이드(Ops)

- 로그 레벨: 기능 검증 단계에서는 `org.example.order=DEBUG` 로 상세 추적, 운영은 `INFO` 권장
- 장애 전파: Service 레이어에서 도메인/외부 연동 예외를 던지고, Advice 가 표준 응답으로 변환
- 토픽 관리: 환경별 토픽명은 `KafkaTopicProperties` 를 통해 프로파일 별 YAML 로 분리 관리
- **MDC/Trace 운영 팁**
  - API 모듈에서는 **발행 시 자동으로 `traceId`/`orderId` 헤더 주입**(코드 수정 불필요)
  - 워커 모듈에서는 **수신 시 헤더/페이로드로 MDC 복원**(Record/BatchInterceptor)
  - 로그 패턴에 `%X{traceId}`·`%X{orderId}` 포함 권장

--------------------------------------------------------------------------------

## 10) 트러블슈팅(Troubleshooting)

증상 | 원인 | 해결책
---|---|---
테스트에서 Redis/Redisson 접속 오류 | 테스트 컨텍스트가 Redis 오토컨피그를 활성화 | 테스트 클래스에 `spring.autoconfigure.exclude` 로 `RedisAutoConfiguration`, `RedisRepositoriesAutoConfiguration` 제외
테스트에서 Security 관련 `HttpSecurity` 빈 예외 또는 403 | Security 오토컨피그 활성화 + MockMvc 보안 필터 작동 | 테스트 클래스에 Security 관련 오토컨피그 제외 + `@AutoConfigureMockMvc(addFilters = false)`
`KafkaProducerCluster` 빈 미존재 | 외부 Kafka 클라이언트 의존 | 테스트에서 `@MockBean` 처리
`integrationTest` 가 build 와 함께 실행됨 | Gradle 스크립트에서 `check.dependsOn(integrationTest)` | 빌드에서 분리하려면 의존 제거 또는 `-PincludeIT=true` 같은 조건부 실행
REST Docs 실패(`SnippetException`) | 응답의 모든 필드를 문서화하지 않음 | `responseFields` 에 누락된 필드(`success`, `metadata.*` 등) 추가
Asciidoctor 결과 없음 | 스니펫 미생성 또는 `onlyIf` 조건 미충족 | 먼저 `rest` 실행 후 `asciidoctor` 실행, `snippetsDir` 경로 확인
MDC 헤더가 수신측 로그에 없음 | 수신 서비스에서 MDC 복원 미구성 | 수신 서비스(order-worker)에 Record/BatchInterceptor(`KafkaMdcInterceptorConfig`) 적용 확인

--------------------------------------------------------------------------------

## 11) 커맨드 모음(Command Cheatsheet)

- 전체 빌드 + 단위 테스트  
  `./gradlew clean build test`

- 통합 테스트만 실행(모듈 스크립트에 정의)  
  `./gradlew :order-api:order-api-master:integrationTest`

- **REST Docs**: 스니펫 생성(@Tag("restdocs"))  
  `./gradlew :order-api:order-api-master:rest`

- **Asciidoctor**: 스니펫 포함하여 HTML 생성  
  `./gradlew :order-api:order-api-master:asciidoctor`

- 빌드(문서 포함)  
  `./gradlew :order-api:order-api-master:build`

- 특정 테스트만 실행  
  `./gradlew :order-api:order-api-master:test --tests "org.example.order.api.master.controller.order.*"`

--------------------------------------------------------------------------------

## 12) 한 줄 요약

API 요청을 안전하게 Kafka 메시지로 변환하는 **주문 API 게이트웨이**입니다.  
컨트롤러·파사드·서비스·프로듀서로 역할을 분리해 유지보수성과 확장성을 확보했고,  
**Producer 인터셉터(자동 구성)** 로 MDC(`traceId`/`orderId`)를 Kafka 헤더에 싱크하고,  
수신측(워커)은 인터셉터로 이를 복원해 **엔드-투-엔드 추적성**을 보장합니다.
