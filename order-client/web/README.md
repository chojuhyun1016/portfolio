# 🌐 order-client.web 모듈

---

## 1) 모듈 개요 (현재 코드 기준)

Spring WebFlux **`WebClient`**를 팀 표준으로 **간단/안전/일관**되게 쓰기 위한 **경량 클라이언트 레이어**입니다.  
최신 구조는 **설정 기반(@Bean) + `@Import` 조립**과 **조건부 빈 등록**으로, 필요할 때만 켜지도록 설계되었습니다.

| 구성요소 | 역할 | 핵심 포인트(코드 반영) |
|---|---|---|
| `WebClientModuleConfig` | 모듈 대표 Import | **내부 `WebClientConfig`만 로드**. 실제 빈 생성 여부는 프로퍼티로 제어 |
| `config.internal.WebClientConfig` | `WebClient` 자동 구성 | **`web-client.enabled=true`일 때만 활성화**. Reactor Netty 타임아웃/압축/리다이렉트, **기본 헤더(X-USER-*) 주입**, Jackson 코덱/`maxInMemorySize` |
| `property.WebClientUrlProperties` | `web-client.*` 설정 바인딩 | `enabled` ON/OFF, `client.clientId`, `client.url.{order,user}`, `timeout(connect/read)`, `codec.max-bytes` |
| `service.WebClientService` | 호출 인터페이스 | 현재 **`get(url, headers, params, clz)`** 제공 (제네릭 타입 파싱) |
| `service.impl.WebClientServiceImpl` | WebClient 기반 구현 | **`@ConditionalOnBean(WebClient)`** → WebClient 빈 존재 시에만 활성. `UriComponentsBuilder`로 쿼리 구성, `retrieve().bodyToMono(clz).block()` |
| **테스트** | IT/단위 검증 | `WebClientIT`(JDK `HttpServer`로 로컬 JSON 서버), `Enabled/Disabled` 테스트로 조건부 빈 생성 검증 |

> 현재 레이어는 **GET JSON** 호출을 간단히 처리하는 **코어 최소 기능**을 제공합니다. (POST/PUT 등은 §7 확장 포인트)

---

## 2) 설정 (application.yml / profile)

### 2.1 최소/공통 설정 키 (코드 반영)
```yaml
web-client:
  enabled: true                # ✅ ON/OFF 스위치 — true여야 WebClient 빈 생성
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
> `ObjectMapper`는 **컨텍스트에 이미 존재하면 재사용**, 없으면 `JavaTimeModule` 포함한 **fallback**을 생성합니다.

---

## 3) 구성/동작 흐름

```
web-client.enabled=true
        ↓
WebClientConfig (@ConditionalOnProperty "web-client.enabled=true")
 ├─ Reactor Netty HttpClient (connect/read timeout, compress, redirect)
 ├─ ExchangeStrategies (Jackson Encoder/Decoder, maxInMemorySize)
 └─ WebClient (기본 헤더: X-USER-ID, X-LOGIN-ID, X-USER-TYPE)
        ↓
WebClientServiceImpl.get(url, headers, params, clz)
        ↓
retrieve().bodyToMono(clz).block() → 동기 반환
```

- **기본 헤더**는 `AccessUserInfo.system()`과 `HttpConstant`를 사용해 `X-USER-ID`, `X-LOGIN-ID`, `X-USER-TYPE`를 자동 주입합니다.
- **타임아웃**: `read-ms`는 Reactor Netty의 `responseTimeout`과 `Read/WriteTimeoutHandler` 양쪽에 적용.
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

> **주의**: 현재 구현은 **동기(block)** 반환입니다. 비동기가 필요하면 **서비스에서 `WebClient`를 직접 사용**하거나, §7처럼 `Mono<T>`를 반환하는 메서드 확장을 고려하세요.

---

## 5) 핵심 코드 스니펫(반영 확인)

### 5.1 `config.internal.WebClientConfig` 요지
```java
@Configuration
@EnableConfigurationProperties(WebClientUrlProperties.class)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "web-client", name = "enabled", havingValue = "true")
public class WebClientConfig {

    private final ObjectProvider<ObjectMapper> objectMapperProvider;
    private final WebClientUrlProperties props;

    @Bean
    public WebClient webClient() {
        AccessUserInfo accessUser = AccessUserInfo.system();

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(props.getTimeout().getReadMs()))
                .compress(true)
                .followRedirect(true)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(props.getTimeout().getReadMs(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(props.getTimeout().getReadMs(), TimeUnit.MILLISECONDS)))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getTimeout().getConnectMs());

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(this::customizeCodecs)
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
    }

    private void customizeCodecs(ClientCodecConfigurer c) {
        ObjectMapper om = objectMapperProvider.getIfAvailable(this::fallbackObjectMapper);
        c.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(om, MediaType.APPLICATION_JSON));
        c.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(om, MediaType.APPLICATION_JSON));
        c.defaultCodecs().maxInMemorySize(props.getCodec().getMaxBytes());
    }

    private ObjectMapper fallbackObjectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
    }
}
```

### 5.2 `service.impl.WebClientServiceImpl` 요지
```java
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(WebClient.class)
public class WebClientServiceImpl implements WebClientService {

