# 📦 order-api-master — 통합 분석( REST → Kafka 퍼블리시 어댑터 )

----------------------------------------------------------------------------------------------------

## 1) 최상위 개요(아키텍처 & 의존 흐름)

    요청 플로우(요지)
      [Client] → [REST API(Controller)] → [Facade] → [Service]
        → [Application Mapper(core)] → [Message(validation)]
        → [KafkaProducerService] → [KafkaProducerCluster] → [Topic]

    모듈 루트
      org.example.order.api.master
      ├─ config/           ← 스프링 구성(@Import, ObjectMapper 조건 빈)
      ├─ controller/       ← REST 엔드포인트(/order)
      ├─ dto/              ← API 요청/응답(LocalOrderRequest/Response)
      ├─ facade/           ← API↔Service 얇은 조정자
      ├─ mapper/           ← API DTO → Application Command 변환
      ├─ service/
      │   ├─ common/       ← KafkaProducerService(토픽 라우팅)
      │   └─ order/        ← 유스케이스(Service → 메시지 생성/검증/전송)
      └─ web/advice/       ← 모듈 전용 예외 핸들러

    외부 의존
      - org.example.order.core (OrderCoreConfig import)
      - org.example.order.client.kafka (KafkaModuleConfig import)
      - KafkaTopicProperties(토픽명 주입, enum MessageCategory 기반)

    핵심 원칙
      - API 레이어는 입력 검증/호출 오케스트레이션만 담당(얇게 유지)
      - 메시지 스키마/검증은 core 의 Mapper/Message(validation)에서 책임
      - 토픽명/브로커는 설정 기반으로 주입(환경별 분리)

----------------------------------------------------------------------------------------------------

## 2) 실행/구동(필수 설정 · application.yml · 4-space 블록)

    spring:
      application:
        name: order-api-master
    server:
      port: 8080

    # Kafka 클라이언트/토픽 매핑 예시
    kafka:
      bootstrap-servers: localhost:9092
      producer:
        enabled: true
        acks: all
        retries: 10
        linger-ms: 5
        batch-size: 65536
        compression-type: lz4
      # KafkaTopicProperties 바인딩 규약에 맞춰 enum 기반 맵핑(예시)
      topics:
        ORDER_LOCAL: order.local.v1

    logging:
      level:
        root: INFO
        org.example.order: INFO

- 위 키 이름은 프로젝트의 `KafkaModuleConfig / KafkaTopicProperties` 바인딩 규약에 맞추어 조정하십시오(예: `kafka.topic.order-local` 형태를 사용한다면 동일하게 매핑). 핵심은 **MessageCategory.ORDER_LOCAL → 실제 토픽명** 이 1:1로 설정되는 것입니다.

----------------------------------------------------------------------------------------------------

## 3) 사용법(가장 중요)

3.1 REST 엔드포인트

    POST /order
    Content-Type: application/json

    요청(LocalOrderRequest)
      - orderId: Long, 필수(@NotNull)
      - methodType: MessageMethodType(enum), 필수(@NotNull)
        예) CREATE | UPDATE | DELETE ... (프로젝트 정의에 따름)

    성공 응답(LocalOrderResponse wrapped by ApiResponse)
      - HTTP 202 Accepted
      - body.data = { orderId: <요청 ID>, status: "ACCEPTED" }

3.2 즉시 실행 예시(curl)

    curl -X POST http://localhost:8080/order \
         -H "Content-Type: application/json" \
         -d '{ "orderId": 12345, "methodType": "CREATE" }'

    # 개념적 성공 응답 예시(ApiResponse 래핑 규격은 common에 따름)
    {
      "success": true,
      "data": { "orderId": 12345, "status": "ACCEPTED" },
      "error": null
    }

3.3 유효성 실패/예외 응답
- `orderId` 또는 `methodType` 누락 시 Bean Validation 예외 발생.
- `MasterApiExceptionHandler`가 모듈 전용 로그 태깅으로 공통 규격 응답을 반환.
- 알 수 없는 예외는 `CommonExceptionCode.UNKNOWN_SERVER_ERROR`로 표준화.

----------------------------------------------------------------------------------------------------

