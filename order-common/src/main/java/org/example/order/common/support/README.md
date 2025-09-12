# 🧰 공통 지원 모듈 – JSON/JPA/로깅 유틸 (Converter + Jackson + MDC AOP)

`order-common`에 포함된 **JPA AttributeConverter**, **Jackson 구성/유틸**, **MDC 상관관계 AOP**를 한 번에 정리했습니다.  
도메인/인프라/애플리케이션 어디에서든 재사용 가능한 **순수 유틸/설정형 컴포넌트**들입니다.

---

## 1) 구성 개요

| 구성 요소 | 목적 | 비고 |
|---|---|---|
| **`BooleanToYNConverter`** | `Boolean` ↔ `"Y"/"N"` DB 매핑 | JPA Converter, `@Converter(autoApply = false)` |
| **`CodeEnumJsonConverter`** | `CodeEnum` 계열 직렬화/역직렬화 | `@JsonComponent`로 Jackson에 자동 등록 |
| **`ObjectMapperFactory`** | 표준 `ObjectMapper` 빌더 | 날짜/시간 + `CodeEnum` 시/역 직렬화 구성 |
| **`ObjectMapperUtils`** | JSON 편의 유틸 모음 | 안전한 변환/추출 + 예외 래핑 |
| **`@Correlate` / `CorrelationAspect`** | SpEL 기반 MDC 주입/복원 | `traceId` 오버라이드/보조 MDC 키 저장 |

> 원칙: **라이브러리 계층**에서는 런타임 스캔/컴포넌트 자동 주입에 과도하게 의존하지 않고,  
> “도구(Converter/Utils) + 구성(ObjectMapperFactory) + AOP(명시적인 오토컨피그 등록)”로 단순/명확하게 유지합니다.

---

## 2) JPA – BooleanToYNConverter

### 2.1 개요
- DB ↔ 엔티티 간 **불리언-문자** 변환기로, `"Y"`/`"N"` 문자열 컬럼을 `Boolean` 필드와 매핑합니다.
- JPA 표준 `AttributeConverter<Boolean, String>` 구현.

### 2.2 적용 방법

#### (A) 엔티티 필드에 직접 지정
~~~java
@Entity
public class UserEntity {

  @Convert(converter = BooleanToYNConverter.class)
  private Boolean active;

  // ...
}
~~~

#### (B) 전역 자동 적용
- 현재는 `@Converter(autoApply = false)` 이므로 **필드 단위로 명시**합니다.
- 전역 적용이 필요하면 `autoApply = true` 로 바꿀 수 있으나,  
  *모든* `Boolean` 필드에 적용되므로 **의도치 않은 테이블에 퍼질 수 있음**에 유의하세요.

---

## 3) Jackson – CodeEnumJsonConverter

### 3.1 개요
- `CodeEnum`(조직 표준 열거형 인터페이스) 값을 **DTO 형태**로 serialize 하고,
- 역직렬화는 문자열(이넘 name) 또는 `{ "code": "..." }` 형태 양쪽을 수용합니다.
- `@JsonComponent` 이므로 Spring Boot가 자동으로 `ObjectMapper`에 등록합니다.

### 3.2 직렬화 동작
- `CodeEnum` 구현체 → `CodeEnumDto.toDto(value)` 로 변환해 JSON 출력.

### 3.3 역직렬화 동작
- 입력이 **문자열**이면: `Enum.valueOf(target, text)`
- 입력이 **객체**면: `node.get("code")` 를 찾아 enum 상수의 `name()`(또는 `toString()`)과 매칭

### 3.4 사용 예
~~~java
// 예) CodeEnum 구현체
public enum OrderStatus implements CodeEnum {
  CREATED("C"), PAID("P"), SHIPPED("S");
  // ...
}

// JSON 직렬화 예시 출력 (의존하는 CodeEnumDto 포맷에 따라)
// { "code": "CREATED", "label": "생성됨" } 형태 등으로 나갈 수 있음
~~~

> 팁: 어떤 포맷으로 나갈지는 `CodeEnumDto.toDto(...)` 구현을 확인하세요.  
> 역직렬 시에는 `"CREATED"` 또는 `{ "code": "CREATED" }` 둘 다 허용됩니다.

---

## 4) Jackson – ObjectMapperFactory

