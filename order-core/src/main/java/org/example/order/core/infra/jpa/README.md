# 🧩 JPA 모듈 (QueryDSL + 설정 기반 @Bean + @Import 조립)

Spring Boot에서 **JPA/QueryDSL 인프라**를 가볍고 통제 가능하게 조립해 주는 모듈입니다.  
본 모듈은 **Spring Data Repository 스캔을 사용하지 않으며**,  
**설정(@Configuration) + @Bean + 조건부 등록 + @Import** 방식만을 사용합니다.

---

## 1) 구성 개요

| 구성 요소 | 설명 |
|---|---|
| **`JpaInfraConfig`** | 단일 진입점. `jpa.enabled=true`일 때만 활성화되며 하위 Config를 명시적으로 Import |
| `JPAQueryFactory` | `EntityManager`가 존재할 때 단일 빈으로 등록되는 QueryDSL 진입점 |
| `OrderCommandRepositoryJdbcImpl` | JDBC 기반 대량 Insert/Update Command 저장소 |
| `LocalOrderCommandRepositoryJdbcImpl` | local_order 전용 JDBC Command 저장소 |
| `OrderQueryRepositoryJpaImpl` | QueryDSL 기반 Order 조회 저장소 |
| `LocalOrderQueryRepositoryJpaImpl` | QueryDSL 기반 LocalOrder 조회 저장소 |
| `OrderRepositoryJpaImpl` | JPA + QueryDSL 기반 Order 저장/삭제 리포지토리 |
| `LocalOrderRepositoryJpaImpl` | JPA + QueryDSL 기반 LocalOrder 저장/삭제 리포지토리 |
| `BooleanToYNConverter` | Boolean ↔ "Y"/"N" 전역 컨버터 (`@Converter(autoApply=true)`) |
| `QuerydslUtils` | QueryDSL 5.x 대응 페이지네이션/스트림 유틸 |
| `WhereClauseBuilder`, `LazyBooleanExpression` | 가독성 높은 동적 where DSL |

> 원칙
> - 라이브러리 계층에는 `@Component`, `@Repository` 사용 금지
> - 모든 빈은 **설정(@Bean) + 조건부 애노테이션**으로만 등록
> - Spring Data JPA 인터페이스 스캔 미사용

---

## 2) 동작 모드

### 2.1 OFF (기본)
~~~yaml
jpa:
  enabled: false
~~~
- JPA 관련 인프라 빈이 **단 하나도 등록되지 않음**
- JPA/QueryDSL/Repository/Converter 전부 미로딩
- 다른 모듈에 영향 없음

### 2.2 ON
~~~yaml
jpa:
  enabled: true
~~~
- `JpaInfraConfig`가 활성화되며, **존재 조건을 만족하는 구성요소만 개별 조립**

조립 조건 요약
- `JPAQueryFactory` → `EntityManager` 존재 시
- `OrderCommandRepositoryJdbcImpl` → `JdbcTemplate` 존재 시
- `LocalOrderCommandRepositoryJdbcImpl` → `JdbcTemplate` 존재 시
- `OrderQueryRepositoryJpaImpl` → `JPAQueryFactory` 존재 시
- `LocalOrderQueryRepositoryJpaImpl` → `JPAQueryFactory` 존재 시
- `OrderRepositoryJpaImpl` → `JPAQueryFactory` + `EntityManager` 존재 시
- `LocalOrderRepositoryJpaImpl` → `JPAQueryFactory` + `EntityManager` 존재 시

---

## 3) 빠른 시작 (@Import 기반 조립)

### 3.1 의존성
~~~groovy
dependencies {
  implementation "org.springframework.boot:spring-boot-starter-data-jpa"
  implementation "com.querydsl:querydsl-jpa"
  implementation "com.github.f4b6a3:tsid-creator:5.2.6"

  runtimeOnly "org.mariadb.jdbc:mariadb-java-client"
}
~~~

### 3.2 애플리케이션 조립
~~~java
@Import(org.example.order.core.infra.jpa.config.JpaInfraConfig.class)
public class App {
}
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
  datasource:
    url: jdbc:mariadb://localhost:3306/orderdb
    username: app
    password: secret
~~~

---

## 4) 저장소 사용 예시

### 4.1 Command 저장소 (JDBC)
~~~java
@Service
@RequiredArgsConstructor
public class OrderBulkService {

  private final OrderCommandRepository commandRepo;

  public void insertAll(List<OrderEntity> rows) {
    commandRepo.bulkInsert(rows);
  }

