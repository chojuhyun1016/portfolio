# 📘 order-api-web 서비스 README (API 웹 · 구성/확장/운영 가이드)

Spring Boot 기반 **주문 단건 조회 API**입니다.  
HTTP 엔드포인트를 통해 외부 요청을 받고, 이를 **파사드(Facade) → 서비스(Service) → 리포지토리(Repository)** 계층으로 위임해 **단건 조회** 후 **표준 응답(ApiResponse)** 으로 반환합니다.  
전역 예외 처리, DTO 매핑(MapStruct), 공용 `ObjectMapper` 제공, REST Docs 파이프라인을 포함합니다.

본 문서는 **설정(Setup) → 사용(Usage) → 개발(Dev) → 확장(Extend) → 테스트(Test) → REST Docs(Documentation) → 트러블슈팅(Troubleshooting) → 커맨드(Cheatsheet)** 순서로 정리되어 있습니다.

--------------------------------------------------------------------------------

## 1) 전체 구조

레이어 | 주요 클래스/파일 | 핵심 역할
---|---|---
부트스트랩/조립 | `OrderApiWebApplication`, `OrderApiWebConfig` | 애플리케이션 구동, Core·Kafka 모듈 Import, 공용 ObjectMapper 제공
컨트롤러 | `com.example.order.api.web.controller.order.OrderController` | **POST 3종** `/order/mysql`, `/order/dynamo`, `/order/redis` API, 요청 검증/로그, 표준 응답 반환
DTO | `OrderRequest`(입력), `OrderQueryResponse`(응답) | 요청/응답 구조 정의
파사드 | `OrderFacade`, `OrderFacadeImpl` | QueryService 호출 및 응답 매핑
매퍼 | `OrderResponseMapper` (MapStruct) | Application View(`OrderView`) → API 응답 DTO 변환
서비스 | `OrderQueryService`, `OrderQueryServiceImpl` | **저장소별 조회(MySQL/Dynamo/Redis)** 및 View 투영
공통 | `KafkaProducerService`(+Impl 골격) | (옵션) Kafka 발행 추상화 골격
예외/웹 | `WebApiExceptionHandler` | 웹 모듈 전용 전역 예외 처리 (표준 응답 변환)
MDC/Kafka | (자동) `MdcToHeaderProducerInterceptor` · `CommonKafkaProducerAutoConfiguration` | **Producer 발행 시 MDC(traceId/orderId) → Kafka 헤더 자동 주입**

> 의존 방향: `adapter(api-web) → application(core) → domain` 을 엄격히 유지합니다.  
> API 레이어에서는 **애플리케이션 DTO/View만 참조**하며, 도메인 엔티티 직접 노출을 금지합니다.

--------------------------------------------------------------------------------

## 2) 코드 개요 (핵심 흐름)

### 2.1 부트스트랩/조립

    // File: src/main/java/com/example/order/api/web/OrderApiWebApplication.java
    @SpringBootApplication
    public class OrderApiWebApplication {
        public static void main(String[] args) {
            SpringApplication.run(OrderApiWebApplication.class, args);
        }
    }

    // File: src/main/java/com/example/order/api/web/config/OrderApiWebConfig.java
    @Configuration(proxyBeanMethods = false)
    @Import({ OrderCoreConfig.class })
    @ImportAutoConfiguration(KafkaAutoConfiguration.class)
    @EnableConfigurationProperties(KafkaTopicProperties.class)
    @ComponentScan(basePackages = { "com.example.order.api.web" })
    public class OrderApiWebConfig {
        @Bean
        @ConditionalOnMissingBean(ObjectMapper.class)
        ObjectMapper objectMapper() {
            return ObjectMapperFactory.defaultObjectMapper();
        }
    }

