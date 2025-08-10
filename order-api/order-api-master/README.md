# 📦 order-api-master

`order-api-master` 모듈은 **서버 간(Server-to-Server)** API 호출을 처리하는 전용 서비스입니다.  
내부 시스템, 외부 연동 시스템, 게이트웨이 등을 통해 들어오는 요청을 인증, 처리, 전송하는 역할을 수행합니다.

---

## 🧭 모듈 목적 및 주요 기능

- 서버 간 API 요청 수신 및 응답 처리
- API Key 기반 인증 및 요청 검증
- 요청/응답 DTO 변환 및 매핑
- Kafka 등 메시징 시스템으로 데이터 전송
- 내부 비즈니스 서비스 호출을 위한 파사드(Facade) 역할 수행

---

## 📁 패키지 구조

| 디렉토리 | 책임 |
|----------|------|
| `controller` | 서버 간 API 엔드포인트 정의 (`@RestController`) |
| `facade` | 복잡한 서비스 호출을 단일 진입점으로 제공 |
| `service` | 핵심 비즈니스 로직 수행 |
| `mapper` | 요청/응답 DTO ↔ 내부 도메인 객체 변환 |
| `config` | 서버 to 서버 환경에 맞춘 전역 설정 (Web, Security 등) |
| `exception` | API 예외 처리 및 응답 변환 |

---

## ⚙️ 주요 클래스

- **OrderController**
    - 서버 간 주문 요청 수신 및 처리
    - `OrderFacade` 호출을 통해 비즈니스 로직 실행

- **OrderFacadeImpl**
    - Controller → Service 호출을 조율
    - DTO 변환, 서비스 호출, 메시지 전송을 한 곳에서 관리

- **OrderServiceImpl**
    - 주문 처리 로직 구현
    - Kafka 전송, DB 저장, 외부 API 호출 등의 동작 포함

- **KafkaProducerServiceImpl**
    - Kafka 토픽으로 메시지 발행
    - 재시도/에러 로깅 처리

- **OrderApiConfig**
    - 서버 to 서버 환경에 맞춘 `@Configuration` 클래스
    - Security, Logging, Format, Web 설정과 common 모듈 연동

---

## 🔐 보안 처리

- **API Key 기반 인증**
    - Gateway 헤더(`X-Internal-Auth`) + Secret 키 검증
    - 요청 IP 화이트리스트 검사 지원 (`allowed-ips`)

- **CommonSecurityAutoConfiguration**
    - common 모듈에서 기본 시큐리티 설정 제공
    - master 모듈에서는 필요한 부분만 오버라이드 가능

---

## 🔄 요청 처리 흐름

1. **Gateway 요청 수신**
    - 헤더와 IP 검증
2. **Controller 호출**
    - 요청 DTO → 내부 도메인 객체 변환
3. **Facade 처리**
    - 서비스 호출, Kafka 발행, 응답 생성
4. **응답 반환**
    - JSON 포맷, 상태 코드, 예외 처리 반영

---

## 📦 common 모듈 연계

- master 모듈은 `order-api-common` 모듈을 dependency로 추가하여 다음 자동 구성을 활용:
    - `CommonSecurityAutoConfiguration`
    - `CommonLoggingAutoConfiguration`
    - `CommonFormatAutoConfiguration`
    - `CommonWebAutoConfiguration`
- `spring.factories` 또는 `AutoConfiguration.imports`에 의해 자동 적용
- master 모듈에서는 최소한의 설정만 두고, 나머지는 common에서 관리

---

## 📄 예시 application.yml

```yaml
spring:
  application:
    name: order-api-master

api:
  infra:
    security:
      enabled: true
      gateway:
        header: X-Internal-Auth
        secret: ${INTERNAL_GATEWAY_SECRET}
    logging:
      enabled: true
    format:
      enabled: true
    web:
      enabled: true
    api-key: example-key
    allowed-ips:
      - 127.0.0.1
      - 192.168.1.0/24

logging:
  level:
    root: INFO
    org.springframework.web: DEBUG
```

---

## ✅ 요약

- **목적**: 서버 간 API 호출 전용 서비스
- **구성**: Controller → Facade → Service → Kafka
- **보안**: API Key + IP 화이트리스트
- **연계**: common 모듈의 AutoConfiguration 사용
- **확장성**: 비즈니스 로직 추가 시 Facade/Service 계층만 수정하면 됨
