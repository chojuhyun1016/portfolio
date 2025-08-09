# order-api-master 모듈 README

order-api-master 모듈은 주문 마스터 API 서버로서, 공통 모듈(order-api-common)의 AutoConfiguration과 `@ConfigurationProperties`를 적극 활용하여 설정, 보안, 예외 처리, 포맷, 로깅을 일관되게 제공합니다.  
마스터 모듈은 비즈니스 로직(Controller, Facade, Mapper, Service) 중심으로 최소 설정만 유지합니다.

---

## 모듈 설계 철학

1. 공통 모듈은 AutoConfiguration + @ConfigurationProperties 기본값으로 "행동과 디폴트"만 제공합니다.  
   각 서비스 모듈은 필요한 경우 application.yml 등을 통해 오버라이딩합니다.
2. 서비스 모듈에서의 설정은 최소화하고, 공통 모듈은 재사용성과 확장성을 보장합니다.
3. 글로벌 예외 처리, 포맷 바인딩, 로깅, 시큐리티 설정 등을 표준화하여 모든 모듈에서 동일한 방식으로 동작하게 합니다.

---

## 주요 기능

- 전역 예외 처리: 공통(GlobalExceptionHandler)과 마스터 전용 핸들러의 얇은 커스터마이징
- WebMvc 공통 설정: 포맷 바인딩(DateTime, Enum), 메시지 컨버터, 요청 로깅 필터
- 포맷 바인딩 지원: Jackson 날짜/시간 직렬화·역직렬화 기본 제공
- 로깅 필터: MDC 기반 Correlation ID 자동 처리
- 보안 설정: Stateless, URL 패턴 기반 접근 제어, 게이트웨이 시크릿 헤더 검증

---

## 디렉토리 구조

### config
- 목적: 마스터 모듈 기동과 최소 로컬 설정
- 핵심 클래스:
    - OrderApiMasterApplication (유일한 main)
    - OrderApiConfig (필요 시 @Configuration 로컬 설정)

### web.controller
- 목적: API 진입점
- 핵심 클래스:
    - OrderController

### web.advice
- 목적: 모듈 전용 전역 예외 처리
- 핵심 클래스:
    - MasterApiExceptionHandler
- 설명:
    - 공통(GlobalExceptionHandler)이 전역 규약을 담당
    - 마스터 전용 태깅과 로그만 얇게 추가

### facade
- 목적: 마스터 도메인에 특화된 조합 로직 제공
- 핵심 클래스:
    - OrderFacadeImpl

### mapper
- 목적: API DTO ↔ 내부 모델 변환
- 핵심 클래스:
    - OrderRequestMapper
    - OrderResponseMapper

### service
- 목적: 외부 시스템 연동, 메시징 등 I/O 구현
- 핵심 클래스:
    - KafkaProducerServiceImpl

### dto
- 목적: API 입출력 모델
- 핵심 클래스:
    - LocalOrderRequest, LocalOrderResponse
    - OrderRequest, OrderResponse

---

## 사용 방법

1. master 모듈에서 common 모듈을 dependency로 추가합니다.

   의존성 예시(gradle)

        implementation project(":order-api-common")
        implementation 'org.springframework.boot:spring-boot-starter-web'
        implementation 'org.springframework.boot:spring-boot-starter-validation'
        implementation 'org.springframework.boot:spring-boot-starter-security'   # 보안 자동 구성 사용 시
        implementation 'org.springframework.kafka:spring-kafka'

2. 메인 클래스는 하나만 유지합니다.

   올바른 예시

        @SpringBootApplication(scanBasePackages = "org.example.order")
        public class OrderApiMasterApplication {
            public static void main(String[] args) {
                SpringApplication.run(OrderApiMasterApplication.class, args);
            }
        }

   참고: OrderApiConfig는 @Configuration 등 보조 설정 클래스로만 사용하고 main 메서드는 제거합니다.

3. AutoConfiguration 동작 방식

    - Spring Boot가 자동으로 Common*AutoConfiguration 클래스를 로드합니다.
    - @ConfigurationProperties를 통해 설정값이 주입됩니다.
    - master는 최소한의 설정으로 공통 동작을 즉시 사용합니다.

---

## application.yml 예시

경로: src/main/resources/application-local.yml (환경별 파일: -dev, -beta, -prod 동일 패턴)

    server:
      port: 7711

    spring:
      config:
        activate:
          on-profile: local
        import:
          - application-core-local.yml
          - application-kafka-local.yml
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
              secret: ${INTERNAL_GATEWAY_SECRET:local-secret}
            permit-all-patterns:
              - /actuator/health
              - /actuator/info
            authenticated-patterns:
              - /order/**
          format:
            write-dates-as-timestamps: false

로그 레벨 예시(필요 시)

    logging:
      level:
        root: INFO
        org.springframework.web: DEBUG

---

## 동작 점검

1) 허용 엔드포인트 점검  
   curl -i http://localhost:7711/actuator/health  
   기대: 200 OK

2) 인증 필요한 엔드포인트(/order/**) 점검  
   curl -i http://localhost:7711/order/1 → 401 또는 403  
   curl -i -H "X-Internal-Auth: <your-secret>" http://localhost:7711/order/1 → 200

3) 요청 ID 전달 확인  
   curl -i -H "X-Request-Id: test-123" http://localhost:7711/actuator/health  
   기대: 응답 헤더 X-Request-Id: test-123

---

## 요약

| 디렉토리 | 책임 |
|----------|------|
| config | 마스터 기동과 최소 로컬 설정 |
| web.controller | 주문 API 진입점 |
| web.advice | 모듈 전용 전역 예외 처리(얇은 커스터마이징) |
| facade | 마스터 도메인 조합 로직 |
| mapper | API DTO ↔ 내부 모델 변환 |
| service | 외부 연동 및 메시징 구현 |
| dto | API 입출력 모델 |

---

## 권장 구성 방식

- 공통 모듈: AutoConfiguration + @ConfigurationProperties 기본값 제공
- master 모듈: application.yml로 환경별 값만 오버라이딩
- 필요 시 같은 타입의 빈을 정의해 공통 빈을 대체(예: SecurityFilterChain)
- 장점: 공통 로직 유지보수 용이, 설정 최소화, 재사용성 극대화
