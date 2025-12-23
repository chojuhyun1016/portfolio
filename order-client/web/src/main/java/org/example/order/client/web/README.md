# 🌐 order-client.web 모듈

---

## 1) 모듈 개요 (현행 코드 기준)

Spring WebFlux **`WebClient`**를 팀 표준으로 **간단·안전·일관**되게 사용하기 위한  
**경량 HTTP 클라이언트 인프라 모듈**이다.

본 모듈은 **AutoConfiguration + 조건부 빈 등록** 방식으로 구성되며,  
`web.enabled=true` 일 때만 활성화되어 **WebClient / WebService** 빈을 노출한다.

| 구성요소 | 역할 | 핵심 포인트 (현행 코드 반영) |
|---|---|---|
| `WebAutoConfiguration` | 모듈 자동 구성 진입점 | `web.enabled=true`일 때만 활성, WebClient/WebService 생성 |
| `WebUrlProperties` | 설정 바인딩 | `web.*` 네임스페이스, timeout/codec/client.url 관리 |
| `WebClient` | HTTP 클라이언트 | Reactor Netty 기반, 타임아웃/압축/리다이렉트 설정 |
| `WebService` | 호출 인터페이스 | GET/POST 공통 추상화 |
| `WebServiceImpl` | WebClient 래퍼 구현 | 동기(block) 호출, 헤더/쿼리 파라미터 처리 |

> 특징
> - **WebClient 클래스 존재 시에만** 활성 (`@ConditionalOnClass`)
> - **시스템 계정 헤더(X-USER-*)를 기본 헤더로 자동 주입**
> - ObjectMapper는 **컨텍스트 재사용 우선**, 없으면 **fallback 생성**

---

## 2) 활성 조건 및 설계 원칙

- **활성 스위치**
  - `web.enabled=true` → WebAutoConfiguration 활성
- **빈 생성 정책**
  - `WebClient` : `@ConditionalOnMissingBean`
  - `WebService` : `@ConditionalOnMissingBean`
- **사용 방식**
  - 모든 호출은 `WebService`를 통해 수행
  - 내부적으로 `WebClient`를 직접 노출하지 않음
- **동기 정책**
  - 현재 구현은 `block()` 기반 **동기 호출**
  - 비동기/리액티브 체인이 필요하면 확장 포인트로 분리

---

## 3) 설정 (application.yml)

### 3.1 기본 설정 (현행 코드 기준)

    web:
      enabled: true
      client:
        client-id: order-service
        url:
          order: https://api.example.com/order
          user:  https://api.example.com/user
      timeout:
        connect-ms: 3000
        read-ms: 10000
      codec:
        max-bytes: 2097152

설명:

- `web.enabled`
  - **모듈 ON/OFF 스위치**
  - false면 WebClient/WebService 빈이 생성되지 않는다.
- `web.client.client-id`
  - 현재는 내부에서 직접 사용하지 않지만,
    로그/확장 목적의 호출자 식별 값
- `web.client.url.*`
  - 외부 API Base URL 보관용
  - URL 조합은 호출부 또는 `getWithPathVariable` 활용
- `web.timeout.connect-ms`
  - TCP 연결 타임아웃(ms)
- `web.timeout.read-ms`
  - 응답/읽기 타임아웃(ms)
  - Reactor Netty의 `responseTimeout` + `Read/WriteTimeoutHandler`에 동시에 적용
- `web.codec.max-bytes`
  - JSON 디코딩 시 최대 메모리 크기

---

## 4) 빈 구성 및 동작 흐름

    web.enabled=true
        ↓
    WebAutoConfiguration
        ↓
    WebClient (@ConditionalOnMissingBean)
        - Reactor Netty HttpClient
          - connectTimeout
          - responseTimeout
          - Read/WriteTimeoutHandler
          - compress(true)
          - followRedirect(true)
        - ExchangeStrategies
          - Jackson Encoder/Decoder
          - maxInMemorySize 설정
        - defaultHeaders
          - X-USER-ID
          - X-LOGIN-ID
          - X-USER-TYPE
        ↓
    WebServiceImpl
        - WebClient 래핑
        - GET / POST API 제공
        - retrieve().bodyToMono(...).block()

