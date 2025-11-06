package org.example.order.batch.service.common;

import org.example.order.contract.order.messaging.dlq.DeadLetter;
import org.example.order.contract.order.messaging.event.OrderApiMessage;
import org.example.order.contract.order.messaging.event.OrderCrudMessage;
import org.example.order.contract.order.messaging.event.OrderLocalMessage;

import java.util.List;
import java.util.Map;

public interface KafkaProducerService {

    void sendToLocal(OrderLocalMessage m);

    void sendToOrderApi(OrderApiMessage m);

    void sendToOrderCrud(OrderCrudMessage m);

    void sendToLocal(OrderLocalMessage m, Map<String, String> headers);

    void sendToOrderApi(OrderApiMessage m, Map<String, String> headers);

    void sendToOrderCrud(OrderCrudMessage m, Map<String, String> headers);

    <T> void sendToDiscard(DeadLetter<T> message);

    <T> void sendToDlq(List<DeadLetter<T>> messages, Exception currentException);

    <T> void sendToDlq(DeadLetter<T> message, Exception currentException);

    <T> void sendToDlq(DeadLetter<T> message, Map<String, String> originalHeaders, Exception currentException);
}
