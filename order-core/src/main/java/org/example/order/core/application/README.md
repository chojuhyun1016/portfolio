# 📦 order-core:application 패키지 구조 및 책임 정리

본 문서는 `order-core:application` 계층 중 `org.example.order.core.application.order` 패키지의 **현행 코드(캐시/DTO/Mapper 구성)** 를 기준으로 디렉토리 구조, 책임, 포함 파일 유형을 정리합니다.  
구조는 DDD / MSA 원칙을 기반으로 Application Layer에서 **Command / Query / Sync(Internal) / View** 역할을 분리하며,  
도메인 계층과의 **변환 책임(Mapper)** 을 Application 계층에서 명확히 담당하도록 설계되어 있습니다.

> ✅ 본 문서는 “설계 의도”가 아니라 **현재 코드 기준(현행화)** 으로 작성되었습니다.  
> (OrderCacheConfig/Properties/Services, Command/Query/Sync/View DTO, MapStruct Mapper 구성 반영)

---

## 📁 order (org.example.order.core.application.order)

현재 `order` 패키지는 “주문 유스케이스(Application) + 캐시 어댑션”을 중심으로 구성되며, 주요 하위 영역은 다음과 같습니다.

- **cache**: 캐시 사용을 코어에서 캡슐화 (`order-cache` 모듈 직접 의존 최소화)
- **dto**: Application 계층 전용 DTO (Command / Query / Sync / View)
- **mapper**: Domain ↔ Application DTO 변환, 메시지 변환, 캐시 레코드 변환
- **mapper.config**: Mapper 패키지 스캔 구성

---

# 📁 cache

캐시 기능을 코어에서 통일 관리하며, 상위 모듈(API/worker 등)이 `order-cache` 모듈 타입에 직접 의존하지 않도록 캡슐화합니다.

## 📁 cache/config

### ✅ OrderCacheConfig.java
- 목적
    - 코어 모듈에서 `@Service/@Component` 스캔 의존을 피하고, 캐시 관련 빈을 **@Bean 팩토리 방식**으로 제공
    - 상위 AutoConfig에서 포함되며 캐시 사용 여부는 `order.cache.*` 토글로 제어

- 활성화 조건(현행)
    - `order.cache.enabled=true` AND `order.cache.redis.enabled=true`
    - `OrderCacheRepository` 클래스 존재(모듈 존재)
    - `OrderCacheRepository` 빈 존재(저장소 구성 완료)
    - 위 조건 만족 시 `OrderCacheService`, `OrderCacheWriteService`를 생성 (`@ConditionalOnMissingBean` 포함)

> ⚠️ 주의  
> `OrderCacheService` 생성 시 Mapper 주입 타입이  
> `org.example.order.core.application.order.mapper.OrderCacheViewMapper` 이므로,  
> **Mapper 구현체(MapStruct)가 스캔 등록되지 않으면 캐시 서비스 빈 생성이 실패**할 수 있습니다.

---

## 📁 cache/props

### ✅ OrderCacheProperties.java
- `@ConfigurationProperties(prefix = "order.application.cache")`
- 속성
    - `defaultTtlSeconds: Long`
        - `null`이면 기본 TTL을 사용하지 않으며, **호출자가 TTL을 직접 지정해야 함**
- 설정 예시(현행)
    - 토글(캐시 on/off)
        - `order.cache.enabled=true`
        - `order.cache.redis.enabled=true`
    - 코어 캐시 TTL
        - `order.application.cache.default-ttl-seconds=300`

---

## 📁 cache (service)

### ✅ OrderCacheService.java
- 목적
    - `order-cache` 레이어를 감싸는 코어 서비스
    - 상위 모듈이 `order-cache` 모듈에 직접 의존하지 않고 조회를 수행하도록 제공
- 주요 동작
    - `getViewByOrderId(orderId)`:
        - `OrderCacheRepository.get(orderId)` 결과를 `OrderCacheViewMapper.toView`로 변환하여 `Optional<OrderView>` 반환
- 트랜잭션
    - `@Transactional(readOnly = true)`

### ✅ OrderCacheWriteService.java
- 목적
    - 캐시 쓰기/삭제 책임을 코어에서 통일 관리
    - 상위 모듈(worker 등)이 캐시 레코드 타입(`OrderCacheRecord`)에 직접 의존하지 않도록 캡슐화
- 주요 동작(현행)
    - `upsert(LocalOrderSync sync, Long ttlSeconds)`
        - `OrderCacheAssembler.from(sync)` → `OrderCacheRecord` 생성 후 `repo.put(rec, ttlSeconds)`
    - `upsert(LocalOrderSync sync)`
        - `OrderCacheProperties.defaultTtlSeconds` 가 `null`이면 **warn 로그 후 skip** (보수적 정책)
    - `evict(Long orderId)`
        - `repo.evict(orderId)`
- 트랜잭션
    - `@Transactional`

---

# 📁 dto