    private final WebClient webClient;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String url, Map<String, String> headers, MultiValueMap<String, String> params, Class<T> clz) {
        URI uri = UriComponentsBuilder.fromUriString(url)
                .queryParams(params)
                .build(true)
                .toUri();

        log.info("WebClient GET → uri: {}", uri);

        return webClient.get()
                .uri(uri)
                .headers(h -> { if (headers != null) h.setAll(headers); })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(clz)
                .block();
    }
}
```

### 5.3 대표 모듈 Import (`WebClientModuleConfig`)
```java
@Configuration
@Import({org.example.order.client.web.config.internal.WebClientConfig.class})
public class WebClientModuleConfig {}
```

---

## 6) 테스트 구성 (코드 반영 해설)

### 6.1 비활성화 테스트 — `WebClientConfigDisabledTest`
- `web-client.enabled=false`일 때 **WebClient 빈이 없어야 함**
- `ApplicationContext`에서 `WebClient` 조회 시 `NoSuchBeanDefinitionException` 검증

```java
@SpringBootTest(classes = WebClientConfig.class)
@TestPropertySource(properties = { "web-client.enabled=false" })
class WebClientConfigDisabledTest {

    @Autowired ApplicationContext ctx;

    @Test
    @DisplayName("web-client.enabled=false → WebClient 빈 없음")
    void webClientBeanAbsent() {
        assertThrows(NoSuchBeanDefinitionException.class,
                () -> ctx.getBean(org.springframework.web.reactive.function.client.WebClient.class));
    }
}
```

### 6.2 활성화 테스트 — `WebClientConfigEnabledTest`
- `web-client.enabled=true`일 때 **WebClient** 및 **WebClientServiceImpl** 빈이 정상 생성됨을 검증

```java
@SpringBootTest(classes = {WebClientConfig.class, WebClientServiceImpl.class})
@TestPropertySource(properties = {
        "web-client.enabled=true",
        "web-client.timeout.connect-ms=1000",
        "web-client.timeout.read-ms=2000",
        "web-client.codec.max-bytes=2097152"
})
class WebClientConfigEnabledTest {

    @Autowired ApplicationContext ctx;

    @Test
    @DisplayName("web-client.enabled=true → WebClient/Service 빈 생성")
    void webClientBeansPresent() {
        WebClient wc = ctx.getBean(WebClient.class);
        assertNotNull(wc);

        WebClientServiceImpl svc = ctx.getBean(WebClientServiceImpl.class);
        assertNotNull(svc);
    }
}
```

### 6.3 통합 테스트 — `WebClientIT`
- **JDK `HttpServer`**를 포트 `0`(임의 포트)로 띄워 `/hello` 엔드포인트 제공
- `WebClientServiceImpl`로 GET 호출 → JSON 응답(`status`, `name`, `len`) 검증

```java
@SpringBootTest(classes = {WebClientConfig.class, WebClientServiceImpl.class})
@TestPropertySource(properties = {
        "web-client.enabled=true",
        "web-client.timeout.connect-ms=2000",
        "web-client.timeout.read-ms=5000",
        "web-client.codec.max-bytes=2097152"
})
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebClientIT {

    static HttpServer server;
    static int port;

    @Autowired WebClientService webClientService;

    @BeforeAll
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/hello", new JsonHandler());
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
    }

    @AfterAll
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    @DisplayName("WebClientService → GET 호출/응답 JSON 파싱")
    void callLocalJsonEndpoint() {
        String url = "http://127.0.0.1:" + port + "/hello";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "neo");

        @SuppressWarnings("unchecked")
        Map<String, Object> res = (Map<String, Object>) webClientService.get(url, null, params, Map.class);

        assertEquals("ok", res.get("status"));
        assertEquals("neo", res.get("name"));
        assertEquals(3, res.get("len"));
    }

    static class JsonHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            String query = uri.getQuery();
            String name = (query != null && query.startsWith("name="))
                    ? query.substring("name=".length())
                    : "unknown";

            String body = String.format("{\"status\":\"ok\",\"name\":\"%s\",\"len\":%d}", name, name.length());
            exchange.getResponseHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
        }
    }
}
```

---

## 7) 예외/타임아웃/메모리

- **타임아웃**
  - 연결: `web-client.timeout.connect-ms` → `ChannelOption.CONNECT_TIMEOUT_MILLIS`
  - 응답/읽기: `web-client.timeout.read-ms` → `responseTimeout` + `Read/WriteTimeoutHandler`
- **디코딩 메모리 제한**: `web-client.codec.max-bytes` → `ExchangeStrategies`의 `maxInMemorySize`
- **에러 처리**: 기본은 `retrieve()`의 표준 동작(4xx/5xx 예외). 필요 시 `onStatus(...)`로 도메인 예외 매핑 권장.

---

## 8) 확장/개선 제안 (현 구조 유지 전제, 선택)

- **비동기 API 병행 제공**: `Mono<T> getAsync(...)` 제공해 호출부 리액티브 체인을 활용.
- **HTTP 메서드 확장**: `post/put/patch/delete` 메서드 오버로드(바디/헤더/쿼리/폼 지원).
- **에러 전략 표준화**: 4xx/5xx → `RemoteBadRequest`, `RemoteUnavailable` 등으로 매핑.
- **Base URL Util**: `WebClientUrlProperties.client.url.order/user` 활용 유틸 추가 (`getWithPathVariable(...)` 이미 제공).
- **공통 헤더 확장**: trace-id/tenant-id 삽입 인터셉터, 요청/응답 요약 로깅.

---

## 9) 마지막 한 줄 요약
**“설정은 `web-client.*` yml로 표준화, 사용은 `WebClientService.get()`에 집중.”**  
조건부 빈으로 **ON/OFF 제어**하고, 타임아웃/코덱/기본 헤더까지 **일관된 방식**으로 관리합니다.
