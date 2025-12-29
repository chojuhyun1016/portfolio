# 📘 order-api-master 서비스 README (API 마스터 · 구성/확장/운영 가이드)

Spring Boot 기반 **주문 API 마스터**입니다.  
HTTP 엔드포인트(`/api/v1/local-orders`)를 통해 외부/내부 요청을 받고, 이를 **파사드(Facade) → 서비스(Service) → Kafka 프로듀서(Producer)** 계층으로 위임해 메시지를 발행합니다.  
또한 단건 조회(`/api/v1/local-orders/query`)를 제공하며, 전역 예외 처리, DTO 매핑, Kafka 토픽 발행 구조를 포함합니다.  
`ObjectMapper`, `Validation`, `ExceptionAdvice`, `@Correlate(MDC)`를 통해 안정성과 추적성을 확보합니다.

본 문서는 **설정(Setup) → 사용(Usage) → 개발(Dev) → 테스트(Test) → 확장(Extend) → 운영(Ops) → 트러블슈팅(Troubleshooting) → 커맨드(Cheatsheet)** 순서로 정리되어 있습니다.

--------------------------------------------------------------------------------

## 1) 전체 구조

레이어 | 주요 클래스 | 핵심 역할
---|---|---
부트스트랩/조립 | `OrderApiMasterApplication`, `OrderApiMasterConfig` | 애플리케이션 구동, Core·Kafka Import, ObjectMapper 제공
비동기/스케줄 | `AsyncConfig`, `CustomSchedulerConfig` | @Async / @Scheduled 경계에서 MDC 전파 보장(Decorator + Scheduler 오버라이드)
보안 | `SecurityConfig` | 게이트웨이 종결형 최소 보안(permitAll, 무세션, 기본 기능 off)
컨트롤러 | `LocalOrderController` | `/api/v1/local-orders/*` API, 요청 DTO 검증, 응답 표준화(ApiResponse)
DTO | `LocalOrderPublishRequest/Response`, `LocalOrderQueryRequest/Response` | 요청/응답 구조(Contract DTO) 정의
파사드 | `LocalOrderFacade`, `LocalOrderFacadeImpl` | Request → Command/Query 매핑 후 Service 호출
매퍼 | `LocalOrderRequestMapper(수동)`, `LocalOrderResponseMapper(MapStruct)` | API DTO ↔ Application DTO/View 변환
서비스 | `LocalOrderService`, `LocalOrderServiceImpl` | Kafka 발행, 단건 조회(가공 포함)
Kafka | `KafkaProducerService`, `KafkaProducerServiceImpl` | `KafkaProducerCluster`로 발행 + 토픽 라우팅
예외/웹 | `MasterApiExceptionHandler` | 마스터 모듈 전용 예외 로깅/표준 응답(전역은 common 담당)

메시지 오퍼레이션(Operation):
- `CREATE`
- `UPDATE`
- `DELETE`

--------------------------------------------------------------------------------

## 2) 코드 개요 (현행 코드 기준)

