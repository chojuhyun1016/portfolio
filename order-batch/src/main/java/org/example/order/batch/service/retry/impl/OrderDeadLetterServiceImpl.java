package org.example.order.batch.service.retry.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.batch.service.common.KafkaProducerService;
import org.example.order.batch.service.retry.OrderDeadLetterService;
import org.example.order.common.support.json.ObjectMapperUtils;
import org.example.order.common.support.logging.Correlate;
import org.example.order.contract.order.messaging.dlq.DeadLetter;
import org.example.order.contract.order.messaging.event.OrderApiMessage;
import org.example.order.contract.order.messaging.event.OrderCrudMessage;
import org.example.order.contract.order.messaging.event.OrderLocalMessage;
import org.example.order.contract.shared.error.ErrorDetail;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * DLQ 메시지 유형별 재처리 (기존 방식 유지)
 * <p>
 * 변경사항
 * - Facade에서 타입 분류 후 각 메서드(retryLocal/retryApi/retryCrud)로 직접 위임하도록 구조 단순화.
 * - 각 메서드 내부에서 재시도 카운트 +1, 임계치 비교, 전송/폐기 결정.
 * - 전송 시 헤더 포함 전송 지원.
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
    @Correlate(
            paths = {
                    "#p1?.get('orderId')",
                    "#p1?.get('traceId')",
                    "#p1?.get('X-Request-Id')",
                    "#p1?.get('x-request-id')"
            },
            mdcKey = "orderId",
            overrideTraceId = true
    )
    public void retryLocal(Object rawMessage, Map<String, String> headers) {
        DeadLetter<OrderLocalMessage> dlq = toDeadLetter(rawMessage, OrderLocalMessage.class);

        if (dlq == null || dlq.payload() == null) {
            log.warn("skip: empty DLQ payload (ORDER_LOCAL)");

            return;
        }

        Bumped<OrderLocalMessage> bumped = bumpRetryCount(dlq, headers);

        if (shouldDiscardWithMax(bumped.deadLetter(), LOCAL_MAX_RETRY, bumped.headers())) {
            kafkaProducerService.sendToDiscard(bumped.deadLetter());

            return;
        }

        kafkaProducerService.sendToLocal(bumped.deadLetter().payload(), bumped.headers());
    }

    @Override
    @Correlate(
            paths = {
                    "#p1?.get('orderId')",
                    "#p1?.get('traceId')",
                    "#p1?.get('X-Request-Id')",
                    "#p1?.get('x-request-id')"
            },
            mdcKey = "orderId",
            overrideTraceId = true
    )
    public void retryApi(Object rawMessage, Map<String, String> headers) {
        DeadLetter<OrderApiMessage> dlq = toDeadLetter(rawMessage, OrderApiMessage.class);

        if (dlq == null || dlq.payload() == null) {
            log.warn("skip: empty DLQ payload (ORDER_API)");

            return;
        }

        Bumped<OrderApiMessage> bumped = bumpRetryCount(dlq, headers);

        if (shouldDiscardWithMax(bumped.deadLetter(), API_MAX_RETRY, bumped.headers())) {
            kafkaProducerService.sendToDiscard(bumped.deadLetter());

            return;
        }

        kafkaProducerService.sendToOrderApi(bumped.deadLetter().payload(), bumped.headers());
    }

    @Override
    @Correlate(
            paths = {
                    "#p1?.get('orderId')",
                    "#p1?.get('traceId')",
                    "#p1?.get('X-Request-Id')",
                    "#p1?.get('x-request-id')"
            },
            mdcKey = "orderId",
            overrideTraceId = true
    )
    public void retryCrud(Object rawMessage, Map<String, String> headers) {
        DeadLetter<OrderCrudMessage> dlq = toDeadLetter(rawMessage, OrderCrudMessage.class);

        if (dlq == null || dlq.payload() == null) {
            log.warn("skip: empty DLQ payload (ORDER_CRUD)");

            return;
        }

        Bumped<OrderCrudMessage> bumped = bumpRetryCount(dlq, headers);

        if (shouldDiscardWithMax(bumped.deadLetter(), CRUD_MAX_RETRY, bumped.headers())) {
            kafkaProducerService.sendToDiscard(bumped.deadLetter());

            return;
        }

        kafkaProducerService.sendToOrderCrud(bumped.deadLetter().payload(), bumped.headers());
    }

    private <T> DeadLetter<T> toDeadLetter(Object rawMessage, Class<T> clazz) {
        DeadLetter<?> dlqRaw = ObjectMapperUtils.valueToObject(rawMessage, DeadLetter.class);

        if (dlqRaw == null) {
            return null;
        }

        T payload = ObjectMapperUtils.valueToObject(dlqRaw.payload(), clazz);

        return DeadLetter.of(dlqRaw.type(), dlqRaw.error(), payload);
    }

    /**
     * 재시도 카운트 증가:
     * - 메타와 헤더에서 추출한 현재값의 최대치에 +1
     * - 메타(PRIMARY_RETRY_KEY)와 헤더(RETRY_HEADER_KEYS[0])에 동시 반영
     */
    private <T> Bumped<T> bumpRetryCount(DeadLetter<T> dlq, Map<String, String> headers) {
        ErrorDetail old = dlq.error();
        int fromMeta = resolveFailCount(old);
        int fromHeader = resolveHeaderRetryCount(headers);
        int base = Math.max(fromMeta, fromHeader);
        int next = base + 1;

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

    private boolean shouldDiscardWithMax(DeadLetter<?> message, int maxRetry, Map<String, String> headers) {
        int currentMeta = resolveFailCount(message != null ? message.error() : null);
        int currentHeader = resolveHeaderRetryCount(headers);
        int current = Math.max(currentMeta, currentHeader);

        boolean discard = current >= maxRetry;

        if (discard) {
            log.warn("DLQ discard: retryCount={} >= maxRetry={}, type={}, code={}, msg={}",
                    current, maxRetry,
                    message != null ? message.type() : null,
                    message != null && message.error() != null ? message.error().code() : null,
                    message != null && message.error() != null ? message.error().message() : null);
        } else {
            log.info("DLQ keep retrying: retryCount={} < maxRetry={} (type={})",
                    current, maxRetry, message != null ? message.type() : null);
        }

        return discard;
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

    private record Bumped<T>(DeadLetter<T> deadLetter, Map<String, String> headers) {
    }
}
