package org.example.order.api.web.service.common.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.api.web.service.common.KafkaProducerService;
import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.client.kafka.service.KafkaProducerCluster;
import org.example.order.contract.order.messaging.event.OrderLocalMessage;
import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaProducerServiceImpl implements KafkaProducerService {

    /**
     * KafkaProducerCluster가 존재하지 않을 수 있으므로 ObjectProvider로 선택 주입
     * - kafka.producer.enabled=false 인 경우에도 본 서비스 빈은 생성되지만,
     * 내부적으로 NoOp로 동작한다.
     */
    private final ObjectProvider<KafkaProducerCluster> clusterProvider;
    private final KafkaTopicProperties kafkaTopicProperties;
    private final AtomicBoolean warnedNoOp = new AtomicBoolean(false);

    @Override
    public void sendToOrder(OrderLocalMessage message) {
        send(message, kafkaTopicProperties.getName(MessageOrderType.ORDER_LOCAL));
    }

    private void send(Object message, String topic) {
        KafkaProducerCluster cluster = clusterProvider.getIfAvailable();

        if (cluster == null) {
            if (warnedNoOp.compareAndSet(false, true)) {
                log.info("[KafkaProducer][NoOp] kafka.producer.enabled=false 또는 프로듀서 미구성. 메시지는 전송되지 않습니다. topic={}, sample={}",
                        topic, safeSample(message));
            } else {
                log.debug("[KafkaProducer][NoOp] drop message. topic={}", topic);
            }

            return;
        }

        cluster.sendMessage(message, topic);
    }

    private static Object safeSample(Object payload) {
        try {
            String s = String.valueOf(payload);

            return s.length() > 200 ? s.substring(0, 200) + "..." : s;
        } catch (Throwable ignore) {
            return "<unprintable>";
        }
    }
}
