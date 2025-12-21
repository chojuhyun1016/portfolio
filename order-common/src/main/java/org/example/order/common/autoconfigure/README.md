# 🧩 공통 로깅/웹 오토컨피그 모듈 (Logging + Web)

Spring Boot 애플리케이션에 **MDC(Logback/Slf4j) 컨텍스트 전파**와  
**요청 단위 Correlation ID(requestId ↔ traceId) 브리지**를 자동으로 구성하는 오토컨피그 모듈입니다.

본 구성은 **order-common 최신 코드 기준**으로 정리되었으며,  
API / Worker / Batch 전 구간에서 **동일한 로깅·트레이싱 규칙**을 보장하는 것을 목표로 합니다.

---

## 1) 구성 개요

| 구성 요소 | 설명 |
|---|---|
| **`org.example.order.common.autoconfigure.logging.LoggingAutoConfiguration`** | MDC 전파용 `TaskDecorator` + `@Correlate` 처리를 위한 `CorrelationAspect` 자동 등록 |
| `TaskDecorator` (빈 이름: `mdcTaskDecorator`) | 스레드 경계(@Async, Executor, Scheduler)에서 MDC(ThreadLocal) 캡처 → 복원 |
| `CorrelationAspect` | `@Correlate(paths / key)` 기반 SpEL 평가 → MDC 주입, 필요 시 `traceId` 덮어쓰기 |
| `TraceIdTurboFilter` | MDC["traceId"]가 비어 있으면 **UUID를 즉시 생성/보장** (AOP/웹 진입 이전 로그까지 커버) |
| **`org.example.order.common.autoconfigure.web.WebAutoConfiguration`** | `CorrelationIdFilter`를 `FilterRegistrationBean`으로 자동 등록 |
| `CorrelationIdFilter` | `X-Request-Id` → MDC["requestId"]; MDC["traceId"] 비어있으면 브리지; 응답 헤더 세팅 |

> 원칙
> - **라이브러리 모듈은 `@Component` 스캔을 사용하지 않음**
> - 모든 공통 기능은 **`@AutoConfiguration` 기반 자동 조립**
> - 애플리케이션은 추가 설정 없이 바로 사용 가능
> - 필요 시 `spring.autoconfigure.exclude` 로 선택적 비활성화 가능

---

## 2) 동작 흐름 요약

### 2.1 요청/로그 초기 구간
1. `TraceIdTurboFilter`
  - MDC["traceId"]가 없으면 UUID 생성
  - 배치/초기화/프레임워크 로그까지 traceId 보장

2. `CorrelationIdFilter`
  - 요청 헤더 `X-Request-Id` 수신
  - MDC["requestId"] 설정
  - MDC["traceId"]가 비어 있으면 requestId로 브리지
  - 응답 헤더에 `X-Request-Id` 반환

### 2.2 애플리케이션 레이어
3. `@Correlate`
  - SpEL로 **도메인 키(orderId 등)** 추출
  - MDC 보조 키(`mdcKey`) 저장
  - `overrideTraceId=true` 이면 traceId를 도메인 키로 덮어씀

### 2.3 비동기/스레드 경계
4. `TaskDecorator (mdcTaskDecorator)`
  - 실행 시점의 MDC 스냅샷 캡처
  - 대상 스레드에서 MDC 복원
  - 종료 후 이전 MDC 복구

---

## 3) LoggingAutoConfiguration

### 등록 내용
- `TaskDecorator` 빈 (`mdcTaskDecorator`)
- `CorrelationAspect`
- AspectJ 프록시 활성화

### 중복 회피 조건
- `@ConditionalOnMissingBean(name = "mdcTaskDecorator")`
- `@ConditionalOnMissingBean(CorrelationAspect.class)`

### CorrelationAspect 특징
- `paths` → **우선순위 SpEL 배열**
- `key` → 레거시/보조 단일 SpEL (paths 실패 시)
- `MethodBasedEvaluationContext` 사용
- SpEL Expression 캐시로 성능 보강
- `overrideTraceId=true` 이고 실제 변경 시에는 **finally에서 traceId 복원하지 않음**

