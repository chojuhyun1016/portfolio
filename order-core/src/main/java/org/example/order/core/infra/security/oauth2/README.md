# Oauth2 모듈 구조 및 역할 분석

## 개요

이 모듈은 OAuth2 기반 인증 및 인가 처리를 구현합니다. JWT 토큰을 발급하고 검증하며, Spring Security 필터 체인과 통합되어 액세스 제어를 수행합니다.

구성은 다음과 같은 책임으로 나뉩니다.

## 주요 패키지 및 클래스

### 1. `config`

- **Oauth2ClientProperties**
    - 클라이언트 정보 설정을 위한 클래스.
    - `clientId`, `clientSecret`, `scopes` 목록을 관리.

- **Oauth2ServerProperties**
    - 서버 측 토큰 설정을 위한 클래스.
    - `issuer`, `accessTokenValiditySeconds`, `refreshTokenValiditySeconds`, `signingKey` 등을 관리.

- **Oauth2SecurityConfig**
    - Spring Security의 `SecurityFilterChain`을 정의.
    - `/oauth2/**` 엔드포인트는 인증 없이 접근 허용.

### 2. `constants`

- **Oauth2Constants**
    - 인증 헤더(`Authorization`), `Bearer` 접두사 상수를 정의.

### 3. `core.contract`

- **Oauth2ClientService**
    - `clientId`로 클라이언트 정보를 조회하는 서비스 인터페이스.

- **Oauth2TokenProvider**
    - 액세스 토큰 및 리프레시 토큰의 발급 및 검증 인터페이스.

### 4. `core.client`

- **DefaultOauth2ClientService**
    - `Oauth2ClientService` 구현체.
    - 메모리에 설정된 클라이언트 정보를 조회.

### 5. `core.issuer`

- **Oauth2TokenIssuer**
    - 실제 토큰 발급을 담당.
    - `TokenProvider`를 이용해 Access/Refresh 토큰을 생성하고 Redis에 Refresh 토큰을 저장.

### 6. `core.provider`

- **DefaultOauth2TokenProvider**
    - 토큰 발급/검증을 위한 표준 구현체.
    - 액세스 토큰 생성, 리프레시 토큰 재발급, 토큰 검증 로직을 위임 처리.

- **CustomClaimEnhancer**
    - 토큰 생성 시 커스텀 클레임을 추가하는 유틸 클래스.

### 7. `core.validator`

- **Oauth2TokenValidator**
    - 액세스 토큰(JWT)의 유효성 및 블랙리스트 검증.
    - 리프레시 토큰의 Redis 기반 검증.

### 8. `exception`

- **Oauth2ExceptionHandler**
    - OAuth2 관련 예외 발생 시 처리하는 글로벌 예외 핸들러.
    - `AuthenticationException` 및 일반 예외를 처리.

### 9. `filter`

- **Oauth2AuthenticationFilter**
    - HTTP 요청 시 토큰을 추출하고 검증 후 `SecurityContext`에 인증 객체를 등록.
    - 블랙리스트 여부도 체크하여 불법 토큰 차단.

### 10. `util`

- **JwtTokenManager**
    - JWT 토큰 생성 및 검증 기능 제공.

- **Oauth2HeaderResolver**
    - HTTP 헤더에서 `Bearer` 토큰을 추출하는 유틸리티.

## 도메인 및 애플리케이션 계층 연계

- **Oauth2ClientMetadata** (`application.security.vo`)
    - 클라이언트 정보 VO.

- **Oauth2AccessToken** (`application.security.response`)
    - 액세스 토큰 응답 모델.

- **Oauth2TokenIssueRequest** (`domain.security.vo`)
    - 토큰 발급 요청 VO.

## 동작 흐름 요약

1. 클라이언트는 `/oauth2/**` 엔드포인트로 인증 요청.
2. `Oauth2TokenIssuer`가 토큰을 생성.
3. 생성된 Access/Refresh 토큰은 응답으로 내려가며, Refresh 토큰은 Redis에 저장.
4. 클라이언트 요청 시 `Oauth2AuthenticationFilter`가 JWT 토큰을 검증하고 `SecurityContext`에 등록.
5. 만료 또는 재발급 요청 시 `DefaultOauth2TokenProvider`가 새 토큰을 발급.
