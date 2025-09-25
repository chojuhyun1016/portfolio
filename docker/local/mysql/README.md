# 🐬 MySQL Local (Docker Compose)

로컬 개발/테스트용 MySQL 8.3 환경을 Docker Compose로 구성합니다.  
DB 스키마 초기화, 샘플 데이터 주입, 영구 데이터 저장까지 포함됩니다.

---

## 1) 시작하기

```bash
cd mysql
docker compose up -d
# 또는
docker-compose up -d
```

> 최초 기동 시 이미지 Pull 및 볼륨 초기화로 수십 초 소요될 수 있습니다.

---

## 2) 상태 확인

컨테이너 기동 후 Healthcheck가 `healthy` 가 될 때까지 대기하세요:

```bash
docker ps
docker logs -f mysql
```

> healthcheck 조건: `mysqladmin ping -h localhost -uroot -p$MYSQL_ROOT_PASSWORD`

---

## 3) 접속 방법

로컬 CLI 접속:
```bash
mysql -h localhost -P 3306 -uorder -porder1234 order_local
```

root 계정:
```bash
mysql -h localhost -P 3306 -uroot -proot
```

---

## 4) DB 초기화 (Init SQL)

- `./init/*.sql` 파일들은 **컨테이너 최초 실행 시** 자동 적용됩니다.
- 예시:
    - `init/01_schema.sql` → 스키마 생성
    - `init/02_seed.sql` → 샘플 데이터 삽입

재실행하려면:
```bash
docker compose down
docker volume rm -f mysql_data
docker compose up -d
```

---

## 5) 볼륨 & 데이터 유지

- 데이터는 네임드 볼륨 `mysql_data` 에 저장됩니다.
- 컨테이너 재기동(`./stop.sh && ./start.sh`) 시 데이터는 유지됩니다.
- 완전 초기화하려면 볼륨을 삭제해야 합니다:

```bash
docker compose down --volumes
# 또는
./stop.sh mysql --volumes
```

---

## 6) 환경 변수

| 변수명              | 기본값      | 설명 |
|---------------------|-------------|------|
| `MYSQL_ROOT_PASSWORD` | root        | root 계정 비밀번호 |
| `MYSQL_DATABASE`      | order_local | 기본 생성 DB 이름 |
| `MYSQL_USER`          | order       | 일반 사용자 이름 |
| `MYSQL_PASSWORD`      | order1234   | 일반 사용자 비밀번호 |
| `TZ`                  | Asia/Seoul  | 컨테이너 타임존 |

---

## 7) MySQL 설정 (my.cnf)

`./conf/my.cnf` 파일을 통해 추가 설정이 적용됩니다.  
예시:

```ini
[mysqld]
character-set-server = utf8mb4
collation-server     = utf8mb4_unicode_ci
max_connections      = 300
```

---

## 8) Compose 파일 (전체)

```yaml
services:
  mysql:
    image: mysql:8.3
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: order_local
      MYSQL_USER: order
      MYSQL_PASSWORD: order1234
      TZ: Asia/Seoul
    ports:
      - "3306:3306"
    healthcheck:
      test: [ "CMD-SHELL", "mysqladmin ping -h localhost -uroot -p$${MYSQL_ROOT_PASSWORD} || exit 1" ]
      interval: 5s
      timeout: 3s
      retries: 30
      start_period: 10s
    volumes:
      - mysql_data:/var/lib/mysql
      - ./conf/my.cnf:/etc/mysql/conf.d/my.cnf:ro
      - ./init:/docker-entrypoint-initdb.d:ro
    restart: unless-stopped

volumes:
  mysql_data:
    name: mysql_data

networks:
  default:
    name: mysql_default
    driver: bridge
```

---

## 9) 주요 특징

- MySQL 8.3 (최신 안정 버전)
- DB: `order_local`
- 사용자 계정: `order / order1234`
- Root 계정: `root / root`
- 타임존: Asia/Seoul
- 문자셋: UTF-8 (utf8mb4)
- InnoDB 스토리지 엔진
- 초기화 SQL 자동 실행
- Healthcheck 내장 (애플리케이션 의존성 보장)

---

## 10) 자주 쓰는 명령어

- 현재 컨테이너 상태:
```bash
docker compose ps
```

- 로그 확인:
```bash
docker compose logs -f mysql
```

- 특정 DB 덤프:
```bash
docker exec -i mysql mysqldump -uorder -porder1234 order_local > backup.sql
```

- 덤프 복원:
```bash
docker exec -i mysql mysql -uorder -porder1234 order_local < backup.sql
```

---

## 11) 한 줄 요약

**“MySQL 8.3 로컬 인스턴스 — 컨테이너 재기동 시 데이터 유지, 필요 시 `--volumes` 옵션으로 초기화.”**
