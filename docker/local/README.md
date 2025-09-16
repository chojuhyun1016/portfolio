# 🚀 Local Infra Control — `start.sh` & `stop.sh`

> AWS(LocalStack) · Kafka · MySQL · Redis를 **한 번에** 또는 **선택적으로** 실행/중지하는 통합 스크립트 세트

---

## 1) 실행 준비 (권한 설정)

가장 먼저 스크립트 파일에 실행 권한을 부여해야 합니다:

```bash
chmod 755 start.sh
chmod 755 stop.sh
```

---

## 2) 필요 조건

- Docker Desktop / Docker Engine
- Docker Compose v2
    - 자동 감지: `docker compose` 우선, 없으면 `docker-compose` 사용

---

## 3) 빠른 시작

### 전체 서비스 기동
```bash
./start.sh
```

### 전체 서비스 종료
```bash
./stop.sh
```

> 기본 대상 서비스: `aws kafka mysql redis` (docker-compose.yml의 서비스명과 일치)

---

## 4) 선택 기동/종료

### 특정 서비스만 기동 (예: kafka, redis)
```bash
./start.sh kafka redis
```

### 특정 서비스만 종료 (예: mysql만)
```bash
./stop.sh mysql
```

---

## 5) `start.sh` 옵션

- `--recreate` : **정지+삭제 후 재기동** (기본값, 안전)
- `--no-recreate` : 기존 컨테이너 유지(빠름)
- `--build` : 이미지 빌드 포함
- `-p, --project <NAME>` : compose 프로젝트명 지정
- 인자 미지정 시: `aws kafka mysql redis` 전부 기동

### 예시

#### 이미지 빌드 포함, 전체 재기동
```bash
./start.sh --recreate --build
```

#### 재생성 없이 빠른 기동
```bash
./start.sh --no-recreate
```

#### 선택 + 빌드
```bash
./start.sh --build aws mysql
```

---

## 6) `stop.sh` 옵션

- `--volumes` : **네임드 볼륨까지 삭제** (데이터 초기화 — 주의)
- `-p, --project <NAME>` : compose 프로젝트명 지정
- 인자 미지정 시: `aws kafka mysql redis` 전부 종료
    - **전부**가 선택되고 `--volumes`를 쓰면 내부적으로 `docker compose down --volumes --remove-orphans` 수행

### 예시

#### 전체 종료 + 데이터 초기화
```bash
./stop.sh --volumes
```

#### 선택 종료 (네임드 볼륨 유지)
```bash
./stop.sh kafka
```

---

## 7) 헬스체크(Healthcheck)

`start.sh`는 기동 직후 각 서비스의 Health 상태를 **최대 180초** 대기하며 확인합니다.

- Healthcheck가 정의된 서비스 → `healthy` 확인
- Healthcheck가 **없으면** → 즉시 Ready 처리(스킵)

현재 상태 보기:
```bash
docker compose ps
```

실시간 로그:
```bash
docker compose logs -f <service>
```

---

## 8) 트러블슈팅

### “Unknown service …”
서비스명이 compose 파일과 다릅니다. 목록 확인:
```bash
docker compose config --services
```

### 포트 충돌
해당 포트를 다른 프로세스가 사용 중입니다. 점유 프로세스를 종료하거나 compose 포트를 변경하세요.

### 컨테이너는 떴는데 연결이 안 됨
- 방화벽/보안 제품 차단 여부 확인
- `docker compose logs -f <service>` 로 초기화/마이그레이션 에러 확인
- MySQL은 초기화에 수십 초가 걸릴 수 있음 (볼륨 최초 생성 시)

---

## 9) 자주 쓰는 명령 모음

- 전체 상태:
```bash
docker compose ps
```

- 특정 서비스 로그 팔로우:
```bash
docker compose logs -f mysql
```

- 특정 서비스만 재시작:
```bash
./stop.sh mysql
./start.sh mysql
```

- 모든 리소스 정리(컨테이너/네트워크/볼륨):
```bash
./stop.sh --volumes
```

---

## 10) 서비스 이름 표

| 논리 이름 | Compose 서비스명 | 비고 |
|---|---|---|
| AWS(LocalStack) | `aws` | S3/SecretsManager/DynamoDB 등 |
| Kafka | `kafka` | External: `localhost:29092` |
| MySQL | `mysql` | External: `localhost:3306` |
| Redis | `redis` | External: `localhost:6379` |

---

## 11) 권장 워크플로우

1) **권한 설정** (최초 1회)
```bash
chmod 755 start.sh stop.sh
```

2) 한 번에 모두 기동
```bash
./start.sh
```

3) 애플리케이션 기동 & 연결 확인
```bash
docker compose ps
docker compose logs -f mysql
```

4) 특정 서비스만 수정/재기동
```bash
./stop.sh mysql
./start.sh mysql
```

5) 전체 종료(데이터 유지) 또는 초기화 종료
```bash
./stop.sh
./stop.sh --volumes
```

---

## 12) 한 줄 요약

**“`start.sh`로 서비스 기동, `stop.sh`로 종료 — 가장 먼저 `chmod 755`로 권한부터 설정하세요!”**
