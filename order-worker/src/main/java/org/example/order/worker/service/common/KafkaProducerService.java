package org.example.order.worker.service.common;

import org.example.order.contract.order.messaging.dlq.DeadLetter;
import org.example.order.contract.order.messaging.event.OrderApiMessage;
import org.example.order.contract.order.messaging.event.OrderCloseMessage;
import org.example.order.contract.order.messaging.event.OrderCrudMessage;
import org.example.order.contract.order.messaging.event.OrderLocalMessage;

import java.util.List;
import java.util.Map;

/**
 * KafkaProducerService
 * - 정상 전송: 헤더 변경 없음
 * - DLQ 전송: DeadLetter 생성 또는 그대로 사용
 * - DLQ 전송 시 원본 헤더 복원 전송 지원
 */
public interface KafkaProducerService {
    void sendToLocal(OrderLocalMessage message);

    void sendToOrderApi(OrderApiMessage message);

    void sendToOrderCrud(OrderCrudMessage message);

    void sendToOrderRemote(OrderCloseMessage message);

    <T extends DeadLetter> void sendToDlq(T message, Map<String, String> originalHeaders, Exception currentException);

    void sendToDlq(Object payload, Map<String, String> originalHeaders, Exception currentException);

    <T extends DeadLetter> void sendToDlq(List<T> messages, List<Map<String, String>> originalHeadersList, Exception currentException);
}