### 2.2 컨트롤러

    // File: src/main/java/com/example/order/api/web/controller/order/OrderController.java
    @Slf4j
    @Validated
    @RestController
    @RequiredArgsConstructor
    @RequestMapping("/order")
    public class OrderController {

        private final OrderFacade facade;

        @PostMapping("/mysql")
        public ResponseEntity<ApiResponse<OrderQueryResponse>> findByMySql(@RequestBody @Valid OrderRequest req,
                                                                           HttpServletRequest httpReq) {
            log.info("[OrderController][MySQL] orderId={}", req.orderId());
            return ApiResponse.ok(facade.findByMySql(req.orderId()));
        }

        @PostMapping("/dynamo")
        public ResponseEntity<ApiResponse<OrderQueryResponse>> findByDynamo(@RequestBody @Valid OrderRequest req,
                                                                            HttpServletRequest httpReq) {
            log.info("[OrderController][Dynamo] orderId={}", req.orderId());
            return ApiResponse.ok(facade.findByDynamo(req.orderId()));
        }

        @PostMapping("/redis")
        public ResponseEntity<ApiResponse<OrderQueryResponse>> findByRedis(@RequestBody @Valid OrderRequest req,
                                                                           HttpServletRequest httpReq) {
            log.info("[OrderController][Redis] orderId={}", req.orderId());
            return ApiResponse.ok(facade.findByRedis(req.orderId()));
        }
    }

### 2.3 파사드/매퍼

    // File: src/main/java/com/example/order/api/web/facade/order/OrderFacade.java
    public interface OrderFacade {
        OrderQueryResponse findByMySql(Long id);
        OrderQueryResponse findByDynamo(Long id);
        OrderQueryResponse findByRedis(Long id);
    }

    // File: src/main/java/com/example/order/api/web/facade/order/impl/OrderFacadeImpl.java
    @Component
    @RequiredArgsConstructor
    public class OrderFacadeImpl implements OrderFacade {

        private final OrderQueryService orderQueryService;
        private final OrderResponseMapper orderResponseMapper;

        @Override
        public OrderQueryResponse findByMySql(Long id) {
            var view = orderQueryService.findByMySql(new OrderQuery(id));
            return orderResponseMapper.toResponse(view);
        }

        @Override
        public OrderQueryResponse findByDynamo(Long id) {
            var view = orderQueryService.findByDynamo(new OrderQuery(id));
            return orderResponseMapper.toResponse(view);
        }

        @Override
        public OrderQueryResponse findByRedis(Long id) {
            var view = orderQueryService.findByRedis(new OrderQuery(id));
            return orderResponseMapper.toResponse(view);
        }
    }

    // File: src/main/java/com/example/order/api/web/mapper/order/OrderResponseMapper.java
    /**
     * Application 내부 View -> API 응답 DTO 매핑 (MapStruct)
     * - 계약(API) DTO는 여기서만 생성
     */
    @Mapper(
        config = AppMappingConfig.class,
        uses = {TimeMapper.class},
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
    )
    public interface OrderResponseMapper {
        OrderQueryResponse toResponse(OrderView view);
    }

