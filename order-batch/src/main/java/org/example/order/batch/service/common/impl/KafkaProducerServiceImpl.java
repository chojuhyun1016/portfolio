package org.example.order.batch.service.common.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.client.kafka.service.KafkaProducerCluster;
import org.example.order.contract.order.messaging.dlq.DeadLetter;
import org.example.order.contract.order.messaging.event.OrderApiMessage;
import org.example.order.contract.order.messaging.event.OrderCrudMessage;
import org.example.order.contract.order.messaging.event.OrderLocalMessage;
import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.example.order.contract.shared.error.ErrorDetail;
import org.example.order.batch.service.common.KafkaProducerService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties({KafkaTopicProperties.class})
public class KafkaProducerServiceImpl implements KafkaProducerService {

    private final KafkaProducerCluster cluster;
    private final KafkaTopicProperties kafkaTopicProperties;

    @Override
    public void sendToLocal(OrderLocalMessage m) {
        send(m, kafkaTopicProperties.getName(MessageOrderType.ORDER_LOCAL.name()));
    }

    @Override
    public void sendToOrderApi(OrderApiMessage m) {
        send(m, kafkaTopicProperties.getName(MessageOrderType.ORDER_API.name()));
    }

    @Override
    public void sendToOrderCrud(OrderCrudMessage m) {
        send(m, kafkaTopicProperties.getName(MessageOrderType.ORDER_CRUD.name()));
    }

    @Override
    public void sendToLocal(OrderLocalMessage m, Map<String, String> headers) {
        send(m, kafkaTopicProperties.getName(MessageOrderType.ORDER_LOCAL.name()), headers);
    }

    @Override
    public void sendToOrderApi(OrderApiMessage m, Map<String, String> headers) {
        send(m, kafkaTopicProperties.getName(MessageOrderType.ORDER_API.name()), headers);
    }

    @Override
    public void sendToOrderCrud(OrderCrudMessage m, Map<String, String> headers) {
        send(m, kafkaTopicProperties.getName(MessageOrderType.ORDER_CRUD.name()), headers);
    }

    @Override
    public <T> void sendToDiscard(DeadLetter<T> message) {
        String topic = kafkaTopicProperties.getName(MessageOrderType.ORDER_ALARM.name());

        log.info("Sending message to discard topic: {}", topic);

        cluster.sendMessage(message, topic);
    }

    /**
     * 폐기(ALARM) 전송(헤더 포함)
     * - 운영 가시성: 원본 상관키/추적 헤더 보존 전송이 필요한 경우 사용
     */
    public <T> void sendToDiscard(DeadLetter<T> message, Map<String, String> headers) {
        String topic = kafkaTopicProperties.getName(MessageOrderType.ORDER_ALARM.name());

        log.info("Sending message to discard topic with headers: {}", topic);

        cluster.sendMessage(message, topic, headers);
    }

    @Override
    public <T> void sendToDlq(List<DeadLetter<T>> messages, Exception currentException) {
        if (ObjectUtils.isEmpty(messages)) {
            return;
        }

        for (DeadLetter<T> m : messages) {
            sendToDlq(m, currentException);
        }
    }

    @Override
    public <T> void sendToDlq(DeadLetter<T> message, Exception currentException) {
        if (message == null) {
            return;
        }

        String topic = kafkaTopicProperties.getName(MessageOrderType.ORDER_DLQ.name());
        ErrorDetail err = buildErrorDetail(currentException, null, 4000);
        DeadLetter<T> dlq = DeadLetter.of(message.type(), err, message.payload());

        log.info("Sending message to DLQ: {}", topic);

        cluster.sendMessage(dlq, topic);
    }

    @Override
    public <T> void sendToDlq(DeadLetter<T> message, Map<String, String> originalHeaders, Exception currentException) {
        if (message == null) {
            return;
        }

        String topic = kafkaTopicProperties.getName(MessageOrderType.ORDER_DLQ.name());
        ErrorDetail err = buildErrorDetail(currentException, null, 4000);
        DeadLetter<T> dlq = DeadLetter.of(message.type(), err, message.payload());

        log.info("Sending message to DLQ with headers: {}", topic);

        cluster.sendMessage(dlq, topic, originalHeaders);
    }

    private void send(Object message, String topic) {
        if (topic == null || topic.isBlank()) {
            log.error("Kafka topic is empty. skip: {}", message);

            return;
        }

        log.debug("Resolved topic for send: {}", topic);

        cluster.sendMessage(message, topic);
    }

    private void send(Object message, String topic, Map<String, String> headers) {
        if (topic == null || topic.isBlank()) {
            log.error("Kafka topic is empty. skip: {}", message);

            return;
        }

        log.debug("Resolved topic for send (with headers): {}", topic);

        cluster.sendMessage(message, topic, headers);
    }

    private ErrorDetail buildErrorDetail(Exception ex, Map<String, String> meta, int stackLimit) {
        String code = "UNKNOWN";
        String msg = ex == null ? "unknown" : nullSafe(ex.getMessage(), "unknown");
        String fqcn = ex == null ? null : ex.getClass().getName();
        String stack = stackTraceOf(ex);

        if (stack != null && stack.length() > stackLimit) {
            stack = stack.substring(0, stackLimit);
        }

        return new ErrorDetail(code, msg, fqcn, System.currentTimeMillis(), meta, stack);
    }

    private static String stackTraceOf(Throwable t) {
        if (t == null) {
            return null;
        }

        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);

            return sw.toString();
        } catch (Exception ignore) {
            return null;
        }
    }

    private static String nullSafe(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }
}
