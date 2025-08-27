# infra:idgen — TABLE/TSID 기반 식별자 생성 모듈

Spring Boot/JPA에서 **RDB TableGenerator**와 **TSID(Time-Sorted ID)** 두 가지 전략으로 식별자를 안정적으로 생성합니다.  
**엔티티 자동(@GeneratedValue TABLE)**, **Hibernate 커스텀 TSID**, **서비스 코드 직접 생성(단건/배치)** 모두 지원합니다.

================================================================================

## 1) 구성 개요

| 구성요소                              | 설명 |
|---------------------------------------|------|
| `@DefaultTableId`                     | 테이블 기반 ID 사용을 명시하는 **마커 어노테이션** |
| `SequenceDefine`                      | 공용 시퀀스 테이블명/제네레이터명 상수 정의 |
| `@CustomTsid`                         | Hibernate용 **TSID 식별자 생성기** 마커(필드/메서드) |
| `CustomTsidGenerator`                 | `IdentifierGenerator` 구현. `TsidFactoryHolder`에서 팩토리 가져와 ID 생성 |
| `TsidConfig`                          | `TsidFactory` Bean 구성(노드비트/노드ID/타임존/랜덤) + 정적 홀더에 등록 |
| `TsidFactoryHolder`                   | Hibernate 생성기 접근을 위한 **정적 팩토리 홀더** |

> **빈 등록 원칙**
> - 생성기/마커는 라이브러리이므로 `@Component` 금지
> - `TsidConfig` 만 Spring Bean (`@Configuration`)에서 `TsidFactory` 제공

================================================================================

## 2) TABLE 기반 ID — 사용법(우선순위: 설정/가독성/이식성)

### 2.1 시퀀스 테이블 준비
```sql
CREATE TABLE id_sequence (
    seq_name  VARCHAR(100) NOT NULL PRIMARY KEY,
    next_val  BIGINT       NOT NULL
);
```

### 2.2 엔티티 선언 예시
```java
@Entity
@Table(name = "sample_entity")
@TableGenerator(
    name = "DefaultTableIdGenerator",
    table = "id_sequence",      // 공용 시퀀스 테이블
    pkColumnName = "seq_name",  // 시퀀스 키 컬럼
    valueColumnName = "next_val", // 증가 값 컬럼
    pkColumnValue = "sample_entity_id", // 이 엔티티 전용 시퀀스 이름
    allocationSize = 50         // 성능/간격 절충
)
public class SampleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "DefaultTableIdGenerator")
    @org.example.order.core.infra.common.idgen.table.annotation.DefaultTableId
    private Long id;

    private String name;
}
```

### 2.3 공통 상수(선택)
```java
public final class SequenceDefine {
    public static final String SEQUENCE_TABLE = "id_sequence";
    public static final String GENERATOR_SALES_ORDER = "SalesOrderIdGenerator";
    // ... 필요 제네레이터명 상수 정의
}
```

> **TIP**
> - `allocationSize` 는 ID 갭과 성능의 절충. 트래픽/장애 허용도에 맞게 조정
> - 다중 서비스/리전에서 **단일 DB** 로 일관 시 **TABLE 전략**은 충돌 없이 안전

================================================================================

## 3) TSID 기반 ID — 사용법(우선순위: 분산/정렬/스케일)

TSID(Time-Sorted ID)는 시간순 정렬이 가능한 64-bit ID 입니다. 분산 노드에서 충돌 낮고 **삽입 정렬/파트셔닝**에 유리합니다.

### 3.1 설정(Boot)
```java
@Slf4j
@Configuration
public class TsidConfig {

    private static final int DEFAULT_NODE_BITS = 10; // 노드ID 비트(기본 10 → 0~1023)
    @Bean
    public TsidFactory tsidFactory() {
        int nodeBits = Integer.parseInt(System.getenv().getOrDefault("ENV_NODE_BITS", String.valueOf(DEFAULT_NODE_BITS)));

        // 노드ID 결정: EC2 instance-id + POD(HOSTNAME) 해시 → XOR → 10-bit
        int nodeId = computeNodeId();       // 내부적으로 metadata나 HOSTNAME 없으면 랜덤 fallback
        ZoneId zone = getZone();            // ENV_TZ 없으면 systemDefault
        TsidFactory f = TsidFactory.builder()
                .withNodeBits(nodeBits)
                .withNode(nodeId)
                .withClock(java.time.Clock.system(zone))
                .withRandom(new java.security.SecureRandom())
                .build();

        // Hibernate 생성기가 접근할 수 있도록 정적 홀더에 주입
        TsidFactoryHolder.set(f);
        return f;
    }
    // ... fetchInstanceId(EC2 메타데이터 토큰), murmur 해시, HOSTNAME, fallback 랜덤 등 내장
}
```

### 3.2 엔티티에 TSID 적용(Hibernate 전용)
```java
@Entity
public class TsidEntity {
    @Id
    @org.example.order.core.infra.common.idgen.tsid.annotation.CustomTsid
    private Long id; // Hibernate가 CustomTsidGenerator 통해 생성
    private String name;
}
```

- `@CustomTsid` → `@IdGeneratorType(CustomTsidGenerator.class)`
- Hibernate가 **기본 생성자**로 `CustomTsidGenerator` 를 생성 → `TsidFactoryHolder.get()` 로 팩토리 획득 → `toLong()`

