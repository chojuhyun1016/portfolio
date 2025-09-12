# 🧩 공통 로깅/웹 오토컨피그 모듈 (Logging + Web)

Spring Boot 애플리케이션에 **MDC(Logback/Slf4j) 컨텍스트 전파**와  
**요청 단위 Correlation ID 브리지**를 자동으로 구성하는 오토컨피그입니다.

---

## 1) 구성 개요

| 구성 요소 | 설명 |
|---|---|
| **`org.example.order.common.autoconfigure.logging.LoggingSupportAutoConfiguration`** | MDC 컨텍스트 전파용 `TaskDecorator` 와 `CorrelationAspect`(for `@Correlate`) 자동 등록 |
| `TaskDecorator` (빈 이름: `mdcTaskDecorator`) | @Async/스레드풀 실행 경계에서 MDC(ThreadLocal) 값을 복제/복원 |
| `CorrelationAspect` | `@Correlate(key="...")` 값(SpEL)을 MDC에 주입, 필요 시 `traceId`를 덮어씀 |
| **`org.example.order.common.autoconfigure.web.WebCommonAutoConfiguration`** | `CorrelationIdFilter`를 `FilterRegistrationBean`으로 자동 등록 (가장 앞단에 가까운 order) |
| `CorrelationIdFilter` | X-Request-Id → MDC["requestId"]; MDC["traceId"] 비어있으면 같은 값으로 브리지; 응답 헤더도 설정 |

> 원칙: **라이브러리 모듈**은 `@Component` 대신 **오토컨피그(@AutoConfiguration)** 로 제공 →  
> 애플리케이션은 스캔 범위에 의존하지 않고 **자동 조립**됩니다.  
> (필요 시 `spring.autoconfigure.exclude` 로 손쉽게 끌 수 있음)

---

## 2) 동작 모드

### 2.1 LoggingSupportAutoConfiguration
- 항상 후보가 되지만, 아래 조건으로 **중복을 회피**합니다.
    - `@ConditionalOnMissingBean(name = "mdcTaskDecorator")` → 동일 이름 빈이 있으면 등록 안 함
    - `@ConditionalOnMissingBean(CorrelationAspect.class)` → 이미 존재하면 등록 안 함

### 2.2 WebCommonAutoConfiguration
- 항상 후보가 되지만, `@ConditionalOnMissingBean` 으로 **필터 등록 중복을 회피**합니다.
    - 커스텀 `FilterRegistrationBean<CorrelationIdFilter>` 가 이미 있으면 건너뜀

---

## 3) 빠른 시작 (필수 단계)

### 3.1 의존성
~~~groovy
dependencies {
  implementation project(":order-common")              // 본 모듈
  implementation "org.springframework.boot:spring-boot-starter-web"
  implementation "org.springframework.boot:spring-boot-starter-aop" // @Correlate 사용 시 권장
}
~~~

### 3.2 자동구성 등록 파일 (Boot 3.x 필수)
다음 파일을 **order-common** 모듈에 반드시 포함하세요.

