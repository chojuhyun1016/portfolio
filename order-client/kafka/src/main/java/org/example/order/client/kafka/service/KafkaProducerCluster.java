package org.example.order.client.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.concurrent.CompletableFuture;

/**
 * KafkaTemplate을 이용한 Producer 서비스 (순수 POJO)
 * - 빈 등록은 설정 클래스(KafkaProducerServiceConfig)에서 조건부로 수행
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

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sending kafka message - topic: {}, message: {}, offset: {}",
                        topic, result.getProducerRecord().value(), result.getRecordMetadata().offset());
            } else {
                log.error("error : Sending kafka message failed - topic: {}, message: {}", topic, ex.getMessage(), ex);
            }
        });
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
