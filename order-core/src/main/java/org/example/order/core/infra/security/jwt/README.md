# 🔐 JWT 모듈

Spring Security + JWT 기반의 인증 시스템 모듈입니다. Access/Refresh 토큰 관리, Redis + DB 기반의 검증, AWS SecretsManager 연동까지 통합된 구조로 설계되었습니다.

---

## 📦 구성 개요

| 클래스/인터페이스 | 설명 |
| --- | --- |
| `JwtConfigurationProperties` | `jwt.*` 설정 프로퍼티 매핑 (secret, 만료시간 등) |
| `JwtClaimsConstants` | JWT 클레임 상수 정의 (roles, scope, ip 등) |
| `JwtErrorConstants` | 에러 메시지 상수 정의 |
| `JwtHeaderConstants` | HTTP 헤더 관련 상수 정의 (Authorization 등) |
| `TokenProvider` | JWT 인터페이스 (검증, 정보 추출 등) |
| `JwtSecurityAuthenticationFilter` | AccessToken 검증 및 SecurityContext 등록 필터 |
| `JwtSecurityExceptionHandler` | 인증 예외 전역 처리 (토큰 만료 등) |
| `AbstractJwtTokenManager` | JWT 유틸 추상 클래스 (공통 로직 제공) |
| `JwtTokenManager` | AWS SecretsManager 연동 HMAC 키 관리 및 토큰 발급/검증 |
| `HybridRefreshTokenStore` | RefreshToken 저장소 (Redis + DB fallback) |
| `RefreshTokenStore` | RefreshToken 저장소 인터페이스 |
| `JwtHeaderResolver` | Servlet/WebFlux 요청에서 Bearer 토큰 추출 유틸 |

---

## 🔑 인증 및 처리 흐름

### ✅ Access/Refresh Token 발급

- `JwtTokenManager.createAccessToken()` → roles, scope, device, ip 포함 Claims
- `JwtTokenManager.createRefreshToken()`

HMAC 서명 키는 AWS SecretsManager에서 동적으로 가져옴.

### ✅ 인증 절차 (서블릿 환경)

1️⃣ **JwtSecurityAuthenticationFilter**
- Authorization 헤더 → 토큰 추출
- `validateToken()` 호출 → 기본 유효성 검증
- RefreshToken 검증 (Redis → DB fallback)
- 블랙리스트 검사
- 인증 성공 시 `SecurityContext` 등록

2️⃣ **예외 처리**
- JWT 만료 → `JwtSecurityExceptionHandler.handleExpiredToken()` (401)
- 인증 실패 → `JwtSecurityExceptionHandler.handleAuthException()` (401)

---

## 🔄 RefreshToken 관리

- **저장:** Redis + DB 동시 저장
- **검증:** Redis 우선 → DB fallback → 필요 시 Redis 복구
- **블랙리스트:** AccessToken 블랙리스트 등록/검증
- **청소:** `HybridRefreshTokenStore.cleanExpiredTokens()`로 만료 토큰 주기적 삭제

---

## ⚙️ 특징

- **이중 저장소:** 빠른 조회(캐시) + 영속성 확보
- **실시간 HMAC 키 관리:** AWS SecretsManager에서 키 동적 로드
- **확장성:** `AbstractJwtTokenManager`로 RSA 등 다른 알고리즘으로 쉽게 확장 가능
- **보안 강화:** roles, scope, ip, device 등 Claims 기반 추가 검증 지원
- **TTL 관리:** Redis RefreshToken은 TTL 기반으로 만료 관리

---

## ✅ 주요 설정 예시 (`application.yml`)

```yaml
jwt:
  secret: your-base64-encoded-secret
  access-token-validity-in-seconds: 3600
  refresh-token-validity-in-seconds: 1209600
