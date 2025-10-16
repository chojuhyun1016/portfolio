package org.example.order.worker.service.common.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.kafka.config.properties.KafkaTopicProperties;
import org.example.order.client.kafka.service.KafkaProducerCluster;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.contract.order.messaging.dlq.DeadLetter;
import org.example.order.contract.order.messaging.event.OrderApiMessage;
import org.example.order.contract.order.messaging.event.OrderCloseMessage;
import org.example.order.contract.order.messaging.event.OrderCrudMessage;
import org.example.order.contract.order.messaging.event.OrderLocalMessage;
import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.example.order.contract.shared.error.ErrorDetail;
import org.example.order.worker.service.common.KafkaProducerService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

/**
 * KafkaProducerServiceImpl
 * - 정상 전송은 헤더 변경 없이 수행
 * - DLQ 전송은 DeadLetter 생성 또는 그대로 사용
 * - DLQ 전송 시 원본 헤더 복원 전송 지원
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties({KafkaTopicProperties.class})
public class KafkaProducerServiceImpl implements KafkaProducerService {

    private final KafkaProducerCluster cluster;
    private final KafkaTopicProperties kafkaTopicProperties;

    @Override
    public void sendToLocal(OrderLocalMessage message) {
        send(message, kafkaTopicProperties.getName(MessageOrderType.ORDER_LOCAL.name()));
    }

    @Override
    public void sendToOrderApi(OrderApiMessage message) {
        send(message, kafkaTopicProperties.getName(MessageOrderType.ORDER_API.name()));
    }

    @Override
    public void sendToOrderCrud(OrderCrudMessage message) {
        send(message, kafkaTopicProperties.getName(MessageOrderType.ORDER_CRUD.name()));
    }

    @Override
    public void sendToOrderRemote(OrderCloseMessage message) {
        send(message, kafkaTopicProperties.getName(MessageOrderType.ORDER_REMOTE.name()));
    }

    @Override
    public <T extends DeadLetter> void sendToDlq(T message, Map<String, String> originalHeaders, Exception currentException) {
        if (message == null) {
            return;
        }

        try {
            String dlqTopic = kafkaTopicProperties.getName(MessageOrderType.ORDER_DLQ.name());

            if (!hasText(dlqTopic)) {
                log.error("DLQ topic name is empty. message={}", message);
                return;
            }

            ErrorDetail error = buildErrorDetail(currentException, null, 4000);
            DeadLetter<?> dlq = new DeadLetter<>(toStringSafe(message.type()), error, message.payload());

            log.info("Sending message to DLQ. topic={}", dlqTopic);

            cluster.sendMessage(dlq, dlqTopic, originalHeaders);

        } catch (Exception e) {
            log.error("error: sending message to DLQ failed. message={}", message, e);
        }
    }

    @Override
    public void sendToDlq(Object payload, Map<String, String> originalHeaders, Exception currentException) {
        if (payload == null) {
            return;
        }

        try {
            String dlqTopic = kafkaTopicProperties.getName(MessageOrderType.ORDER_DLQ.name());

            if (!hasText(dlqTopic)) {
                log.error("DLQ topic name is empty. payload={}", payload);
                return;
            }

            MessageOrderType srcType = resolveSourceType(payload);
            ErrorDetail error = buildErrorDetail(currentException, null, 4000);
            DeadLetter<Object> dlq = new DeadLetter<>(srcType.name(), error, payload);

            log.info("Sending payload to DLQ with headers. topic={} type={}", dlqTopic, srcType.toString());

            cluster.sendMessage(dlq, dlqTopic, originalHeaders);

        } catch (Exception e) {
            log.error("error: sending payload to DLQ with headers failed. payload={}", payload, e);
        }
    }

    @Override
    public <T extends DeadLetter> void sendToDlq(List<T> messages, List<Map<String, String>> originalHeadersList, Exception currentException) {
        if (ObjectUtils.isEmpty(messages)) {
            return;
        }

        for (int i = 0; i < messages.size(); i++) {
            Map<String, String> headers = (originalHeadersList != null && originalHeadersList.size() > i)
                    ? originalHeadersList.get(i)
                    : null;

            sendToDlq(messages.get(i), headers, currentException);
        }
    }

    private void send(Object message, String topic) {
        if (!hasText(topic)) {
            log.error("Kafka topic is empty. skip sending. message={}", message);
            return;
        }

        cluster.sendMessage(message, topic);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static String toStringSafe(Object any) {
        return any == null ? "" : String.valueOf(any);
    }

    private MessageOrderType resolveSourceType(Object payload) {
        if (payload instanceof OrderLocalMessage) {
            return MessageOrderType.ORDER_LOCAL;
        }

        if (payload instanceof OrderApiMessage) {
            return MessageOrderType.ORDER_API;
        }

        if (payload instanceof OrderCrudMessage) {
            return MessageOrderType.ORDER_CRUD;
        }

        if (payload instanceof OrderCloseMessage) {
            return MessageOrderType.ORDER_REMOTE;
        }

        return MessageOrderType.ORDER_DLQ;
    }

    /**
     * Exception -> ErrorDetail 변환
     * - CommonException이면 code(Integer)를 문자열로 변환해 채움
     * - stack trace는 제한 길이로 잘라서 수집(기본 4000자)
     * - ErrorDetail은 record 생성자 직접 사용
     */
    private ErrorDetail buildErrorDetail(Exception ex, Map<String, String> meta, int stackLimit) {
        String code = "UNKNOWN";
        String msg = ex == null ? "unknown" : nullSafe(ex.getMessage(), "unknown");
        String fqcn = ex == null ? null : ex.getClass().getName();

        if (ex instanceof CommonException ce) {
            code = (ce.getCode() == null) ? "COMMON" : String.valueOf(ce.getCode());
            if (hasText(ce.getMsg())) {
                msg = ce.getMsg();
            }
        }

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
