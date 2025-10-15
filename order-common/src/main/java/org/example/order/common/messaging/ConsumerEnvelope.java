package org.example.order.common.messaging;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ConsumerEnvelope
 * - Kafka 레코드의 payload + headers(+ 관측용 메타)를 하나로 묶은 래퍼
 * - 배치 처리에서 각 레코드의 헤더를 안전하게 보존
 */
@Getter
@ToString
public class ConsumerEnvelope<T> {

    private final T payload;
    private final Map<String, String> headers;

    private final String key;
    private final Integer partition;
    private final Long offset;
    private final Long timestamp;

    @Builder
    public ConsumerEnvelope(
            T payload,
            Map<String, String> headers,
            String key,
            Integer partition,
            Long offset,
            Long timestamp
    ) {
        this.payload = payload;
        this.headers = (headers == null ? Collections.emptyMap() : Collections.unmodifiableMap(headers));
        this.key = key;
        this.partition = partition;
        this.offset = offset;
        this.timestamp = timestamp;
    }

    public static <K, V, T> ConsumerEnvelope<T> fromRecord(
            ConsumerRecord<K, V> record,
            T mappedPayload
    ) {
        Map<String, String> map = toStringMap(record.headers());

        return ConsumerEnvelope.<T>builder()
                .payload(mappedPayload)
                .headers(map)
                .key(record.key() == null ? null : record.key().toString())
                .partition(record.partition())
                .offset(record.offset())
                .timestamp(record.timestamp())
                .build();
    }

    private static Map<String, String> toStringMap(Headers headers) {
        if (headers == null) {
            return Collections.emptyMap();
        }

        Map<String, String> m = new HashMap<>();

        headers.forEach(h -> {
            byte[] v = h.value();
            m.put(h.key(), v == null ? null : new String(v, StandardCharsets.UTF_8));
        });

        return m;
    }
}