핵심 파일 요약

    // File: org/example/order/api/master/OrderApiMasterApplication.java
    @SpringBootApplication
    @Import(OrderApiMasterConfig.class)
    public class OrderApiMasterApplication {
        public static void main(String[] args) {
            SpringApplication.run(OrderApiMasterApplication.class, args);
        }
    }

    // File: org/example/order/api/master/config/OrderApiMasterConfig.java
    @Configuration(proxyBeanMethods = false)
    @Import({ OrderCoreConfig.class })
    @ImportAutoConfiguration({ KafkaAutoConfiguration.class })
    @EnableConfigurationProperties(KafkaTopicProperties.class)
    @ComponentScan(basePackages = { "org.example.order.api.master" })
    public class OrderApiMasterConfig {

        @Bean
        @ConditionalOnMissingBean(ObjectMapper.class)
        ObjectMapper objectMapper() {
            return ObjectMapperFactory.defaultObjectMapper();
        }
    }

    // File: org/example/order/api/master/config/AsyncConfig.java
    // - @Async 경로 MDC 전파 보장(TaskDecorator)
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
                    if (context != null) MDC.setContextMap(context);
                    else MDC.clear();
                    try { runnable.run(); }
                    finally {
                        if (prev != null) MDC.setContextMap(prev);
                        else MDC.clear();
                    }
                };
            };
        }
    }

    // File: org/example/order/api/master/config/CustomSchedulerConfig.java
    // - 스케줄러/비동기 경계에서 MDC 전파 보장
    // - ThreadPoolTaskScheduler 확장으로 schedule* 지점에서 Runnable 데코레이션
    @Configuration
    @EnableAsync
    @EnableScheduling
    public class CustomSchedulerConfig implements SchedulingConfigurer {
        @Bean
        public ThreadPoolTaskScheduler taskScheduler() { ... }
        @Override
        public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
            taskRegistrar.setTaskScheduler(taskScheduler());
        }
        static final class MdcThreadPoolTaskScheduler extends ThreadPoolTaskScheduler { ... }
    }

    // File: org/example/order/api/master/config/SecurityConfig.java
    // - 게이트웨이 종결형 최소 보안(permitAll, CSRF/basic/form/logout/session off)
    @Configuration
    public class SecurityConfig {
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                .csrf(c -> c.disable())
                .securityMatcher(request -> true)
                .authorizeHttpRequests(reg -> reg.anyRequest().permitAll())
                .httpBasic(c -> c.disable())
                .formLogin(c -> c.disable())
                .logout(c -> c.disable())
                .sessionManagement(sm -> sm.disable())
                .exceptionHandling(ex ->
                    ex.authenticationEntryPoint((req, res, e) -> res.setStatus(200))
                );
            return http.build();
        }
    }

--------------------------------------------------------------------------------

## 3) API 스펙(Usage)

Base Path:
- `/api/v1/local-orders`

--------------------------------------------------------------------------------

## 3.1 주문 메시지 발행 API

- Endpoint: `POST /api/v1/local-orders/publish`
- Content-Type: `application/json`
- Request Body

  {
  "orderId": 1001,
  "operation": "CREATE"
  }

- Response (202 Accepted)

  {
  "data": {
  "orderId": 1001,
  "status": "ACCEPTED"
  },
  "success": true,
  "code": "SUCCESS",
  "message": null
  }

- 주요 동작
  - Controller에서 요청 DTO 검증: `@Valid`
  - `@Correlate`로 MDC 세팅(우선순위: body → querystring → header)
  - Facade에서 Request → Command 변환 후 Service 위임
  - Service에서 Command → Message 변환, `message.validation()` 수행 후 Kafka 발행

--------------------------------------------------------------------------------

## 3.2 주문 단건 조회 API(가공 포함)

- Endpoint: `POST /api/v1/local-orders/query`
- Content-Type: `application/json`
- Request Body

  {
  "orderId": 1001
  }

- Response (200 OK 예시)

  {
  "data": {
  "id": 1,
  "userId": 10,
  "userNumber": "U10",
  "orderId": 1001,
  "orderNumber": "O-1001",
  "orderPrice": 15000,
  "deleteYn": false,
  "version": 0,
  "createdUserId": 0,
  "createdUserType": "SYSTEM",
  "createdDatetime": "2025-12-29T10:00:00",
  "modifiedUserId": 0,
  "modifiedUserType": "SYSTEM",
  "modifiedDatetime": "2025-12-29T10:00:00",
  "publishedTimestamp": 1735437600000,
  "failure": false
  },
  "success": true,
  "code": "SUCCESS",
  "message": null
  }

- 주요 동작
  - `LocalOrderQueryRequest`는 `Long orderId`를 가진 POJO (Getter/NoArgsConstructor)
  - Controller에서 `orderId == null`이면 `INVALID_REQUEST` 반환(방어)
  - Service에서 DB 조회 후 `LocalOrderView`로 매핑하고, 필요한 필드 덮어쓰기 후 반환

--------------------------------------------------------------------------------

