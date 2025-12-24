# 📦 infra:common.idgen.tsid 모듈

---

## 1) 모듈 개요 (현재 코드 기준)

Spring Boot + Hibernate/JPA 기반의 **TSID(Time-Sorted ID) 표준 인프라 모듈**입니다.  
현행 구조는 **설정 기반(@Bean) + `@Import` 조립**과 **조건부 빈 등록**을 사용하여,  
필요한 경우에만 활성화되고 그렇지 않으면 **안전한 폴백(Fallback)** 으로 동작하도록 설계되었습니다.

> 패키지 루트  
> `org.example.order.core.infra.common.idgen.tsid`

| 구성요소 | 역할 | 핵심 포인트(현행 코드 반영) |
|---|---|---|
| `TsidInfraConfig` | 모듈 엔트리포인트 | 외부에서는 **이 클래스 하나만 Import** |
| `TsidConfig` | TSID 핵심 설정 | `tsid.enabled=true` 일 때만 활성 |
| `FallbackIdGeneratorConfig` | 안전 폴백 | `IdGenerator` 미존재 시 자동 등록 |
| `TsidProperties` | 설정 바인딩 | `tsid.enabled / nodeBits / zoneId / preferEc2Meta` |
| `TsidFactory` | ID 팩토리 | 노드비트/노드ID/타임존/난수 기반 생성 |
| `TsidFactoryHolder` | 정적 홀더 | Hibernate 생성기에서 DI 없이 접근 |
| `@CustomTsid` | Hibernate 마커 | 엔티티 ID 필드에 부착 |
| `CustomTsidGenerator` | Hibernate 생성기 | `TsidFactoryHolder.get()` 사용 |
| `TsidIdGenerator` | 도메인 어댑터 | Domain `IdGenerator` 포트 구현 |

---

## 2) 설정 (application.yml / profile)

### 최소 설정 (현행 코드 기준)

    tsid:
      enabled: true            # ✅ 켜면 TsidFactory 생성 + 정적 홀더 초기화
      node-bits: 10            # (옵션) 기본 10 → 0~1023 노드
      zone-id: Asia/Seoul      # (옵션) 미설정 시 시스템 기본
      prefer-ec2-meta: true   # (옵션) EC2 IMDSv2 instance-id 우선 사용

> 주의
> - 프로퍼티 prefix는 **`idgen.tsid`가 아니라 `tsid`** 입니다.
> - 잘못된 `zone-id`가 들어오면 **시스템 기본 타임존으로 자동 폴백**합니다.

---

## 3) 노드 ID 결정 규칙 (현행 코드)

노드 ID는 다음 규칙으로 계산됩니다.

1. (옵션) EC2 IMDSv2에서 `instance-id` 조회 (`preferEc2Meta=true`)
2. 컨테이너/호스트의 `HOSTNAME` 환경변수
3. 각 값을 **Murmur3 32-bit 해시**
4. 두 해시를 XOR
5. `nodeBits` 하위 비트만 마스킹하여 사용

   nodeId = (hash(instanceId) ^ hash(HOSTNAME)) & ((1 << nodeBits) - 1)

- 모든 소스가 실패하면 **암호학적 랜덤(16자)** 기반 ID로 폴백
- EC2 메타데이터 접근 실패(로컬/비EC2)는 **정상 시나리오**이며 debug 로그만 남김

---

## 4) 빠른 시작 (가장 중요한 사용법)

### 4.1 엔티티 — Hibernate ID에 `@CustomTsid` 부착

    @Entity
    public class OrderEntity {

        @Id
        @org.example.order.core.infra.common.idgen.tsid.annotation.CustomTsid
        private Long id;

        private Long orderId;
    }

- `@CustomTsid`
    - `@IdGeneratorType(CustomTsidGenerator.class)` 로 연결
- Hibernate는 생성기를 **리플렉션으로 직접 생성**
- 생성기 내부에서 `TsidFactoryHolder.get()` → TSID 생성

---

### 4.2 서비스/도메인 — `IdGenerator` 포트 사용

    @Service
    @RequiredArgsConstructor
    public class OrderIdService {

        private final org.example.order.domain.common.id.IdGenerator idGenerator;

        public long nextId() {
            return idGenerator.nextId();
        }
    }

- `tsid.enabled=true`
    - `TsidConfig` 에서 TSID 기반 `IdGenerator` 제공
- `tsid.enabled=false`
    - `FallbackIdGeneratorConfig` 가 안전한 TSID 폴백 제공
- **상위 레이어는 TSID 구현을 전혀 알 필요 없음**

