package org.example.order.api.master.service.order.impl;

import lombok.RequiredArgsConstructor;
import org.example.order.api.master.service.common.KafkaProducerService;
import org.example.order.api.master.service.order.OrderService;
import org.example.order.core.application.dto.OrderRemoteMessageDto;
import org.example.order.core.application.message.OrderRemoteMessage;
import org.example.order.core.application.vo.OrderVo;
import org.example.order.core.repository.OrderRepository;
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
        OrderRemoteMessage message = dto.toMessage();
        kafkaProducerService.sendToOrder(message);
    }
}
