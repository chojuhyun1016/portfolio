# 📦 infra:idgen.tsid 모듈

---

## 1) 모듈 개요 (현재 코드 기준)

Spring Boot + Hibernate/JPA 기반의 **TSID(Time-Sorted ID) 표준 모듈**입니다.  
최신 구조는 **설정 기반(@Bean) + `@Import` 조립**과 **조건부 빈 등록**으로, 필요할 때만 켜지도록 설계되었습니다.

| 구성요소 | 역할 | 핵심 포인트(코드 반영) |
|---|---|---|
| `TsidModuleConfig` | 모듈 통합 Import | 코어 설정을 **한 번에 로드**, 내부 Config는 `@ConditionalOnProperty`로 ON/OFF |
| `TsidCoreConfig` | `TsidFactory` 빈 | `idgen.tsid.enabled=true`일 때만 활성, **노드비트/노드ID/타임존** 설정, 정적 홀더 등록 |
| `TsidProperties` | 설정 바인딩 | `idgen.tsid.enabled`, `node-bits(옵션)`, `timezone(옵션)` |
| `@CustomTsid` | Hibernate 마커 | ID 필드에 부착 → Hibernate가 `CustomTsidGenerator` 사용 |
| `CustomTsidGenerator` | 식별자 생성기 | `TsidFactoryHolder.get().create().toLong()` 으로 64-bit TSID 생성 |
| `TsidFactoryHolder` | 정적 팩토리 홀더 | Hibernate 생성기에서 스프링 DI 없이 팩토리 접근 |
| **테스트** | 단위/통합 검증 | 정렬성/충돌 없음/환경변수 대체(Fallback) 동작 검증, enabled/disabled 조건부 빈 생성 테스트 |

> 패키지 예시:  
> `org.example.order.core.infra.common.idgen.tsid.config.*` / `.config.internal` / `.config.property` / `.annotation` / `.generator`

---

## 2) 설정 (application.yml / profile)

최소/공통 (코드 반영)

    idgen:
      tsid:
        enabled: true          # ✅ 켜면 TsidFactory 생성 + 정적 홀더 등록
        node-bits: 10          # (옵션) 기본 10 → 0~1023 노드
        timezone: Asia/Seoul   # (옵션) 미설정 시 시스템 기본/ENV_TZ

환경변수(Fallback)

    ENV_NODE_BITS=10   # 프로퍼티 미설정 시 노드비트 대체
    ENV_TZ=UTC         # 프로퍼티 미설정 시 타임존 대체

> **노드ID 결정**: EC2 `instance-id` + 컨테이너 `HOSTNAME` 를 Murmur3-32 해시 후 **하위 10비트**만 사용하고 XOR.  
> 어떤 값도 없으면 **암호학적 랜덤(16자)** 기반 해시로 대체하여 **충돌 가능성 최소화**.

---

## 3) 빠른 시작 (가장 중요한 사용법)

### 3.1 엔티티 — Hibernate 필드에 `@CustomTsid` 부착

    @Entity
    public class Payment {
        @Id
        @org.example.order.core.infra.common.idgen.tsid.annotation.CustomTsid
        private Long id;   // Hibernate가 CustomTsidGenerator로 TSID 생성
        private Long orderId;
    }

- `@CustomTsid` → `@IdGeneratorType(CustomTsidGenerator.class)`
- Hibernate가 기본 생성자로 생성기 인스턴스화 → `TsidFactoryHolder.get()` → `toLong()`

### 3.2 서비스 — 스프링 빈으로 직접 생성(단건/배치)

    @Service
    @RequiredArgsConstructor
    public class OrderIdService {
        private final com.github.f4b6a3.tsid.TsidFactory tsidFactory;

        public long nextId() {
            return tsidFactory.create().toLong();
        }

        public List<Long> nextIds(int size) {
            List<Long> ids = new ArrayList<>(size);
            for (int i = 0; i < size; i++) ids.add(tsidFactory.create().toLong());
            return ids;
        }
    }

- 프레임워크 의존 없이 **서비스 레이어에서 직접 생성** 가능
- 동일 시각대에서도 **시간 정렬성** 유지(쓰기/인덱싱에 유리)

---

## 4) 동작 흐름

    idgen.tsid.enabled=true
      └─ TsidCoreConfig
           ├─ TsidFactory (node-bits / node-id / timezone / secure-random)
           └─ TsidFactoryHolder.set(factory)  // Hibernate 생성기 접근 경로

    JPA 엔티티(@CustomTsid)
      └─ CustomTsidGenerator.generate(...)
           └─ TsidFactoryHolder.get().create().toLong()

- **조건부 활성화**: `idgen.tsid.enabled=false` 면 어떤 빈/생성기도 **미등록**
- **정적 홀더**: Hibernate가 스프링 컨텍스트 밖에서 생성기를 만들더라도 **동일 팩토리** 접근 보장

---

