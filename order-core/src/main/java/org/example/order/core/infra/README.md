# 📁 order-core.infra 디렉토리 구조 및 역할

`order-core.infra`는 도메인 및 애플리케이션 계층이 **직접 의존하지 않아야 하는 기술적 구현**을 담당하는  
**Infrastructure Layer** 이다.

DB, Redis, 분산락, 암호화, ID 생성, Secrets, AOP, 외부 시스템 연동 등  
**기술 중심 관심사(Technical Concerns)** 를 캡슐화하며,  
Clean Architecture / Hexagonal Architecture 의 **Infra Adapter Layer** 역할을 수행한다.

- 도메인 → 인프라 **의존 금지**
- 인프라 → 도메인 **포트(인터페이스) 구현**
- 모든 인프라 구성은 **설정 기반(@Bean) + 조건부 조립(@Import)**

--------------------------------------------------------------------------------
## 📂 crypto

암호화 및 해시 관련 **모든 기술적 구현**을 포함한다.  
도메인은 암호화 방식이나 키 관리 전략을 알지 못한다.

### 구성

- **contract**  
  암호화/복호화/서명에 대한 인터페이스(Port) 정의

- **algorithm**  
  실제 암호화 알고리즘 구현
  - AES128 / AES256 / AES-GCM
  - HMAC / SHA / Bcrypt / Argon2 등

- **factory**  
  알고리즘 및 Encryptor/Hasher/Signer 객체 생성 책임

- **util**  
  Base64, Byte 변환, 해시 계산 등 보조 유틸

- **constant**  
  알고리즘 이름, 포맷 버전 등 상수 정의

- **exception**  
  암복호화 실패, 키 미존재 등 보안 관련 예외

- **config**  
  CryptoInfraConfig, 키 시딩, 알고리즘 매핑 설정

> 특징
> - 키 로딩은 Secrets 모듈과 연동
> - 도메인은 암호화 구현체를 직접 참조하지 않음

--------------------------------------------------------------------------------
## 📂 config

**시스템 전반의 공통 인프라 설정 진입점**

- 여러 infra 설정들을 조립(@Import)하는 상위 Config
- 컴포넌트 스캔 최소화
- AutoConfiguration 스타일 유지

--------------------------------------------------------------------------------
## 📂 redis

Redis 기반 캐시 및 자료구조 연동 계층

### 구성

- **repository**  
  Redis 접근을 추상화한 공용 Repository  
  (Value / Hash / List / Set / ZSet / TTL / Transaction)

- **config**  
  RedisConnectionFactory, RedisTemplate 설정  
  직렬화 정책(JSON, JSR-310 등) 강제

- **support**  
  Redis Key 네임스페이스, TTL 전략, 직렬화 헬퍼

> 주의
> - 실제 도메인 캐시는 `OrderCachePort` 같은 **도메인 포트**로 감싸서 사용 권장

--------------------------------------------------------------------------------
## 📂 lock

DB 또는 Redis 기반 **분산 락 인프라 계층**

> 기술 선택은 런타임 설정으로 결정되며,  
> 비즈니스 로직은 락 구현을 전혀 인지하지 않는다.

### 구성

- **annotation**
  - `@DistributedLock`
  - `@DistributedLockT`  
    AOP 기반 락 선언용 애노테이션

- **aspect**  
  `DistributedLockAspect`
  - 애노테이션 해석
  - 키 생성기/실행기 선택
  - 트랜잭션 래핑

- **key**  
  락 키 생성 전략
  - SHA256
  - SpEL
  - Simple

- **lock**  
  LockExecutor 인터페이스 및 구현체
  - NamedLockExecutor (DB)
  - RedissonLockExecutor (Redis)

- **factory**  
  전략/실행기 이름 기반 조회 팩토리

- **config**  
  `LockInfraConfig`
  - 단일 설정 진입점
  - 조건부 Bean 등록
  - NamedLock / RedissonLock 조립

- **support**  
  TransactionalOperator 등 기술적 보조 컴포넌트

- **exception**  
  락 획득 실패, 타임아웃 등 전용 예외

--------------------------------------------------------------------------------
## 📂 jpa

JPA / QueryDSL 기반 **관계형 DB 인프라 계층**

### 구성

- **repository**  
  Spring Data 미사용  
  EntityManager + QueryDSL 직접 구현체

- **config**  
  JPAInfraConfig
  - EntityManager
  - JPAQueryFactory
  - 트랜잭션 설정

- **querydsl**  
  QueryDSL 유틸리티
  - 페이징/카운트 처리
  - 동적 where 헬퍼
  - Stream 조회 지원

> 특징
> - Spring Data 의존 제거
> - 인프라 제어권을 완전히 코드에 유지

--------------------------------------------------------------------------------
## 📂 dynamo

AWS DynamoDB 연동 인프라 계층

### 구성

- **repository**  
  AWS SDK v2 Enhanced Client 기반 구현체
  - PK/GSI Query 우선
  - Scan fallback 옵션화

- **config**  
  DynamoInfraConfig
  - Client / EnhancedClient
  - 엔드포인트 / 크리덴셜 구성

- **support**
  - 테이블 스키마 정의
  - 마이그레이션/시드
  - Query 공용 유틸

--------------------------------------------------------------------------------
## 📂 jdbc

JdbcTemplate 기반 **고성능 / 벌크 처리 전용 계층**

- 대량 Insert / Update
- Chunk 기반 배치 처리
- TSID 기반 ID 생성 연계

> 주 용도
> - 대량 적재
> - 이벤트 소비
> - CDC 동기화

--------------------------------------------------------------------------------
## 📂 persistence

**애그리거트-우선 / 기술-하위** 구조의 실제 Repository Adapter 배치 레이어

예시:

    persistence/
      └─ order/
         ├─ jpa/
         ├─ jdbc/
         ├─ dynamo/
         └─ redis/

- 도메인 Port ↔ 인프라 Adapter 매핑 위치
- 기술 교체 시 이 레이어만 변경

--------------------------------------------------------------------------------
## 📂 common

여러 인프라 모듈에서 공통으로 사용하는 기술 컴포넌트 모음

### 구성

- **idgen**  
  TSID / UUID 기반 ID 생성 인프라

- **secrets**  
  AWS Secrets Manager 기반 키 로딩 / 갱신

- **aop**  
  인프라 레벨 AOP 공통 설정

### AOP 설정

    package org.example.order.core.infra.common.aop;

    import org.springframework.context.annotation.Configuration;
    import org.springframework.context.annotation.EnableAspectJAutoProxy;

    /**
     * AOP 설정 클래스
     * - AspectJ 기반 AOP 활성화
     * - proxyTargetClass = true → CGLIB 프록시 강제
     * - 인터페이스 없는 클래스도 AOP 적용 가능
     */
    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    public class AopConfig {
    }

> 의미
> - Lock, Logging, Correlation, Monitoring 등  
    >   **Infra 전반 AOP 동작을 보장하는 기반 설정**

--------------------------------------------------------------------------------
## 🧭 설계 원칙 요약

- Infra 는 **도메인을 침범하지 않는다**
- 모든 외부 의존성은 Infra 에서 격리
- 설정 기반 조립으로 **기술 선택을 런타임으로 이연**
- 테스트 시 Infra 를 손쉽게 대체 가능

> 이 구조는 Clean Architecture / Hexagonal Architecture 의  
> **Infra Adapter Layer** 를 충실히 반영한다.
