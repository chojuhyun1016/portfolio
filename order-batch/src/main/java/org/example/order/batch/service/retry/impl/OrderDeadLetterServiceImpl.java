package org.example.order.batch.service.retry.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.batch.exception.BatchExceptionCode;
import org.example.order.batch.service.common.KafkaProducerService;
import org.example.order.batch.service.retry.OrderDeadLetterService;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.common.support.json.ObjectMapperUtils;
import org.example.order.contract.order.messaging.dlq.DeadLetter;
import org.example.order.contract.order.messaging.event.OrderApiMessage;
import org.example.order.contract.order.messaging.event.OrderCrudMessage;
import org.example.order.contract.order.messaging.event.OrderLocalMessage;
import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.example.order.contract.shared.error.ErrorDetail;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DLQ 메시지 유형별 재처리 (기존 방식 유지)
 * <p>
 * 변경사항
 * - ObjectMapperUtils.constructParametricType(...) 의존 제거.
 * → 1) DeadLetter<?> 로 1차 역직렬화 후, 2) payload 만 원하는 타입으로 재역직렬화.
 * - 타입별 전용 메서드 분리 (retryLocal / retryApi / retryCrud)
 * - 재전송 전 카운트 증가(하드코딩) 및 타입별 폐기 임계치(하드코딩) 적용
 * - LOCAL: 5, API: 3, CRUD: 5
 * - 실패 카운트는 DeadLetter.error.meta 에 저장/증분:
 * - 우선 키: retryCount (없으면 기존 known-keys 사용 후 retryCount 로 기록)
 * - 실패 카운트 파싱은 meta 및 message 에서 유연하게 추출
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

    @Override
    public void retry(Object message) {
        final MessageOrderType type;

        try {
            type = ObjectMapperUtils.getFieldValueFromString(String.valueOf(message), "type", MessageOrderType.class);
        } catch (Exception e) {
            log.error("DLQ retry: failed to resolve type from payload: {}", message);

            throw new CommonException(BatchExceptionCode.UNSUPPORTED_DLQ_TYPE);
        }

        log.info("DLQ 처리 시작 - Type: {}", type);

        // === 타입별 전용 처리로 분기 ===
        switch (type) {
            case ORDER_LOCAL -> retryLocal(message);
            case ORDER_API -> retryApi(message);
            case ORDER_CRUD -> retryCrud(message);
            default -> throw new CommonException(BatchExceptionCode.UNSUPPORTED_DLQ_TYPE);
        }
    }

    /* ======================================================================
     * 타입별 전용 처리
     * ==================================================================== */

    private void retryLocal(Object rawMessage) {
        DeadLetter<OrderLocalMessage> dlq = toDeadLetter(rawMessage, OrderLocalMessage.class);

        if (dlq == null || dlq.payload() == null) {
            log.warn("skip: empty DLQ payload (ORDER_LOCAL)");

            return;
        }

        // 1) 재전송 카운트 증가(하드코딩 규칙)
        DeadLetter<OrderLocalMessage> bumped = bumpRetryCount(dlq);

        // 2) 폐기 여부 판단(하드코딩 임계치)
        if (shouldDiscardWithMax(bumped, LOCAL_MAX_RETRY)) {
            kafkaProducerService.sendToDiscard(bumped);

            return;
        }

        // 3) 원 토픽 재전송
        kafkaProducerService.sendToLocal(bumped.payload());
    }

    private void retryApi(Object rawMessage) {
        DeadLetter<OrderApiMessage> dlq = toDeadLetter(rawMessage, OrderApiMessage.class);

        if (dlq == null || dlq.payload() == null) {
            log.warn("skip: empty DLQ payload (ORDER_API)");

            return;
        }

        DeadLetter<OrderApiMessage> bumped = bumpRetryCount(dlq);

        if (shouldDiscardWithMax(bumped, API_MAX_RETRY)) {
            kafkaProducerService.sendToDiscard(bumped);

            return;
        }

        kafkaProducerService.sendToOrderApi(bumped.payload());
    }

    private void retryCrud(Object rawMessage) {
        DeadLetter<OrderCrudMessage> dlq = toDeadLetter(rawMessage, OrderCrudMessage.class);

        if (dlq == null || dlq.payload() == null) {
            log.warn("skip: empty DLQ payload (ORDER_CRUD)");

            return;
        }

        DeadLetter<OrderCrudMessage> bumped = bumpRetryCount(dlq);

        if (shouldDiscardWithMax(bumped, CRUD_MAX_RETRY)) {
            kafkaProducerService.sendToDiscard(bumped);

            return;
        }

        kafkaProducerService.sendToOrderCrud(bumped.payload());
    }

    /* ======================================================================
     * 공용 유틸
     * ==================================================================== */

    /**
     * DeadLetter<?> → DeadLetter<T> (제네릭 변환)
     */
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
     * - 현재 실패/재시도 카운트를 추출 → +1 → meta(PRIMARY_RETRY_KEY)에 기록
     * - ErrorDetail(레코드)이므로 새 인스턴스 구성
     */
    private <T> DeadLetter<T> bumpRetryCount(DeadLetter<T> dlq) {
        ErrorDetail old = dlq.error();
        int current = resolveFailCount(old);
        int next = current + 1;

        Map<String, String> newMeta = new LinkedHashMap<>();

        if (old != null && old.meta() != null) {
            newMeta.putAll(old.meta());
        }

        newMeta.put(PRIMARY_RETRY_KEY, Integer.toString(next));

        ErrorDetail updated = new ErrorDetail(old != null ? old.code() : null, old != null ? old.message() : null, old != null ? old.exception() : null, old != null ? old.occurredAtMs() : null, newMeta, old != null ? old.stackTrace() : null);

        DeadLetter<T> bumped = DeadLetter.of(dlq.type(), updated, dlq.payload());

        log.info("DLQ retry-count bumped: type={}, newRetryCount={}", dlq.type(), next);

        return bumped;
    }

    /**
     * 폐기 여부 판단(타입별 하드코딩 임계치와 비교)
     */
    private boolean shouldDiscardWithMax(DeadLetter<?> message, int maxRetry) {
        int current = resolveFailCount(message != null ? message.error() : null);

        boolean discard = current >= maxRetry;

        if (discard) {
            log.warn("DLQ discard: retryCount={} >= maxRetry={}, type={}, code={}, msg={}", current, maxRetry, message != null ? message.type() : null, message != null && message.error() != null ? message.error().code() : null, message != null && message.error() != null ? message.error().message() : null);
        } else {
            log.info("DLQ keep retrying: retryCount={} < maxRetry={} (type={})", current, maxRetry, message != null ? message.type() : null);
        }

        return discard;
    }

    /**
     * ErrorDetail 에서 실패/재시도 횟수 추출
     * - 1) meta 맵에서 케이스 무시 키 매칭 (우선순위 키 포함)
     * - 2) message 텍스트에서 정규식으로 숫자 추출(우선 "fail|retry|attempt" 주변)
     * - 실패 시 0
     */
    private int resolveFailCount(ErrorDetail error) {
        if (error == null) {
            return 0;
        }

        Map<String, String> meta = error.meta();

        if (meta != null && !meta.isEmpty()) {
            // 우선 키(우선순위: PRIMARY_RETRY_KEY)
            String[] keys = new String[]{PRIMARY_RETRY_KEY, "failedCount", "failCount", "retryCount", "retries", "attempt", "attempts", "deliveryAttempts", "x-retry-count"};

            for (String k : keys) {
                Integer v = tryGetIntCaseInsensitive(meta, k);

                if (v != null) {
                    return v;
                }
            }

            // 마지막 fallback: 값 중 첫 정수
            for (Map.Entry<String, String> e : meta.entrySet()) {
                Integer v = tryParseIntSafe(e.getValue());

                if (v != null) {
                    return v;
                }
            }
        }

        String msg = error.message();

        if (msg != null && !msg.isBlank()) {
            Pattern p = Pattern.compile("(fail(?:ed)?|retry|attempt)s?\\D*(\\d+)", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(msg);

            if (m.find()) {
                Integer v = tryParseIntSafe(m.group(2));

                if (v != null) {
                    return v;
                }
            }

            Pattern p2 = Pattern.compile("(\\d+)");
            Matcher m2 = p2.matcher(msg);

            if (m2.find()) {
                Integer v = tryParseIntSafe(m2.group(1));

                if (v != null) {
                    return v;
                }
            }
        }

        return 0;
    }

    private Integer tryGetIntCaseInsensitive(Map<String, String> meta, String targetKey) {
        String tk = targetKey.toLowerCase(Locale.ROOT);

        for (Map.Entry<String, String> e : meta.entrySet()) {
            if (e.getKey() != null && e.getKey().toLowerCase(Locale.ROOT).equals(tk)) {
                return tryParseIntSafe(e.getValue());
            }
        }

        return null;
    }

    private Integer tryParseIntSafe(String v) {
        if (v == null) {
            return null;
        }

        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException ignore) {
            return null;
        }
    }
}