### 2.4 서비스

    // File: src/main/java/com/example/order/api/web/service/order/OrderQueryService.java
    public interface OrderQueryService {
        OrderView findByMySql(OrderQuery query);
        OrderView findByDynamo(OrderQuery query);
        OrderView findByRedis(OrderQuery query);
    }

    // File: src/main/java/com/example/order/api/web/service/order/impl/OrderQueryServiceImpl.java
    @Slf4j
    @Service
    @RequiredArgsConstructor
    public class OrderQueryServiceImpl implements OrderQueryService {

        private final OrderRepository orderRepository;                 // JPA (MySQL)
        private final OrderDynamoRepository orderDynamoRepository;     // DynamoDB
        private final RedisRepository redisRepository;                 // Redis
        private final OrderMapper orderMapper;                         // Entity/Sync/View 매퍼

        @Override
        @Transactional(readOnly = true)
        public OrderView findByMySql(OrderQuery query) {
            Long id = query.orderId();
            OrderEntity entity = orderRepository.findById(id).orElseThrow(() -> {
                String msg = "Order not found in MySQL. id=" + id;
                log.warn("[OrderQueryService][MySQL] {}", msg);
                return new CommonException(CommonExceptionCode.NOT_FOUND_RESOURCE, msg);
            });
            return orderMapper.toView(entity);
        }

        @Override
        @Transactional(readOnly = true)
        public OrderView findByDynamo(OrderQuery query) {
            String id = String.valueOf(query.orderId());
            var item = orderDynamoRepository.findById(id).orElseThrow(() -> {
                String msg = "Order not found in DynamoDB. id=" + id;
                log.warn("[OrderQueryService][Dynamo] {}", msg);
                return new CommonException(CommonExceptionCode.NOT_FOUND_RESOURCE, msg);
            });

            // Dynamo → View 수동 투영(도메인 스키마에 맞게 조정)
            return OrderView.builder()
                    .id(null)
                    .userId(item.getUserId())
                    .userNumber(item.getUserNumber())
                    .orderId(item.getOrderId())
                    .orderNumber(item.getOrderNumber())
                    .orderPrice(item.getOrderPrice())
                    .deleteYn("Y".equalsIgnoreCase(item.getDeleteYn()))  // String → Boolean
                    .version(item.getVersion() == null ? null : item.getVersion().longValue())
                    .createdUserId(item.getCreatedUserId())
                    .createdUserType(item.getCreatedUserType())
                    .createdDatetime(item.getCreatedDatetime())
                    .modifiedUserId(item.getModifiedUserId())
                    .modifiedUserType(item.getModifiedUserType())
                    .modifiedDatetime(item.getModifiedDatetime())
                    .publishedTimestamp(item.getPublishedTimestamp())
                    .failure(Boolean.FALSE)
                    .build();
        }

        @Override
        @Transactional(readOnly = true)
        public OrderView findByRedis(OrderQuery query) {
            String key = "order:" + query.orderId();
            Object v = redisRepository.get(key);

            if (v == null) {
                String msg = "Order not found in Redis. key=" + key;
                log.warn("[OrderQueryService][Redis] {}", msg);
                throw new CommonException(CommonExceptionCode.NOT_FOUND_RESOURCE, msg);
            }

            if (v instanceof OrderView view) {
                return view;
            }
            if (v instanceof OrderEntity entity) {
                return orderMapper.toView(entity);
            }

            String msg = "Unsupported redis value type: " + v.getClass().getName();
            log.warn("[OrderQueryService][Redis] {}", msg);
            throw new CommonException(CommonExceptionCode.UNKNOWN_SERVER_ERROR, msg);
        }
    }

### 2.5 예외 처리 (웹 전용)

    // File: src/main/java/com/example/order/api/web/web/advice/WebApiExceptionHandler.java
    @Slf4j
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @RestControllerAdvice(basePackages = "com.example.order.api.web")
    public class WebApiExceptionHandler {

        @ExceptionHandler(CommonException.class)
        public ResponseEntity<ApiResponse<Void>> handleCommon(CommonException e) {
            log.warn("[Web] CommonException: code={}, msg={}", e.getCode(), e.getMsg());
            return ApiResponse.error(e);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e) {
            log.error("[Web] Unknown error", e);
            return ApiResponse.error(CommonExceptionCode.UNKNOWN_SERVER_ERROR);
        }
    }

--------------------------------------------------------------------------------

## 3) 설정(Setup)

### 3.1 애플리케이션 프로퍼티 (프로필/Import 포함)

    // File: src/main/resources/application.yml
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
      port: 18080

### 3.2 로컬 분할 설정 파일 (전체)

#### 3.2.1 AWS (LocalStack/옵션)

    // File: src/main/resources/config/local/aws.yml
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

