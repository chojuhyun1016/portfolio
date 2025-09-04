# 📘 order-api-web 서비스 README (API 웹 · 구성/확장/운영 가이드)

Spring Boot 기반 **주문 단건 조회 API**입니다.  
HTTP 엔드포인트(`/order/{orderId}`)를 통해 외부 요청을 받고, 이를 **파사드(Facade) → 서비스(Service) → 리포지토리(Repository)** 계층으로 위임해 **단건 조회** 후 **표준 응답(ApiResponse)** 으로 반환합니다.  
전역 예외 처리, DTO 매핑, 공용 `ObjectMapper` 제공, REST Docs 파이프라인을 포함합니다.

본 문서는 **설정(Setup) → 사용(Usage) → 개발(Dev) → 확장(Extend) → 테스트(Test) → REST Docs(Documentation) → 트러블슈팅(Troubleshooting) → 커맨드(Cheatsheet)** 순서로 정리되어 있습니다.

--------------------------------------------------------------------------------

## 1) 전체 구조

레이어 | 주요 클래스/파일 | 핵심 역할
---|---|---
부트스트랩/조립 | `OrderApiWebApplication`, `OrderApiWebConfig` | 애플리케이션 구동, Core·Kafka 모듈 Import, 공용 ObjectMapper 제공
컨트롤러 | `com.example.order.api.web.controller.order.OrderController` | `/order/{orderId}` GET API, 요청 검증/로그, 표준 응답 반환
DTO | `OrderRequest`(검증용), `OrderResponse`(API 응답) | 요청/응답 구조 정의
파사드 | `OrderFacade`, `OrderFacadeImpl` | Service 호출 및 응답 매핑
매퍼 | `OrderResponseMapper` | Application DTO → API 응답 DTO 변환
서비스 | `OrderService`, `OrderServiceImpl` | 트랜잭션 조회, 예외 처리, 도메인→애플리케이션 DTO 변환
공통 | `KafkaProducerService`, `KafkaProducerServiceImpl` | (필요 시) Kafka 발행 추상화 골격
예외/웹 | `WebApiExceptionHandler` | 웹 모듈 전용 전역 예외 처리 (표준 응답 변환)
설정 | `application.yml` | JPA 로그, 보안/로깅/포맷 등 모듈 설정
빌드 | `build.gradle` | REST Docs 파이프라인(`rest` 태스크), Asciidoctor, 부트 JAR에 문서 포함

> 의존 방향: `adapter(api-web) → application(core) → domain` 을 엄격히 유지합니다.  
> API 레이어에서는 **애플리케이션 DTO만 참조**하며, 도메인 엔티티 직접 노출을 금지합니다.

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
    @Configuration
    @Import({ OrderCoreConfig.class, KafkaModuleConfig.class })
    @EnableConfigurationProperties(KafkaTopicProperties.class)
    @ComponentScan(basePackages = { "com.example.order.api.web" })
    @RequiredArgsConstructor
    public class OrderApiWebConfig {
        @Bean
        @ConditionalOnMissingBean(ObjectMapper.class)
        ObjectMapper objectMapper() {
            return ObjectMapperFactory.defaultObjectMapper();
        }
    }

### 2.2 컨트롤러

    // File: src/main/java/com/example/order/api/web/controller/order/OrderController.java
    @RestController
    @Validated
    @RequiredArgsConstructor
    @RequestMapping("/order")
    public class OrderController {

        private final OrderFacade facade;

        @GetMapping("/{orderId}")
        public ResponseEntity<ApiResponse<OrderResponse>> findById(@PathVariable Long orderId) {
            log.info("[OrderController][findById] orderId={}", orderId);
            return ApiResponse.ok(facade.findById(orderId));
        }
    }

### 2.3 파사드/매퍼

    // File: src/main/java/com/example/order/api/web/facade/order/impl/OrderFacadeImpl.java
    @Component
    @RequiredArgsConstructor
    public class OrderFacadeImpl implements OrderFacade {

        private final OrderService orderService;
        private final OrderResponseMapper orderResponseMapper;

        @Override
        public OrderResponse findById(Long id) {
            var dto = orderService.findById(id);   // Application DTO (OrderDto)
            return orderResponseMapper.toResponse(dto);
        }
    }

    // File: src/main/java/com/example/order/api/web/mapper/order/OrderResponseMapper.java
    @Component
    public class OrderResponseMapper {
        public OrderResponse toResponse(OrderDto dto) {
            if (dto == null || dto.getOrder() == null) return null;
            LocalOrderDto o = dto.getOrder();
            return new OrderResponse(
                    o.getId(), o.getUserId(), o.getUserNumber(),
                    o.getOrderId(), o.getOrderNumber(), o.getOrderPrice(),
                    o.getDeleteYn(), o.getVersion(), o.getPublishedTimestamp()
            );
        }
    }

