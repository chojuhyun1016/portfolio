# 🌐 order-client.web 모듈

---

## 1) 모듈 개요 (현재 코드 기준)

Spring WebFlux **`WebClient`**를 팀 표준으로 **간단/안전/일관**되게 쓰기 위한 **경량 클라이언트 레이어**입니다.

| 구성요소 | 역할 | 핵심 포인트(코드 반영) |
|---|---|---|
| `WebClientUrlProperties` | `web-client.*` 네임스페이스 설정 바인딩 | `enabled` ON/OFF 스위치, `client.clientId`, `client.url.{order,user}`, `timeout(connect/read)`, `codec.max-bytes` |
| `WebClientConfig` | `WebClient` 빈 자동 구성 | **`web-client.enabled=true`일 때만 활성화**, Reactor Netty 타임아웃/압축/리다이렉트, **기본 헤더(X-USER-*) 주입**, Jackson 코덱/`maxInMemorySize` |
| `WebClientService` | 호출용 인터페이스 | 현재 **`get(url, headers, params, clz)`** 제공 (제네릭 타입 파싱) |
| `WebClientServiceImpl` | WebClient 기반 구현 | **`@ConditionalOnBean(WebClient)`** → WebClient 빈 있을 때만 활성, `UriComponentsBuilder`로 쿼리 파라미터 구성, `retrieve().bodyToMono(clz).block()` |
| **테스트** | IT/단위 검증 | `WebClientIT`(JDK `HttpServer`로 로컬 JSON 서버), `Enabled/Disabled` 테스트로 조건부 빈 생성 검증 |

> 현재 레이어는 **GET JSON** 호출을 간단히 처리하는 **코어 최소 기능**을 제공합니다. (POST/PUT 등은 확장 포인트로 §7에 제안)

---

## 2) 설정 (application.yml / profile)

### 2.1 최소/공통 설정 키 (코드 반영)
```yaml
web-client:
  enabled: true                # ✅ ON/OFF 스위치
  client:
    clientId: order-api        # 호출자 식별(로그/추적 용도 등 확장 가능)
    url:
      order: https://api.example.com/order   # 주문 API Base URL (옵션)
      user:  https://api.example.com/user    # 사용자 API Base URL (옵션)
  timeout:
    connect-ms: 3000           # CONNECT 타임아웃(ms)
    read-ms: 10000             # READ/응답 타임아웃(ms)
  codec:
    max-bytes: 2097152         # 디코딩 최대 메모리(바이트), 기본 2MiB
```

> `web-client.enabled=true`일 때만 `WebClientConfig`가 활성화되어 **WebClient 빈이 생성**됩니다.  
> ObjectMapper는 **컨텍스트에 이미 존재하면 재사용**, 없으면 `JavaTimeModule` 포함한 **fallback**을 생성합니다.

---

## 3) 기본 동작/흐름

```
web-client.enabled=true
        ↓
WebClientConfig
 ├─ Reactor Netty HttpClient (connect/read timeout, compress, redirect)
 ├─ ExchangeStrategies (Jackson Encoder/Decoder, maxInMemorySize)
 └─ WebClient (기본 헤더 주입: X-USER-ID, X-LOGIN-ID, X-USER-TYPE)
        ↓
WebClientServiceImpl.get(url, headers, params, clz)
        ↓
retrieve().bodyToMono(clz).block() → 동기 반환
```

- **기본 헤더**는 `AccessUserInfo.system()`과 `HttpConstant`를 사용해 `X-USER-ID`, `X-LOGIN-ID`, `X-USER-TYPE`를 자동 주입합니다.
- **타임아웃**: `read-ms`는 Reactor Netty의 responseTimeout과 Read/WriteTimeoutHandler 둘 다로 적용.
- **코덱**: Jackson 인코더/디코더를 명시 등록하고, **`maxInMemorySize`**를 설정합니다.

---

## 4) 빠른 시작 (가장 중요한 사용법)

### 4.1 GET JSON 호출
```java
@Service
@RequiredArgsConstructor
public class UserLookup {
    private final WebClientService webClientService;

    public Map<String, Object> findHello(String baseUrl, String name) {
        var params = new LinkedMultiValueMap<String, String>();
        params.add("name", name);

        @SuppressWarnings("unchecked")
        Map<String, Object> res = (Map<String, Object>) webClientService.get(
                baseUrl + "/hello",     // ex) "http://127.0.0.1:8080/hello"
                null,                   // 추가 헤더 없으면 null
                params,                 // 쿼리 파라미터
                Map.class               // 응답 타입
        );
        return res;
    }
}
```

### 4.2 헤더 추가 & 타입 매핑
```java
var headers = Map.of("X-REQUEST-ID", UUID.randomUUID().toString());

UserDto dto = (UserDto) webClientService.get(
        props.getClient().getUrl().getUser() + "/v1/users",
        headers,
        new LinkedMultiValueMap<>(),
        UserDto.class
);
```

> **주의**: 현재 구현은 **동기(block)** 반환입니다. 비동기가 필요하면 **서비스에서 `WebClient`를 직접 사용**하거나, §7 개선안처럼 `Mono<T>`를 반환하는 메서드 확장을 고려하세요.

---

## 5) 테스트 구성 (코드 반영 해설)

### 5.1 통합 테스트 `WebClientIT`
- **JDK `HttpServer`**를 포트 `0`(임의 포트)로 띄워 `/hello` 엔드포인트 제공
- `WebClientServiceImpl`로 GET 호출 → JSON 응답(`status`, `name`, `len`) 검증
- `@SpringBootTest(classes = {WebClientConfig.class, WebClientServiceImpl.class})`
    + `@TestPropertySource`로 `web-client.enabled=true`, timeout/codec 설정 주입

