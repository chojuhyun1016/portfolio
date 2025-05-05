# 🛡️ Gateway Security Module

Spring Cloud Gateway 기반의 **JWT 인증 및 화이트리스트 관리 기능 모듈**입니다.

---

## 📦 구성 개요

이 모듈은 다음과 같은 역할을 수행합니다:

- **화이트리스트 관리**: 인증이 필요 없는 경로를 관리 (동적 추가 가능)
- **JWT 인증 필터**: 모든 요청에 대해 JWT 토큰을 검증 (화이트리스트 제외)
- **Claims 검증**: IP, Scope 등 추가 검증으로 보안 강화
- **전역 예외 처리**: 게이트웨이에서 발생하는 에러를 글로벌하게 처리

---

## 🗂️ 클래스 구조 및 역할

| 클래스명 | 설명 |
| --- | --- |
| `GatewayConfig` | `WhiteListMatcher` Bean 등록 및 `/actuator/health` 경로 자동 추가 |
| `GatewaySecurityProperties` | `application.yml`의 `custom.security.whitelist` 설정과 매핑 |
| `WhiteListMatcher` | 화이트리스트 경로 관리 및 AntPathMatcher 기반 매칭 지원 |
| `AbstractJwtAuthenticationFilter` | JWT 인증 공통 로직 제공 (토큰 유효성 + Claims 검증) |
| `DefaultJwtAuthenticationFilter` | 글로벌 필터 등록 및 실행 순서 설정 |
| `GatewayExceptionHandler` | WebFlux 환경 전역 예외 처리 핸들러 |
| `GatewayJwtHeaderResolver` | `Authorization` 헤더에서 Bearer 토큰 추출 |
| `DefaultJwtTokenValidator` | 기본 JWT 검증 (서명, 만료, IP & Scope Claims 검증 포함) |
| `JwtTokenValidator` | JWT 검증 인터페이스 (계약용) |

---

## 🔐 인증 흐름

Client → Gateway →  
&nbsp;&nbsp;1️⃣ 화이트리스트 검사 (WhiteListMatcher)  
&nbsp;&nbsp;&nbsp;&nbsp;└ YES → 통과  
&nbsp;&nbsp;&nbsp;&nbsp;└ NO → JWT 인증  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;├ 토큰 유효성 검사 (isValid)  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;├ Claims 검증 (validateClaims)  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;└ 결과:  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- 유효 → 통과  
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- 실패 → 401 / 403 응답

- **화이트리스트 경로:** 인증 무시
- **비화이트리스트 경로:** 토큰 검증 및 Claims 체크

---

## ⚙️ 주요 설정

`application.yml` 예시:

```yaml
custom:
  security:
    whitelist:
      - /api/v1/public/**
      - /swagger-ui/**
