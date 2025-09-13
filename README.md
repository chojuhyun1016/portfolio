# Portfolio Project
====

## Local 환경 구축

1) **docker-compose 실행**
```bash
# docker 디렉토리로 이동
cd docker

# docker-compose 실행 
docker compose up -d

# docker-compose 종료 
docker compose down
```

2) **Local DB 스키마 반영 (Flyway 권장)**

> 일반적으로 **운영 외 환경(로컬/개발/베타)** 에서는 **Flyway가 자동으로 마이그레이션**을 수행하도록 구성합니다.  
> 필요 시 수동으로 적용하는 방법(아래 Option B)도 함께 제공합니다.

**Option A) 자동 (권장: 로컬/개발/베타)**  
`application-local.yml`(또는 해당 프로필)에 다음 설정이 있어야 합니다.
```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
  jpa:
    hibernate:
      ddl-auto: validate
```
애플리케이션 기동 시 Flyway가 `order-core/src/main/resources/db/migration` 내 `V__*.sql` 을 순서대로 적용합니다.
```bash
# 예: 로컬 프로필로 애플리케이션 실행 시 자동 마이그레이션 수행
./gradlew :order-core:bootRun -Dspring.profiles.active=local
```

**Option B) 수동 (mysql 콘솔에서 직접 실행)**
```bash
# core 모듈 하위 db 디렉토리로 이동 
cd ../order-core/src/main/resources/db/migration

# mysql 접속  
mysql -h 127.0.0.1 -P 3306 -u root -proot
```
```sql
-- mysql 콘솔에서 db 디렉토리의 스키마 파일을 순서대로 로드
source V1_create_order_table.sql;
source V2_add_index_order.sql;
```

3) **설계도**
```bash
# image
open ./architecture.png
```

4) **기타 docker 유틸 커맨드**
```bash
# 백그라운드 실행
docker compose up -d

# 로그 보기 (전체/특정 서비스)
docker compose logs -f
docker compose logs -f redis

# 상태 확인 / 종료
docker compose ps
docker compose down          # 컨테이너만 제거
docker compose down -v       # + 볼륨(데이터)도 제거 (주의!)
docker compose restart redis # 특정 서비스만 재시작

# 쉘 진입
docker compose exec redis sh
```

---

## 도커 컴포즈 구성 (참고)

현재 `docker/docker-compose.yml` 은 다음 서비스를 기동합니다.

- `portfolio-db` (MySQL 8.3.0) – 포트 3306, 루트 비밀번호 `root`
- `localstack` (DynamoDB) – 포트 4566, 로컬 데이터 `.localstack/`
- `redis` (7.0.5) – 포트 6379, AOF 활성화

헬스체크가 설정되어 있어 컨테이너 준비 후 애플리케이션을 기동하면 안정적입니다.

---

## 빌드/테스트

### 기본
```bash
# 전체 빌드(단위 테스트 포함)
./gradlew clean build
```

### 통합 테스트
```bash
# 통합 테스트 태스크가 구성된 모듈에서 실행(예: order-worker)
./gradlew :order-worker:integrationTest
```

### 자주 쓰는 테스트 팁
- 보안/Redis 등 외부 인프라가 필요한 오토컨피그는 **테스트 컨텍스트에서만 제외**하세요.  
  예: `@SpringBootTest` + `@TestPropertySource(properties = "spring.autoconfigure.exclude=...RedisAutoConfiguration,...SecurityAutoConfiguration,...")`
- **Embedded Kafka** 테스트 시, **수동 Producer/Consumer** 를 생성해 라운드트립 검증을 권장합니다.  
  (테스트 스코프에서만 `spring-kafka-test` 사용)

---

## Gradle 버전 카탈로그(요약)

- Java 17, Spring Boot 3.2.x
- Kafka 관련
  - 런타임: `org.springframework.kafka:spring-kafka`
  - 테스트: `org.springframework.kafka:spring-kafka-test` (테스트 스코프)
- 공통 모듈 의존 예시
  - `order-common` → 비즈니스 무관한 공통 코드 제공
  - `order-api-common` → 오토컨피그 모듈로, `spring-boot-autoconfigure` 포함

---

## 트러블슈팅(흔한 오류와 원인/해결)

증상
- DB 마이그레이션 오류

원인
- Flyway 설정 누락 또는 SQL 스크립트 버전 충돌

해결
- `spring.flyway.*` 프로퍼티 확인
- SQL 스크립트 파일명(`V1__`, `V2__`) 버전 순서 점검

증상
- 테스트에서 Redis/보안 관련 예외 발생

원인
- 테스트 컨텍스트가 불필요한 오토컨피그를 포함

해결
- `spring.autoconfigure.exclude` 프로퍼티로 제외
- 또는 슬라이스 테스트 사용(`@WebMvcTest`, `@DataJpaTest` 등)

---

## 한 줄 요약

도커 컴포즈로 인프라를 손쉽게 띄우고, Flyway로 스키마를 관리하며, Gradle 태스크로 빌드/문서를 자동화할 수 있는 **멀티 모듈 프로젝트**입니다.
