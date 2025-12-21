# 🧰 공통 지원 모듈 – JSON / JPA / 로깅 유틸 (Converter + Jackson + MDC Correlation AOP)

`order-common` 의 `support` 패키지는 **JPA 변환기**, **Jackson 표준 ObjectMapper 구성/유틸**,  
**MDC 기반 상관관계(@Correlate) AOP** 를 제공하는 **순수 지원 레이어**입니다.

- 비즈니스 도메인에 종속되지 않음
- 특정 인프라(Kafka/Web/JPA)에 강하게 묶이지 않음
- AutoConfiguration과 결합되어 애플리케이션 코드 변경 없이 동작

---

## 1) 구성 개요 (현행 기준)

| 구성 요소 | 목적 | 비고 |
|---|---|---|
| **`BooleanToYNConverter`** | `Boolean` ↔ `"Y"/"N"` DB 매핑 | JPA `AttributeConverter`, `@Converter(autoApply = false)` |
| **`ObjectMapperFactory`** | 조직 표준 `ObjectMapper` 생성 | 날짜/시간 직렬화 포맷 고정, 역직렬화 관대화 |
| **`ObjectMapperUtils`** | JSON 변환/추출 유틸 | 예외를 `CommonException`으로 래핑 |
| **`@Correlate` / `CorrelationAspect`** | SpEL 기반 MDC 주입/복원 | `paths` 우선, `key` 보조, `traceId` 오버라이드 규칙 |
| **`TraceIdTurboFilter`** | MDC["traceId"] UUID 보장 | AOP/웹 진입 이전 로그까지 커버 |
| **`MdcPropagation`** | 비동기/콜백 MDC 전파 | Runnable/Consumer/BiConsumer 래핑 |
| **`PathValueExtractor`** | 리플렉션 기반 경로 탐색 | 프레임워크 타입 비의존(메시징/헤더/POJO 공용) |

> 원칙: 라이브러리 계층에서는 런타임 스캔/컴포넌트 자동 주입에 과도하게 의존하지 않고,  
> “도구(Converter/Utils) + 구성(ObjectMapperFactory) + AOP(오토컨피그에서 명시 등록)”로 단순/명확하게 유지합니다.

> 참고(현행 코드 기준):
> - 과거 문서에서 언급되던 `CodeEnumJsonConverter` 는 **현재 order-common 코드에 존재하지 않습니다.**
> - `ObjectMapperFactory` 는 **계약 모듈(order-contract)과 충돌을 피하기 위해** 커스텀 enum 직렬화/역직렬화를 등록하지 않습니다.

---

## 2) JPA – BooleanToYNConverter

### 2.1 개요
- DB ↔ 엔티티 간 불리언-문자 변환기
- DB: VARCHAR(1) `'Y'/'N'`, Java: `Boolean`(TRUE/FALSE/null)
- JPA 표준 `AttributeConverter<Boolean, String>` 구현
- 기본 설정: `@Converter(autoApply = false)` → 전역 확산 방지, **필드 단위 명시 적용**

### 2.2 적용 방법

#### (A) 엔티티 필드에 직접 지정 (권장)
~~~java
@Entity
public class UserEntity {

  @Convert(converter = BooleanToYNConverter.class)
  private Boolean active;

  // ...
}
~~~

#### (B) 전역 자동 적용 (비권장)
- 현재는 `autoApply = false` 이므로 필드 단위로 명시합니다.
- 전역 적용이 필요하면 `autoApply = true` 로 바꿀 수 있으나, 모든 `Boolean` 필드에 적용되어  
  의도치 않은 컬럼까지 퍼질 수 있으므로 매우 신중해야 합니다.

---

## 3) Jackson – ObjectMapperFactory

### 3.1 개요
- 조직 표준 ObjectMapper 구성을 **단일 진입점**으로 제공
- 직렬화 포맷은 **고정(불변)**, 역직렬화만 **관대화**하는 원칙
- 주요 세팅(현행 코드):
  - `failOnUnknownProperties(false)`
  - `DEFAULT_VIEW_INCLUSION` 비활성
  - `WRITE_DATES_AS_TIMESTAMPS` 비활성
  - `LocalDate/LocalTime/LocalDateTime/YearMonth` 직렬화 포맷 고정
  - `LocalDateTime` 역직렬화 관대화 (다중 포맷 허용)