### 4.1 개요
- 조직 표준 **ObjectMapper 설정**을 한 곳에서 생성합니다.
- 주요 세팅:
    - `failOnUnknownProperties(false)` (스프링 기본)
    - `DEFAULT_VIEW_INCLUSION` 비활성
    - `WRITE_DATES_AS_TIMESTAMPS` 활성 (스프링 기본)
    - `LocalDate/LocalTime/LocalDateTime/YearMonth` 시/역직렬화 포맷 지정 (조직 공통 포맷 사용)
    - `CodeEnum`/`Enum` 커스텀 시/역직렬화 등록

### 4.2 사용 방법

#### (A) 직접 빈으로 노출
~~~java
@Configuration
public class JacksonConfig {

  @Bean
  public ObjectMapper objectMapper() {
    return ObjectMapperFactory.defaultObjectMapper();
  }
}
~~~

#### (B) 유틸에서 내부적으로 사용
- `ObjectMapperUtils` 가 공통 `ObjectMapper`로 이 팩토리를 사용합니다.

> 팁: Boot의 기본 `ObjectMapper` 대신 **정확히 이 구성**이 필요하다면  
> 애플리케이션 쪽에서 `@Primary` 로 오버라이드 하세요.

---

## 5) Jackson – ObjectMapperUtils

### 5.1 개요
- 안전한 JSON 변환/추출을 돕는 **정적 유틸** 모음.
- 예외 발생 시 도메인 공통 예외(`CommonExceptionCode.DATA_*`)로 래핑하여 일관된 에러 처리.

### 5.2 대표 메서드
| 메서드 | 설명 |
|---|---|
| `writeValueAsString(obj)` | 객체 → JSON 문자열 |
| `readValue(json, Class<T>)` | JSON 문자열 → 타입 |
| `readValue(json, TypeReference<T>)` | 제네릭 타입 파싱 |
| `getFieldValueFromString(json, field, clz)` | 특정 필드만 추출/매핑 |
| `valueToMap(obj)` | 객체 → `Map<String, Object>` |
| `valueToObject(obj, clz)` | 타입 변환 (예: Map → DTO) |
| `convertToList(json, clz)` | JSON 배열 문자열 → `List<T>` |
| `convertTreeToValue(obj, clz)` | Tree 변환 경유 매핑 |
| `convertTreeToValues(Object[], clz)` | 배열 → 리스트 매핑 |
| `writeValue(outStream, obj)` | JSON 직렬화 후 스트림에 기록 |

### 5.3 사용 예
~~~java
var json = ObjectMapperUtils.writeValueAsString(dto);
var view = ObjectMapperUtils.readValue(json, OrderView.class);

String code = ObjectMapperUtils.getFieldValueFromString(json, "code", String.class);

Map<String,Object> map = ObjectMapperUtils.valueToMap(dto);
OrderDto converted = ObjectMapperUtils.valueToObject(map, OrderDto.class);
~~~

> 팁: 성능 민감 구간에서는 **불필요한 트리/중간 객체 생성**을 피하세요.  
> (위 유틸은 안정성/가독성을 우선합니다.)

---

## 6) 로깅 – @Correlate & CorrelationAspect

### 6.1 개요
- 메서드 호출 시 **SpEL**로 파라미터에서 **비즈니스 키**(예: `orderId`)를 추출하여
    - 선택 키(`mdcKey`)로 MDC에 저장
    - `overrideTraceId=true`면 MDC["traceId"]도 동일 값으로 덮어씀
- 실행 후 **이전 MDC 상태 복원**으로 누수/오염 방지

### 6.2 사용 예
~~~java
@Service
public class OrderService {

  @Correlate(key = "#cmd.orderId", mdcKey = "orderId", overrideTraceId = true)
  public void send(LocalOrderCommand cmd) {
    log.info("send kafka");
  }

  @Correlate(key = "#user.id", overrideTraceId = false) // traceId 유지, 보조키 저장 안함
  public void audit(User user) {
    log.info("audit");
  }
}
~~~

### 6.3 동작 규칙
1) SpEL 평가에 실패하거나 값이 `null/blank`면 **아무것도 하지 않음**
2) `mdcKey` 가 비워져 있지 않으면 **MDC[mdcKey] = extracted**
3) `overrideTraceId=true`면 **MDC["traceId"] = extracted**
4) `finally` 에서 **기존 값 복원** (없던 키는 제거)

### 6.4 전제 조건
- AOP 의존성 필요: `spring-boot-starter-aop`
- `CorrelationAspect` 등록:
    - **오토컨피그**에서 `@Bean` 제공(권장) 또는
    - 직접 `@Component` 스캔 (현재 코드는 오토컨피그 방식과 궁합이 좋음)

