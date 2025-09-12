# 🌐 Web 공통 모듈 – API 응답, 사용자 주입, 요청 상관관계(MDC) 브리지

`order-common`의 **웹 레이어 공통 컴포넌트**를 정리했습니다. 컨트롤러 응답 표준화, 게이트웨이 헤더→도메인 객체 주입, 그리고 요청 단위 상관관계 ID(requestId/traceId) 브리지를 제공합니다.

---

## 1) 구성 개요

| 구성 요소 | 목적 | 비고 |
|---|---|---|
| **`ApiResponse<T>`** | API 응답 바디 표준 클래스 | `data` + `metadata(ResponseMeta)` / 정적 팩토리 제공 |
| **`ResponseMeta`** | 응답 메타(코드/메시지/타임스탬프) | `ok()`, `of(code,msg)` |
| **`AccessUserArgumentResolver`** | 게이트웨이 헤더 → `AccessUserInfo` 주입 | `X-User-* / X-Client-*` 헤더 파싱 |
| **`CorrelationIdFilter`** | `X-Request-Id` ↔ MDC 브리지 | `MDC["requestId"]`, `MDC["traceId"]` 초기화/복원 |

> 원칙: **컨트롤러 진입 전(필터)**에 **요청 상관관계 ID**를 확보하고,  
> 컨트롤러 내부에서는 **표준 응답 스키마**로 반환합니다.

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

### 2.2 필터 자동 등록 (권장: 오토컨피그 사용)
- `order-common`에 포함된 `WebCommonAutoConfiguration`이 `CorrelationIdFilter`를 자동 등록합니다.
- 별도 설정이 없다면, 기본 순서(아주 이른 순서)로 모든 요청 경로에 적용됩니다.

> 수동 등록이 필요하면 `FilterRegistrationBean<CorrelationIdFilter>`를 앱에서 직접 정의하세요.

### 2.3 `AccessUserInfo` 주입 활성화
- `AccessUserArgumentResolver`는 *스프링 MVC 설정*에 등록해야 합니다.

~~~java
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new AccessUserArgumentResolver());
  }
}
~~~

> 이후 컨트롤러 메서드 파라미터에 `AccessUserInfo` 타입을 선언하면 자동 주입됩니다.

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
  private Integer code;         // HTTP 상태 코드(또는 도메인 코드)
  private String  msg;          // 메시지(상태/오류 등)
  private LocalDateTime timestamp;

  public static ResponseMeta of(Integer code, String msg) { ... }
  public static ResponseMeta ok() { ... } // 200 OK + now()
}
~~~

### 3.2 컨트롤러 사용 예

#### OK 응답
~~~java
@GetMapping("/orders/{id}")
public ResponseEntity<ApiResponse<OrderView>> get(@PathVariable Long id) {
  OrderView view = service.find(id);
  return ApiResponse.ok(view); // 200 OK + metadata.ok()
}
~~~

#### Accepted 응답
~~~java
@PostMapping("/orders")
public ResponseEntity<ApiResponse<OrderCommandResult>> create(@RequestBody CreateOrder cmd) {
  var result = service.create(cmd);
  return ApiResponse.accepted(result); // 202 Accepted + metadata.ok()
}
~~~

#### 예외 → 에러 응답 (도메인 공통 예외)
~~~java
@ExceptionHandler(CommonException.class)
public ResponseEntity<ApiResponse<Void>> handle(CommonException e) {
  return ApiResponse.error(e); // e.getHttpStatus(), e.getCode(), e.getMessage()
}
~~~

#### 예외 코드 → 에러 응답 (정적 코드 사용)
~~~java
@ExceptionHandler(IllegalArgumentException.class)
public ResponseEntity<ApiResponse<Void>> badRequest(IllegalArgumentException e) {
  return ApiResponse.error(CommonExceptionCode.INVALID_REQUEST);
}
~~~

> 팁: 전역 `@ControllerAdvice`에서 `CommonException`을 처리하면, 컨트롤러는 **정상 흐름**에 집중할 수 있습니다.

---

## 4) AccessUserArgumentResolver

### 4.1 헤더 매핑 규칙