## 4) 동작 흐름(요청→토픽 퍼블리시)

    [Controller] OrderController.sendOrderMasterMessage()
      - @Valid LocalOrderRequest 검증 + 수신 로그
      - Facade.sendOrderMessage(request) 호출
      - ApiResponse.accepted(LocalOrderResponse(orderId, "ACCEPTED")) 반환(202)

    [Facade] OrderFacadeImpl
      - OrderRequestMapper.toCommand(request) → LocalOrderCommand
      - OrderService.sendMessage(command)

    [Service] OrderServiceImpl
      - OrderMapper.toOrderLocalMessage(command) → OrderLocalMessage
      - message.validation() 수행(코어 메시지의 비즈니스 규칙 검증)
      - 로그("[OrderService] sending message ...")
      - KafkaProducerService.sendToOrder(message)

    [Producer] KafkaProducerServiceImpl
      - KafkaTopicProperties.getName(MessageCategory.ORDER_LOCAL)로 토픽명 결정
      - KafkaProducerCluster.sendMessage(message, topic)

----------------------------------------------------------------------------------------------------

## 5) 확장 사용법(상황별 가이드 · 바로 적용)

A) 신규 methodType 을 별도 토픽으로 라우팅(예: CANCEL 전용 토픽)

    1) enum MessageCategory 에 CANCEL_LOCAL 추가(코어)
    2) application.yml 에 토픽명 매핑
         kafka:
           topics:
             CANCEL_LOCAL: order.cancel.v1
    3) KafkaProducerService 에 전송 메서드 추가
         void sendToCancel(OrderLocalMessage message);
       KafkaProducerServiceImpl 에 구현 추가
         public void sendToCancel(OrderLocalMessage message) {
             send(message, kafkaTopicProperties.getName(MessageCategory.CANCEL_LOCAL));
         }
    4) OrderServiceImpl 에 분기 추가(명령/메시지 methodType 기준)
         if (message.getMethodType() == MessageMethodType.CANCEL) {
             kafkaProducerService.sendToCancel(message);
         } else {
             kafkaProducerService.sendToOrder(message);
         }

B) 다중 토픽 브로드캐스트(동일 메시지 여러 소비자)

    - KafkaProducerServiceImpl 에 다중 토픽 전송 유틸 추가
        public void sendToTopics(Object message, List<String> topics) {
            topics.forEach(t -> cluster.sendMessage(message, t));
        }
    - Facade/Service 에서 시나리오별 토픽 목록 구성 후 호출

C) 메시지 스키마 확장(필드 추가/검증 강화)

    - LocalOrderCommand 에 신규 필드 추가 → OrderMapper.toOrderLocalMessage 에 매핑 확장
    - OrderLocalMessage.validation() 에 규칙 추가(널/범위/상태 일관성)
    - Controller 의 DTO(LocalOrderRequest)에 @NotNull/@Pattern 등 전처리 검증 권장

D) 환경별 토픽 분리

    # 예: dev/stg/prod 각각 별도 토픽 운영
    kafka:
      topics:
        ORDER_LOCAL: order.local.v1.dev   # dev
    # stage/prod 프로파일별 yml에서 override
    kafka:
      topics:
        ORDER_LOCAL: order.local.v1.stg   # stg
    kafka:
      topics:
        ORDER_LOCAL: order.local.v1       # prod

E) 로컬 개발시 카프카 없는 환경

    kafka:
      producer:
        enabled: false
    # KafkaModuleConfig 가 비활성화 시 no-op/memory-buffer 전략을 제공하도록 구성
    # (프로젝트 규약에 따름)

----------------------------------------------------------------------------------------------------

## 6) 구성/코드 스니펫(4-space 고정폭 · 복붙 안전)

6.1 Controller(요약)

        @RestController
        @RequestMapping("/order")
        @RequiredArgsConstructor
        public class OrderController {
            private final OrderFacade facade;

            @PostMapping
            public ResponseEntity<ApiResponse<LocalOrderResponse>> sendOrderMasterMessage(
                    @RequestBody @Valid LocalOrderRequest request
            ) {
                facade.sendOrderMessage(request);
                return ApiResponse.accepted(new LocalOrderResponse(request.orderId(), "ACCEPTED"));
            }
        }