#### 3.2.2 Crypto (애플리케이션 키 매핑)

    // File: src/main/resources/config/local/crypto.yml
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
            kid: "key-2025-09-27"
          orderAes256:
            alias: "order.aes256"
            encryptor: "AES-256"
            version: 3
          orderAes128:
            alias: "order.aes128"
            encryptor: "AES-128"
            version: 2
          orderHmac:
            alias: "order.hmac"
            encryptor: "HMAC_SHA256"
            kid: "key-2025-01-10"

#### 3.2.3 데이터소스 (MySQL)

    // File: src/main/resources/config/local/datasource.yml
    spring:
      datasource:
        url: jdbc:mysql://localhost:3306/order_local?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false
        username: order
        password: order1234
        driver-class-name: com.mysql.cj.jdbc.Driver
        hikari:
          connection-timeout: 3000
          max-lifetime: 58000
          maximum-pool-size: 16
          auto-commit: false
          data-source-properties:
            connectTimeout: 3000
            socketTimeout: 60000
            useUnicode: true
            characterEncoding: utf-8
            rewriteBatchedStatements: true

      sql:
        init:
          mode: never

#### 3.2.4 DynamoDB (LocalStack) — **활성화됨**

    // File: src/main/resources/config/local/dynamodb.yml
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
      enabled: true
      endpoint: "http://localhost:4566"
      region: "ap-northeast-2"
      access-key: "local"
      secret-key: "local"
      table-name: "order_dynamo"

      auto-create: false
      migration-location: classpath:dynamodb/migration
      seed-location: classpath:dynamodb/seed
      create-missing-gsi: true

      schema-reconcile:
        enabled: true
        dry-run: true
        allow-destructive: false
        delete-extra-gsi: false
        copy-data: false
        max-item-count: 10000

      # 선택 토글 (실험 목적)
      # schema-reconcile:
      #   enabled: true
      #   dry-run: false
      #   allow-destructive: true
      #   copy-data: true
      #   max-item-count: 100000
      #   delete-extra-gsi: true

#### 3.2.5 Flyway

    // File: src/main/resources/config/local/flyway.yml
    spring:
      flyway:
        enabled: true
        locations: classpath:db/migration
        baseline-on-migrate: true
        validate-on-migrate: true
        out-of-order: false
        connect-retries: 10
        clean-disabled: false   # 로컬만 허용

#### 3.2.6 JPA

    // File: src/main/resources/config/local/jpa.yml
    spring:
      jpa:
        hibernate:
          ddl-auto: none      # local
          # ddl-auto: validate  # dev, beta, prod
        open-in-view: false
        properties:
          hibernate:
            highlight_sql: true
            use_sql_comments: true
            show_sql: true
            default_batch_fetch_size: ${chunkSize:1000}
            jdbc.batch_size: ${chunkSize:1000}
            jdbc.batch_versioned_data: true
            order_inserts: true
            order_updates: true
            connection.provider_disables_autocommit: true
            jdbc.time_zone: UTC
            timezone.default_storage: NORMALIZE
            hbm2ddl.import_files_sql_extractor: org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor
            dialect.storage_engine: innodb

    jpa:
      enabled: true

#### 3.2.7 Kafka

    // File: src/main/resources/config/local/kafka.yml
    # ============================================================================
    # config/local/kafka.yml
    #  - 로컬 개발/테스트용 카프카 설정
    # ============================================================================
    spring:
      kafka:
        bootstrap-servers: 127.0.0.1:29092
        admin:
          auto-create: false

    # ------------------------------------------------------------------------
    # 애플리케이션 커스텀 토글
    # ------------------------------------------------------------------------
    app:
      kafka:
        auto-create-topics: false
        ensure-at-startup: true

    kafka:
      # SSL/SASL : 로컬에선 일반적으로 필요 없음 → 끔
      ssl:
        enabled: false
        security-protocol: SASL_SSL
        sasl-mechanism: AWS_MSK_IAM
        sasl-jaas-config: software.amazon.msk.auth.iam.IAMLoginModule required;
        sasl-client-callback-handler-class: software.amazon.msk.auth.iam.IAMClientCallbackHandler

      producer:
        enabled: false
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