## 5) 프로퍼티 상세

- `idgen.tsid.enabled` (boolean) : **ON/OFF 스위치**
- `idgen.tsid.node-bits` (int) : 노드 비트수(기본 10 → 0~1023)
- `idgen.tsid.timezone` (string) : `Asia/Seoul`, `UTC` 등(미설정 시 시스템 기본/ENV_TZ)

환경변수(Fallback)
- `ENV_NODE_BITS` : 프로퍼티 미설정 시 사용
- `ENV_TZ` : 프로퍼티 미설정 시 사용

---

## 6) 테스트 가이드 (코드 반영 해설)

- **조건부 빈 비활성화 테스트** — `enabled=false` ⇒ `TsidFactory` 빈 미생성 (`NoSuchBeanDefinitionException` 검증)
- **활성화 테스트** — `enabled=true` ⇒ `TsidFactory` 빈 생성 + 홀더 초기화 확인
- **정렬성 테스트** — N개 연속 생성 후 **오름차순** 보장 확인
- **동시성/충돌 테스트** — 멀티스레드 병행 생성 시 **중복 없음** 확인
- **환경변수 Fallback** — 프로퍼티 미지정 시 ENV로 노드비트/타임존 적용 확인

핵심 포인트(정렬성 확인):

    List<Long> ids = new ArrayList<>();
    for (int i = 0; i < 10_000; i++) ids.add(tsidFactory.create().toLong());
    assertTrue(Ordering.natural().isOrdered(ids)); // 단조 증가 확인

---

## 7) 운영 팁 & 권장 설정

- **노드ID 안정성**: 오토스케일/Pod 교체가 잦으면 HOSTNAME 해시가 변할 수 있음 → **고정 노드ID 정책**(ENV/설정 확장) 고려
- **시간 동기화**: NTP/Chrony 등으로 **클록 역행** 방지(시간 정렬성 보호)
- **키 활용**: Kafka 키/샤딩 키/CDC 키로 활용 시 파티셔닝 전략과 함께 설계
- **보안/노출**: TSID는 의미 없는 숫자라 PII 노출 위험 낮지만, **시간 기반 추정 가능성**은 고려

---

## 8) 확장/개선 제안 (선택)

- **고정 노드ID 주입**: `idgen.tsid.node-id`(정수) 직접 주입 옵션 추가
- **배치 API 유틸**: 대량 생성 스트림/배치 헬퍼 제공(`LongStream`, `Spliterator`)
- **메트릭 노출**: 생성 속도/충돌 카운트(논리)/노드정보 지표화(Prometheus)
- **테스트 유틸**: `@WithFixedClock(zone, instant)` 같은 고정 클록 주입 테스트 지원

---

## 9) 핵심 코드 스니펫(반영 확인)

### 9.1 TsidCoreConfig 요지

    @Configuration
    @EnableConfigurationProperties(TsidProperties.class)
    @ConditionalOnProperty(prefix = "idgen.tsid", name = "enabled", havingValue = "true")
    public class TsidCoreConfig {
        private final TsidProperties props;
        @Bean
        public TsidFactory tsidFactory() {
            int nodeBits = Optional.ofNullable(props.getNodeBits())
                    .orElseGet(() -> env("ENV_NODE_BITS").map(Integer::parseInt).orElse(10));
            int nodeId = computeNodeId(); // EC2 instance-id ^ HOSTNAME (murmur32 하위 10bit)
            ZoneId zone = Optional.ofNullable(props.getTimezone()).map(ZoneId::of)
                    .orElseGet(() -> env("ENV_TZ").map(ZoneId::of).orElse(ZoneId.systemDefault()));
            TsidFactory f = TsidFactory.builder()
                    .withNodeBits(nodeBits)
                    .withNode(nodeId)
                    .withClock(Clock.system(zone))
                    .withRandom(new SecureRandom())
                    .build();
            TsidFactoryHolder.set(f);
            return f;
        }
    }

### 9.2 CustomTsidGenerator 요지

    public class CustomTsidGenerator implements IdentifierGenerator {
        public CustomTsidGenerator() {}
        @Override
        public Serializable generate(SharedSessionContractImplementor session, Object object) {
            return TsidFactoryHolder.get().create().toLong();
        }
    }

### 9.3 모듈 조립(Import) 요지

    @Configuration
    @Import({TsidCoreConfig.class})
    public class TsidModuleConfig {
        // 대표 EntryPoint: 외부에서 이 클래스 하나만 Import 하면 됨
    }

---

## 10) 마지막 한 줄 요약
**“yml 스위치(`idgen.tsid.enabled`)로 TSID를 명확히 제어하고, 엔티티는 `@CustomTsid`—서비스는 `tsidFactory.create().toLong()`—으로 일관 사용.”**  
노드/타임존/난수까지 표준화하여 **분산 환경에서도 충돌 낮고 시간 정렬성 우수한 ID**를 안전하게 생성합니다.
