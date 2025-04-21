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
    - config : 암호화 관련 설정 (AES, HMAC 등)
    - contract : Encryptor, Hasher, Signer 등 인터페이스
    - encryptor : AES128, AES256, AES-GCM 구현체
    - hasher : SHA256, Bcrypt, Argon2 등 해시 엔진 구현
    - signer : HMAC-SHA256 디지털 서명 엔진
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

## 주요 기능 요약

| 기능 영역       | 설명 |
|----------------|------|
| AOP 설정        | 공통 AOP 기능을 제공하여 트랜잭션, 로깅 등의 횡단 관심사를 처리 |
| ID 생성기       | Table 기반 및 TSID 기반의 고유 식별자 생성 지원 |
| 암호화/해시     | AES, SHA256, Argon2, HMAC 등 다양한 암복호화 및 해시 알고리즘 제공 |
| JPA 지원        | QueryDSL 설정 및 커스텀 리포지토리 기반의 JPA 확장 |
| Redis 연동      | Redis 기반 캐시 저장소 및 key 관리 기능 제공 |
| DynamoDB 연동   | AWS DynamoDB 기반 데이터 저장 및 조회 유틸 제공 |
| 분산 락         | NamedLock (MySQL) 및 Redisson 기반 분산 락 지원 |

## 간단한 사용 예시

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

### 설정 참고

```java
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
```

## 작성자 노트
Infra 모듈은 도메인 계층이 비즈니스 로직에만 집중할 수 있도록
기술적 복잡성과 외부 시스템 연동을 캡슐화하고 추상화하는 역할을 수행합니다.

서비스 규모가 커질수록 분산 락, 캐시, 외부 연동, 암호화 등은 점점 중요해지고 복잡해집니다.
Infra 모듈을 통해 이를 구조화하고 일관된 방식으로 사용할 수 있도록 설계되었습니다.

다른 모듈에서도 손쉽게 재사용할 수 있도록 구성되어 있으며,
변경에 유연하고 유지보수에 유리한 구조를 갖추고 있습니다.

