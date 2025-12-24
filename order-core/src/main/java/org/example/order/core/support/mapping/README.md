# 🧭 org.example.order.core.support.mapping 패키지 분석

`org.example.order.core.support.mapping` 은  
**애플리케이션 전반에서 사용하는 Mapper(MapStruct)의 시간 처리와 공통 매핑 규칙을 표준화**하기 위한  
**Support Layer** 성격의 패키지이다.

- 도메인/애플리케이션/인프라 어디에서도 재사용 가능
- 시간(`LocalDateTime`, epoch millis) 변환 규칙을 **중앙 집중화**
- 테스트 가능성과 결정성을 고려한 **Clock 주입 설계**

--------------------------------------------------------------------------------
## 1) 패키지 역할 요약

| 구분 | 설명 |
|---|---|
| 책임 | MapStruct 기반 매핑 시 **시간 처리 규칙과 공통 설정 제공** |
| 위치 | `core.support` (도메인·인프라 어느 쪽에도 종속되지 않음) |
| 성격 | 순수 기술 지원(Support) 컴포넌트 |
| 핵심 목표 | **일관성 / 컴파일 타임 안정성 / 테스트 재현성** |

--------------------------------------------------------------------------------
## 2) 구성 요소 개요

    org.example.order.core.support.mapping
    ├─ config/
    │  └─ AppMappingConfig.java
    ├─ TimeMapper.java
    └─ TimeProvider.java

--------------------------------------------------------------------------------
## 3) AppMappingConfig — 전역 MapStruct 설정

### 위치
`org.example.order.core.support.mapping.config.AppMappingConfig`

### 역할
- 애플리케이션 전역에서 사용하는 **MapStruct 공통 설정**
- 모든 Mapper 가 **동일한 규칙**을 따르도록 강제

### 핵심 설정 분석

    @MapperConfig(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        imports = {DateTimeUtils.class, LocalDateTime.class},
        uses = {TimeMapper.class, TimeProvider.class}
    )

#### 3.1 componentModel = "spring"
- 모든 Mapper 를 Spring Bean 으로 등록
- DI / 테스트 / AOP 와 자연스럽게 연동

#### 3.2 unmappedTargetPolicy = ERROR
- **매핑 누락 시 컴파일 에러**
- 필드 추가/변경 시 런타임 버그 방지
- “조용한 누락”을 원천 차단

> 이 설정은 실무에서 매우 중요하며,  
> DTO/Entity 필드 불일치로 인한 장애를 강력히 예방한다.

#### 3.3 nullValueCheckStrategy = ALWAYS
- null 체크를 MapStruct 가 항상 생성
- NPE 방지 및 방어적 매핑 보장

#### 3.4 injectionStrategy = CONSTRUCTOR
- 생성자 주입 강제
- 불변성 및 테스트 용이성 확보

#### 3.5 imports
- `DateTimeUtils`, `LocalDateTime` 를 expression 에서 바로 사용 가능
- `expression = "java(DateTimeUtils.localDateTimeToLong(...))"` 같은 패턴 지원

#### 3.6 uses
- `TimeMapper`
- `TimeProvider`

→ 모든 Mapper 에서 **시간 변환/현재 시각 제공 로직을 공통 사용**

--------------------------------------------------------------------------------
## 4) TimeMapper — 시간 변환 전용 매퍼

### 위치
`org.example.order.core.support.mapping.TimeMapper`

### 성격
- 상태 없는 **순수 유틸리티**
- MapStruct `@Named` 메서드 제공

### 제공 기능

#### 4.1 LocalDateTime → epoch millis

    @Named("localDateTimeToEpochMillis")
    public static Long localDateTimeToEpochMillis(LocalDateTime dt)

- DB / 메시지 / 외부 연동에서 사용하는 long 타입 시간 표현 표준화
- 내부적으로 `DateTimeUtils.localDateTimeToLong` 사용

#### 4.2 epoch millis → LocalDateTime

    @Named("epochMillisToLocalDateTime")
    public static LocalDateTime epochMillisToLocalDateTime(Long epochMillis)

- 읽기 모델 / 응답 DTO 에서 사람이 읽을 수 있는 시간으로 변환

### 설계 의도

- 시간 변환 로직이 Mapper 마다 흩어지는 것을 방지
- 포맷/타임존 변경 시 **단일 지점 수정**
- MapStruct `qualifiedByName` 과 결합하여 명시적 사용 가능

--------------------------------------------------------------------------------
## 5) TimeProvider — 현재 시간 제공자

### 위치
`org.example.order.core.support.mapping.TimeProvider`

### 역할
- “현재 시각(now)” 을 제공하는 **주입 가능한 컴포넌트**

### 구현 특징

    public class TimeProvider {
        private final Clock clock;
    }

#### 5.1 Clock 기반 설계
- 기본: `Clock.systemDefaultZone()`
- 테스트 시: `Clock.fixed(...)` 주입 가능

#### 5.2 제공 메서드

    public LocalDateTime now()

- `LocalDateTime.now(clock)` 사용
- 시스템 시간 직접 호출 금지

### 설계 의도

- 테스트 재현성 확보
- “현재 시간” 의존 로직을 **통제 가능한 의존성**으로 전환
- Mapper / Service / Infra 어디서든 동일한 시간 정책 적용 가능

--------------------------------------------------------------------------------
## 6) MapStruct Mapper 에서의 사용 패턴 예시

    @Mapper(config = AppMappingConfig.class)
    public interface OrderMapper {

        @Mapping(
          target = "publishedTimestamp",
          source = "publishedDatetime",
          qualifiedByName = "localDateTimeToEpochMillis"
        )
        OrderView toView(OrderEntity entity);
    }

- 전역 Config 상속
- 시간 변환은 명시적으로 지정
- 누락 필드 발생 시 컴파일 에러

--------------------------------------------------------------------------------
## 7) 아키텍처 관점 정리

- `support.mapping` 은 **도메인/인프라 중립**
- 시간 처리라는 횡단 관심사를 중앙 집중화
- MapStruct 의 단점을 설정으로 보완
    - (누락 필드, 암묵적 null 처리, 시간 의존성)

> 결과적으로  
> **안전한 매핑 + 예측 가능한 시간 처리 + 테스트 친화성** 을 동시에 달성

--------------------------------------------------------------------------------
## 8) 결론

- `AppMappingConfig` 는 애플리케이션 전역 매핑 규약의 기준점
- `TimeMapper` 는 시간 변환의 단일 진실 소스(Single Source of Truth)
- `TimeProvider` 는 “현재 시각” 을 제어 가능한 의존성으로 격리

이 패키지는 **작지만 매우 핵심적인 Support Layer** 이며,  
대규모 시스템에서 시간/매핑 관련 버그를 구조적으로 차단하는 역할을 수행한다.
