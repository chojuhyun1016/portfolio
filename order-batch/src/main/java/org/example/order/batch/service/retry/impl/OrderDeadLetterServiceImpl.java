package org.example.order.batch.service.retry.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.batch.service.common.KafkaProducerService;
import org.example.order.batch.service.retry.OrderDeadLetterService;
import org.example.order.common.support.json.ObjectMapperUtils;
import org.example.order.contract.order.messaging.dlq.DeadLetter;
import org.example.order.contract.order.messaging.event.OrderApiMessage;
import org.example.order.contract.order.messaging.event.OrderCrudMessage;
import org.example.order.contract.order.messaging.event.OrderLocalMessage;
import org.example.order.contract.shared.error.ErrorDetail;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * OrderDeadLetterServiceImpl
 * ------------------------------------------------------------------------
 * 목적
 * - DLQ(Dead-Letter Queue)에 적재된 주문 메시지를 유형별로 재처리한다.
 * - 재시도 임계치 정책에 따라 "재전송" 또는 "폐기(알람 토픽)"를 결정한다.
 * <p>
 * 핵심 동작 (타입별 동일 로직)
 * 1) 현재 재시도 횟수(current)를 계산한다.
 * - error.meta / Kafka headers 의 후보 키에서 정수값을 읽어 최대값을 사용한다.
 * - 우선 키: retryCount(PRIMARY_RETRY_KEY), 그 외 failedCount, attempts, x-retry-count 등 유연 인식.
 * 2) 임계치 비교는 "증가 전 값(current)"으로 수행한다.
 * - current >= MAX 일 때: 폐기(ORDER_ALARM 토픽) 전송.
 * - current <  MAX 일 때: 재전송하며 그때만 +1(next=current+1)로 메타/헤더 값을 갱신한다.
 * (이전 대비 보정: +1 후 비교로 인해 경계값에서 즉시 폐기되던 off-by-one 문제를 제거)
 * 3) 타입별 전송 토픽
 * - ORDER_LOCAL  -> sendToLocal
 * - ORDER_API    -> sendToOrderApi
 * - ORDER_CRUD   -> sendToOrderCrud
 * - 폐기(ALARM)  -> sendToDiscard (주제: ORDER_ALARM)
 * <p>
 * 설계 포인트
 * - payload(Object)는 Map/JsonNode 등으로 들어올 수 있으므로 convertValue 기반의 안전 캐스팅 적용.
 * - 재시도 카운트는 메타/헤더 모두에 동기화하여 운영 가시성 및 후속 시스템 일관성 보장.
 * - 예외/누락/비정상 값에 견고하게 대응(빈 값, 숫자 아님, 대소문자/키 변형 등).
 * - Correlate AOP로 orderId/traceId를 MDC에 일원화하여 로그 추적성 강화.
 * <p>
 * 튜닝 지점
 * - 타입별 임계치(LOCAL/API/CRUD) 상수 정의로 간단하게 조정 가능.
 * - 재시도/폐기 로깅은 운영 정책에 맞춰 레벨 및 메시지 포맷 조정 가능.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderDeadLetterServiceImpl implements OrderDeadLetterService {

    private final KafkaProducerService kafkaProducerService;

    /**
     * 타입별 하드코딩 임계치
     */
    private static final int LOCAL_MAX_RETRY = 5;
    private static final int API_MAX_RETRY = 3;
    private static final int CRUD_MAX_RETRY = 5;

    /**
     * 실패 카운트 키(기록 우선 키)
     */
    private static final String PRIMARY_RETRY_KEY = "retryCount";
    private static final String[] RETRY_HEADER_KEYS = new String[]{
            "x-retry-count", "retry-count", "x_delivery_attempts", "deliveryAttempts"
    };

    @Override
    public void retryLocal(Object dlqObj, Map<String, String> headers) {
        DeadLetter<?> dlqRaw = (DeadLetter<?>) dlqObj;
        DeadLetter<OrderLocalMessage> dlq = castPayload(dlqRaw, OrderLocalMessage.class);

        if (dlq == null || dlq.payload() == null) {
            log.warn("skip: empty DLQ payload (ORDER_LOCAL)");

            return;
        }

        int current = resolveCurrentRetryCount(dlq.error(), headers);

        if (current >= LOCAL_MAX_RETRY) {
            log.warn("DLQ discard: retryCount={} >= maxRetry={}, type={}, code={}, msg={}, orderId={}",
                    current, LOCAL_MAX_RETRY,
                    dlq.type(),
                    dlq.error() != null ? dlq.error().code() : null,
                    dlq.error() != null ? dlq.error().message() : null,
                    resolveOrderId(headers));

            kafkaProducerService.sendToDiscard(dlq);

            return;
        }

        Bumped<OrderLocalMessage> bumped = bumpRetryCountFromBase(dlq, headers, current + 1);

        log.info("DLQ keep retrying: retryCount={} < maxRetry={} (type={}), orderId={}",
                current, LOCAL_MAX_RETRY, dlq.type(), resolveOrderId(headers));

        kafkaProducerService.sendToLocal(bumped.deadLetter().payload(), bumped.headers());
    }

    @Override
    public void retryApi(Object dlqObj, Map<String, String> headers) {
        DeadLetter<?> dlqRaw = (DeadLetter<?>) dlqObj;
        DeadLetter<OrderApiMessage> dlq = castPayload(dlqRaw, OrderApiMessage.class);

        if (dlq == null || dlq.payload() == null) {
            log.warn("skip: empty DLQ payload (ORDER_API)");

            return;
        }

        int current = resolveCurrentRetryCount(dlq.error(), headers);

        if (current >= API_MAX_RETRY) {
            log.warn("DLQ discard: retryCount={} >= maxRetry={}, type={}, code={}, msg={}, orderId={}",
                    current, API_MAX_RETRY,
                    dlq.type(),
                    dlq.error() != null ? dlq.error().code() : null,
                    dlq.error() != null ? dlq.error().message() : null,
                    resolveOrderId(headers));

            kafkaProducerService.sendToDiscard(dlq);

            return;
        }

        Bumped<OrderApiMessage> bumped = bumpRetryCountFromBase(dlq, headers, current + 1);

        log.info("DLQ keep retrying: retryCount={} < maxRetry={} (type={}), orderId={}",
                current, API_MAX_RETRY, dlq.type(), resolveOrderId(headers));

        kafkaProducerService.sendToOrderApi(bumped.deadLetter().payload(), bumped.headers());
    }

    @Override
    public void retryCrud(Object dlqObj, Map<String, String> headers) {
        DeadLetter<?> dlqRaw = (DeadLetter<?>) dlqObj;
        DeadLetter<OrderCrudMessage> dlq = castPayload(dlqRaw, OrderCrudMessage.class);

        if (dlq == null || dlq.payload() == null) {
            log.warn("skip: empty DLQ payload (ORDER_CRUD)");

            return;
        }

        int current = resolveCurrentRetryCount(dlq.error(), headers);

        if (current >= CRUD_MAX_RETRY) {
            log.warn("DLQ discard: retryCount={} >= maxRetry={}, type={}, code={}, msg={}, orderId={}",
                    current, CRUD_MAX_RETRY,
                    dlq.type(),
                    dlq.error() != null ? dlq.error().code() : null,
                    dlq.error() != null ? dlq.error().message() : null,
                    resolveOrderId(headers));

            kafkaProducerService.sendToDiscard(dlq);

            return;
        }

        Bumped<OrderCrudMessage> bumped = bumpRetryCountFromBase(dlq, headers, current + 1);

        log.info("DLQ keep retrying: retryCount={} < maxRetry={} (type={}), orderId={}",
                current, CRUD_MAX_RETRY, dlq.type(), resolveOrderId(headers));

        kafkaProducerService.sendToOrderCrud(bumped.deadLetter().payload(), bumped.headers());
    }

    /**
     * 공용 payload 캐스팅 (Map/JsonNode -> DTO)
     * - DLQ value는 JsonDeserializer에 의해 DeadLetter<?>로 역직렬화됨.
     * - payload(Object)는 LinkedHashMap/JsonNode일 수 있으므로 convertValue만 진행.
     */
    private <T> DeadLetter<T> castPayload(DeadLetter<?> raw, Class<T> clazz) {
        if (raw == null) {
            return null;
        }

        Object p = raw.payload();
        T payload = ObjectMapperUtils.valueToObject(p, clazz);

        return DeadLetter.of(raw.type(), raw.error(), payload);
    }

    /**
     * 현재 재시도 횟수 계산:
     * - 메타와 헤더의 유효숫자 중 최대값을 현재값으로 사용.
     */
    private int resolveCurrentRetryCount(ErrorDetail error, Map<String, String> headers) {
        int fromMeta = resolveFailCount(error);
        int fromHeader = resolveHeaderRetryCount(headers);

        return Math.max(fromMeta, fromHeader);
    }

    /**
     * 재시도 카운트 증가 (기준값 제공 버전):
     * - 호출자가 판단한 next(=current+1)를 그대로 메타/헤더에 반영.
     * - 메타(PRIMARY_RETRY_KEY)와 헤더(RETRY_HEADER_KEYS[0])에 동시 반영.
     */
    private <T> Bumped<T> bumpRetryCountFromBase(DeadLetter<T> dlq, Map<String, String> headers, int next) {
        ErrorDetail old = dlq.error();

        Map<String, String> newMeta = new LinkedHashMap<>();

        if (old != null && old.meta() != null) {
            newMeta.putAll(old.meta());
        }

        newMeta.put(PRIMARY_RETRY_KEY, Integer.toString(next));

        ErrorDetail updated = new ErrorDetail(
                old != null ? old.code() : null,
                old != null ? old.message() : null,
                old != null ? old.exception() : null,
                old != null ? old.occurredAtMs() : null,
                newMeta,
                old != null ? old.stackTrace() : null
        );

        DeadLetter<T> bumped = DeadLetter.of(dlq.type(), updated, dlq.payload());

        Map<String, String> newHeaders = new LinkedHashMap<>();

        if (headers != null) {
            newHeaders.putAll(headers);
        }

        newHeaders.put(RETRY_HEADER_KEYS[0], Integer.toString(next));

        log.info("DLQ retry-count bumped: type={}, newRetryCount={} (meta/header)", dlq.type(), next);

        return new Bumped<>(bumped, newHeaders);
    }

    private int resolveFailCount(ErrorDetail error) {
        if (error == null) {
            return 0;
        }

        Map<String, String> meta = error.meta();

        if (meta != null && !meta.isEmpty()) {
            String[] keys = new String[]{
                    PRIMARY_RETRY_KEY, "failedCount", "failCount", "retryCount",
                    "retries", "attempt", "attempts", "deliveryAttempts", "x-retry-count"
            };

            for (String k : keys) {
                Integer v = tryGetIntCaseInsensitive(meta, k);

                if (v != null) {
                    return v;
                }
            }

            for (Map.Entry<String, String> e : meta.entrySet()) {
                Integer v = tryParseIntSafe(e.getValue());

                if (v != null) {
                    return v;
                }
            }
        }

        String msg = error.message();

        if (msg != null && !msg.isBlank()) {
            Integer v = extractFirstInt(msg);

            if (v != null) {
                return v;
            }
        }

        return 0;
    }

    private int resolveHeaderRetryCount(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return 0;
        }

        for (String key : RETRY_HEADER_KEYS) {
            Integer v = tryGetIntCaseInsensitive(headers, key);

            if (v != null) {
                return v;
            }
        }

        for (Map.Entry<String, String> e : headers.entrySet()) {
            Integer v = tryParseIntSafe(e.getValue());

            if (v != null) {
                return v;
            }
        }

        return 0;
    }

    private static Integer extractFirstInt(String s) {
        if (s == null) {
            return null;
        }

        StringBuilder digits = new StringBuilder();
        boolean found = false;

        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                digits.append(c);
                found = true;

                break;
            }
        }

        return found ? tryParseIntSafe(digits.toString()) : null;
    }

    private Integer tryGetIntCaseInsensitive(Map<String, String> map, String targetKey) {
        String tk = targetKey.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, String> e : map.entrySet()) {
            if (e.getKey() != null && e.getKey().toLowerCase(Locale.ROOT).equals(tk)) {
                return tryParseIntSafe(e.getValue());
            }
        }

        return null;
    }

    private static Integer tryParseIntSafe(String v) {
        if (v == null) {
            return null;
        }

        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException ignore) {
            return null;
        }
    }

    private String resolveOrderId(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return "-";
        }

        String[] keys = new String[]{
                "orderId", "x-order-id", "X-Order-Id", "order-id"
        };

        for (String k : keys) {
            String v = headers.get(k);

            if (v != null && !v.isBlank()) {
                return v;
            }
        }

        return "-";
    }

    private record Bumped<T>(DeadLetter<T> deadLetter, Map<String, String> headers) {
    }
}