### 3.3 서비스 코드에서 직접 생성(프레임워크 의존 無)
```java
@Service
@RequiredArgsConstructor
public class OrderIdService {
    private final com.github.f4b6a3.tsid.TsidFactory tsidFactory;

    // A) 단건 생성
    public long nextId() {
        return tsidFactory.create().toLong();
    }

    // B) N개 배치 생성(연속성/정렬성 유지)
    public List<Long> nextIds(int size) {
        List<Long> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) ids.add(tsidFactory.create().toLong());
        return ids;
    }

    // C) 범위(batch) 스트림 (대량 생성 파이프라인)
    public LongStream streamIds(int size) {
        return LongStream.generate(() -> tsidFactory.create().toLong()).limit(size);
    }
}
```

> **언제 TSID?**
> - 멀티 리전/멀티 인스턴스/샤딩에서 **충돌 없는 고성능** ID
> - 시간순 정렬로 **인덱스 분할/쓰기 증폭** 감소
> - Kafka 키/파티셔닝 키/URL-safe 키 등으로 활용

================================================================================

## 4) 노드 ID 결정 규칙(운영 안전성)

- **입력 소스**: EC2 `instance-id`(IMDSv2) + `HOSTNAME`(컨테이너/pod)
- **해시/마스킹**: Murmur3 32-bit → 하위 10bit 사용(`& 0x3FF`) → 두 값 XOR
- **Fallback**: 어떤 값도 없으면 **암호학적 랜덤 16자 문자열** 생성 후 해시
- **환경 변수**:
    - `ENV_NODE_BITS` → 노드 비트(기본 10)
    - `ENV_TZ` → 타임존(없으면 시스템 기본)

> **주의**: 노드 충돌 확률이 낮지만, **스테이트리스 배포/오토스케일** 환경에서 HOSTNAME이 자주 바뀌면 노드ID도 바뀔 수 있음.  
> 장기 고정이 필요하면 환경변수/설정으로 **고정 노드ID**를 주입하도록 확장 가능.

================================================================================

## 5) 전략 선택 가이드

| 상황/요구                                | 추천 |
|------------------------------------------|------|
| 단일 RDB 내 일관성, DB가 단일 권위       | **TABLE** |
| 분산/샤딩/멀티리전, 타임소팅 인덱싱 유리 | **TSID** |
| 이식성/표준 JPA 우선                     | **TABLE** |
| Kafka 키/정렬·파티셔닝/NoSQL 친화        | **TSID** |

> **혼용 가능**: 핵심 엔티티는 TSID, 레거시는 TABLE 등 **도메인별 선택**.

================================================================================

## 6) 빠른 시작(요약)

### 6.1 TABLE
1) `id_sequence` 테이블 생성
2) 엔티티에 `@TableGenerator` + `@GeneratedValue(TABLE)`
3) 필요 시 `SequenceDefine` 상수 재사용

### 6.2 TSID
1) `TsidConfig` 로 `TsidFactory` 빈 구성(자동)
2) JPA/Hibernate: `@CustomTsid` 를 ID 필드에 부착
3) 서비스 코드: `tsidFactory.create().toLong()` 으로 직접 생성(단건/배치)

================================================================================

## 7) 테스트 가이드

- **TABLE**:
    - H2/MySQL 로 `id_sequence` 준비 후 `@DataJpaTest` 로 증가/동시성 검증
    - `allocationSize` 효과(갭/성능) 벤치마크

- **TSID**:
    - 동일 시간대 생성 ID 정렬/단조 증가 확인
    - 다중 쓰레드에서 충돌 없음 검증
    - `ENV_NODE_BITS`, `ENV_TZ` 주입 테스트
    - EC2 메타데이터 불가 환경에서 **fallback 경고 로그** 확인

================================================================================

## 8) 예외/트러블슈팅

- **TSID 생성 시 `IllegalStateException`**: `TsidFactoryHolder` 미초기화 → `TsidConfig` 누락 여부 확인
- **TABLE 간헐적 갭**: `allocationSize` 특성(배치 사전할당) → 정상
- **노드ID 불안정**: Pod/인스턴스 교체 잦음 → 환경변수로 고정/외부 주입 고려
- **시간역행**: 시스템 클록 역행 시 TSID 순서성 영향 → NTP/ClockSync 권장

================================================================================

## 9) 보안/운영 권장

- TSID는 의미 없는 숫자이므로 PII 노출 위험 낮음(단, **추정 가능성** 고려: 타임 기반)
- 멀티 리전에서 **노드ID 설계/운영 규칙** 문서화
- 대량 삽입에서 **TSID 정렬성**을 활용해 B-Tree 분할 최소화
- CDC/Kafka 키에 TSID 사용 시 **파티셔닝 전략** 일관 유지

================================================================================

## 10) 샘플 묶음

### 10.1 TABLE — 주문 엔티티
```java
@Entity
@Table(name = "sales_order")
@TableGenerator(
    name = "SalesOrderIdGenerator",
    table = "id_sequence",
    pkColumnName = "seq_name",
    valueColumnName = "next_val",
    pkColumnValue = "sales_order_id",
    allocationSize = 100
)
public class SalesOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.TABLE, generator = "SalesOrderIdGenerator")
  @DefaultTableId
  private Long id;
  private String title;
}
```

### 10.2 TSID — Hibernate 엔티티
```java
@Entity
public class Payment {
  @Id @CustomTsid
  private Long id;
  private Long orderId;
}
```

### 10.3 서비스에서 TSID 직접 생성(배치)
```java
List<Long> ids = orderIdService.nextIds(10); // 10개 연속 생성
```

================================================================================

## 11) 마지막 한 줄 요약
**TABLE**: 단일 DB에서 안전하고 단순. **TSID**: 분산 환경에서 충돌 낮고 시간 정렬 우수.  
모듈은 두 전략을 **엔티티/서비스 코드** 모두에서 쉽게 쓰도록 표준화합니다.
