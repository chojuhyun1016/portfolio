package org.example.order.api.master.service.order.impl;

import lombok.RequiredArgsConstructor;
import org.example.order.api.master.service.common.KafkaProducerService;
import org.example.order.api.master.service.order.OrderService;
import org.example.order.core.application.order.event.OrderRemoteMessageDto;
import org.example.order.core.application.order.event.message.OrderRemoteEvent;
import org.example.order.core.application.order.model.OrderVo;
import org.example.order.core.infra.jpa.repository.OrderRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository repository;
    private final KafkaProducerService kafkaProducerService;

    @Override
    public OrderVo fetchByIds(Long orderId) {
        return repository.fetchByOrderId(orderId);
    }

    @Override
    public void sendMessage(OrderRemoteMessageDto dto){
        OrderRemoteEvent message = dto.toMessage();
        kafkaProducerService.sendToOrder(message);
    }
}
