package org.example.order.client.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaProducerCluster implements SmartLifecycle {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private Boolean isRunning = false;

    public void sendMessage(Object data, String topic) {
        Message<Object> message = MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(message);

        future.whenComplete(MdcPropagation.wrap((result, ex) -> {
            if (ex == null) {
                log.info("Sending kafka message - topic: {}, offset: {}", topic, result.getRecordMetadata().offset());
            } else {
                log.error("error: Sending kafka message failed - topic: {}, message: {}", topic, ex.getMessage(), ex);
            }
        }));
    }

    /**
     * 헤더 복원 전송
     */
    public void sendMessage(Object data, String topic, Map<String, String> originalHeaders) {
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
                log.info("Sending kafka message with headers - topic: {}, offset: {}", topic, result.getRecordMetadata().offset());
            } else {
                log.error("error: Sending kafka message with headers failed - topic: {}, message: {}", topic, ex.getMessage(), ex);
            }
        }));
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

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }
}