#### 3.2.8 분산락(Redisson/옵션)

    // File: src/main/resources/config/local/lock.yml
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

#### 3.2.9 로깅 (레벨 갱신)

    // File: src/main/resources/config/local/logging.yml
    logging:
      file:
        path: ./logs
      level:
        org.example: info
        org.hibernate.orm.jdbc.bind: info

#### 3.2.10 Redis — **활성화됨**

    // File: src/main/resources/config/local/redis.yml
    spring:
      redis:
        # Redis 사용 여부 토글
        # true  → Redis 연결을 활성화 (host/port 반드시 설정 필요)
        # false → Redis 자동 구성 비활성화 (client-type=none 권장)
        enabled: true
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

#### 3.2.11 서버(그레이스풀 셧다운)

    // File: src/main/resources/config/local/server.yml
    server:
      shutdown: graceful

    spring:
      lifecycle:
        timeout-per-shutdown-phase: 30s

#### 3.2.12 TSID (옵션)

    // File: src/main/resources/config/local/tsid.yml
    tsid:
      enabled: false
      node-bits: 10
      zone-id: UTC
      prefer-ec2-meta: false

#### 3.2.13 웹(클라이언트/앱 식별) — **application name 포함**

    // File: src/main/resources/config/local/web.yml
    spring:
      application:
        name: order-api-web

    web:
      enabled: false
      client:
        clientId: order-api-web
        url:
          order: http://localhost:18080
      timeout:
        connectMs: 3000
        readMs: 10000
      codec:
        maxBytes: 2097152

--------------------------------------------------------------------------------

## 4) Gradle 스크립트(REST Docs + MapStruct + Querydsl)

    // File: order-api/order-api-web/build.gradle
    // - REST Docs 파이프라인, MapStruct(Processor) + Lombok-MapStruct 바인딩, Querydsl(Jakarta) 포함

    plugins {
        alias(libs.plugins.springBoot)
        alias(libs.plugins.asciidoctor)
    }

    ext { snippetsDir = file("build/generated-snippets") }

    configurations { asciidoctorExt }

    dependencies {
        // --- 프로젝트 모듈 ---
        implementation project(":order-common")
        implementation project(":order-core")
        implementation project(":order-domain")
        implementation project(":order-contracts")
        implementation project(":order-api:order-api-common")
        implementation project(":order-client:kafka")

        // --- Spring Boot 스타터 ---
        implementation(libs.springBootStarterWeb)
        implementation(libs.springBootStarterDataJpa)
        implementation(libs.springBootStarterValidation)
        implementation(libs.springBootStarterActuator)
        implementation(libs.springBootStarterSecurity)

        // --- Kafka ---
        implementation(libs.springKafka)
        implementation(libs.awsMskIamAuth)

        // --- Querydsl (Jakarta) ---
        implementation(libs.querydslCore)
        implementation("com.querydsl:querydsl-jpa:${libs.versions.querydsl.get()}:jakarta")
        annotationProcessor("com.querydsl:querydsl-apt:${libs.versions.querydsl.get()}:jakarta")

        // --- MapStruct ---
        implementation(libs.mapstruct)
        annotationProcessor(libs.mapstructProcessor)
        // Lombok-MapStruct 바인딩(컴파일러 인식 보정)
        annotationProcessor(libs.lombokMapstructBinding)

        // --- Lombok ---
        compileOnly(libs.lombok)
        annotationProcessor(libs.lombok)
        testCompileOnly(libs.lombok)
        testAnnotationProcessor(libs.lombok)

        // --- 로깅 / Jackson ---
        implementation(libs.logbackJsonClassic)
        implementation(libs.logbackJackson)
        implementation(libs.jacksonDatabind)

        // --- DB 런타임 (RDS JDBC) ---
        runtimeOnly(libs.awsMysqlJdbc)

        // --- 테스트 공통 ---
        testImplementation(libs.springBootStarterTest)
        testImplementation(libs.springSecurityTest)

        // --- REST Docs ---
        testImplementation(libs.restdocsMockmvc)
        asciidoctorExt(libs.restdocsAsciidoctor)
    }

    tasks.named("test", Test) {
        useJUnitPlatform()
        doFirst { snippetsDir.mkdirs() }
        systemProperty "org.springframework.restdocs.outputDir", snippetsDir
        outputs.dir snippetsDir
    }

    tasks.register("rest", Test) {
        description = "Run only REST Docs tests (@Tag(\"restdocs\")) and generate snippets."
        group = "verification"
        useJUnitPlatform { includeTags "restdocs" }
        testClassesDirs = sourceSets.test.output.classesDirs
        classpath = sourceSets.test.runtimeClasspath
        doFirst { snippetsDir.mkdirs() }
        systemProperty "org.springframework.restdocs.outputDir", snippetsDir
        outputs.dir snippetsDir
    }

    asciidoctor {
        configurations "asciidoctorExt"
        inputs.dir(snippetsDir).withPropertyName("snippets").optional(true)
        baseDirFollowsSourceFile()
        attributes(
                "snippets": snippetsDir,
                "doctype": "book",
                "sectanchors": true,
                "source-highlighter": "highlightjs"
        )
        dependsOn("test", "rest")
        onlyIf { snippetsDir.exists() && snippetsDir.listFiles()?.length > 0 }
    }

    tasks.named("bootJar") {
        dependsOn tasks.named("asciidoctor")
        from("${asciidoctor.outputDir}/") { into "static/docs" }
    }

