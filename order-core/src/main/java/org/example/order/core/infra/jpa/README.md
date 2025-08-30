# 🧩 JPA 모듈 (QueryDSL + 설정 기반 @Bean + @Import 조립)

Spring Boot에서 **JPA/QueryDSL 인프라**를 가볍게 조립해 주는 모듈입니다.  

---

## 1) 구성 개요

| 구성 요소 | 설명 |
|---|---|
| **`JpaInfraConfig`** | 단일 진입점. `jpa.enabled=true`일 때만 조건부 등록 (OFF면 아무것도 로딩하지 않음) |
| `JPAQueryFactory` | `EntityManager` 존재 시 등록되는 QueryDSL 진입점 |
| `OrderCommandRepositoryJdbcImpl` | 대량 Insert/Update 등 JDBC 명령형 저장소 (조건: `JdbcTemplate` + `TsidFactory`) |
| `OrderQueryRepositoryJpaImpl` | QueryDSL 기반 조회 저장소 (조건: `JPAQueryFactory`) |
| `OrderRepositoryJpaImpl` | Spring Data JPA 어댑터(Infra→Domain) (조건: `SpringDataOrderJpaRepository`) |
| `SpringDataOrderJpaRepository` | 순수 Spring Data JPA 어댑터. Infra에서만 사용 |
| `QuerydslUtils` | 페이지네이션/스트림 헬퍼 |
| `WhereClauseBuilder`, `LazyBooleanExpression` | 가독성 높은 동적 where DSL |

> 원칙: 라이브러리 계층에는 `@Component`/`@Repository`를 사용하지 않고, **설정(@Bean) + 조건부 등록**만 사용합니다.

---

## 2) 동작 모드

### 2.1 OFF (기본)
~~~yaml
jpa:
  enabled: false
~~~
- 어떤 빈도 등록되지 않습니다. (Aspect/Factory/Repository 전부 미로딩)

### 2.2 ON
~~~yaml
jpa:
  enabled: true
~~~
- 아래 **존재 조건**을 만족하는 컴포넌트만 개별적으로 조립됩니다:
    - `JPAQueryFactory` → `EntityManager`가 이미 존재할 때
    - `OrderCommandRepositoryJdbcImpl` → `JdbcTemplate`, `TsidFactory`가 존재할 때
    - `OrderQueryRepositoryJpaImpl` → `JPAQueryFactory`가 존재할 때
    - `OrderRepositoryJpaImpl` → `SpringDataOrderJpaRepository`가 존재할 때

---

## 3) 빠른 시작 (설정 기반 + @Import 조립)

### 3.1 의존성
~~~groovy
dependencies {
  implementation "org.springframework.boot:spring-boot-starter-data-jpa"
  implementation "com.querydsl:querydsl-jpa"        // KAPT/AnnotationProcessor 설정은 프로젝트 규칙에 맞게
  implementation "com.github.f4b6a3:tsid-creator:5.2.6"

  runtimeOnly "org.mariadb.jdbc:mariadb-java-client" // 또는 사용하는 DB 드라이버
}
~~~

### 3.2 애플리케이션 조립
~~~java
// @SpringBootApplication 클래스 혹은 Infra 조립 전용 @Configuration
@Import(org.example.order.core.infra.jpa.config.JpaInfraConfig.class)
public class App { }
~~~

### 3.3 설정(YAML)
~~~yaml
jpa:
  enabled: true

spring:
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        format_sql: true
        jdbc:
          batch_size: 1000
        order_inserts: true
        order_updates: true
        generate_statistics: false
  datasource:
    url: jdbc:mariadb://localhost:3306/orderdb
    username: app
    password: secret
~~~

---

## 4) 저장소 사용 예시

### 4.1 명령형 저장소(JDBC) — 대량 Insert/Update
~~~java
@Service
@RequiredArgsConstructor
public class OrderBulkService {
  private final OrderCommandRepository commandRepo;

  public void upsertAll(List<OrderEntity> rows) {
    commandRepo.bulkInsert(rows);  // 존재 시 insert ignore
  }

  public void syncAll(List<OrderUpdate> updates) {
    commandRepo.bulkUpdate(updates); // 조건부 업데이트 + version 증가
  }
}
~~~

### 4.2 조회 저장소(QueryDSL) — View DTO
~~~java
@Service
@RequiredArgsConstructor
public class OrderQueryService {
  private final OrderQueryRepository orderQuery;

  public OrderView find(Long orderId) {
    return orderQuery.fetchByOrderId(orderId);
  }
}
~~~

### 4.3 Spring Data JPA 어댑터 — 단순 CRUD
~~~java
@Service
@RequiredArgsConstructor
public class OrderCrudService {
  private final OrderRepository orderRepo;

  public Optional<OrderEntity> load(Long id) {
    return orderRepo.findById(id);
  }

  public void save(OrderEntity e) {
    orderRepo.save(e);
  }

  public void removeAllByOrderIds(List<Long> ids) {
    orderRepo.deleteByOrderIdIn(ids);
  }
}
~~~

---

## 5) QueryDSL 유틸 & 동적 where

### 5.1 WhereClauseBuilder
~~~java
import static org.example.order.core.infra.jpa.querydsl.builder.QuerydslUtils.where;

