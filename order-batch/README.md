# 🧰 order-batch (Spring Batch Execution Module)

`order-batch` 는 **실행 후 종료되는 단발성 Spring Batch 모듈**로,  
운영 중 적재된 **Kafka DLQ(Dead Letter) 메시지를 재처리**하고,  
**Secrets 기반 Crypto 키 시딩**, **S3 로그 동기화(시작/종료 훅)**,  
**잡 상태 → 프로세스 종료코드 매핑**까지 포함하는 **운영 전용 배치 레이어**입니다.

---

## ✅ 역할과 정의

- **역할**
    - Kafka DLQ 메시지 재처리 및 재발행(Local / Api / Crud)
    - 재시도 한계 초과 시 ALARM/Discard 처리
    - 실행 결과를 OS 종료코드로 명확히 전달 (성공=0, 실패=1)

- **정의**
    - Web 서버 없이 실행 (`WebApplicationType.NONE`)
    - 잡 수행 후 즉시 종료
    - 운영 자동화(CI/CD, 스케줄러)와 직접 연계 가능한 배치

---

## 🧭 핵심 책임

- **Batch Job**
    - `ORDER_DEAD_LETTER_JOB`
    - DLQ 토픽을 직접 consume → 타입별 재처리
    - 파티션 단위 안전 커밋(offset + 1)

- **DLQ 재처리 정책**
    - 메시지 타입별 최대 재시도 횟수 관리
    - 재시도 증가 전 기준으로 임계치 판단(off-by-one 방지)
    - 초과 시 ALARM 토픽으로 폐기

- **Secrets / Crypto**
    - `app.crypto.keys` 기반 키 선택 및 시딩
    - AES128 / AES256 / AESGCM / HMAC_SHA256 지원
    - Secrets 갱신 시 자동 최신 승격 금지(운영 승인 필요)

- **S3 로그 동기화**
    - 시작/종료 시 로그 업로드
    - MD5 ↔ ETag 비교로 중복 업로드 방지
    - HOSTNAME 기반 인스턴스 로그 분리

---

## 🗂️ 구조 요약

- **Bootstrap**
    - `OrderBatchApplication` (UTC timezone, exitCode 반환)
    - `OrderBatchConfig` (Core/Web/TSID + S3/Kafka/Cache/Application AutoConfig)

- **Batch**
    - `OrderDeadLetterJobConfig`
    - `OrderDeadLetterFacade` / `OrderDeadLetterService`

- **Kafka**
    - DLQ 전용 `ConsumerFactory`
    - `KafkaTopicProperties` 기반 토픽 이름 주입

- **Lifecycle**
    - Startup / Shutdown Handler (S3, Secrets 정리)

- **Exit Code**
    - Batch Status → OS Exit Code 매핑

---

## 🔑 한 줄 요약

**order-batch는 DLQ 재처리, Secrets/Crypto 키 시딩, S3 로그 업로드, 종료코드 제어까지 포함한  
운영 자동화를 위한 단발성 Spring Batch 실행 모듈입니다.**