---

## 4) WebAutoConfiguration

### 등록 내용
- `CorrelationIdFilter` 빈
- `FilterRegistrationBean<CorrelationIdFilter>`

### 필터 등록 정책
- `@ConditionalOnMissingBean(CorrelationIdFilter.class)`
- `@ConditionalOnMissingBean(name = "correlationIdFilterRegistration")`
- 기본 순서: `Ordered.HIGHEST_PRECEDENCE`
- URL 패턴: `/*`

---

## 5) 빠른 시작

### 5.1 의존성
~~~groovy
dependencies {
  implementation project(":order-common")
  implementation "org.springframework.boot:spring-boot-starter-web"
  implementation "org.springframework.boot:spring-boot-starter-aop"
}
~~~

### 5.2 AutoConfiguration 등록 파일 (Boot 3.x 필수)

경로  
`order-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

내용
~~~text
org.example.order.common.autoconfigure.logging.LoggingAutoConfiguration
org.example.order.common.autoconfigure.web.WebAutoConfiguration
~~~

> 이 파일이 없으면 오토컨피그는 **자동 로딩되지 않습니다.**

---

## 6) 사용법

### 6.1 `@Correlate` 사용 예

~~~java
@Service
public class OrderService {

  @Correlate(
    paths = {"#command.orderId"},
    mdcKey = "orderId",
    overrideTraceId = true
  )
  public void process(OrderCommand command) {
    log.info("processing order");
    // MDC:
    // traceId = orderId
    // orderId = orderId
  }
}
~~~

### 6.2 비동기 MDC 전파 (@Async / Executor)

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
    exec.setTaskDecorator(mdcTaskDecorator);
    return exec;
  }
}
~~~

> 커스텀 Executor / Scheduler에는 **반드시 직접 지정**해야 합니다.

---

## 7) Logback 패턴 예시

~~~xml
<encoder>
  <pattern>
    %d{yyyy-MM-dd HH:mm:ss.SSS}
    [%thread]
    %-5level
    [trace:%X{traceId:-NA}]
    [req:%X{requestId:-NA}]
    %logger - %msg%n
  </pattern>
</encoder>
~~~

---

## 8) 오버라이드 / 비활성화

### 8.1 TaskDecorator 교체
~~~java
@Configuration
public class CustomLoggingConfig {

  @Bean(name = "mdcTaskDecorator")
  public TaskDecorator customTaskDecorator() {
    return runnable -> () -> runnable.run();
  }
}
~~~

### 8.2 필터 직접 등록
~~~java
@Configuration
public class CustomFilterConfig {

  @Bean(name = "correlationIdFilterRegistration")
  public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration() {
    var reg = new FilterRegistrationBean<>(new CorrelationIdFilter());
    reg.setOrder(-10);
    reg.addUrlPatterns("/api/*");
    return reg;
  }
}
~~~

### 8.3 오토컨피그 완전 비활성화
~~~yaml
spring:
  autoconfigure:
    exclude:
      - org.example.order.common.autoconfigure.logging.LoggingAutoConfiguration
      - org.example.order.common.autoconfigure.web.WebAutoConfiguration
~~~

---

## 9) 트러블슈팅

- **`@Correlate` 미동작**
  - `spring-boot-starter-aop` 누락 여부 확인
  - 동일 타입 `CorrelationAspect` 중복 등록 여부 확인

- **비동기에서 MDC 소실**
  - 커스텀 Executor / Scheduler에 `mdcTaskDecorator` 지정 여부 확인

- **traceId/requestId 로그 미출력**
  - Logback 패턴에 `%X{traceId}`, `%X{requestId}` 포함 여부 확인

---

## 10) 한 줄 요약
**order-common 오토컨피그를 등록파일에 포함하는 것만으로**  
요청 → AOP → 비동기 → 로그 전 구간에서 **MDC 기반 traceId/requestId 추적이 자동으로 보장됩니다.**