> 💡 IDE에서 `@Mapper`, `ReportingPolicy` 인식 오류가 난다면 **annotationProcessor** 설정(위 스크립트)과 JDK 설정을 확인하세요.

--------------------------------------------------------------------------------

## 5) 사용(Usage)

### 5.1 HTTP 요청/응답

- 엔드포인트(POST, `Content-Type: application/json`)
  - `POST /order/mysql`
  - `POST /order/dynamo`
  - `POST /order/redis`

- 요청(JSON)

      { "orderId": 5555 }

- 성공 응답(JSON · 표준 ApiResponse · 공통 스키마)

      {
        "metadata": {
          "code": 200,
          "msg": "OK",
          "timestamp": "2025-10-24T02:34:56Z"
        },
        "data": {
          "id": 1,
          "userId": 1000,
          "userNumber": "U-0001",
          "orderId": 5555,
          "orderNumber": "O-5555",
          "orderPrice": 99000,
          "deleteYn": false,
          "version": 1,
          "createdUserId": 10,
          "createdUserType": "SYSTEM",
          "createdDatetime": "2025-10-24T02:00:00",
          "modifiedUserId": 10,
          "modifiedUserType": "SYSTEM",
          "modifiedDatetime": "2025-10-24T02:10:00",
          "publishedTimestamp": 1720000000000,
          "failure": false
        }
      }

> DynamoDB 원천의 `deleteYn` 이 `"Y"/"N"` 문자열인 경우, API 응답에서는 **Boolean** 으로 정규화되어 반환됩니다.

--------------------------------------------------------------------------------

## 6) 개발(Dev)

### 6.1 DTO/모델 매핑 정책
- API 응답 DTO(`OrderQueryResponse`)에는 **실제로 필요한 필드만** 투영합니다.
- 매퍼(`OrderResponseMapper`)는 **Application View(`OrderView`) → API DTO** 변환만 담당합니다.

