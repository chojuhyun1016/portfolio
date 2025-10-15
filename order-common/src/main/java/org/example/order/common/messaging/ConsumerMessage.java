package org.example.order.common.messaging;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.Map;

/**
 * 컨슈머 내부 전달용 래퍼 (와이어 스키마 아님)
 * - payload: 계약 DTO 또는 내부 DTO
 * - retryCount: 헤더 기반 재시도 횟수 (정책 메타)
 * - key/partition/offset/timestamp: 관측/로그 용
 * - headers: 문자열 맵으로 평탄화(카프카 타입 의존 제거)
 */
@Getter
@ToString
public class ConsumerMessage<T> {

    private final T payload;
    private final int retryCount;

    private final String key;
    private final Integer partition;
    private final Long offset;
    private final Long timestamp;

    private final Map<String, String> headers;

    @Builder
    public ConsumerMessage(
            T payload,
            int retryCount,
            String key,
            Integer partition,
            Long offset,
            Long timestamp,
            Map<String, String> headers
    ) {
        this.payload = payload;
        this.retryCount = retryCount;
        this.key = key;
        this.partition = partition;
        this.offset = offset;
        this.timestamp = timestamp;
        this.headers = (headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(headers));
    }

    public static <T> ConsumerMessage<T> of(T payload, int retryCount) {
        return ConsumerMessage.<T>builder()
                .payload(payload)
                .retryCount(retryCount)
                .build();
    }
}
