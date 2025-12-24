# ⚙️ JPA 인프라 설정 (JpaInfraConfig 계열)

`order-core.infra.jpa.config` 패키지는 **JPA / QueryDSL / JDBC 기반 저장소 어댑터를 명시적으로 조립**하기 위한 인프라 설정 모음이다.  
전역 스위치 **`jpa.enabled=true`** 일 때만 활성화되며, **컴포넌트 스캔이나 Spring Data JPA 자동 등록을 전혀 사용하지 않는다.**

---

## 📂 JpaInfraConfig

- **역할**  
  JPA 인프라의 단일 진입점. 하위 조립 Config 를 **명시적으로 Import**하여 조립한다.  
  `@EnableTransactionManagement` 로 트랜잭션 경계를 활성화한다.
- **활성 조건**  
  `jpa.enabled=true`
- **특징**
  - 전체 스캔 금지 (`@ComponentScan` 사용 안 함)
  - Spring Data JPA Repository 인터페이스 스캔 미사용
  - 하위 Config 들은 **JpaInfraConfig 게이트 뒤에서만** 활성화

### Import 구성
~~~java
@Import({
        // ----- order -----
        JpaOrderQueryInfraConfig.class,         // JPAQueryFactory & 조회 리포지토리
        JpaOrderRepositoryInfraConfig.class,    // 저장 리포지토리
        JpaOrderCommandInfraConfig.class,       // JDBC 기반 Command 리포지토리

        // ----- local_order -----
        JpaLocalOrderQueryInfraConfig.class,        // JPAQueryFactory & 조회 리포지토리
        JpaLocalOrderRepositoryInfraConfig.class,   // 저장 리포지토리
        JpaLocalOrderCommandInfraConfig.class       // JDBC 기반 Command 리포지토리
})
public class JpaInfraConfig {
}
~~~

---

## 📂 JpaOrderCommandInfraConfig

- **역할**  
  `OrderCommandRepositoryJdbcImpl`을 등록하여 **대량 Insert/Update** 처리를 지원한다.
- **등록 빈**
  - `OrderCommandRepository` → `OrderCommandRepositoryJdbcImpl(JdbcTemplate)`
- **활성 조건**
  - `jpa.enabled=true` (상위 게이트에 의해 보장)
  - `OrderCommandRepository` 빈이 없을 때 (`@ConditionalOnMissingBean`)
- **비고**
  - 별도의 `@ConditionalOnProperty(jpa.enabled=...)`를 두지 않고, 상위 Import 구조로 통제하는 패턴을 유지한다.

---

## 📂 JpaOrderQueryInfraConfig

- **역할**  
  QueryDSL 기반 조회 인프라를 담당한다.  
  **(중요)** `JPAQueryFactory`를 여기서 **단일로 제공**하고, `OrderQueryRepositoryJpaImpl`을 조립한다.
- **등록 빈**
  - `JPAQueryFactory(EntityManager)`
  - `OrderQueryRepository` → `OrderQueryRepositoryJpaImpl(JPAQueryFactory)`
- **활성 조건**
  - `jpa.enabled=true`
  - `JPAQueryFactory` 미등록 시에만 등록 (`@ConditionalOnMissingBean`)
  - `OrderQueryRepository` 미등록 시에만 등록 (`@ConditionalOnMissingBean`)
- **설계 포인트**
  - 조건 경합을 막기 위해 `@ConditionalOnBean(EntityManager...)` 같은 조건을 두지 않는다.  
    필요한 의존성은 **메서드 파라미터 주입**으로 명확히 표현한다.

---

## 📂 JpaOrderRepositoryInfraConfig

- **역할**  
  `OrderRepositoryJpaImpl`을 등록하여 EntityManager + QueryDSL 기반 저장/삭제를 지원한다.
- **등록 빈**
  - `OrderRepository` → `OrderRepositoryJpaImpl(JPAQueryFactory, EntityManager)`
- **활성 조건**
  - `jpa.enabled=true`
  - `OrderRepository` 미등록 시 (`@ConditionalOnMissingBean`)