### 6.2 서비스/예외
- 저장소별 미존재 시 `CommonException(NOT_FOUND_RESOURCE)` 을 던집니다.
- 모든 예외는 `WebApiExceptionHandler` 가 받아 표준 `ApiResponse.error(...)` 로 변환합니다.

### 6.3 ObjectMapper
- 공용 `ObjectMapper` 빈은 `@ConditionalOnMissingBean` 으로 제공되며, 외부에서 다른 빈을 주입하면 자동 대체됩니다.

### 6.4 MapStruct
- `order-api-web` 모듈은 **MapStruct** 를 사용합니다. `annotationProcessor` 구성과 `lombok-mapstruct-binding` 을 반드시 포함하세요.

--------------------------------------------------------------------------------

## 7) 확장(Extend)

### 7.1 API 엔드포인트 추가
- 새로운 저장소/전략(API) 추가 시:
  1) 컨트롤러 메서드 정의(POST, JSON Body)
  2) 파사드에 위임 로직 추가
  3) 서비스에 저장소별 조회 구현
  4) 응답 DTO 및 매퍼 확장

### 7.2 Kafka 활용(옵션)
- 현재 웹 모듈에는 `KafkaProducerService` 골격이 있으며, 필요 시 도메인 이벤트 발행/감사 로깅 등 확장 가능합니다.
- 토픽명은 `KafkaTopicProperties` 로 관리(코어/Kafka 모듈 설정 파일 참조).

--------------------------------------------------------------------------------

## 8) 테스트(Test)

### 8.1 컨트롤러 슬라이스 테스트(권장)

- `@WebMvcTest(controllers = OrderController.class)` + `@AutoConfigureMockMvc(addFilters = false)`
- 협력 객체(파사드)는 `@MockBean` 으로 대체
- 전역 예외 핸들러는 `@Import(WebApiExceptionHandler.class)` 로 포함

- 예시:

      @WebMvcTest(controllers = OrderController.class)
      @AutoConfigureMockMvc(addFilters = false)
      @Import(WebApiExceptionHandler.class)
      class OrderControllerHttpIT {

          @Autowired private MockMvc mvc;
          @Autowired private ObjectMapper om;
          @MockBean private OrderFacade facade;

          @Test
          @DisplayName("HTTP: POST /order/dynamo → 200 OK")
          void post_dynamo_should_return_200() throws Exception {
              var req = Map.of("orderId", 101L);
              var resp = new OrderQueryResponse( /* 필드 세팅 */ );

              when(facade.findByDynamo(101L)).thenReturn(resp);

              mvc.perform(post("/order/dynamo")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(om.writeValueAsString(req)))
                 .andExpect(status().isOk())
                 .andExpect(jsonPath("$.metadata.code").value(200));
          }
      }

### 8.2 통합 테스트 시 유의점

- 외부 인프라(보안/Redis/JPA)가 개입되면 컨텍스트 로딩 실패가 날 수 있으므로, **컨트롤러 슬라이스** 또는 **Standalone MockMvc**를 추천합니다.
- 부득이하게 `@SpringBootTest` 를 쓴다면, 테스트 컨텍스트 한정으로 불필요한 오토컨피그를 제외하세요(예: Redis/Security 등).

--------------------------------------------------------------------------------

## 9) REST Docs(Documentation)

### 9.1 스니펫 생성 → Asciidoctor 변환 → 부트 JAR 포함

1) REST Docs 전용 테스트 실행
- `./gradlew :order-api:order-api-web:rest`
- 산출물: `order-api-web/build/generated-snippets/**`

2) Asciidoctor HTML 생성
- `./gradlew :order-api:order-api-web:asciidoctor`
- 산출물: `order-api-web/build/docs/asciidoc/**.html`

3) 부트 JAR 생성(문서 포함)
- `./gradlew :order-api:order-api-web:bootJar`
- JAR 내 정적 문서 위치: `/static/docs/`

