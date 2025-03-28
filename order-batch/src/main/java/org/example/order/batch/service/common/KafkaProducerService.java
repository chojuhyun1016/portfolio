package org.example.order.batch.service.common;

import org.example.order.common.application.message.DlqMessage;
import org.example.order.core.application.message.OrderApiMessage;
import org.example.order.core.application.message.OrderCrudMessage;
import org.example.order.core.application.message.OrderLocalMessage;

import java.util.List;

public interface KafkaProducerService {
    void sendToLocal(OrderLocalMessage message);
    void sendToOrderApi(OrderApiMessage message);
    void sendToOrderCrud(OrderCrudMessage message);
    <T extends DlqMessage> void sendToDiscard(T message);
    <T extends DlqMessage> void sendToDlq(List<T> messages, Exception currentException);
    <T extends DlqMessage> void sendToDlq(T message, Exception currentException);
}