핵심 검증:
```java
Map<String, Object> res = (Map<String, Object>) webClientService.get(url, null, params, Map.class);
assertEquals("ok", res.get("status"));
assertEquals("neo", res.get("name"));
assertEquals(3, res.get("len"));
```

### 5.2 비활성화 테스트 `WebClientConfigDisabledTest`
- `web-client.enabled=false`일 때 **WebClient 빈이 없어야 함**
- `ApplicationContext`에서 `WebClient` 조회 시 `NoSuchBeanDefinitionException` 발생 검증

### 5.3 활성화 테스트 `WebClientConfigEnabledTest`
- `web-client.enabled=true`일 때 **WebClient** 및 **WebClientServiceImpl** 빈이 정상 생성됨을 검증
- 외부 호출 없이 빈 존재 여부만 확인

---

## 6) 예외/타임아웃/메모리

- **타임아웃**
    - 연결: `web-client.timeout.connect-ms` → `ChannelOption.CONNECT_TIMEOUT_MILLIS`
    - 응답/읽기: `web-client.timeout.read-ms` → `responseTimeout` + `ReadTimeoutHandler/WriteTimeoutHandler`
- **디코딩 메모리 제한**: `web-client.codec.max-bytes` → `ExchangeStrategies`의 `maxInMemorySize`
- **에러 처리**: 현재는 `retrieve()` 기본 동작(4xx/5xx 시 예외) 사용. 필요 시 `onStatus(...)`로 커스텀 매핑 가능(§7 참조).

---

## 7) 확장/개선 제안 (현 구조 유지 전제, 선택)

> **기존 코드를 바꾸지 않고도** 안전하게 확장 가능한 지점들입니다.

- **비동기 API 병행 제공**:
    - 현재 `get(..., Class<T>)`는 `block()`로 동기 반환.
    - 추가로 `Mono<T> getAsync(...)`를 제공하면 호출부에서 **리액티브 체인**을 활용 가능.
- **HTTP 메서드 확장**: `post/put/patch/delete` 메서드 오버로드(바디/헤더/쿼리/폼 지원).
- **에러 전략 표준화**: `onStatus`를 통해 4xx/5xx를 도메인 예외(`RemoteBadRequest`, `RemoteUnavailable` 등)로 매핑.
- **Base URL Util**: `WebClientUrlProperties.client.url.order/user` 활용 유틸 추가 (`url.getWithPathVariable(...)` 이미 존재).
- **공통 헤더 확장**: trace-id/tenant-id 삽입 인터셉터, 로깅(요청/응답 요약) 추가.
- **타입 세이프 응답**: `ParameterizedTypeReference<T>` 오버로드로 제네릭 컬렉션 응답 지원.

---

## 8) 운영 팁 & 권장 설정

- **타임아웃**은 환경(내부망/인터넷)에 맞게 프로필별로 조정(`local`은 짧게, `prod`는 여유).
- **최대 메모리(`maxInMemorySize`)**는 응답 페이로드 상한에 맞춰 설정(대용량 JSON은 스트리밍을 고려).
- **리트라이**가 필요한 외부 API는 호출부에서 `retryWhen`(리액티브) 또는 별도 재시도 유틸(동기)을 조합.
- **기본 헤더**(X-USER-*)는 시스템 계정 문맥으로 주입됩니다. 사용자 컨텍스트 전파가 필요하면 **별도 빈/필터**로 대체/오버라이드.

---

## 9) 핵심 코드 스니펫(반영 확인)

### 9.1 `WebClientConfig` 요지
```java
HttpClient httpClient = HttpClient.create()
    .responseTimeout(Duration.ofMillis(props.getTimeout().getReadMs()))
    .compress(true)
    .followRedirect(true)
    .doOnConnected(conn -> conn
        .addHandlerLast(new ReadTimeoutHandler(props.getTimeout().getReadMs(), MILLISECONDS))
        .addHandlerLast(new WriteTimeoutHandler(props.getTimeout().getReadMs(), MILLISECONDS)))
    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getTimeout().getConnectMs());

ExchangeStrategies strategies = ExchangeStrategies.builder()
    .codecs(this::customizeCodecs) // Jackson Encoder/Decoder + maxInMemorySize
    .build();

return WebClient.builder()
    .clientConnector(new ReactorClientHttpConnector(httpClient))
    .exchangeStrategies(strategies)
    .defaultHeaders(h -> {
        h.add(HttpConstant.X_USER_ID, accessUser.userId().toString());
        h.add(HttpConstant.X_LOGIN_ID, accessUser.loginId());
        h.add(HttpConstant.X_USER_TYPE, accessUser.userType());
    })
    .build();
```

### 9.2 `WebClientServiceImpl` 요지
```java
URI uri = UriComponentsBuilder.fromUriString(url)
        .queryParams(params)
        .build(true)
        .toUri();

return webClient.get()
        .uri(uri)
        .headers(h -> { if (headers != null) h.setAll(headers); })
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(clz)
        .block();
```

---

## 10) 마지막 한 줄 요약
**“설정은 `web-client.*` yml로 표준화, 사용은 `WebClientService.get()`에 집중.”**  
조건부 빈으로 **ON/OFF 제어**하고, 타임아웃/코덱/기본 헤더까지 **일관된 방식**으로 관리합니다.
