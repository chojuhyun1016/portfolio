package org.example.order.worker.facade.order;

import org.example.order.worker.dto.consumer.OrderApiConsumerDto;

public interface OrderApiMessageFacade {
    void requestApi(OrderApiConsumerDto dto);
}
