# 📘 order-api-web 서비스 README (API 웹 · 구성/확장/운영 가이드)

Spring Boot 기반 **주문 단건 조회 API(Web Adapter)** 입니다.  
HTTP 엔드포인트를 통해 외부 요청을 받고, 이를  
**파사드(Facade) → 서비스(Service) → 리포지토리(Repository)** 계층으로 위임하여  
**단건 조회(Query)** 후 **표준 응답(ApiResponse)** 으로 반환합니다.

본 모듈은 **조회 전용 API** 이며,  
쓰기(Command)·이벤트 발행은 order-api-master / order-worker 계층에서 담당합니다.

전역 예외 처리, DTO 매핑(MapStruct), 공용 ObjectMapper,  
CorrelationId(MDC) 전파, REST Docs 파이프라인을 포함합니다.

본 문서는 다음 순서로 구성됩니다.

설정(Setup) → 사용(Usage) → 개발(Dev) → 확장(Extend) →  
테스트(Test) → REST Docs(Documentation) →  
트러블슈팅(Troubleshooting) → 커맨드(Cheatsheet)

--------------------------------------------------------------------------------

## 1) 전체 구조

레이어 | 주요 클래스/파일 | 핵심 역할
---|---|---
부트스트랩/조립 | OrderApiWebApplication, OrderApiWebConfig | 애플리케이션 구동, Core·Kafka 모듈 Import, 공용 ObjectMapper 제공
컨트롤러 | OrderQueryController | **POST 3종** 조회 API 제공, 요청 검증, Correlation 설정
DTO | OrderQueryRequest, OrderQueryResponse | API 요청/응답 계약 정의
파사드 | OrderQueryFacade, OrderQueryFacadeImpl | 저장소별 조회 서비스 호출 및 응답 매핑
매퍼 | OrderResponseMapper (MapStruct) | Application View → API 응답 DTO 변환
서비스 | OrderQueryService, OrderQueryServiceImpl | **저장소별 조회(MySQL/Dynamo/Redis)** 및 View 투영
공통 | KafkaProducerService(+NoOp) | (옵션) Kafka 발행 추상화
예외/웹 | WebApiExceptionHandler | 웹 모듈 전용 전역 예외 처리
MDC/Kafka | MdcToHeaderProducerInterceptor, CommonKafkaProducerAutoConfiguration | Producer 발행 시 MDC(traceId/orderId) → Kafka 헤더 자동 주입

> 의존 방향은 `adapter(api-web) → application(core) → domain` 을 **엄격히 유지**합니다.  
> API 레이어에서는 **Application DTO/View만 참조**하며,  
> 도메인 엔티티 직접 노출을 **금지**합니다.

--------------------------------------------------------------------------------

## 2) 코드 개요 (핵심 흐름)

### 2.1 부트스트랩 / 조립

- OrderApiWebApplication
    - Spring Boot 엔트리 포인트
    - OrderApiWebConfig Import

- OrderApiWebConfig
    - OrderCoreConfig 명시 Import
    - KafkaAutoConfiguration ImportAutoConfiguration
    - KafkaTopicProperties 바인딩
    - ObjectMapperFactory 기반 ObjectMapper 제공
    - Correlation/MDC/Web 설정은 order-common AutoConfiguration 사용

### 2.2 컨트롤러

- OrderQueryController
    - POST /api/v1/orders/mysql/query
    - POST /api/v1/orders/dynamo/query
    - POST /api/v1/orders/redis/query
    - @Correlate 적용
        - body → query → header 순서로 orderId 추출
        - mdcKey = orderId
        - overrideTraceId = true
    - ApiResponse.ok(...) 형태로 응답

### 2.3 파사드 / 매퍼

- OrderQueryFacade
    - 저장소별 조회 메서드 정의

- OrderQueryFacadeImpl
    - Service 호출
    - Application View → API Response DTO 변환 책임

- OrderResponseMapper (MapStruct)
    - Application View → API 응답 DTO
    - unmappedTargetPolicy = ERROR
    - TimeMapper 사용
    - API 계약 DTO는 Mapper에서만 생성

### 2.4 서비스

- OrderQueryServiceImpl
    - MySQL (JPA)
    - DynamoDB
        - Y/N 문자열 → Boolean deleteYn 변환
    - Redis
        - OrderView 우선
        - Entity fallback 허용
    - 미존재 시 CommonException(NOT_FOUND_RESOURCE) 발생

### 2.5 예외 처리 (웹 전용)

- WebApiExceptionHandler
    - basePackages = order-api-web
    - HIGHEST_PRECEDENCE
    - CommonException → ApiResponse.error
    - Unknown Exception → UNKNOWN_SERVER_ERROR

- GlobalExceptionHandler (order-api.common)
    - LOWEST_PRECEDENCE
    - Validation / Binding / Deserialization 공통 처리

--------------------------------------------------------------------------------

## 3) 설정 (Setup)

### 3.1 애플리케이션 프로퍼티

- spring.config.import 기반 분할 설정
- local 프로파일 기준
- 서버 포트: 18080
- graceful shutdown 활성화

### 3.2 로컬 환경 주요 설정 요약

- MySQL (JPA)
    - ddl-auto: none
    - open-in-view: false
    - UTC 타임존 고정