### 9.2 문서 스켈레톤(예시 · AsciiDoc)

      = Order API Web REST Docs
      :toc: left
      :toclevels: 3
      :sectanchors:
      :source-highlighter: highlightjs
      :snippets: build/generated-snippets

      == POST /order/mysql → 200 OK
      .Request
      include::{snippets}/order-post-mysql/http-request.adoc[]
      .Response
      include::{snippets}/order-post-mysql/http-response.adoc[]

      == POST /order/dynamo → 200 OK
      .Request
      include::{snippets}/order-post-dynamo/http-request.adoc[]
      .Response
      include::{snippets}/order-post-dynamo/http-response.adoc[]

      == POST /order/redis → 200 OK
      .Request
      include::{snippets}/order-post-redis/http-request.adoc[]
      .Response
      include::{snippets}/order-post-redis/http-response.adoc[]

> 실패 원인 빈출
> - 스니펫 없음 → 먼저 `rest` 또는 `test` 를 실행해 스니펫 생성 필요
> - 403(Forbidden) → 테스트에서 보안 필터 꺼야 함(`@AutoConfigureMockMvc(addFilters=false)`)
> - 응답 필드 누락 → 실제 응답(`metadata`/`data`)과 문서 필드 정의 일치 필요

--------------------------------------------------------------------------------

## 10) 트러블슈팅(Troubleshooting)

증상 | 원인 | 해결책
---|---|---
`Cannot resolve symbol 'Mapper' / 'ReportingPolicy'` | MapStruct 의존/프로세서 누락 | `implementation(libs.mapstruct)` + `annotationProcessor(libs.mapstructProcessor)` + `annotationProcessor(libs.lombokMapstructBinding)`
`No value at JSON path '$.success'` | 응답 스키마에 `success` 없음 | `metadata`/`data`로 검증 포인트 변경
DynamoDB 연결 실패 | LocalStack 미기동/엔드포인트 상이 | `dynamodb.enabled=true`, `endpoint`, `region` 확인, LocalStack 기동
Redis 접속 오류 | Redis 미기동/비번 미스 | `spring.redis.enabled=true`, host/port/password 확인
보안 관련 403/필터 예외 | Security 오토컨피그 활성 | `@AutoConfigureMockMvc(addFilters=false)` + 보안 오토컨피그 제외
JPA/Repository 주입 실패 | 전체 컨텍스트 로딩 + DB 미설정 | 슬라이스 테스트 또는 JPA 오토컨피그 제외

--------------------------------------------------------------------------------

## 11) 커맨드 모음(Command Cheatsheet)

명령 | 설명
---|---
`./gradlew clean build` | 전체 빌드
`./gradlew :order-api:order-api-web:test` | 테스트 실행
`./gradlew :order-api:order-api-web:rest` | **REST Docs 전용 테스트(@Tag("restdocs"))만 실행**하여 스니펫 생성
`./gradlew :order-api:order-api-web:asciidoctor` | 스니펫 포함 Asciidoctor HTML 생성
`./gradlew :order-api:order-api-web:bootJar` | 부트 JAR 생성(정적 문서 `/static/docs` 포함)

--------------------------------------------------------------------------------

## 12) 한 줄 요약

**저장소(MySQL/Dynamo/Redis)별 주문 단건 조회 API**를 제공하는 웹 어댑터 모듈입니다.  
컨트롤러·파사드·서비스·매퍼로 역할을 분리하여 유지보수성을 높였고, 테스트는 **분할 설정과 슬라이스 전략**으로 안정적으로 수행합니다.  
로컬 환경에서 **DynamoDB/Redis 활성화** 구성을 제공하여 즉시 검증 가능하며, 추후 이벤트 발행을 추가해도 **Producer 인터셉터(자동 구성)** 로 MDC(`traceId`/`orderId`)를 Kafka 헤더에 싱크해, 수신측이 이를 복원하여 **엔드-투-엔드 추적성**을 보장합니다.
