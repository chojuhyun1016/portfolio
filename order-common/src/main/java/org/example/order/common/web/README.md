# 🌐 Web 공통 모듈 – API 응답, 사용자 주입, 요청 상관관계(MDC) 브리지

`order-common`의 **웹 레이어 공통 컴포넌트**를 정리했습니다.  
컨트롤러 응답 표준화, 게이트웨이 헤더 → 도메인 사용자 객체 주입,  
그리고 요청 단위 상관관계 ID(`requestId` / `traceId`) 브리지를 제공합니다.

---

## 1) 구성 개요

| 구성 요소 | 목적 | 비고 |
|---|---|---|
| **`ApiResponse<T>`** | API 응답 바디 표준 클래스 | `data` + `metadata(ResponseMeta)` / 정적 팩토리 제공 |
| **`ResponseMeta`** | 응답 메타(코드/메시지/타임스탬프) | `ok()`, `of(code,msg)` |
| **`AccessUserArgumentResolver`** | 게이트웨이 헤더 → `AccessUserInfo` 주입 | `X-User-* / X-Client-*` 헤더 파싱 |
| **`CorrelationIdFilter`** | `X-Request-Id` ↔ MDC 브리지 | `MDC["requestId"]`, `MDC["traceId"]` 초기화/복원 |
| **`WebAutoConfiguration`** | 웹 공통 자동 구성 | `CorrelationIdFilter`를 최상위 우선순위로 자동 등록 |

> 원칙
> - **컨트롤러 진입 전(Filter)** 에 요청 상관관계 ID를 확보
> - 컨트롤러 내부에서는 **표준 응답 스키마(ApiResponse)** 만 사용
> - MDC 설정/복원은 웹 공통 레이어에서 책임

---

## 2) 빠른 시작

### 2.1 의존성
~~~groovy
dependencies {
  implementation project(":order-common")
  implementation "org.springframework.boot:spring-boot-starter-web"
  implementation "org.springframework.boot:spring-boot-starter-logging"
}
~~~

### 2.2 필터 자동 등록 (권장)

- `order-common`의  
  **`org.example.order.common.autoconfigure.web.WebAutoConfiguration`** 이
  `CorrelationIdFilter`를 자동 등록합니다.
- 기본 동작:
  - `FilterRegistrationBean` 사용
  - `Ordered.HIGHEST_PRECEDENCE` 에 가까운 순서
  - URL 패턴: `/*`

> ⚠️ 기존 문서에 있던 `WebCommonAutoConfiguration` 은 **현재 사용되지 않습니다.**  
> 현행 기준은 **`WebAutoConfiguration` 단일 진입점**입니다.

---

### 2.3 `AccessUserInfo` 주입 활성화

`AccessUserArgumentResolver` 는 **Spring MVC 설정에 명시적으로 등록**해야 합니다.

~~~java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new AccessUserArgumentResolver());
  }
}
~~~

이후 컨트롤러 메서드 파라미터에 `AccessUserInfo` 타입을 선언하면 자동 주입됩니다.

---

## 3) ApiResponse / ResponseMeta

### 3.1 응답 구조

~~~java
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ApiResponse<T> {

    private T data;
    private ResponseMeta metadata;
}
~~~

`ResponseMeta`:

~~~java
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@ToString
public class ResponseMeta {

    private Integer code;
    private String msg;
    private LocalDateTime timestamp;

    public static ResponseMeta of(Integer code, String msg) {
        return new ResponseMeta(code, msg, LocalDateTime.now());
    }

    public static ResponseMeta ok() {
        return of(HttpStatus.OK.value(), HttpStatus.OK.name());
    }
}
~~~

---

### 3.2 컨트롤러 사용 예

#### OK 응답
~~~java
@GetMapping("/orders/{id}")
public ResponseEntity<ApiResponse<OrderView>> get(@PathVariable Long id) {
    OrderView view = service.find(id);
    return ApiResponse.ok(view);
}
~~~

#### Accepted 응답
~~~java
@PostMapping("/orders")
public ResponseEntity<ApiResponse<OrderCommandResult>> create(@RequestBody CreateOrder cmd) {
    var result = service.create(cmd);
    return ApiResponse.accepted(result);
}
~~~

#### 공통 예외 → 에러 응답
~~~java
@ExceptionHandler(CommonException.class)
public ResponseEntity<ApiResponse<Void>> handle(CommonException e) {
    return ApiResponse.error(e);
}
~~~

#### 코드 기반 에러 응답
~~~java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<ApiResponse<Void>> badRequest(IllegalArgumentException e) {
    return ApiResponse.error(CommonExceptionCode.INVALID_REQUEST);
}
~~~

> 권장: 전역 `@ControllerAdvice` 에서 `CommonException`을 처리하여  
> 컨트롤러는 **정상 플로우**에만 집중

---

## 4) AccessUserArgumentResolver

### 4.1 헤더 매핑 규칙 (현행 코드)

| 헤더 | 필수 | 매핑 대상 |
|---|---|---|
| `X-User-Id` | 선택 | `AccessUserInfo.userId` (`Long`, 실패/없음 → `0L`) |
| `X-Login-Id` | 선택 | `AccessUserInfo.loginId` (기본 `""`) |
| `X-User-Type` | 선택 | `AccessUserInfo.userType` (기본 `"UNKNOWN"`) |
| `X-Client-Roles` | 선택 | `AccessUserInfo.roles` (기본 `""`) |
| `X-Client-Groups` | 선택 | `AccessUserInfo.groups` (기본 `""`) |

- `userId == 0L` **AND** `loginId` blank  
  → `AccessUserInfo.unknown()` 반환

---

### 4.2 컨트롤러 사용 예
~~~java
@GetMapping("/me")
public ResponseEntity<ApiResponse<UserProfile>> me(AccessUserInfo user) {
    if (user.isUnknown()) {
        throw new CommonException(CommonExceptionCode.UNAUTHORIZED);
    }
    UserProfile profile = service.load(user.getUserId());
    return ApiResponse.ok(profile);
}
~~~

> ⚠️ 게이트웨이/프록시에서 헤더 전달이 **신뢰 가능하게 보장**되어야 합니다.

---

## 5) CorrelationIdFilter – 요청 상관관계 브리지

### 5.1 동작 개요 (현행 구현)

1. 요청 헤더 `X-Request-Id` 확인
  - 없거나 blank → UUID 생성
2. `MDC["requestId"] = id`
3. 기존 `MDC["traceId"]` 가 비어 있으면  
   → `MDC["traceId"] = id` (브리지)
4. 응답 헤더 `X-Request-Id: id` 설정
5. 체인 종료 후 **필터 진입 시점의 MDC 상태로 정확히 복원**

> 결과: 로그 패턴에서 `%X{requestId}`, `%X{traceId}` 가 항상 안정적으로 출력

---

### 5.2 로그 패턴 예 (Logback)

~~~xml
<encoder>
  <pattern>
    %d{HH:mm:ss.SSS}
    [%thread]
    %-5level
    [req:%X{requestId:-NA}]
    [trace:%X{traceId:-NA}]
    %logger - %msg%n
  </pattern>
</encoder>
~~~

---

### 5.3 커스터마이징 포인트

- **헤더명 변경**
  - `CorrelationIdFilter.HEADER`
- **MDC 키 변경**
  - `MDC_REQUEST_ID`, `MDC_TRACE_ID`
- **필터 순서 조정**
  - 사용자 정의 `FilterRegistrationBean<CorrelationIdFilter>` 제공

---

## 6) 운영 체크리스트 (Best Practice)

- 전역 예외 처리에서 `ApiResponse.e
