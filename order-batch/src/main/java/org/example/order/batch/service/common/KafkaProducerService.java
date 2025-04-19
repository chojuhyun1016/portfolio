package org.example.order.batch.service.common;

import org.example.order.common.event.DlqMessage;
import org.example.order.core.application.event.OrderApiEvent;
import org.example.order.core.application.event.OrderCrudEvent;
import org.example.order.core.application.event.OrderLocalEvent;

import java.util.List;

public interface KafkaProducerService {
    void sendToLocal(OrderLocalEvent message);
    void sendToOrderApi(OrderApiEvent message);
    void sendToOrderCrud(OrderCrudEvent message);
    <T extends DlqMessage> void sendToDiscard(T message);
    <T extends DlqMessage> void sendToDlq(List<T> messages, Exception currentException);
    <T extends DlqMessage> void sendToDlq(T message, Exception currentException);
}
