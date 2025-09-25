# 🔴 Redis Local (Docker Compose)

로컬 개발/테스트용 Redis 환경을 Docker Compose로 구성합니다.  
비밀번호 설정, 영구 데이터 저장, Spring/Redisson 연동까지 포함됩니다.

---

## 📂 프로젝트 구조

```
redis/
├─ .env                        # 환경 변수 (버전/포트/비밀번호)
├─ docker-compose.yml          # Compose 설정
├─ conf/
│  └─ redis.conf               # Redis 설정 파일
├─ data/                       # 영구 데이터 저장소 (네임드 볼륨)
└─ scripts/
   └─ redis-cli.sh             # redis-cli 진입 스크립트 (옵션)
```

---

## 🚀 빠른 시작

```bash
cd redis
docker compose up -d
docker compose ps
```

Health 상태 확인:
```bash
docker compose exec redis redis-cli ping
# -> PONG
```

---

## 🔐 비밀번호 설정 (선택)

1) `.env` 파일 수정:
```env
REDIS_PASSWORD=changeme
```

2) 컨테이너 재생성:
```bash
docker compose down
docker compose up -d
```

3) 접속:
```bash
docker compose exec redis redis-cli -a changeme ping
# -> PONG
```

---

## ⚙️ 환경 변수

| 변수명            | 기본값     | 설명 |
|------------------|------------|------|
| `REDIS_VERSION`  | `7.2`      | Redis 버전 |
| `REDIS_PORT`     | `6379`     | 호스트 포트 |
| `REDIS_PASSWORD` | *(비워둠)* | 비밀번호 (미설정 시 없음) |
| `TZ`             | `Asia/Seoul` | 타임존 |

---

## 📡 Spring / Redisson 연동 예시

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    database: 0
    # password: changeme   # 설정 시 활성화

lock:
  redisson:
    enabled: true
    address: redis://${spring.redis.host}:${spring.redis.port}
    database: 10
    wait-time: 3000      # ms
    lease-time: 10000    # ms
    retry-interval: 150  # ms
```

---

## 🧹 데이터 초기화

- 데이터만 초기화:
```bash
docker compose exec redis redis-cli FLUSHALL
```

- 컨테이너/네트워크 유지 + 데이터 삭제:
```bash
docker compose down -v
```

---

## 📝 Compose 파일 (전체)

```yaml
services:
  redis:
    image: redis:${REDIS_VERSION:-7.2}
    restart: unless-stopped
    ports:
      - "${REDIS_PORT:-6379}:6379"
    volumes:
      - redis_data:/data
      - ./conf/redis.conf:/usr/local/etc/redis/redis.conf:ro
    command: >
      sh -c '
      if [ -n "$${REDIS_PASSWORD}" ]; then
        exec redis-server /usr/local/etc/redis/redis.conf --requirepass "$${REDIS_PASSWORD}";
      else
        exec redis-server /usr/local/etc/redis/redis.conf;
      fi
      '
    healthcheck:
      test: [ "CMD-SHELL", "if [ -n \"$${REDIS_PASSWORD}\" ]; then redis-cli -a \"$${REDIS_PASSWORD}\" ping; else redis-cli ping; fi" ]
      interval: 10s
      timeout: 3s
      retries: 5
      start_period: 5s
    environment:
      - TZ=Asia/Seoul
      - REDIS_PASSWORD=${REDIS_PASSWORD:-}

volumes:
  redis_data:
    name: redis_data

networks:
  default:
    name: redis_default
    driver: bridge
```

---

## ✅ 주요 특징

- Redis 7.2 (최신 안정 버전)
- 영구 데이터 저장 (네임드 볼륨 `redis_data`)
- 비밀번호 옵션 (`REDIS_PASSWORD`) 지원
- Healthcheck 내장 (`redis-cli ping`)
- Spring Boot/Redisson 연동 예시 제공

---

## 🔄 자주 쓰는 명령어

- 현재 상태:
```bash
docker compose ps
```

- 로그 확인:
```bash
docker compose logs -f redis
```

- CLI 접속:
```bash
docker compose exec redis redis-cli
```

- 비밀번호 있는 CLI 접속:
```bash
docker compose exec redis redis-cli -a changeme
```

- 모든 키 확인:
```bash
docker compose exec redis redis-cli keys '*'
```

---

## ⚡ 한 줄 요약

**“Redis 7.2 로컬 인스턴스 — 비밀번호 옵션과 데이터 영속성을 지원하며, Spring/Redisson과 즉시 연동 가능.”**