### 2.4 서비스

    // File: src/main/java/com/example/order/api/web/service/order/impl/OrderServiceImpl.java
    @Service
    @Slf4j
    @RequiredArgsConstructor
    public class OrderServiceImpl implements OrderService {

        private final OrderRepository orderRepository; // domain 레포지토리
        private final OrderMapper orderMapper;         // application 매퍼

        @Transactional(readOnly = true)
        public OrderDto findById(Long id) {
            return orderRepository.findById(id)
                    .map(orderMapper::toDto)
                    .map(OrderDto::fromInternal)
                    .orElseThrow(() -> {
                        String msg = "Order not found. id=" + id;
                        log.warn("[OrderService] {}", msg);
                        return new CommonException(CommonExceptionCode.NOT_FOUND_RESOURCE, msg);
                    });
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

### 3.1 애플리케이션 프로퍼티(YAML)

    // File: src/main/resources/application.yml
    server:
      port: 8080

    spring:
      config:
        activate:
          on-profile: prod
        import:
          - application-core-prod.yml
          - application-kafka-prod.yml
      jpa:
        show-sql: true
        properties:
          hibernate:
            format_sql: true
            highlight_sql: true
            use_sql_comments: true

    order:
      api:
        infra:
          logging:
            filter-order: 10
            mdc-filter-order: 5
            incoming-header: X-Request-Id
            response-header: X-Request-Id
          security:
            enabled: true
            gateway:
              header: X-Internal-Auth
              secret: ${INTERNAL_GATEWAY_SECRET}
            permit-all-patterns:
              - /actuator/health
              - /actuator/info
            authenticated-patterns:
              - /order/**
          format:
            write-dates-as-timestamps: false

> prod 활성 시 Core/Kafka 모듈 프로퍼티를 외부 파일에서 로드합니다.  
> 운영 환경에 맞춰 `INTERNAL_GATEWAY_SECRET` 을 안전하게 주입하세요.

### 3.2 Gradle 스크립트(REST Docs 파이프라인)

    // File: order-api/order-api-web/build.gradle
    // 핵심: rest 태스크(REST Docs 전용 테스트) + asciidoctor 연결 + bootJar에 문서 포함

    ext { snippetsDir = file("build/generated-snippets") }

    configurations { asciidoctorExt }

    dependencies {
        // ... (모듈/스타터/로깅/Kafka/Querydsl/JPA/테스트 의존성 – 제공된 전체 스크립트 그대로 사용)
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

--------------------------------------------------------------------------------

## 4) 사용(Usage)

### 4.1 HTTP 요청/응답

- 엔드포인트: `GET /order/{orderId}`

- 성공 응답(JSON · 표준 ApiResponse 예시)

      {
        "metadata": {
          "code": 200,
          "msg": "OK",
          "timestamp": "2025-09-04T02:34:56Z"
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
          "publishedTimestamp": 1720000000000
        }
      }

> 주의: `order-api-web`의 표준 응답에는 `success` 플래그가 없습니다. `metadata` 와 `data`만 검증하세요.

--------------------------------------------------------------------------------

## 5) 개발(Dev)

### 5.1 DTO/모델 매핑 정책
- API 응답 DTO(`OrderResponse`)에는 **실제로 필요한 필드만** 투영합니다.
- 매퍼(`OrderResponseMapper`)는 **애플리케이션 DTO**(`OrderDto`/`LocalOrderDto`)만 입력으로 받습니다. (도메인 엔티티 직접 노출 금지)

### 5.2 서비스/예외
- `OrderServiceImpl#findById` 에서 미존재 시 `CommonException(NOT_FOUND_RESOURCE)` 을 던집니다.
- 모든 예외는 `WebApiExceptionHandler` 가 받아 표준 `ApiResponse.error(...)` 로 변환합니다.

### 5.3 ObjectMapper
- 공용 `ObjectMapper` 빈은 `@ConditionalOnMissingBean` 으로 제공되며, 외부에서 다른 빈을 주입하면 자동 대체됩니다.

--------------------------------------------------------------------------------

## 6) 확장(Extend)

### 6.1 API 엔드포인트 추가
- 새로운 조회/목록/검색 API 추가 시:
  1) 컨트롤러 메서드 정의
  2) 파사드에 위임 로직 추가
  3) 서비스에서 트랜잭션/검증/조회 처리
  4) 응답 DTO 및 매퍼 확장

### 6.2 Kafka 활용(옵션)
- 현재 웹 모듈에는 `KafkaProducerService` 골격이 있으며, 필요 시 도메인 이벤트 발행/감사 로깅 등 확장 가능합니다.
- 토픽명은 `KafkaTopicProperties` 로 관리(코어/Kafka 모듈 설정 파일 참조).

--------------------------------------------------------------------------------

## 7) 테스트(Test)

### 7.1 컨트롤러 슬라이스 테스트(권장)

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
          @DisplayName("HTTP: GET /order/{id} → 200 OK")
          void get_order_should_return_200() throws Exception {
              Long id = 101L;
              OrderResponse resp = new OrderResponse(
                      id, 1000L, "U-0001", 5555L, "O-5555",
                      99000L, false, 1L, 1720000000000L
              );
              when(facade.findById(id)).thenReturn(resp);

              mvc.perform(get("/order/{id}", id).accept(MediaType.APPLICATION_JSON))
                      .andExpect(status().isOk())
                      .andExpect(jsonPath("$.metadata.code").value(200))
                      .andExpect(jsonPath("$.data.id").value(id))
                      .andExpect(jsonPath("$.data.orderNumber").value("O-5555"));
          }
      }

> 포인트: `$.success` 를 검증하지 않습니다(존재하지 않는 필드). `$.metadata.*` 와 `$.data.*` 만 검증하세요.

### 7.2 통합 테스트 시 유의점

- 외부 인프라(보안/Redis/JPA)가 개입되면 컨텍스트 로딩 실패가 날 수 있으므로, **컨트롤러 슬라이스** 또는 **Standalone MockMvc**를 추천합니다.
- 부득이하게 `@SpringBootTest` 를 쓴다면, 테스트 컨텍스트 한정으로 아래 오토컨피그를 제외하세요.
  - `RedisAutoConfiguration`, `RedisRepositoriesAutoConfiguration`
  - `SecurityAutoConfiguration`, `UserDetailsServiceAutoConfiguration`,
    `OAuth2ResourceServerAutoConfiguration`, `OAuth2ClientAutoConfiguration`,
    `ManagementWebSecurityAutoConfiguration`
  - (DB/JPA가 필요 없으면) `DataSourceAutoConfiguration`, `HibernateJpaAutoConfiguration`, `JpaRepositoriesAutoConfiguration`

--------------------------------------------------------------------------------

## 8) REST Docs(Documentation)

### 8.1 스니펫 생성 → Asciidoctor 변환 → 부트 JAR 포함

1) REST Docs 전용 테스트 실행
  - `./gradlew :order-api:order-api-web:rest`
  - 산출물: `order-api-web/build/generated-snippets/**`

2) Asciidoctor HTML 생성
  - `./gradlew :order-api:order-api-web:asciidoctor`
  - 산출물: `order-api-web/build/docs/asciidoc/**.html`

3) 부트 JAR 생성(문서 포함)
  - `./gradlew :order-api:order-api-web:bootJar`
  - JAR 내 정적 문서 위치: `/static/docs/`

### 8.2 문서 스켈레톤(예시 · AsciiDoc)

      = Order API Web REST Docs
      :toc: left
      :toclevels: 3
      :sectanchors:
      :source-highlighter: highlightjs
      :snippets: build/generated-snippets

      == GET /order/{id} → 200 OK

      .Request
      include::{snippets}/order-get-by-id/http-request.adoc[]

      .Response
      include::{snippets}/order-get-by-id/http-response.adoc[]

> 실패 원인 빈출
> - 스니펫 없음 → 먼저 `rest` 또는 `test` 를 실행해 스니펫 생성 필요
> - 403(Forbidden) → 테스트에서 보안 필터 꺼야 함(`@AutoConfigureMockMvc(addFilters=false)`)
> - 응답 필드 누락 → 실제 응답(`metadata`/`data`)과 문서 필드 정의 일치 필요

--------------------------------------------------------------------------------

## 9) 트러블슈팅(Troubleshooting)

증상 | 원인 | 해결책
---|---|---
`No value at JSON path '$.success'` | 응답 스키마에 `success` 없음 | `metadata`/`data`로 검증 포인트 변경
Redis 접속 오류 | 테스트 컨텍스트에 Redis 오토컨피그 개입 | 슬라이스/Standalone 사용 또는 테스트에서 Redis 오토컨피그 제외
보안 관련 403/필터 예외 | Security 오토컨피그 활성 | `@AutoConfigureMockMvc(addFilters=false)` + 보안 오토컨피그 제외
JPA/Repository 주입 실패 | 전체 컨텍스트 로딩 + DB 미설정 | 슬라이스 테스트 또는 JPA 오토컨피그 제외
`rest` 태스크 없음 | Gradle 스크립트에 미정의 | `tasks.register("rest", Test)` 추가(본 모듈은 이미 포함)

--------------------------------------------------------------------------------

## 10) 커맨드 모음(Command Cheatsheet)

명령 | 설명
---|---
`./gradlew clean build` | 전체 빌드
`./gradlew :order-api:order-api-web:test` | 테스트 실행
`./gradlew :order-api:order-api-web:rest` | **REST Docs 전용 테스트(@Tag("restdocs"))만 실행**하여 스니펫 생성
`./gradlew :order-api:order-api-web:asciidoctor` | 스니펫 포함 Asciidoctor HTML 생성
`./gradlew :order-api:order-api-web:bootJar` | 부트 JAR 생성(정적 문서 `/static/docs` 포함)

--------------------------------------------------------------------------------

## 11) 한 줄 요약

**주문 단건 조회 API**를 제공하는 웹 어댑터 모듈입니다.  
컨트롤러·파사드·서비스·매퍼로 역할을 분리하여 유지보수성을 높였고, 테스트는 **불필요한 오토컨피그를 차단**해 안정적으로 수행합니다.  
REST Docs 파이프라인(`rest` → `asciidoctor` → `bootJar`)으로 **운영 문서 자동화**를 지원합니다.
