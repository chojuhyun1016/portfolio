# 📁 order-core.infra 디렉토리 구조 및 역할

`order-core.infra`는 도메인과 애플리케이션 계층이 직접 의존하지 않는 외부 시스템, 기술적 세부사항을 담당하는 **Infrastructure Layer**로 구성되어 있다. DB, Redis, 보안, 암호화, 외부 API 연동 등 기술 기반의 기능을 캡슐화한다.

---

## 📂 crypto

암호화와 관련된 모든 기술적 구현을 포함한다.

- **contract**  
  암호화 관련 인터페이스 정의. 예: `Encryptor`, `Decryptor`.

- **util**  
  해시, 바이트 변환 등의 암호화 관련 유틸리티.

- **config**  
  암호화 설정 관련 구성. 예: 알고리즘 선택, 키 설정.

- **algorithm**  
  실제 사용하는 암호화 알고리즘 구현. 예: AES, RSA 등.

- **constant**  
  암호화 관련 상수 정의. 예: 알고리즘명, 키 사이즈.

- **exception**  
  암복호화 중 발생할 수 있는 예외 정의. 예: 키 불일치, 암호화 실패 등.

- **factory**  
  암호화 객체 생성 책임을 가지는 팩토리. 예: KMS 기반 Encryptor 생성.

---

## 📂 config

전체 시스템 레벨의 공통 설정을 담당하는 모듈.  
Spring 설정 클래스, 글로벌 빈 등록, 공통 설정 파일 로딩 등이 포함된다.

---

## 📂 redis

Redis 캐시 및 분산 저장소와의 연동 기능.

- **repository**  
  Redis 접근 및 데이터 처리용 Repository 구현체. 예: TTL 설정, 키 관리 등.

- **config**  
  RedisConnectionFactory 및 RedisTemplate 설정.

- **support**  
  Redis 직렬화 설정, 커스텀 Key 전략 등 지원 유틸.

---

## 📂 lock

분산 락 기능을 위한 Redisson 및 NamedLock 처리 계층.

- **config**  
  Redisson 또는 DB 기반 Lock 설정 클래스.

- **lock**  
  Lock 인터페이스 및 구현체. 예: `RedissonLockImpl`, `NamedLockImpl`.

- **annotation**  
  `@DistributedLock` 등 AOP 기반 Lock 처리용 어노테이션 정의.

- **key**  
  Lock Key 생성 전략 (SHA-256 등).

- **support**  
  공통 Lock 관련 지원 클래스. 예: 스펠링 해시 전략.

- **aspect**  
  Lock 어노테이션 처리용 Aspect 정의.

- **exception**  
  락 획득 실패, 시간 초과 등 Lock 관련 예외 정의.

- **factory**  
  락 구현체 생성 책임. `LockFactory` 등.

---

## 📂 jpa

JPA 기반 DB 처리 기능 전반을 담당한다.

- **repository**  
  Spring Data JPA 기반의 Repository 인터페이스 및 구현체.

- **config**  
  DataSource, EntityManagerFactory 설정.

- **querydsl**  
  QueryDSL 관련 설정 및 커스텀 쿼리 지원 기능.

---

## 📂 dynamo

AWS DynamoDB 연동 모듈.

- **repository**  
  DynamoDB Enhanced Client 기반의 Repository 구현체.

- **config**  
  LocalStack or AWS 환경에서 Dynamo 설정.

- **support**  
  테이블 스키마, 키 전략 등 부가 지원 기능.

---

## 📂 common

기타 공통 인프라 기능.

- **idgen**  
  TSID, UUID 등의 고유 ID 생성기.

- **secrets**  
  AWS Secrets Manager 또는 로컬 키 복호화 지원.

- **aop**  
  인프라 레벨 AOP. 예: 파일 다운로드 대기, 트랜잭션 처리.

---

> 이 구조는 **Clean Architecture**의 Infra Layer 원칙을 따르며, 외부 시스템과의 의존성을 명확히 분리하여 테스트 용이성과 유지보수성을 확보한다.

