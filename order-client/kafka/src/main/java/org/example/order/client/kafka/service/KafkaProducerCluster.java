package org.example.order.client.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.support.json.ObjectMapperFactory;
import org.example.order.common.support.logging.MdcPropagation;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * KafkaTemplate 기반 전송 어댑터
 * - 기본 전송
 * - 헤더 복원 전송
 * - (디버그) 실제 전송되는 payload JSON 을 로그로 확인 가능
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerCluster implements SmartLifecycle {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private volatile boolean isRunning = false;

    /**
     * 기본 전송
     */
    public void sendMessage(Object data, String topic) {
        logProducedPayload("sendMessage", topic, null, data);

        Message<Object> message = MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(message);

        future.whenComplete(MdcPropagation.wrap((result, ex) -> {
            if (ex == null) {
                log.info("Sending kafka message - topic: {}, partition: {}, offset: {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("error: Sending kafka message failed - topic: {}, message: {}",
                        topic, ex.getMessage(), ex);
            }
        }));
    }

    /**
     * 헤더 복원 전송
     */
    public void sendMessage(Object data, String topic, Map<String, String> originalHeaders) {
        logProducedPayload("sendMessageWithHeaders", topic, null, data);

        MessageBuilder<Object> builder = MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.TOPIC, topic);

        if (originalHeaders != null) {
            originalHeaders.forEach(builder::setHeader);
        }

        Message<Object> message = builder.build();

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(message);

        future.whenComplete(MdcPropagation.wrap((result, ex) -> {
            if (ex == null) {
                log.info("Sending kafka message with headers - topic: {}, partition: {}, offset: {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("error: Sending kafka message with headers failed - topic: {}, message: {}",
                        topic, ex.getMessage(), ex);
            }
        }));
    }

    /**
     * 실제 전송 payload 를 JSON 으로 찍어서, Producer 단계에서 이상이 없는지 확인.
     * - 너무 시끄러우면 로그 레벨을 DEBUG 로 낮추거나, 필요 시 주석 처리 가능.
     */
    private void logProducedPayload(String context, String topic, String key, Object payload) {
        try {
            String json = ObjectMapperFactory.defaultObjectMapper().writeValueAsString(payload);

            if (log.isDebugEnabled()) {
                log.debug("[KafkaProducerCluster] {} - topic={}, key={}, payload={}",
                        context, topic, key, json);
            } else {
                log.info("[KafkaProducerCluster] {} - topic={}, key={}, payloadType={}",
                        context, topic, key,
                        (payload == null ? "null" : payload.getClass().getName()));
            }
        } catch (Exception e) {
            log.warn("[KafkaProducerCluster] {} - topic={}, key={}, payload to JSON failed: {}",
                    context, topic, key, e.toString());
        }
    }

    @Override
    public void start() {
        this.isRunning = true;
    }

    @Override
    public void stop() {
        log.info("Stopping kafka producer");
        kafkaTemplate.flush();
        this.isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    /**
     * Producer 는 가능한 한 빨리 떠 있어야 하므로 MIN_VALUE 유지
     */
    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }
}