### 3.2 LocalDateTime 역직렬화 정책 (현행 반영)
- 1순위: 기존 고정 포맷 `yyyy-MM-dd HH:mm:ss`
- 2순위: `ISO_LOCAL_DATE_TIME` (예: `2025-09-21T06:00:00`)
- 3순위: 유연 포맷 `yyyy-MM-dd['T'][' ']HH:mm[:ss][.SSS][.SS][.S]`
- 모두 실패 시: Jackson 컨텍스트의 weird value 처리로 위임

> 주의: Serializer는 그대로 두므로 출력(JSON)은 기존과 동일 포맷을 유지합니다.

### 3.3 사용 방법

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
- `ObjectMapperUtils` 는 내부에서 `ObjectMapperFactory.defaultObjectMapper()` 를 사용합니다.

---

## 4) Jackson – ObjectMapperUtils

### 4.1 개요
- 안전한 JSON 변환/추출을 돕는 정적 유틸 모음
- 예외 발생 시 공통 예외(`CommonException`)로 래핑하여 일관된 에러 처리 제공
- 핵심 포인트(현행 반영):
  - `valueToObject(Object, Class)` 에서 입력이 `String`이면 `readValue`로 직접 역직렬화  
    (기존 `convertValue(String, POJO)` 부적합 이슈 방지)

### 4.2 대표 메서드

| 메서드 | 설명 |
|---|---|
| `writeValueAsString(obj)` | 객체 → JSON 문자열 |
| `readValue(json, Class<T>)` | JSON 문자열 → 타입 |
| `readValue(json, TypeReference<T>)` | 제네릭 타입 파싱 |
| `getFieldValueFromString(json, field, clz)` | 특정 필드만 추출/매핑 |
| `getFieldValueFromObject(obj, field, clz)` | Object → JSON → 필드 추출 |
| `valueToMap(obj)` | 객체 → `Map<String,Object>` |
| `valueToObject(obj, clz)` | Map/String → DTO 변환 |
| `convertToList(json, clz)` | JSON 배열 문자열 → `List<T>` |
| `convertTreeToValue(obj, clz)` | Tree 변환 경유 매핑 |
| `convertTreeToValues(Object[], clz)` | 배열 → 리스트 매핑 |
| `writeValue(outStream, obj)` | JSON 직렬화 후 스트림 기록 |

### 4.3 사용 예
~~~java
var json = ObjectMapperUtils.writeValueAsString(dto);
var view = ObjectMapperUtils.readValue(json, OrderView.class);

String code = ObjectMapperUtils.getFieldValueFromString(json, "code", String.class);

Map<String,Object> map = ObjectMapperUtils.valueToMap(dto);
OrderDto converted = ObjectMapperUtils.valueToObject(map, OrderDto.class);
~~~

---

## 5) 로깅 – @Correlate & CorrelationAspect

### 5.1 개요
- 메서드 호출 시 SpEL로 파라미터에서 비즈니스 키(예: `orderId`)를 추출하여:
  - `mdcKey` 지정 시 MDC 보조 키로 저장
  - `overrideTraceId=true`면 MDC["traceId"]를 동일 값으로 덮어씀
- 실행 후 기존 MDC 상태를 복원하여 누수/오염 방지

### 5.2 @Correlate (현행 속성)
- `paths`: 우선순위 SpEL 배열 (첫 성공 값 사용)
- `key`: 레거시/보조 단일 SpEL (paths 실패 시만 평가)
- `overrideTraceId`: 추출값으로 traceId 덮어쓰기 여부 (기본 true)
- `mdcKey`: 보조 MDC 키명(비어있으면 저장 안 함)

### 5.3 사용 예
~~~java
@Service
public class OrderService {

  @Correlate(paths = {"#cmd.orderId"}, mdcKey = "orderId", overrideTraceId = true)
  public void send(LocalOrderCommand cmd) {
    log.info("send kafka");
  }

