package org.example.order.worker.service.common;

import org.example.order.common.core.messaging.message.DlqMessage;
import org.example.order.core.messaging.order.message.OrderApiMessage;
import org.example.order.core.messaging.order.message.OrderCrudMessage;
import org.example.order.core.messaging.order.message.OrderLocalMessage;
import org.example.order.core.messaging.order.message.OrderCloseMessage;

import java.util.List;

public interface KafkaProducerService {
    void sendToLocal(OrderLocalMessage message);
    void sendToOrderApi(OrderApiMessage message);
    void sendToOrderCrud(OrderCrudMessage message);
    void sendToOrderRemote(OrderCloseMessage message);
    <T extends DlqMessage> void sendToDlq(List<T> messages, Exception currentException);
    <T extends DlqMessage> void sendToDlq(T message, Exception currentException);
}
