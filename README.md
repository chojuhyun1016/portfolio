# Portfolio Project
====

## Local 환경 구축
1. docker-compose 실행
```bash
# docker 디렉토리로 이동
$ cd docker

# docker-compose 실행 
$ docker-compose up -d

# docker-compose 종료 
$ docker-compose down
```

2. local DB 스키마 반영

> 일반적으로 **운영 외 환경(로컬/개발/베타)** 에서는 **Flyway가 자동으로 마이그레이션**을 수행하도록 구성합니다.  
> 필요 시 수동으로 적용하는 방법(아래 Option B)도 함께 제공합니다.

**Option A) 자동 (권장: 로컬/개발/베타)**
- 전제: `application-local.yml`(또는 해당 프로필)에 아래와 같이 설정되어 있어야 합니다.
  ```yaml
  spring:
    flyway:
      enabled: true
      locations: classpath:db/migration
    jpa:
      hibernate:
        ddl-auto: validate
  ```
- 애플리케이션을 실행하면 Flyway가 `order-core/src/main/resources/db/migration` 내의 `V__*.sql` 스크립트를 순서대로 적용합니다.
  ```bash
  # 예: 로컬 프로필로 애플리케이션 실행 시 자동 마이그레이션 수행
  $ ./gradlew :order-core:bootRun -Dspring.profiles.active=local
  # 또는 통합테스트/로컬 실행 등 애플리케이션 기동 시 자동 반영
  ```

**Option B) 수동 (mysql 콘솔에서 직접 실행)**
```bash
# core 모듈 하위 db 디렉토리로 이동 
$ cd ../order-core/src/main/resources/db/migration

# mysql 접속  
$ mysql -h 127.0.0.1 -P 3306 -u root -proot
```
```sql
-- mysql 콘솔에서 db 디렉토리의 스키마 파일을 순서대로 로드
MySQL> source V1_create_order_table.sql;
MySQL> source V2_add_index_order.sql;
```

3. 설계도
```bash
# image
$ open ./architecture.png
```

4. 기타
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
