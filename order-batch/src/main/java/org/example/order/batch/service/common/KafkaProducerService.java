package org.example.order.batch.service.common;

import org.example.order.contract.order.messaging.dlq.DeadLetter;
import org.example.order.contract.order.messaging.event.OrderApiMessage;
import org.example.order.contract.order.messaging.event.OrderCrudMessage;
import org.example.order.contract.order.messaging.event.OrderLocalMessage;

import java.util.List;

public interface KafkaProducerService {
    void sendToLocal(OrderLocalMessage message);
    void sendToOrderApi(OrderApiMessage message);
    void sendToOrderCrud(OrderCrudMessage message);

    <T extends DeadLetter> void sendToDiscard(T message);
    <T extends DeadLetter> void sendToDlq(List<T> messages, Exception currentException);
    <T extends DeadLetter> void sendToDlq(T message, Exception currentException);
}
