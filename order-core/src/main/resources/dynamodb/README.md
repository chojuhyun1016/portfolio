# DynamoDB Migration & Seed (Local Only)

로컬(LocalStack)에서 애플리케이션 기동 시 **최신 버전(Vn) 하나만** 자동 적용하여 DynamoDB 테이블을 생성하고, 필요 시 시드 데이터를 넣습니다.  
운영/개발 서버에서는 동작하지 않습니다. (조건: `spring.profiles.active=local` + `dynamodb.enabled=true` + `dynamodb.auto-create=true`)

> ⚠️ JSON 표준은 **주석을 허용하지 않습니다.** 아래 예시 JSON에는 주석을 넣지 않았습니다.  
> ⚠️ 본 README는 **order-core 모듈**에 위치한 리소스를 기준으로 합니다.

---

## 1) 디렉터리 구조 (order-core 모듈)

아래 경로에 마이그레이션/시드 JSON 파일을 둡니다. 최신 버전(Vn)만 적용됩니다.

```text
order-core/
└─ src/main/resources/dynamodb/
   ├─ migration/      # 스키마 정의(JSON). 같은 버전(Vn)의 여러 파일을 병합 처리
   │  ├─ V1__init.json
   │  └─ V2__order_dynamo_schema.json
   └─ seed/           # 시드 데이터(JSON). 같은 버전(Vn)의 여러 파일을 모두 적용
      ├─ V1__order_seed.json
      └─ V2__order_seed.json
```

> ✅ 마이그/시드 리소스는 **order-core**에 두세요. 초기화 로더가 order-core의 classpath에서 읽습니다.

---

## 2) 마이그레이션 JSON 포맷(스키마)

- `attributes`에는 **테이블 키 + 모든 GSI/LSI 키 속성**이 포함되어야 합니다.
- LSI를 사용하려면 **테이블 `rangeKey`가 필수**입니다.
- `projection`: `ALL` / `KEYS_ONLY` / `INCLUDE` (`INCLUDE`는 `nonKeyAttributes` 필요)
- 타입: `S`(String), `N`(Number), `B`(Binary)

```json
{
  "tables": [
    {
      "name": "tableName",
      "hashKey": "pk",
      "rangeKey": "sk",
      "readCapacity": 5,
      "writeCapacity": 5,
      "attributes": [
        { "name": "pk", "type": "S" },
        { "name": "sk", "type": "S" }
      ],
      "globalSecondaryIndexes": [
        {
          "name": "gsi_name",
          "hashKey": "gsi_pk",
          "rangeKey": "gsi_sk",
          "projection": "ALL",
          "readCapacity": 5,
          "writeCapacity": 5
        }
      ],
      "localSecondaryIndexes": [
        {
          "name": "lsi_name",
          "rangeKey": "lsi_sk",
          "projection": "KEYS_ONLY"
        }
      ]
    }
  ]
}
```

---

## 3) 시드 JSON 포맷(데이터)

```json
{
  "table": "tableName",
  "items": [
    { "pk": "ID-1", "sk": "A", "attr": "value" }
  ]
}
```

- `items` 배열의 각 항목이 `PutItem`으로 삽입됩니다.
- 값 타입은 문자열/숫자/불리언/배열/객체를 지원합니다.

---

## 4) 예시 파일 (테스트용)

### 4.1 V1 — 단순 스키마/시드

**`order-core/src/main/resources/dynamodb/migration/V1__init.json`**
```json
{
  "tables": [
    {
      "name": "order_dynamo",
      "hashKey": "id",
      "readCapacity": 5,
      "writeCapacity": 5,
      "attributes": [
        { "name": "id", "type": "S" },
        { "name": "orderId", "type": "N" },
        { "name": "userId", "type": "N" },
        { "name": "orderPrice", "type": "N" }
      ]
    }
  ]
}
```