public List<OrderEntity> search(Long userId, String orderNo) {
  var w = where()
      .optionalAnd(userId, () -> ORDER.userId.eq(userId))
      .optionalAnd(orderNo, () -> ORDER.orderNumber.eq(orderNo));

  return queryFactory
      .selectFrom(ORDER)
      .where(w)
      .fetch();
}
~~~

### 5.2 페이지네이션
~~~java
public Page<OrderEntity> page(SearchCond cond, Pageable pageable) {
  var query = queryFactory.selectFrom(ORDER)
      .where(
          where()
            .optionalAnd(cond.userId(), () -> ORDER.userId.eq(cond.userId()))
            .optionalAnd(cond.deletedOnly(), () -> ORDER.deleteYn.eq((byte)1))
      );

  return QuerydslUtils.page(new Querydsl(entityManager, new PathBuilder<>(OrderEntity.class, "orderEntity")), query, pageable);
}
~~~

### 5.3 스트리밍 (대량 조회 시)
~~~java
try (var stream = QuerydslUtils.stream(
        queryFactory.selectFrom(ORDER).where(ORDER.deleteYn.eq((byte)0))
)) {
  stream.forEach(o -> /* 처리 */ {});
}
~~~

---

## 6) 구성 세부 — 조건부 등록 규칙 요약

- **전역 게이트**: `jpa.enabled=true` 일 때만 아래 빈 후보를 검토
- **`JPAQueryFactory`**: `EntityManager` 빈이 있을 때만
- **`OrderCommandRepositoryJdbcImpl`**: `JdbcTemplate` + `TsidFactory` 둘 다 있을 때만
- **`OrderQueryRepositoryJpaImpl`**: `JPAQueryFactory` 있을 때만
- **`OrderRepositoryJpaImpl`**: `SpringDataOrderJpaRepository` 있을 때만

> 결과적으로, **필요한 인프라만 조립**되고 나머지는 로딩되지 않습니다.

---

## 7) 운영 체크리스트 (Best Practice)

- **Open Session In View 비활성화**: `spring.jpa.open-in-view=false` 권장
- **배치 최적화**: `hibernate.jdbc.batch_size`, `order_inserts/updates` 활성화
- **트랜잭션 경계 명확화**: 서비스 계층에서 `@Transactional` 관리
- **N+1 방지**: fetch join / batch fetch size / DTO 프로젝션
- **인덱스 관리**: 조회 조건/조인 컬럼에 적절한 인덱스 설계
- **메모리 관리**: 대량 처리 시 주기적 `flush()`/`clear()` 고려

---

## 8) 에러/예외 안내

- `NoSuchBeanDefinitionException: OrderCommandRepository...`  
  → `jpa.enabled=true`인지, 그리고 `JdbcTemplate`/`TsidFactory`가 빈으로 존재하는지 확인
- `NoSuchBeanDefinitionException: JPAQueryFactory`  
  → `EntityManager`가 등록되어 있는지, `spring-boot-starter-data-jpa` 의존성이 있는지 확인
- `QueryDSL Q타입 미생성`  
  → annotation processing 설정 및 Q클래스 생성 플러그인 설정 점검

---

## 9) 테스트 팁

### 9.1 OFF 동작 검증
~~~java
new ApplicationContextRunner()
  .withPropertyValues("jpa.enabled=false")
  .withConfiguration(UserConfigurations.of(JpaInfraConfig.class))
  .run(ctx -> assertThat(ctx).doesNotHaveBean(JPAQueryFactory.class));
~~~

### 9.2 ON + 조건부 조립 검증
~~~java
new ApplicationContextRunner()
  .withPropertyValues("jpa.enabled=true")
  .withUserConfiguration(TestInfraBeans.class) // EntityManager/JdbcTemplate/TsidFactory/SpringData 스텁 제공
  .withConfiguration(UserConfigurations.of(JpaInfraConfig.class))
  .run(ctx -> {
      assertThat(ctx).hasSingleBean(JPAQueryFactory.class);
      assertThat(ctx).hasSingleBean(OrderCommandRepository.class);
      assertThat(ctx).hasSingleBean(OrderQueryRepository.class);
      assertThat(ctx).hasSingleBean(OrderRepository.class);
  });
~~~

---

## 10) 클래스 다이어그램 (개념)

~~~text
JpaInfraConfig (gate: jpa.enabled)
 ├─(if EntityManager)──────────────> JPAQueryFactory
 ├─(if JdbcTemplate & TsidFactory)→ OrderCommandRepositoryJdbcImpl (OrderCommandRepository)
 ├─(if JPAQueryFactory)───────────> OrderQueryRepositoryJpaImpl (OrderQueryRepository)
 └─(if SpringDataOrderJpaRepository)→ OrderRepositoryJpaImpl (OrderRepository)
~~~

---

## 11) 마이그레이션 노트 (중요 변경점)

- ✅ **단일 구성 `JpaInfraConfig` 로 통합** (예전 개별 Config 제거)
- ✅ **전역 스위치 키 → `jpa.enabled`** 로 통일
- ✅ 저장소 구현체에서 `@Repository` 제거 → **설정(@Bean)에서만 조건부 등록**
- ✅ OFF 기본값 — 빈/서비스 미등록으로 **다른 모듈에 영향 0**

---

## 12) 마지막 한 줄 요약
**`@Import(JpaInfraConfig)` + `jpa.enabled=true`** 만으로 필요한 JPA/QueryDSL 인프라가 **필요한 만큼만** 자동 조립됩니다.
