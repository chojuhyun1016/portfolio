package org.example.order.client.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 큰 맥락
 * - KafkaTemplate 을 이용해 메시지를 전송하는 Producer 서비스.
 * - @ConditionalOnBean(KafkaTemplate.class) → producer.enabled=true 로 설정되어 KafkaTemplate 빈이 있을 때만 등록된다.
 * - SmartLifecycle 구현:
 * · start() → isRunning 플래그 true
 * · stop()  → flush 후 안전하게 종료
 * · getPhase() → Integer.MIN_VALUE 로 설정하여, 다른 SmartLifecycle 빈들보다 먼저 시작되고 가장 마지막에 종료됨.
 * - sendMessage():
 * · data 와 topic 을 받아 MessageBuilder 로 메시지 생성
 * · KafkaTemplate.send() 호출 → CompletableFuture 반환
 * · 성공 시 offset 로그, 실패 시 에러 로그 출력
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(KafkaTemplate.class)
public class KafkaProducerCluster implements SmartLifecycle {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private Boolean isRunning = false;

    /**
     * 메시지 전송
     */
    public void sendMessage(Object data, String topic) {
        Message<Object> message = MessageBuilder
                .withPayload(data)
                .setHeader(KafkaHeaders.TOPIC, topic)
                .build();

        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(message);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sending kafka message - topic: {}, message: {}, offset: {}",
                        topic,
                        result.getProducerRecord().value(),
                        result.getRecordMetadata().offset());
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
        kafkaTemplate.flush(); // 잔여 메시지 비우기
        this.isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    /**
     * SmartLifecycle phase → 가장 먼저 시작, 가장 나중에 종료
     */
    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }
}