- DynamoDB
    - local 프로파일에서만 enabled
    - LocalStack endpoint 사용 가능
    - schema-reconcile 기본 DRY-RUN

- Redis
    - enabled 토글 제공
    - OrderCacheService 연계

- Kafka
    - Producer / Consumer 비활성 가능
    - Topic ensure-at-startup 옵션
    - ProducerInterceptor로 MDC 자동 주입

- Crypto / Secrets / TSID
    - 모두 optional
    - 공통 모듈 AutoConfiguration에 의해 제어

--------------------------------------------------------------------------------

## 4) Gradle 구성 (REST Docs + MapStruct + Querydsl)

- REST Docs 파이프라인 포함
- MapStruct + Lombok MapStruct Binding
- Querydsl (Jakarta)
- bootJar 시 Asciidoctor 결과물 포함

주의 사항:
- annotationProcessor 설정 필수
- JDK 버전과 IDE 설정 확인 필요

--------------------------------------------------------------------------------

## 5) 사용 (Usage)

### 5.1 HTTP 요청 / 응답

엔드포인트 (POST, application/json):

- /api/v1/orders/mysql/query
- /api/v1/orders/dynamo/query
- /api/v1/orders/redis/query

요청 예시:

{ "orderId": 5555 }

성공 응답 (ApiResponse):

- metadata
    - code
    - msg
    - timestamp

- data
    - orderId
    - orderNumber
    - userId
    - deleteYn (Boolean)
    - version
    - publishedTimestamp
    - failure

> DynamoDB 원천 deleteYn 값이 "Y"/"N" 인 경우  
> API 응답에서는 Boolean으로 정규화되어 반환됩니다.

--------------------------------------------------------------------------------

## 6) 개발 (Dev)

### 6.1 DTO / View 매핑 정책

- API Response DTO는 **계약 객체**
- Application View는 **내부 전용**
- Entity → API DTO 직접 매핑 금지

### 6.2 서비스 / 예외 규칙

- 리소스 미존재
    - CommonException(NOT_FOUND_RESOURCE)

- API 레이어에서 RuntimeException 직접 throw 금지

### 6.3 ObjectMapper

- @ConditionalOnMissingBean 기반 제공
- 서비스별 커스터마이징 가능

### 6.4 MapStruct

- annotationProcessor 필수
- lombok-mapstruct-binding 포함
- unmappedTargetPolicy = ERROR 유지

--------------------------------------------------------------------------------

## 7) 확장 (Extend)

### 7.1 API 엔드포인트 추가 절차

1) Controller 메서드 추가
2) Facade 인터페이스/구현 확장
3) Service 로직 구현
4) Mapper 및 Response DTO 확장

### 7.2 Kafka 연계 (옵션)

- Web 모듈에는 Producer 골격만 존재
- 실제 이벤트 발행은 Worker/Master 담당
- MDC(traceId/orderId)는 ProducerInterceptor로 자동 주입

--------------------------------------------------------------------------------

## 8) 테스트 (Test)

### 8.1 권장 테스트 전략

- Controller Slice Test
    - WebMvcTest
    - addFilters = false
    - Facade Mock 처리

- Service 단위 테스트
    - Repository Mock
    - 저장소별 분기 검증

### 8.2 주의 사항

- SpringBootTest 전체 로딩 지양
- Redis / Security / Kafka 오토컨피그 필요 시 명시적 exclude 권장

--------------------------------------------------------------------------------

## 9) REST Docs (Documentation)

- REST Docs 전용 테스트 실행
- generated-snippets 생성
- Asciidoctor 변환
- bootJar 시 static/docs 포함

자주 발생하는 문제:

- 스니펫 미생성
- 보안 필터 미제외
- 응답 스키마 불일치

--------------------------------------------------------------------------------

## 10) 트러블슈팅 (Troubleshooting)

증상 | 원인 | 해결책
---|---|---
MapStruct 인식 오류 | annotationProcessor 누락 | MapStruct + lombok-mapstruct-binding 설정 확인
DynamoDB 연결 실패 | LocalStack 미기동 | endpoint, enabled 설정 확인
Redis 접속 오류 | Redis 미기동/비번 오류 | redis 설정 확인
403 오류 | Security 필터 활성 | 테스트 시 필터 비활성화
JPA 주입 실패 | DB 미설정 | 슬라이스 테스트 사용

--------------------------------------------------------------------------------

## 11) 커맨드 모음 (Command Cheatsheet)

명령 | 설명
---|---
./gradlew clean build | 전체 빌드
./gradlew :order-api:order-api-web:test | 테스트 실행
./gradlew :order-api:order-api-web:rest | REST Docs 전용 테스트
./gradlew :order-api:order-api-web:asciidoctor | 문서 생성
./gradlew :order-api:order-api-web:bootJar | 문서 포함 JAR 생성

--------------------------------------------------------------------------------

## 12) 한 줄 요약

**MySQL / DynamoDB / Redis 기반 주문 단건 조회 API**를 제공하는  
조회 전용 Web Adapter 모듈입니다.  
표준화된 계층 분리와 공통 AutoConfiguration을 통해  
확장성과 추적성(traceId/orderId)을 동시에 보장합니다.