  public void updateAll(List<OrderUpdate> updates) {
    commandRepo.bulkUpdate(updates);
  }
}
~~~

### 4.2 Query 저장소 (QueryDSL)
~~~java
@Service
@RequiredArgsConstructor
public class OrderQueryService {

  private final OrderQueryRepository queryRepo;

  public OrderView find(Long orderId) {
    return queryRepo.fetchByOrderId(orderId);
  }
}
~~~

### 4.3 기본 CRUD (JPA + QueryDSL)
~~~java
@Service
@RequiredArgsConstructor
public class OrderCrudService {

  private final OrderRepository orderRepository;

  public Optional<OrderEntity> find(Long id) {
    return orderRepository.findById(id);
  }

  public void save(OrderEntity entity) {
    orderRepository.save(entity);
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

  return queryFactory.selectFrom(ORDER).where(w).fetch();
}
~~~

### 5.2 페이지네이션 (Querydsl 5.x 대응)
~~~java
public Page<OrderEntity> page(Pageable pageable) {
  var query = queryFactory.selectFrom(ORDER);

  return QuerydslUtils.page(
          new Querydsl(entityManager, new PathBuilder<>(OrderEntity.class, "order")),
          query,
          pageable
  );
}
~~~

### 5.3 스트리밍
~~~java
try (var stream = QuerydslUtils.stream(
        queryFactory.selectFrom(ORDER)
)) {
        stream.forEach(e -> {
        // 처리
        });
        }
~~~

---

## 6) Boolean ↔ Y/N 컨버터

~~~java
@Converter(autoApply = true)
public class BooleanToYNConverter
        implements AttributeConverter<Boolean, String> {

  @Override
  public String convertToDatabaseColumn(Boolean attribute) {
    if (attribute == null) return null;
    return attribute ? "Y" : "N";
  }

  @Override
  public Boolean convertToEntityAttribute(String dbData) {
    if (dbData == null) return null;
    return "Y".equalsIgnoreCase(dbData);
  }
}
~~~

- 전 엔티티 자동 적용
- 도메인은 컨버터를 인지하지 않음
- DB 표현 통일 (`VARCHAR(1)`)

---

## 7) 조건부 등록 규칙 요약

- 전역 게이트: `jpa.enabled=true`
- 하위 Config는 **모두 JpaInfraConfig에서만 Import**
- `@ConditionalOnMissingBean`으로 사용자 확장 허용
- 빈 경합 방지를 위해 `@ConditionalOnBean(EntityManager...)` 사용하지 않음

---

## 8) 테스트 팁

### 8.1 OFF 검증
~~~java
new ApplicationContextRunner()
  .withPropertyValues("jpa.enabled=false")
  .withConfiguration(UserConfigurations.of(JpaInfraConfig.class))
        .run(ctx -> assertThat(ctx).doesNotHaveBean(JPAQueryFactory.class));
~~~

### 8.2 ON 검증
~~~java
new ApplicationContextRunner()
  .withPropertyValues("jpa.enabled=true")
  .withUserConfiguration(TestInfraBeans.class)
  .withConfiguration(UserConfigurations.of(JpaInfraConfig.class))
        .run(ctx -> {
assertThat(ctx).hasSingleBean(JPAQueryFactory.class);
assertThat(ctx).hasSingleBean(OrderRepository.class);
  });
~~~

---

## 9) 클래스 다이어그램 (개념)

~~~text
JpaInfraConfig (gate: jpa.enabled)
 ├─> JPAQueryFactory (if EntityManager)
 ├─> OrderCommandRepositoryJdbcImpl (if JdbcTemplate)
 ├─> LocalOrderCommandRepositoryJdbcImpl (if JdbcTemplate)
 ├─> OrderQueryRepositoryJpaImpl (if JPAQueryFactory)
 ├─> LocalOrderQueryRepositoryJpaImpl (if JPAQueryFactory)
 ├─> OrderRepositoryJpaImpl (if JPAQueryFactory + EntityManager)
 └─> LocalOrderRepositoryJpaImpl (if JPAQueryFactory + EntityManager)
~~~

---

## 10) 마이그레이션 노트

- ✅ 단일 진입점 `JpaInfraConfig`
- ✅ 전역 스위치 `jpa.enabled`
- ✅ Repository 어노테이션 제거 → 설정 기반 등록
- ✅ OFF 기본값 → 다른 모듈 영향 없음

---

## 11) 마지막 한 줄 요약
**`@Import(JpaInfraConfig)` + `jpa.enabled=true` 만으로 JPA/QueryDSL 인프라가 필요한 만큼만 안전하게 조립됩니다.**
