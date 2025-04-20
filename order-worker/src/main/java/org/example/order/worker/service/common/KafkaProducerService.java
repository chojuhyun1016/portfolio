package org.example.order.worker.service.common;

import org.example.order.common.event.DlqMessage;
import org.example.order.core.application.order.event.message.OrderApiEvent;
import org.example.order.core.application.order.event.message.OrderCrudEvent;
import org.example.order.core.application.order.event.message.OrderLocalEvent;
import org.example.order.core.application.order.event.message.OrderRemoteEvent;

import java.util.List;

public interface KafkaProducerService {
    void sendToLocal(OrderLocalEvent message);
    void sendToOrderApi(OrderApiEvent message);
    void sendToOrderCrud(OrderCrudEvent message);
    void sendToOrderRemote(OrderRemoteEvent message);
    <T extends DlqMessage> void sendToDlq(List<T> messages, Exception currentException);
    <T extends DlqMessage> void sendToDlq(T message, Exception currentException);
}