  @Correlate(paths = {"#user.id"}, overrideTraceId = false) // traceId 유지, 보조키 저장 안함
  public void audit(User user) {
    log.info("audit");
  }
}
~~~

### 5.4 동작 규칙 (현행 구현)
1) `paths` 를 순서대로 평가하여 첫 성공 값을 사용
2) 모두 실패 시 `key` 를 평가(존재할 때만)
3) 값이 `null/blank`면 아무 것도 하지 않음
4) `mdcKey` 가 비어있지 않으면 MDC[mdcKey] = extracted
5) `overrideTraceId=true`면 MDC["traceId"] = extracted
6) finally에서 복원:
  - 보조키(mdcKey)는 이전 값 복원 또는 제거
  - traceId는 **실제로 변경하지 않았던 경우에만** 이전 값 복원/제거  
    (변경했다면 복원하지 않음: 도메인 키 기반 추적 유지)

---

## 6) TraceIdTurboFilter

### 6.1 목적
- 모든 로깅 이벤트에서 MDC["traceId"]가 비어있으면 UUID를 생성/주입하여 **항상 traceId 존재를 보장**

### 6.2 특징
- AOP 밖(초기화 로그, 프레임워크 로그, 배치/콘솔 로그) 구간까지 커버
- 이후 애플리케이션 레이어(@Correlate overrideTraceId=true)가 도메인 키를 확보하면 자연스럽게 덮어씀
- 극초기 상황에서도 로깅을 방해하지 않음(예외 삼킴)

---

## 7) 비동기 MDC 전파 – MdcPropagation

### 7.1 목적
- 현재 스레드의 MDC를 캡처하여 다른 스레드에서 실행되는 작업에 전파/복원
- `CompletableFuture.whenComplete`, 각종 콜백에서 사용

### 7.2 제공 API
- `wrap(Runnable)`
- `wrap(Consumer<T>)`
- `wrap(BiConsumer<T,U>)`

### 7.3 예시
~~~java
CompletableFuture
  .supplyAsync(() -> doWork())
  .whenComplete(MdcPropagation.wrap((r, e) -> {
    log.info("completed");
  }));
~~~

---

## 8) 메시징/헤더 접근 보조 – PathValueExtractor (현행)

- 프레임워크 타입에 컴파일 의존하지 않기 위해 리플렉션 기반 경로 탐색
- 지원 예:
  - `key`, `value`, `payload` (zero-arg 메서드/게터 탐색)
  - `headers.<k>` (Map / Kafka Headers / Spring MessageHeaders 형태를 유연하게 지원)
  - POJO getter/is-getter/필드 직접 접근 fallback

> 현재 `CorrelationAspect` 는 SpEL 기반 구현이지만,  
> `PathValueExtractor` 는 별도 유틸로서 메시징/래퍼 타입에서 키 추출이 필요한 구간에서 사용할 수 있습니다.

---

## 9) 통합 사용 가이드 (샘플)

### 9.1 의존성
~~~groovy
dependencies {
  implementation project(":order-common")
  implementation "org.springframework.boot:spring-boot-starter-web"
  implementation "org.springframework.boot:spring-boot-starter-aop"   // @Correlate AOP
  implementation "com.fasterxml.jackson.datatype:jackson-datatype-jsr310"
  // JPA 사용 시
  implementation "org.springframework.boot:spring-boot-starter-data-jpa"
}
~~~

### 9.2 오토컨피그 등록 (Boot 3.x)
- 경로: `order-common/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- 내용 예: (로깅/웹 자동구성 사용 시)
~~~text
org.example.order.common.autoconfigure.logging.LoggingAutoConfiguration
org.example.order.common.autoconfigure.web.WebAutoConfiguration
~~~

---

## 10) 트러블슈팅

- 엔티티 `Boolean`이 `"Y"/"N"`으로 저장되지 않음  
  → 해당 필드에 `@Convert(converter = BooleanToYNConverter.class)` 누락 여부 확인

- JSON 파싱이 특정 포맷에서 실패  
  → `LocalDateTime` 입력이 지원 포맷(기존/ISO/유연 포맷)인지 확인  
  → `ObjectMapperUtils.valueToObject` 사용 시 입력이 String이면 직접 역직렬화됨(