- 경로  
  `order-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- 내용
~~~text
org.example.order.common.autoconfigure.logging.LoggingSupportAutoConfiguration
org.example.order.common.autoconfigure.web.WebCommonAutoConfiguration
~~~

> 이 파일이 없으면 Boot이 오토컨피그 클래스를 **자동으로 로딩하지 않습니다.**  
> (애플리케이션에서 직접 `@Import(...)` 한다면 예외지만, 일반적으로는 반드시 필요)

### 3.3 애플리케이션
별도 @Import 없이 바로 사용됩니다.

~~~java
@SpringBootApplication
public class OrderApiApplication {
  public static void main(String[] args) {
    SpringApplication.run(OrderApiApplication.class, args);
  }
}
~~~

### 3.4 추가 설정(YAML)
기본적으로 **추가 설정은 필요 없습니다.**

---

## 4) 사용법

### 4.1 `@Correlate`로 비즈니스 키를 MDC/traceId에 주입
- `key`(SpEL)로 메서드 파라미터에서 값을 추출
- `mdcKey` 지정 시 MDC에 보조키로 저장
- `overrideTraceId=true`면 MDC["traceId"]를 동일 값으로 덮어씀

~~~java
@Service
public class OrderService {

  @Correlate(key = "#command.orderId", mdcKey = "orderId", overrideTraceId = true)
  public void process(OrderCommand command) {
    log.info("processing order"); 
    // 실행 중 MDC:
    //   traceId = command.orderId
    //   orderId = command.orderId
  }
}
~~~

> 참고: `order-common` 모듈이 `CorrelationAspect` 를 자동 등록하므로 별도 @Component 스캔 필요 없음.  
> 단, AOP 사용을 위해 `spring-boot-starter-aop` 의존성을 권장합니다.

### 4.2 @Async / 커스텀 스레드풀에서 MDC 전파
- 자동 등록된 `TaskDecorator` 빈(`mdcTaskDecorator`)을 **스레드풀에 연결**하세요.

~~~java
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class ExecutorConfig {

  private final TaskDecorator mdcTaskDecorator;

  @Bean
  public ThreadPoolTaskExecutor appExecutor() {
    ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
    exec.setCorePoolSize(8);
    exec.setTaskDecorator(mdcTaskDecorator); // ★ MDC 전파 포인트
    return exec;
  }
}
~~~

> Spring Boot의 기본 실행기에도 `TaskExecutorCustomizer` 통해 자동 반영되지만,  
> **커스텀 풀**에는 반드시 `setTaskDecorator(...)` 로 직접 지정하세요.

### 4.3 요청 단위 Correlation (웹)
- 요청 헤더 `X-Request-Id` → MDC["requestId"]
- MDC["traceId"]가 비어있으면 동일 값으로 브리지
- 응답 헤더 `X-Request-Id` 도 세팅

~~~http
# 클라이언트 요청
GET /orders/123
X-Request-Id: abc-123

# 서버 로그 MDC
requestId=abc-123, traceId=abc-123
~~~

### 4.4 Logback 패턴 예시
~~~xml
<encoder>
  <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [trace:%X{traceId:-NA}] [req:%X{requestId:-NA}] %logger - %msg%n</pattern>
</encoder>
~~~

---

## 5) 오버라이드 / 비활성화

### 5.1 특정 빈 오버라이드 (예: TaskDecorator 교체)
~~~java
@Configuration
public class CustomLoggingConfig {
  @Bean(name = "mdcTaskDecorator")
  public TaskDecorator customMdcTaskDecorator() {
    return runnable -> () -> {
      // 조직 표준 MDC 전파 커스터마이징
      runnable.run();
    };
  }
}
~~~
> 동일 이름(`mdcTaskDecorator`)으로 빈을 제공하면, 오토컨피그는 조건(@ConditionalOnMissingBean) 때문에 등록하지 않습니다.

### 5.2 필터 동작 순서/범위 조정
필요시 직접 등록하여 순서/패턴 제어:
~~~java
@Configuration
public class CustomFilterConfig {
  @Bean
  public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration() {
    var reg = new FilterRegistrationBean<>(new CorrelationIdFilter());
    reg.setOrder(-10);         // 더 앞쪽으로
    reg.addUrlPatterns("/api/*");
    return reg;
  }
}
~~~

### 5.3 오토컨피그 완전 비활성화
~~~yaml
spring:
  autoconfigure:
    exclude:
      - org.example.order.common.autoconfigure.logging.LoggingSupportAutoConfiguration
      - org.example.order.common.autoconfigure.web.WebCommonAutoConfiguration
~~~

---

## 6) 트러블슈팅

- **`@Correlate` 적용이 안 되는 경우**
    - AOP 의존성(`spring-boot-starter-aop`)이 누락되지 않았는지 확인
    - 동일 타입의 `CorrelationAspect` 를 앱이 또 등록하고 있지 않은지 확인(중복이면 한쪽만 활성)

- **MDC 값이 비동기 경계에서 끊기는 경우**
    - 커스텀 `ThreadPoolTaskExecutor` 에 `setTaskDecorator(mdcTaskDecorator)` 설정 필수
    - 스케줄러(`TaskScheduler`)도 동일하게 데코레이터 지정

- **필터가 동작하지 않는 경우**
    - 이미 사용자 정의 `FilterRegistrationBean<CorrelationIdFilter>` 가 등록되어 있는지 확인
    - 순서가 너무 뒤라면 `setOrder(...)` 값 조정

- **로그에 traceId/requestId가 안 보임**
    - Logback 패턴에 `%X{traceId}` / `%X{requestId}` 가 포함되어 있는지 확인

---

## 7) 클래스 다이어그램(개념)

~~~text
LoggingSupportAutoConfiguration
 ├─(if missing)────────────→ TaskDecorator bean ("mdcTaskDecorator")
 └─(if missing)────────────→ CorrelationAspect bean (@Correlate 처리)

WebCommonAutoConfiguration
 └─(if missing)────────────→ FilterRegistrationBean<CorrelationIdFilter>
                                 ↳ CorrelationIdFilter: 
                                    - X-Request-Id → MDC["requestId"]
                                    - if empty(MDC["traceId"]) → bridge to requestId
                                    - set response header X-Request-Id
~~~

---

## 8) 체크리스트 (Best Practice)

- 로그 패턴에 `%X{traceId}` / `%X{requestId}` 포함
- 커스텀 실행기/스케줄러에 `mdcTaskDecorator` 명시 적용
- `@Correlate` 로 **비즈니스 키와 로그 추적 키(traceId)** 를 일치시켜 운영 가독성 향상
- 필요 시 오토컨피그 **선택적 비활성화**(exclude)로 서비스 별 유연성 확보

---

## 9) 마지막 한 줄 요약
**`order-common` 의 오토컨피그를 등록파일에 포함**시키면,  
추가 코드 없이도 **요청-비동기-메시지** 전 구간에서 **MDC 기반 추적(traceId)** 이 매끄럽게 전파됩니다.