- **주의**
  - 토글은 상위 `JpaInfraConfig`에서만 관리한다.
  - 조건 경합 방지를 위해 `@ConditionalOnBean(EntityManager...)`를 의도적으로 사용하지 않는다.

---

## 📂 JpaLocalOrderCommandInfraConfig

- **역할**  
  `LocalOrderCommandRepositoryJdbcImpl`을 등록하여 LocalOrder의 대량 Insert/Update를 지원한다.
- **등록 빈**
  - `LocalOrderCommandRepository` → `LocalOrderCommandRepositoryJdbcImpl(JdbcTemplate)`
- **활성 조건**
  - `jpa.enabled=true` (상위 게이트에 의해 보장)
  - `LocalOrderCommandRepository` 미등록 시 (`@ConditionalOnMissingBean`)
- **특징**
  - Order Command 리포지토리와 **동일한 패턴**을 따른다.

---

## 📂 JpaLocalOrderQueryInfraConfig

- **역할**  
  QueryDSL 기반 `LocalOrderQueryRepositoryJpaImpl`을 조립한다.
- **등록 빈**
  - `LocalOrderQueryRepository` → `LocalOrderQueryRepositoryJpaImpl(JPAQueryFactory)`
- **활성 조건**
  - `jpa.enabled=true`
  - `LocalOrderQueryRepository` 미등록 시 (`@ConditionalOnMissingBean`)
- **의존성**
  - `JPAQueryFactory`는 `JpaOrderQueryInfraConfig`에서 단일 제공된다(미등록이면 생성).

---

## 📂 JpaLocalOrderRepositoryInfraConfig

- **역할**  
  `LocalOrderRepositoryJpaImpl`을 등록하여 LocalOrder 저장/삭제를 지원한다.
- **등록 빈**
  - `LocalOrderRepository` → `LocalOrderRepositoryJpaImpl(JPAQueryFactory, EntityManager)`
- **활성 조건**
  - `jpa.enabled=true`
  - `LocalOrderRepository` 미등록 시 (`@ConditionalOnMissingBean`)

---

## ✅ 설정 방법

~~~yaml
jpa:
  enabled: true
~~~

> ✅ 사용 방법: 상위 모듈(예: `OrderCoreConfig`)에서 `JpaInfraConfig` **하나만 Import**하면 전체 조립이 활성화된다.

---

## 🧩 활성 구조 (개념)

~~~text
JpaInfraConfig
 ├─ JpaOrderQueryInfraConfig
 │   ├─ JPAQueryFactory
 │   └─ OrderQueryRepositoryJpaImpl
 │
 ├─ JpaOrderRepositoryInfraConfig
 │   └─ OrderRepositoryJpaImpl
 │
 ├─ JpaOrderCommandInfraConfig
 │   └─ OrderCommandRepositoryJdbcImpl
 │
 ├─ JpaLocalOrderQueryInfraConfig
 │   └─ LocalOrderQueryRepositoryJpaImpl
 │
 ├─ JpaLocalOrderRepositoryInfraConfig
 │   └─ LocalOrderRepositoryJpaImpl
 │
 └─ JpaLocalOrderCommandInfraConfig
     └─ LocalOrderCommandRepositoryJdbcImpl
~~~

---

## 🔒 설계 원칙 요약

- 기본은 OFF (`jpa.enabled=false`면 미활성)
- `jpa.enabled=true`에서만 조립
- 전체 스캔 금지 / Spring Data JPA 미사용
- 하위 Config 는 **명시적 Import**로만 활성화
- `@ConditionalOnMissingBean` 중심으로 “중복 등록 방지”
- 의존성은 파라미터 주입으로 명확히 표현(조건 경합 최소화)

---

## 🧾 마지막 한 줄 요약

**`JpaInfraConfig` 하나로 JPA(QueryDSL) 조회 + JPA 저장 + JDBC Command를 분리 조립하여, 스캔 없는 통제형 JPA 인프라를 제공한다.**
