package org.example.order.worker.facade.order;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface OrderLocalMessageFacade {
    void sendOrderApiTopic(ConsumerRecord<String, Object> record);
}
