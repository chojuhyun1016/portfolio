# Infra 모듈

Infra 모듈은 도메인 계층과 외부 시스템 간의 기술적인 연결을 담당합니다.  
JPA, Redis, DynamoDB, 분산 락, 암호화, ID 생성기 등 다양한 기술 인프라를 캡슐화하여 제공합니다.

## 목적

- 도메인 계층에서 기술적인 세부 사항을 분리
- 공통 기능 및 외부 시스템 연동의 재사용성 확보
- AOP, 분산락, 암호화 등의 인프라성 기능 제공

## 디렉토리 구조

- common
  - aop : AOP 설정 클래스 제공 (예: AopConfig.java)
  - idgen
    - table : 시퀀스 기반 ID 생성기
    - tsid : TSID 기반 글로벌 ID 생성기

- config
  - OrderCoreConfig.java : Infra 전체 Bean 구성 및 설정

- crypto
  - algorithm : 암호화 엔진, 구현체
    - hasher : SHA256, Bcrypt, Argon2 등 해시 엔진 구현
    - signer : HMAC-SHA256 디지털 서명 엔진
    - encryptor : AES128, AES256, AES-GCM 구현체
  - config : 암호화 관련 설정 (AES, HMAC 등)
  - contract : Encryptor, Hasher, Signer 등 인터페이스
  - decryptor : AWS KMS 통하여 키값 복호화
  - exception : 예외 정의
  - factory : 복호화 구현체 생성
  - util : 키 생성기 (EncryptionKeyGenerator.java)

- dynamo
  - config : DynamoDB 설정 및 클라이언트 구성
  - model : 도메인 모델 (예: OrderDynamoEntity)
  - repository : DynamoRepository 인터페이스 및 구현
  - support : DynamoDB 쿼리 유틸

- jpa
  - config : JPA 및 QueryDSL 설정 (QuerydslConfig.java)
  - querydsl : Where절 조합기 및 BooleanExpression DSL
  - repository : 사용자 정의 JPA 리포지토리 및 구현체

- lock
  - annotation : @DistributedLock, @DistributedLockT 등 락 어노테이션
  - aspect : 분산 락 AOP 처리 (DistributedLockAspect.java)
  - config : Named Lock, Redisson 설정 클래스
  - exception : 락 획득 실패 예외 정의
  - factory : LockExecutor, KeyGenerator 팩토리
  - key : 다양한 키 생성 전략 (SHA256, SpEL 등)
  - lock
    - impl : NamedLockExecutor, RedissonLockExecutor
  - service : 트랜잭션 처리 유틸 (TransactionalService.java)

- redis
  - config : Redis 설정 (RedisConfig.java)
  - repository : Redis 저장소 인터페이스 및 구현체
  - support : Redis 키 관리 및 직렬화 유틸

- security
  - jwt
    - provider : JWT 토큰 발급/검증 클래스 (AbstractJwtTokenManager, JwtTokenManager)
    - contract : TokenProvider 인터페이스 정의
    - config : JWT 관련 프로퍼티 구성 (JwtConfigurationProperties)
    - constant : 토큰 클레임 상수 정의 (JwtClaimsConstants)
  - oauth2
    - config : Oauth2 클라이언트/서버 설정, Security FilterChain 구성
    - constants : 공통 상수 (Oauth2Constants)
    - core
      - contract : Oauth2TokenProvider, Oauth2ClientService 등 인터페이스
      - client : DefaultOauth2ClientService (클라이언트 정보 조회)
      - issuer : Oauth2TokenIssuer (토큰 발급 책임)
      - provider : DefaultOauth2TokenProvider (표준 Provider)
      - validator : Oauth2TokenValidator (토큰 검증)
    - exception : Oauth2ExceptionHandler (글로벌 예외 핸들러)
    - filter : Oauth2AuthenticationFilter (SecurityContext 등록용)
    - util : Oauth2HeaderResolver, JwtTokenManager (JWT 발급/검증 유틸)
  - gateway
    - config : Gateway 보안 설정
    - filter : AuthorizationFilter 등 API Gateway용 필터 구현
    - util : JWT 파싱 및 검증 유틸

## 사용 예시

### 분산 락 사용 예

```java
@DistributedLock(key = "#orderId", type = "namedLock")
public void processOrder(String orderId) {
    // 처리 로직
}
```

### TS ID 사용 예

```java
@CustomTsid
private Long userId;
```

### Oauth2 토큰 발급 예시

```java
Oauth2TokenIssueRequest request = Oauth2TokenIssueRequest.builder()
.userId("user123")
.roles(List.of("ROLE_USER"))
.scopes(List.of("read", "write"))
.deviceId("device-abc")
.ipAddress("192.168.0.1")
.clientId("my-client")
.build();

Oauth2AccessToken token = oauth2TokenProvider.createAccessToken(request);
```

### Security 필터 체인 설정 예시

```java
@Bean
public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {
    return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/oauth2/**").permitAll()
                    .anyRequest().authenticated()
            )
            .addFilterBefore(oauth2AuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
}
```

### 설정 참고

```yaml
spring:
datasource:
url: jdbc:mysql://localhost:3306/dbname
username: user
password: pass
driver-class-name: com.mysql.cj.jdbc.Driver

redis:
host: localhost
port: 6379

lock:
named:
wait-time: 5000
retry-interval: 100

encrypt:
aes128:
key: dGhpc2lzMTZieXRla2V5IQ==  # base64 encoded key (16 byte)
aes256:
key: bXlTZWNyZXRLZXlTMjU2MjU2MjU2MjU2MjU2MjU2MjU=  # base64 encoded key (32 byte)
aesgcm:
key: bXlTMzJiYnl0ZXNnY21rZXlzdXBlcnNlY3JldGtleTE=  # base64 encoded key (32 byte)
hmac:
key: test-hmac-secret

oauth2:
client:
clients:
- client-id: my-client
client-secret: my-secret
scopes:
- read
- write
server:
issuer: my-issuer
access-token-validity-seconds: 3600
refresh-token-validity-seconds: 1209600
signing-key: my-jwt-signing-key
```

## 작성자 노트

Infra 모듈은 도메인 계층이 비즈니스 로직에만 집중할 수 있도록  
기술적 복잡성과 외부 시스템 연동을 캡슐화하고 추상화하는 역할을 수행합니다.

**Security (Oauth2 + JWT + Gateway)** 기능은 액세스 제어와 인증 시스템을 인프라 계층에서 통합하여 제공합니다.  
특히 Oauth2 모듈은 Spring Security FilterChain과 결합되어 API 요청마다 토큰 검증 및 인증을 처리하며,  
RefreshToken 관리 기능을 Redis 기반으로 제공해 보다 견고한 인증 구조를 갖추고 있습니다.  
Gateway 모듈은 마이크로서비스 구조에서 API Gateway의 보안 및 인증 관리를 집중화하도록 설계되었습니다.

서비스 규모가 커질수록 분산 락, 캐시, 외부 연동, 암호화, 인증 등은 점점 중요해지고 복잡해집니다.  
Infra 모듈을 통해 이를 구조화하고 일관된 방식으로 사용할 수 있도록 설계되었습니다.

다른 모듈에서도 손쉽게 재사용할 수 있도록 구성되어 있으며,  
변경에 유연하고 유지보수에 유리한 구조를 갖추고 있습니다.