| 헤더 | 필수 | 매핑 대상 |
|---|---|---|
| `X-User-Id` | 선택 | `AccessUserInfo.userId` (`Long`, 파싱 실패/없음 → `0L`) |
| `X-Login-Id` | 선택 | `AccessUserInfo.loginId` (기본 `""`) |
| `X-User-Type` | 선택 | `AccessUserInfo.userType` (기본 `"UNKNOWN"`) |
| `X-Client-Roles` | 선택 | `AccessUserInfo.roles` (기본 `""`) |
| `X-Client-Groups` | 선택 | `AccessUserInfo.groups` (기본 `""`) |

- `userId == 0L` **그리고** `loginId`가 비어 있으면 → `AccessUserInfo.unknown()` 반환

### 4.2 컨트롤러 사용 예
~~~java
@GetMapping("/me")
public ResponseEntity<ApiResponse<UserProfile>> me(AccessUserInfo user) {
  if (user.isUnknown()) throw new CommonException(CommonExceptionCode.UNAUTHORIZED);
  UserProfile profile = service.load(user.getUserId());
  return ApiResponse.ok(profile);
}
~~~

> 주의: 게이트웨이/프록시에서 위 헤더를 **정상적으로 전달**해야 합니다.

---

## 5) CorrelationIdFilter – 요청 상관관계 브리지

### 5.1 동작 개요
1) 요청 헤더 `X-Request-Id`를 확인
    - 없거나 공백이면 새 UUID 생성
2) `MDC["requestId"] = id`
3) 기존 `MDC["traceId"]`가 비어 있으면 **브리지**: `MDC["traceId"] = id`
4) 응답 헤더에도 `X-Request-Id: id` 설정
5) 체인 종료 후, **필터 진입 시점의 MDC 상태로 복원**

> 결과적으로, 애플리케이션 로그 패턴에서 `%X{requestId}`와 `%X{traceId}`가 안정적으로 출력됩니다.

### 5.2 로그 패턴 예 (Logback)
~~~xml
<encoder>
  <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level [req:%X{requestId:-NA}] [trace:%X{traceId:-NA}] %logger - %msg%n</pattern>
</encoder>
~~~

### 5.3 커스터마이징 포인트
- **헤더명 변경**: `HEADER` 상수(`"X-Request-Id"`) 교체
- **MDC 키 변경**: `MDC_REQUEST_ID`, `MDC_TRACE_ID` 상수 조정
- **필터 순서**: `FilterRegistrationBean#setOrder(...)` 로 조정(가능한 **아주 이른 순서** 권장)

---

## 6) 운영 체크리스트 (Best Practice)

- **전역 예외 처리**: API 응답 스키마를 일관되게 유지하려면 `@ControllerAdvice`에서 `ApiResponse.error(...)` 활용
- **게이트웨이 연동**: `X-Request-Id`(혹은 동등 헤더) **미설정 시 생성/전달**하도록 API Gateway/Ingress 설정
- **로깅 필드 표준화**: 로그 패턴에 `%X{requestId}`, `%X{traceId}` 포함
- **병렬/비동기 환경**: 별도의 스레드풀(@Async/스케줄러/리액티브)에서는 **MDC 전파(TaskDecorator)** 적용
- **보안 주의**: 헤더 기반 `AccessUserInfo`는 *신뢰 가능한 경로*에서만 사용(내부망/인증된 게이트웨이)

---

## 7) 트러블슈팅

- **로그에 `traceId`가 비어 있음**
    - 필터보다 앞서 `traceId`를 덮어쓰는 필터/AOP가 있는지 확인
    - 필터 등록 순서를 가장 앞쪽으로 조정

- **컨트롤러에서 `AccessUserInfo`가 `unknown`**
    - 게이트웨이에서 사용자 헤더 전달 여부 확인
    - 헤더 키 오타, 스프링 MVC 설정에서 `addArgumentResolvers` 누락 여부 점검

- **응답 스키마가 섞임**
    - 일부 컨트롤러가 `ResponseEntity<T>`를 직접 반환하고 `ApiResponse<T>`를 사용하지 않는지 확인
    - 전역 예외 처리에서 일괄 적용하도록 패턴 정리

---

## 8) 클래스 다이어그램(개념)

~~~text
CorrelationIdFilter (OncePerRequestFilter)
  ├─ read "X-Request-Id" → MDC["requestId"]
  ├─ (if MDC["traceId"] blank) → MDC["traceId"] = requestId
  └─ write "X-Request-Id" to response