**`order-core/src/main/resources/dynamodb/seed/V1__order_seed.json`**
```json
{
  "table": "order_dynamo",
  "items": [
    {
      "id": "ORD-V1-0001",
      "orderId": 1,
      "userId": 101,
      "orderPrice": 35000
    },
    {
      "id": "ORD-V1-0002",
      "orderId": 2,
      "userId": 102,
      "orderPrice": 42000
    }
  ]
}
```

---

### 4.2 V2 — 실제 엔티티 컬럼 + GSI/LSI (최신 버전 예시)

**`order-core/src/main/resources/dynamodb/migration/V2__order_dynamo_schema.json`**
```json
{
  "tables": [
    {
      "name": "order_dynamo",
      "hashKey": "id",
      "rangeKey": "orderNumber",
      "readCapacity": 5,
      "writeCapacity": 5,
      "attributes": [
        { "name": "id", "type": "S" },
        { "name": "orderId", "type": "N" },
        { "name": "orderNumber", "type": "S" },
        { "name": "userId", "type": "N" },
        { "name": "userNumber", "type": "S" },
        { "name": "orderPrice", "type": "N" },
        { "name": "deleteYn", "type": "S" },
        { "name": "createdUserId", "type": "N" },
        { "name": "createdUserType", "type": "S" },
        { "name": "createdDatetime", "type": "S" },
        { "name": "modifiedUserId", "type": "N" },
        { "name": "modifiedUserType", "type": "S" },
        { "name": "modifiedDatetime", "type": "S" },
        { "name": "publishedTimestamp", "type": "N" }
      ],
      "globalSecondaryIndexes": [
        {
          "name": "gsi_user_id",
          "hashKey": "userId",
          "projection": "ALL",
          "readCapacity": 5,
          "writeCapacity": 5
        },
        {
          "name": "gsi_user_number_order_price",
          "hashKey": "userNumber",
          "rangeKey": "orderPrice",
          "projection": "INCLUDE",
          "nonKeyAttributes": ["orderNumber", "deleteYn"],
          "readCapacity": 5,
          "writeCapacity": 5
        }
      ],
      "localSecondaryIndexes": [
        {
          "name": "lsi_order_price",
          "rangeKey": "orderPrice",
          "projection": "KEYS_ONLY"
        }
      ]
    }
  ]
}
```

**`order-core/src/main/resources/dynamodb/seed/V2__order_seed.json`**
```json
{
  "table": "order_dynamo",
  "items": [
    {
      "id": "ORD-202409-0001",
      "orderId": 1001,
      "orderNumber": "2024-09-0001",
      "userId": 90001,
      "userNumber": "U-00001",
      "orderPrice": 99000,
      "deleteYn": "N",
      "createdUserId": 90001,
      "createdUserType": "USER",
      "createdDatetime": "2024-09-01T10:00:00",
      "modifiedUserId": 90001,
      "modifiedUserType": "USER",
      "modifiedDatetime": "2024-09-01T10:00:00",
      "publishedTimestamp": 1725162000000
    },
    {
      "id": "ORD-202409-0002",
      "orderId": 1002,
      "orderNumber": "2024-09-0002",
      "userId": 90002,
      "userNumber": "U-00002",
      "orderPrice": 120000,
      "deleteYn": "N",
      "createdUserId": 90002,
      "createdUserType": "USER",
      "createdDatetime": "2024-09-02T11:30:00",
      "modifiedUserId": 90002,
      "modifiedUserType": "USER",
      "modifiedDatetime": "2024-09-02T11:30:00",
      "publishedTimestamp": 1725246600000
    }
  ]
}
```

> ℹ️ V1과 V2가 동시에 존재하면 **V2만 적용**됩니다. 최신 버전만 유지하거나, 이전 버전을 잠시 남길 때도 최신만 반영된다는 점에 유의하세요.

---

## 5) 스프링 설정 (local 프로필)

`application-local.yml` (혹은 `dynamodb.yml` 병합) 예시:

```yaml
dynamodb:
  enabled: true
  endpoint: "http://localhost:4566"
  region: "ap-northeast-2"
  access-key: "local"
  secret-key: "local"
  table-name: "order_dynamo"
  auto-create: true
  migration-location: classpath:dynamodb/migration
  seed-location: classpath:dynamodb/seed
```