## 4) 메시지 전송 흐름(Flow)

    LocalOrderController
      -> LocalOrderFacade
        -> LocalOrderService
          -> LocalOrderMapper (LocalOrderCommand -> OrderLocalMessage)
            -> KafkaProducerService
              -> KafkaProducerCluster
                -> topic: KafkaTopicProperties.getName(MessageOrderType.ORDER_LOCAL)

--------------------------------------------------------------------------------

## 5) MDC/Trace 전략

### 5.1 Controller Correlation 정책(@Correlate)

컨트롤러는 다음 순서로 `orderId`(도메인 키)를 추출하여 MDC에 반영합니다.

- Publish API `@Correlate.paths`

  #p0?.orderId
  #p1?.getParameter('orderId')
  #p1?.getHeader('X-Order-Id')
  #p1?.getHeader('X-Request-Id')
  #p1?.getHeader('x-request-id')

- Query API `@Correlate.paths`

  #p0.orderId
  #p1?.getParameter('orderId')
  #p1?.getHeader('X-Order-Id')
  #p1?.getHeader('X-Request-Id')
  #p1?.getHeader('x-request-id')

추가 설정:
- `mdcKey = "orderId"`
- `overrideTraceId = true` (traceId를 orderId로 덮어씀)

### 5.2 Async/Scheduler MDC 전파

- `AsyncConfig`
  - `TaskDecorator`로 비동기 실행 스레드에 MDC를 복제/복원
- `CustomSchedulerConfig`
  - `ThreadPoolTaskScheduler` 확장으로 schedule* 모든 지점에서 Runnable 데코레이션
  - Date/long deprecated 시그니처는 경고 억제(@SuppressWarnings("deprecation"))
  - Instant/Duration 오버로드도 지원

--------------------------------------------------------------------------------

## 6) Kafka 발행 구조

### 6.1 토픽 라우팅

`KafkaProducerServiceImpl`는 다음과 같이 `KafkaTopicProperties`를 통해 토픽명을 결정합니다.

- ORDER_LOCAL 발행

  kafkaTopicProperties.getName(MessageOrderType.ORDER_LOCAL)

실제 전송은 `KafkaProducerCluster#sendMessage(message, topic)`로 위임합니다.

### 6.2 Producer MDC 헤더 주입(공통 인터셉터)

- 이 모듈의 서비스/프로듀서 코드는 별도 헤더 세팅 로직이 없습니다.
- Producer 발행 시 MDC(traceId/orderId)가 Kafka 헤더에 주입되는 것은 공통 모듈의 ProducerInterceptor에 의해 처리됩니다.
- 즉, 본 모듈은 `KafkaProducerCluster#sendMessage(...)`만 호출해도 일관된 trace 헤더가 실립니다.

--------------------------------------------------------------------------------

## 7) 예외 처리(Exception Handling)

### 7.1 모듈 전용 예외 처리(Master)

- `MasterApiExceptionHandler`는 `basePackages = "org.example.order.api.master"` 범위에서만 동작합니다.
- 의도: 공통 전역 정책은 common(GlobalExceptionHandler)이 담당하고, 마스터는 로그 태깅 등 최소 차별화만 수행합니다.

핵심 동작:
- `CommonException` -> `ApiResponse.error(e)`
- `Exception` -> `ApiResponse.error(UNKNOWN_SERVER_ERROR)`

--------------------------------------------------------------------------------

## 8) 개발(Dev)

### 8.1 DTO 계층 규칙(권장)

- API DTO(Contract)
  - `LocalOrderPublishRequest/Response`
  - `LocalOrderQueryRequest/Response`
- Application DTO
  - `LocalOrderCommand`, `LocalOrderQuery`
- View DTO
  - `LocalOrderView`

### 8.2 매핑 규칙

- Request 매핑: 수동 매퍼(`LocalOrderRequestMapper`)
  - null-safe
  - operation은 enum 기반으로 전달
- Response 매핑: MapStruct(`LocalOrderResponseMapper`)
  - `unmappedTargetPolicy = ERROR`로 누락 필드 방지

### 8.3 Service 규칙

- Publish
  - `message.validation()`은 반드시 호출