6.2 Facade/Mapper

        @Component
        @RequiredArgsConstructor
        public class OrderFacadeImpl implements OrderFacade {
            private final OrderService orderService;
            private final OrderRequestMapper mapper;

            @Override
            public void sendOrderMessage(LocalOrderRequest request) {
                var command = mapper.toCommand(request);
                orderService.sendMessage(command);
            }
        }

        @Component
        public class OrderRequestMapper {
            public LocalOrderCommand toCommand(LocalOrderRequest req) {
                return (req == null) ? null : new LocalOrderCommand(req.orderId(), req.methodType());
            }
        }

6.3 Service(메시지 생성/검증/전송)

        @Service
        @RequiredArgsConstructor
        public class OrderServiceImpl implements OrderService {
            private final KafkaProducerService kafkaProducerService;
            private final OrderMapper orderMapper;

            @Override
            public void sendMessage(LocalOrderCommand command) {
                final var message = orderMapper.toOrderLocalMessage(command);
                message.validation();
                kafkaProducerService.sendToOrder(message);
            }
        }

6.4 Producer 라우팅

        @Component
        @RequiredArgsConstructor
        public class KafkaProducerServiceImpl implements KafkaProducerService {
            private final KafkaProducerCluster cluster;
            private final KafkaTopicProperties topics;

            @Override
            public void sendToOrder(OrderLocalMessage message) {
                cluster.sendMessage(message, topics.getName(MessageCategory.ORDER_LOCAL));
            }
        }

6.5 Config(ObjectMapper 조건 빈 + 모듈 import)

        @Configuration
        @Import({ OrderCoreConfig.class, KafkaModuleConfig.class })
        @EnableConfigurationProperties(KafkaTopicProperties.class)
        @ComponentScan(basePackages = "org.example.order.api.master")
        @RequiredArgsConstructor
        public class OrderApiMasterConfig {

            @Bean
            @ConditionalOnMissingBean(ObjectMapper.class)
            ObjectMapper objectMapper() {
                return ObjectMapperFactory.defaultObjectMapper();
            }
        }

6.6 예외 처리(Master 전용 태깅)

        @RestControllerAdvice(basePackages = "org.example.order.api.master")
        @Order(Ordered.HIGHEST_PRECEDENCE)
        @Slf4j
        public class MasterApiExceptionHandler {
            @ExceptionHandler(CommonException.class)
            public ResponseEntity<ApiResponse<Void>> handleCommon(CommonException e) {
                return ApiResponse.error(e);
            }
            @ExceptionHandler(Exception.class)
            public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e) {
                return ApiResponse.error(CommonExceptionCode.UNKNOWN_SERVER_ERROR);
            }
        }

----------------------------------------------------------------------------------------------------

## 7) 운영/테스트 팁

- 응답 코드는 항상 202(accepted)이며, 실제 처리/소비는 비동기(Kafka)에서 진행됩니다.
- `message.validation()` 실패 시 4xx/5xx 로 올라올 수 있으므로, 컨트롤러 이전의 DTO 검증을 강화하여 개발 초기에 빠르게 실패시키는 것이 좋습니다.
- 토픽명 누락/오타가 가장 흔한 이슈 → `KafkaTopicProperties` 바인딩 키/프로파일별 오버라이드 여부를 우선 확인.
- 운영에서 재시도/순서/파티셔닝 요구사항이 있으면 `KafkaModuleConfig` 의 producer 설정(acks, retries, idempotence 등)을 정책화하십시오.

----------------------------------------------------------------------------------------------------

## 8) 점검 체크리스트

    [ ] kafka.bootstrap-servers 가 올바른가(프로파일별 상이)
    [ ] kafka.producer.enabled 가 true 인가(운영)
    [ ] MessageCategory ↔ topics 매핑이 모두 등록되었는가(ORDER_LOCAL 등)
    [ ] LocalOrderRequest DTO 의 validation 이 충분한가
    [ ] OrderMapper → OrderLocalMessage 매핑 누락 필드 없는가
    [ ] message.validation() 규칙이 최신 비즈니스 룰과 일치하는가
    [ ] 로그/마스킹/상태코드/오류 응답 포맷이 공통 정책과 일치하는가

----------------------------------------------------------------------------------------------------

## 9) 한 줄 요약

- **POST /order** 로 들어온 명령을 **코어 매퍼/검증**을 거쳐 **설정된 카프카 토픽**으로 퍼블리시하는 **얇고 확장 가능한 REST→Kafka 어댑터**입니다. 현업 적용의 핵심은 **토픽 매핑과 메시지 검증 규칙의 설정화**입니다.