---

## 5) 기본 헤더 정책

- WebClient 생성 시 **항상 기본 헤더 주입**
- 헤더 값:
  - `X-USER-ID`
  - `X-LOGIN-ID`
  - `X-USER-TYPE`
- 값 출처:
  - `AccessUserInfo.system()`
- 목적:
  - 내부 서비스 간 호출 시 **시스템 계정 컨텍스트 통일**
  - 인증/감사/로깅 확장에 대비

---

## 6) 사용법 (현행 API 기준)

### 6.1 GET 호출

    @Autowired
    private WebService webService;

    public Object callGet() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "neo");

        return webService.get(
                "http://localhost:8080/hello",
                null,
                params,
                Map.class
        );
    }

### 6.2 POST 호출

    public Object callPost() {
        Map<String, String> headers = Map.of("X-REQUEST-ID", "req-123");

        return webService.post(
                "http://localhost:8080/submit",
                headers,
                Map.of("value", 1),
                Map.class
        );
    }

주의 사항:
- 반환 타입은 `<T> Object`
- 호출부에서 캐스팅 책임을 가진다.
- body가 null이면 내부적으로 `new Object()`가 전송된다.

---

## 7) ObjectMapper 정책

- 우선순위:
  1. Spring Context에 이미 등록된 `ObjectMapper`
  2. fallback ObjectMapper
    - `JsonMapper`
    - `JavaTimeModule` 등록
- 적용 범위:
  - Jackson2JsonEncoder
  - Jackson2JsonDecoder
- MediaType:
  - `application/json` 고정

---

## 8) 에러/타임아웃/메모리 정책

- **에러 처리**
  - 기본은 `retrieve()`의 표준 동작
  - 4xx/5xx → WebClientResponseException 발생
- **타임아웃**
  - connect: `CONNECT_TIMEOUT_MILLIS`
  - read: `responseTimeout` + Netty Read/WriteTimeoutHandler
- **메모리**
  - `ExchangeStrategies`의 `maxInMemorySize`로 제한
  - 대용량 응답 시 OOM 방지

---

## 9) 테스트 전략 (권장)

현행 코드에는 테스트가 포함되어 있지 않으나, 구조상 권장 테스트는 다음과 같다.

1) 조건부 빈 테스트
- `web.enabled=false`
  - WebClient/WebService 빈 미생성
- `web.enabled=true`
  - WebClient/WebService 빈 생성

2) 통합 테스트
- JDK `HttpServer` 또는 MockWebServer 사용
- GET/POST JSON 응답 검증
- 타임아웃/헤더 주입 여부 확인

---

## 10) 확장 포인트

- 비동기 API
  - `Mono<T> getAsync(...)`
- HTTP 메서드 확장
  - PUT / PATCH / DELETE
- 에러 매핑
  - onStatus(...) → 도메인 예외 변환
- 공통 헤더 확장
  - trace-id / tenant-id 자동 삽입
- Base URL 헬퍼
  - `WebUrlProperties.client.url.*` 적극 활용

---

## 11) 현행 코드 요약

- AutoConfiguration 기반
- `web.enabled` 하나로 완전 제어
- Reactor Netty 기반 WebClient
- 기본 헤더 시스템 계정 주입
- 동기(block) 호출 단순화
- 최소 기능 + 명확한 책임 분리

---

## 12) 마지막 한 줄 요약

**“`web.enabled` 스위치로 WebClient 사용 여부를 명확히 제어하고,  
타임아웃·코덱·기본 헤더를 표준화하여 서비스 간 HTTP 호출을 일관되게 처리하는  
경량 WebClient 인프라 모듈이다.”**