---

## 5) 동작 흐름

    tsid.enabled=true
      └─ TsidInfraConfig
           ├─ TsidConfig (조건부)
           │    ├─ TsidFactory 생성
           │    ├─ TsidFactoryHolder.set(factory)
           │    └─ IdGenerator(bean) 등록
           └─ FallbackIdGeneratorConfig (IdGenerator 없을 때만)

    Hibernate Entity (@CustomTsid)
      └─ CustomTsidGenerator.generate(...)
           └─ TsidFactoryHolder.get().create().toLong()

- **조건부 활성화**
    - `tsid.enabled=false` → TsidConfig 미동작
- **항상 안전**
    - `IdGenerator` 는 항상 하나 이상 존재 (폴백 보장)

---

## 6) 프로퍼티 상세 (현행)

| 프로퍼티 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `tsid.enabled` | boolean | false | TSID 모듈 활성화 스위치 |
| `tsid.node-bits` | int | 10 | 노드 비트 수 (0~1023) |
| `tsid.zone-id` | string | 시스템 기본 | TSID 시간 기준 타임존 |
| `tsid.prefer-ec2-meta` | boolean | true | EC2 instance-id 우선 사용 |

---

## 7) 테스트 가이드 (현행 코드 기준)

권장 검증 포인트:

- **비활성화 테스트**
    - `tsid.enabled=false`
    - `TsidFactory` 빈 미존재
    - `IdGenerator` 는 폴백으로 존재
- **활성화 테스트**
    - `tsid.enabled=true`
    - `TsidFactory` 빈 생성
    - `TsidFactoryHolder` 초기화 확인
- **정렬성 테스트**
    - 연속 생성 시 **단조 증가** 보장
- **동시성 테스트**
    - 멀티스레드 환경에서 중복 없음
- **폴백 안전성**
    - 잘못된 zone-id / EC2 메타데이터 실패 시 정상 기동

정렬성 예시:

    List<Long> ids = new ArrayList<>();
    for (int i = 0; i < 10_000; i++) {
        ids.add(tsidFactory.create().toLong());
    }
    assertTrue(Ordering.natural().isOrdered(ids));

---

## 8) 운영 팁 & 권장 사항

- **노드 안정성**
    - 오토스케일/Pod 교체가 잦다면
        - `nodeBits` 유지 + EC2 instance-id 활용 권장
- **시간 안정성**
    - NTP/Chrony로 클록 역행 방지
- **키 활용**
    - Kafka key / CDC key / Sharding key 로 사용 시
        - 시간 정렬 특성 고려한 파티셔닝 설계
- **보안**
    - TSID는 의미 없는 숫자지만
        - 시간 기반 추정 가능성은 인지 필요

---

## 9) 핵심 코드 요약 (현행 반영)

### 9.1 TsidInfraConfig

    @Configuration
    @Import({
        TsidConfig.class,               // tsid.enabled=true 일 때만
        FallbackIdGeneratorConfig.class // IdGenerator 미존재 시
    })
    public class TsidInfraConfig {
    }

---

### 9.2 TsidConfig (핵심)

    @Configuration
    @ConditionalOnProperty(prefix = "tsid", name = "enabled", havingValue = "true")
    public class TsidConfig {

        @Bean
        public TsidFactory tsidFactory() {
            // nodeBits / nodeId / zone 계산
            TsidFactory factory = TsidFactory.builder()
                    .withNodeBits(nodeBits)
                    .withNode(nodeId)
                    .withClock(Clock.system(zone))
                    .withRandom(new SecureRandom())
                    .build();

            TsidFactoryHolder.set(factory);
            return factory;
        }

        @Bean
        public IdGenerator idGenerator(TsidFactory factory) {
            return () -> factory.create().toLong();
        }
    }

---

### 9.3 CustomTsidGenerator

    public class CustomTsidGenerator implements IdentifierGenerator {

        public CustomTsidGenerator() {
        }

        @Override
        public Serializable generate(SharedSessionContractImplementor session, Object object) {
            return TsidFactoryHolder.get().create().toLong();
        }
    }

---

## 10) 마지막 한 줄 요약

**“`tsid.enabled` 스위치 하나로 TSID를 명확히 제어하고,  
엔티티는 `@CustomTsid`, 서비스/도메인은 `IdGenerator` 포트로 일관 사용한다.”**

→ 분산 환경에서도 **충돌 가능성 낮고 시간 정렬성이 우수한 ID**를  
안전하게 생성하는 표준 인프라 모듈입니다.