- 프로필: `local`
- 위 속성들 중 `enabled=true`, `auto-create=true`일 때만 자동 생성/시드가 동작합니다.
- `DynamoDbClient` 빈은 이미 인프라 설정에서 생성되어 있어야 합니다.

---

## 6) 실행 절차

### 6.1 LocalStack 기동
```bash
docker compose -f docker/local/aws/docker-compose.yml up -d
```

### 6.2 애플리케이션(local) 기동
- 모듈 의존: **order-worker** → **order-core** (order-core에 리소스/오토컨피그/초기화 로직 존재)
- `bootRun` 태스크가 없다면, `application` 플러그인의 `run` 태스크 또는 IDE에서 main 실행을 사용하세요.

Gradle 예시:
```bash
./gradlew :order-worker:clean :order-worker:run -Dspring.profiles.active=local
```

Fat JAR 실행 예시:
```bash
java -jar order-worker/build/libs/order-worker-*.jar --spring.profiles.active=local
```

---

## 7) 생성/적용 확인 (CLI)

AWS CLI(awslocal 또는 표준 aws + endpoint 지정):

```bash
# 테이블 목록
awslocal dynamodb list-tables
# 또는
aws dynamodb list-tables --endpoint-url http://localhost:4566 --region ap-northeast-2

# 테이블 상세 (GSI/LSI 포함 확인)
aws dynamodb describe-table \
  --table-name order_dynamo \
  --endpoint-url http://localhost:4566 \
  --region ap-northeast-2

# 데이터 조회(Scan)
aws dynamodb scan \
  --table-name order_dynamo \
  --endpoint-url http://localhost:4566 \
  --region ap-northeast-2
```

---

## 8) 브라우저 GUI (선택)

DynamoDB Admin 컨테이너 실행 후 브라우저에서 조회:

```bash
docker run --rm -it \
  -e DYNAMO_ENDPOINT=http://host.docker.internal:4566 \
  -e AWS_REGION=ap-northeast-2 \
  -e AWS_ACCESS_KEY_ID=local \
  -e AWS_SECRET_ACCESS_KEY=local \
  -p 8001:8001 \
  aaronshaf/dynamodb-admin
# → http://localhost:8001 접속
```

> macOS의 Docker Desktop에서는 `host.docker.internal`이 호스트를 가리킵니다.

---

## 9) 트러블슈팅 체크리스트

- 테이블이 안 만들어짐
    - `spring.profiles.active=local`인지 확인
    - `dynamodb.enabled=true`, `dynamodb.auto-create=true`인지 확인
    - **리소스 경로**가 `order-core/src/main/resources/dynamodb/...` 인지 확인 (order-worker가 아닌 order-core)
    - **JSON에 주석이 없는지** 확인 (오류: `JSON standard does not allow comments`)
    - LSI 정의 시 **테이블 rangeKey 필수**, 모든 인덱스 키가 `attributes`에 포함되어야 함
    - LocalStack 4566 포트/헬스 상태 확인

- Gradle로 실행이 안 됨 (`bootRun` 없음)
    - 해당 모듈에 Spring Boot 플러그인이 없으면 `bootRun` 태스크가 없습니다.
    - `:order-worker:run` 또는 IDE 실행/패키지된 JAR 실행을 사용하세요.

- DynamoDB Admin 접속 불가
    - `DYNAMO_ENDPOINT` 값/포트 매핑 확인
    - 다른 프로세스가 8001 포트를 사용 중인지 확인

---

## 10) 요약

- 리소스 파일 위치: **order-core/src/main/resources/dynamodb/**
- 최신 버전(Vn) 1개만 적용 (동일 버전 파일은 병합)
- JSON은 **주석 금지**
- V2 예시에는 GSI/LSI가 포함되어 있으며, 해당 키 속성은 `attributes`에 모두 선언됨
- LocalStack + local 프로필에서만 자동 생성/시드