Application 계층 내부에서만 사용하는 DTO를 목적별로 구분합니다.  
현행 코드 기준으로 `command`, `query`, `sync`, `view`가 존재합니다.

## 📁 dto/command

### ✅ LocalOrderCommand.java
- 목적
    - “Local 주문” 관련 커맨드 전달용 Application DTO
- 형태
    - `record (Long orderId, Operation operation)`
- 비고
    - 외부 계약(HTTP/토픽 스키마) DTO가 아닌 **Application 내부 DTO**
    - `Operation`은 `org.example.order.contract.shared.op.Operation` 사용

### ✅ OrderCommand.java
- 목적
    - “Order” 관련 커맨드 전달용 Application DTO
- 형태
    - `record (Long orderId, Operation operation)`

---

## 📁 dto/query

### ✅ LocalOrderQuery.java
- 목적
    - 주문 단건 조회용 Query DTO (Application 계층 전용)
- 형태
    - `record (Long orderId)`

### ✅ OrderQuery.java
- 목적
    - 주문 단건 조회용 Query DTO (Application 계층 전용)
- 형태
    - `record (Long orderId)`

---

## 📁 dto/sync

동기화/파이프라인 처리에 사용하는 Application DTO입니다.  
**불변(record) + with-메서드**로 상태 변형 시 새 인스턴스를 반환합니다.  
또한 외부 계약(HTTP/토픽 스키마) DTO와 분리되어 있습니다.

### ✅ LocalOrderSync.java / OrderSync.java
- 공통 필드(현행)
    - 식별/주문/사용자: `id, userId, userNumber, orderId, orderNumber, orderPrice`
    - 상태: `deleteYn, version`
    - 메타: `createdUserId/Type/Datetime`, `modifiedUserId/Type/Datetime`
    - 발행: `publishedTimestamp (Long, ms)`
    - 내부 플래그: `failure (boolean, @JsonIgnore)`

- 제공 메서드(현행)
    - `withCreatedMeta(userId, userType, datetime)`
    - `withModifiedMeta(userId, userType, datetime)`
    - `withFailure()`
    - `withPublishedTimestamp(newTs)`
    - 부분 필드 업데이트 예시:
        - `withOrderNumber(newOrderNumber)`
        - `withOrderPrice(newOrderPrice)`
        - `withVersion(newVersion)`
    - `publishedDateTimeStr()`
        - `publishedTimestamp(ms)` → `UTC 기준 LocalDateTime` 변환 후 문자열 반환
        - `LocalDateTime.toString()`의 `T`를 `" "`로 치환

> ✅ 설계 포인트
> - 동기화 DTO는 “외부 계약 DTO”가 아니라 Application 내부 DTO이며,  
    >   메시지/DB/도메인 변환은 `mapper` 책임으로 분리됩니다.

---

## 📁 dto/view

조회 결과를 표현하는 Application 계층 전용 View DTO 입니다.  
API 응답 DTO와 1:1이 아니며, 내부 유스케이스 결과를 표현합니다.

### ✅ LocalOrderView.java / OrderView.java
- 형태
    - `@Getter + @Builder + @AllArgsConstructor`
- 필드(현행)
    - `dto/sync`의 주요 필드와 동일 (id, user*, order*, deleteYn, version, created*, modified*, publishedTimestamp)
    - `failure: Boolean`

---

# 📁 mapper

Application ↔ Domain 객체 간 변환 책임을 수행합니다.  
도메인 계층은 Application DTO를 모르므로, 변환은 반드시 Application(또는 그 상위) 계층에서 수행합니다.

## 📁 mapper/config

### ✅ OrderMapperConfig.java
- 목적
    - `org.example.order.core.application.order.mapper` 패키지에 존재하는 MapStruct 구현체(`@Mapper`)를 자동 등록
    - 외부 모듈(worker, api 등)이 mapper 패키지를 직접 스캔하지 않아도 되도록 구성
- 방식(현행)
    - `@ComponentScan(basePackages = "org.example.order.core.application.order.mapper")`

---

## ✅ MapStruct Mapper 목록 (현행)

### ✅ LocalOrderMapper.java
- 담당 변환(현행)
    - `LocalOrderCommand -> OrderLocalMessage`
    - `LocalOrderEntity -> LocalOrderSync`
    - `LocalOrderSync -> LocalOrderEntity`
    - `LocalOrderSync -> OrderUpdate`
    - `LocalOrderSync -> LocalOrderView`
    - `LocalOrderEntity -> LocalOrderView`
- 주요 특징(현행)
    - `publishedDatetime(LocalDateTime) <-> publishedTimestamp(Long)` 변환
    - `failure` 기본값 `false`
    - `record`의 `with*` 메서드는 실제 속성이 아니므로 `ignore`
    - `@ObjectFactory`로 엔티티 생성 시 `id` 주입
    - 메시지 변환 시:
        - `orderType = ORDER_LOCAL`
        - `publishedTimestamp = now() -> epochMillis`

---

### ✅ OrderMapper.java
- 담당 변환(현행)
    - `LocalOrderCommand -
