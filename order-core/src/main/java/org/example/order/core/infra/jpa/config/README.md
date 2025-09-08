# ⚙️ JPA 인프라 설정 (JpaInfraConfig 계열)

`order-core.infra.jpa.config` 패키지는 **JPA/QueryDSL 기반 저장소 어댑터**를 조건부로 등록하기 위한 설정을 제공한다.  
전역 스위치 `jpa.enabled=true` 일 때만 동작하며, 하위 조립 설정을 함께 포함한다.

---

## 📂 JpaInfraConfig

- **역할**  
  JPA/QueryDSL 핵심 설정 및 하위 조립 Config 일괄 Import  
  `EntityManager` 존재 시 `JPAQueryFactory`를 빈으로 등록한다.
- **활성 조건**  
  `jpa.enabled=true`

---

## 📂 JpaOrderCommandInfraConfig

- **역할**  
  `OrderCommandRepositoryJdbcImpl`을 등록하여 대량 Insert/Update 처리 지원
- **활성 조건**  
  `JdbcTemplate`, `TsidFactory` 빈 존재  
  `OrderCommandRepository` 미등록 시

---

## 📂 JpaOrderQueryInfraConfig

- **역할**  
  `OrderQueryRepositoryJpaImpl`을 등록하여 QueryDSL 기반 조회 지원
- **활성 조건**  
  `JPAQueryFactory` 빈 존재  
  `OrderQueryRepository` 미등록 시

---

## 📂 JpaOrderRepositoryInfraConfig

- **역할**  
  `OrderRepositoryJpaImpl`을 등록하여 EntityManager + QueryDSL 기반 저장/삭제 지원
- **활성 조건**  
  `JPAQueryFactory`, `EntityManager` 빈 존재  
  `OrderRepository` 미등록 시

---

> ✅ 설정 방법: `application.yml` 에 `jpa.enabled: true` 지정  
> ✅ 사용 방법: 상위 모듈(`OrderCoreConfig`)에서 `JpaInfraConfig` 하나만 Import 하면 전체 조립 활성화