- Query
  - readOnly 트랜잭션
  - 조회 실패 시 `CommonException(NOT_FOUND_RESOURCE)` 발생

--------------------------------------------------------------------------------

## 9) 테스트(Test)

### 9.1 단위 테스트

- Controller
  - `@WebMvcTest(LocalOrderController.class)`
  - `LocalOrderFacade`, 매퍼 등은 `@MockBean` 처리
- Service
  - Mockito로 `KafkaProducerService`를 mock 처리 후 호출 검증

### 9.2 통합 테스트(외부 인프라 제외) 팁

- Redis/Redisson, Security 오토컨피그가 개입되면 로컬 미기동 시 오류가 날 수 있습니다.
- 테스트 컨텍스트 한정으로 오토컨피그를 제외하고, 보안 필터를 끄는 방식을 권장합니다.

예시 속성(컨텍스트 한정 제외):
- RedisAutoConfiguration
- RedisRepositoriesAutoConfiguration
- SecurityAutoConfiguration 등(필요 시)

MockMvc 보안 필터 off:
- `@AutoConfigureMockMvc(addFilters = false)`

외부 의존 빈 mock:
- `@MockBean KafkaProducerCluster`

--------------------------------------------------------------------------------

## 10) 확장(Extend)

### 10.1 API 엔드포인트 추가

- 컨트롤러 추가
- 요청/응답 DTO 정의
- 파사드/서비스 연결
- 표준 응답은 `ApiResponse` 사용

### 10.2 토픽/메시지 확장

- 새 MessageOrderType/MessageCategory 추가
- `KafkaTopicProperties`에 매핑 추가
- ProducerService에 라우팅 메서드 추가

### 10.3 ObjectMapper 커스터마이즈

- 외부에서 `ObjectMapper` Bean을 제공하면 `@ConditionalOnMissingBean`에 의해 기본 매퍼가 대체됩니다.

--------------------------------------------------------------------------------

## 11) 운영(Ops)

- 로그 레벨
  - 검증 단계: `org.example=DEBUG`
  - 운영: `INFO`
- 장애 전파
  - Service 레이어에서 예외 발생 -> Advice가 표준 응답으로 변환
- MDC 운영 팁
  - 로그 패턴에 `%X{traceId}`, `%X{orderId}` 포함 권장
  - Async/Scheduler에서도 MDC 전파되도록 Config 유지

--------------------------------------------------------------------------------

## 12) 트러블슈팅(Troubleshooting)

증상 | 원인 | 해결책
---|---|---
테스트에서 Redis/Redisson 접속 오류 | 테스트 컨텍스트가 Redis 오토컨피그 활성 | 테스트 컨텍스트에서 Redis 오토컨피그 제외
테스트에서 403 또는 Security 관련 오류 | Security 필터가 켜져 동작 | `@AutoConfigureMockMvc(addFilters = false)` 적용 + 필요 시 Security 오토컨피그 제외
`KafkaProducerCluster` 빈 미존재 | 외부 Kafka 클라이언트 미로딩/조건 미충족 | 테스트에서 `@MockBean KafkaProducerCluster`
MDC(traceId/orderId) 누락 | @Correlate 미적용 또는 수신측 복원 미구성 | Controller @Correlate 확인 + 수신측 인터셉터 구성 확인

--------------------------------------------------------------------------------

## 13) 커맨드 모음(Command Cheatsheet)

- 전체 빌드 + 단위 테스트
  ./gradlew clean build test

- 마스터 모듈 단위 테스트
  ./gradlew :order-api:order-api-master:test

- 특정 테스트만 실행
  ./gradlew :order-api:order-api-master:test --tests "org.example.order.api.master.*"

--------------------------------------------------------------------------------

## 14) 한 줄 요약

`order-api-master`는 `/api/v1/local-orders` HTTP 요청을 Kafka 메시지로 변환/발행하는 **주문 API 마스터**이며,  
`@Correlate + Async/Scheduler MDC 전파 + Producer 헤더 주입` 조합으로 **엔드-투-엔드 추적성(traceId/orderId)**을 보장합니다.