AccessUserArgumentResolver (HandlerMethodArgumentResolver)
  └─ read headers → AccessUserInfo (fallback: unknown)

ApiResponse<T>
  └─ static factory: ok / accepted / error(CommonException|Code)

ResponseMeta
  ├─ code / msg / timestamp
  ├─ ok() : (200, "OK", now)
  └─ of(code, msg)
~~~

---

## 9) 코드 원문

### 9.1 `ApiResponse<T>`
~~~java
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ApiResponse<T> {

    private T data;
    private ResponseMeta metadata;

    public static <T> ResponseEntity<ApiResponse<T>> of(final HttpStatus status, final T data, final ResponseMeta metadata) {
        return ResponseEntity.status(status).body(new ApiResponse<>(data, metadata));
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(final T data) {
        return of(HttpStatus.OK, data, ResponseMeta.ok());
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(CommonException e) {
        return of(e.getHttpStatus(), null, ResponseMeta.of(e.getCode(), e.getMessage()));
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(CommonExceptionCode code) {
        return of(code.getHttpStatus(), null, ResponseMeta.of(code.getCode(), code.getMsg()));
    }

    public static <T> ResponseEntity<ApiResponse<T>> accepted(final T data) {
        return of(HttpStatus.ACCEPTED, data, ResponseMeta.ok());
    }
}
~~~

### 9.2 `ResponseMeta`
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

### 9.3 `AccessUserArgumentResolver`
~~~java
public class AccessUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String H_USER_ID = "X-User-Id";
    public static final String H_LOGIN_ID = "X-Login-Id";
    public static final String H_USER_TYPE = "X-User-Type";
    public static final String H_CLIENT_ROLES = "X-Client-Roles";
    public static final String H_CLIENT_GROUPS = "X-Client-Groups";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AccessUserInfo.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory) {

        String userIdStr = webRequest.getHeader(H_USER_ID);
        String loginId = nvl(webRequest.getHeader(H_LOGIN_ID), "");
        String userType = nvl(webRequest.getHeader(H_USER_TYPE), "UNKNOWN");
        String roles = nvl(webRequest.getHeader(H_CLIENT_ROLES), "");
        String groups = nvl(webRequest.getHeader(H_CLIENT_GROUPS), "");

        Long userId = parseLong(userIdStr);

        if (userId == 0L && loginId.isBlank()) {
            return AccessUserInfo.unknown();
        }

        return AccessUserInfo.builder()
                .userId(userId)
                .loginId(loginId)
                .userType(userType)
                .roles(roles)
                .groups(groups)
                .build();
    }

    private static String nvl(String v, String def) {
        return StringUtils.hasText(v) ? v : def;
    }

    private static Long parseLong(String v) {
        if (!StringUtils.hasText(v)) {
            return 0L;
        }

        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
~~~

### 9.4 `CorrelationIdFilter`
~~~java
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_REQUEST_ID = "requestId";
    public static final String MDC_TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String id = req.getHeader(HEADER);

        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }

        String prevRequestId = MDC.get(MDC_REQUEST_ID);
        String prevTraceId = MDC.get(MDC_TRACE_ID);

        MDC.put(MDC_REQUEST_ID, id);

        if (prevTraceId == null || prevTraceId.isBlank()) {
            // traceId 없으면 requestId로 브리지
            MDC.put(MDC_TRACE_ID, id);
        }

        try {
            res.setHeader(HEADER, id);
            chain.doFilter(req, res);
        } finally {
            // 기존 값 복원
            if (prevRequestId != null) {
                MDC.put(MDC_REQUEST_ID, prevRequestId);
            } else {
                MDC.remove(MDC_REQUEST_ID);
            }

            if (prevTraceId != null) {
                MDC.put(MDC_TRACE_ID, prevTraceId);
            } else {
                MDC.remove(MDC_TRACE_ID);
            }
        }
    }
}
~~~

---

## 10) 마지막 한 줄 요약
**표준 응답(데이터+메타) + 사용자 컨텍스트 주입 + 요청/추적 ID 브리지**를 기본 제공하여,  
컨트롤러/로그의 **일관성**과 **운영 가시성**을 손쉽게 확보합니다.