---

## 7) 통합 사용 가이드 (샘플)

### 7.1 의존성
~~~groovy
dependencies {
  implementation project(":order-common")                  // 본 모듈
  implementation "org.springframework.boot:spring-boot-starter-web"
  implementation "org.springframework.boot:spring-boot-starter-aop" // @Correlate AOP
  implementation "com.fasterxml.jackson.datatype:jackson-datatype-jsr310"
  // JPA 사용 시
  implementation "org.springframework.boot:spring-boot-starter-data-jpa"
}
~~~

### 7.2 오토컨피그 등록 (Boot 3.x)
- 경로: `order-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 내용 예: (로깅/웹 자동구성 사용 시)
~~~text
org.example.order.common.autoconfigure.logging.LoggingAutoConfiguration
org.example.order.common.autoconfigure.web.WebAutoConfiguration
~~~
> `CodeEnumJsonConverter`는 `@JsonComponent`로 자동 등록되므로 별도 오토컨피그가 필요 없습니다.  
> (특별히 ObjectMapper 전체를 교체하는 정책이 있다면 별도 구성 클래스를 추가하세요.)

### 7.3 로그 패턴 (Logback)
~~~xml
<encoder>
  <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [trace:%X{traceId:-NA}] [order:%X{orderId:-NA}] %logger - %msg%n</pattern>
</encoder>
~~~

### 7.4 @Async / 스레드풀 MDC 전파 (선택)
- 오토컨피그가 제공하는 `TaskDecorator("mdcTaskDecorator")`를 **커스텀 풀**에 연결
~~~java
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class ExecutorConfig {
  private final TaskDecorator mdcTaskDecorator;

  @Bean
  public ThreadPoolTaskExecutor asyncExecutor() {
    var exec = new ThreadPoolTaskExecutor();
    exec.setCorePoolSize(8);
    exec.setTaskDecorator(mdcTaskDecorator); // ★ 필수
    return exec;
  }
}
~~~

---

## 8) 트러블슈팅

- **엔티티 `Boolean`이 `"Y"/"N"`으로 저장되지 않음**  
  → 해당 필드에 `@Convert(converter = BooleanToYNConverter.class)` 누락 여부 확인  
  → 전역 적용을 원하면 `autoApply = true` 로 변경(권장 X)

- **`CodeEnum`이 JSON에서 원하는 포맷으로 안 나감**  
  → `CodeEnumDto.toDto(...)` 로직을 점검 (표준 포맷 정의 위치)  
  → 앱 레벨에서 `ObjectMapper` 커스터마이징이 덮어쓰지 않았는지 확인

- **`@Correlate`가 동작하지 않음**  
  → `spring-boot-starter-aop` 의존성 확인  
  → `CorrelationAspect`가 오토컨피그 또는 컴포넌트 스캔으로 등록되어 있는지 확인

- **비동기/스케줄 경계에서 MDC 유실**  
  → 커스텀 스레드풀/스케줄러에 `mdcTaskDecorator` 지정 여부 확인

---

## 9) 클래스 다이어그램(개념)

~~~text
BooleanToYNConverter (JPA AttributeConverter)
    └─(per-field @Convert)→ 엔티티 필드(Boolean) ←→ DB VARCHAR(1) 'Y'/'N'

CodeEnumJsonConverter (@JsonComponent)
    ├─ Serializer(CodeEnum → CodeEnumDto)
    └─ Deserializer(Enum ← "TEXT" or {"code":"TEXT"})

ObjectMapperFactory
    └─ defaultObjectMapper()
          ├─ JSR-310 날짜/시간 시/역직렬화
          ├─ CodeEnum Serializer
          └─ Enum Deserializer(컨텍스트 기반)

ObjectMapperUtils (static)
    ├─ writeValueAsString / readValue
    ├─ valueToMap / valueToObject
    ├─ getFieldValueFromString / getFieldValueFromObject
    └─ convertTreeToValue(s) / writeValue(stream)

@Correlate (annotation)
    └─ CorrelationAspect (AOP)
          ├─ SpEL로 키 추출
          ├─ MDC[mdcKey] 주입 (선택)
          └─ MDC["traceId"] 오버라이드(선택) + 실행 후 복원
~~~

---

## 10) 마지막 한 줄 요약
**DB-문자(Y/N), JSON-이넘(CodeEnum), 로깅-MDC(traceId)** —  
세 축을 표준화/자동화하여, **서비스 전반의 일관성/추적성/생산성**을 높입니다.
