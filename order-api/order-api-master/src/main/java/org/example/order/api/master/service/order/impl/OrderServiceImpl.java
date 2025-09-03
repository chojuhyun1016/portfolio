package org.example.order.api.master.service.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.api.master.service.common.KafkaProducerService;
import org.example.order.api.master.service.order.OrderService;
import org.example.order.core.application.order.dto.command.LocalOrderCommand;
import org.example.order.core.application.order.mapper.OrderMapper;
import org.example.order.core.infra.messaging.order.message.OrderLocalMessage;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final KafkaProducerService kafkaProducerService;
    private final OrderMapper orderMapper;

    @Override
    public void sendMessage(LocalOrderCommand command) {
        final OrderLocalMessage message = orderMapper.toOrderLocalMessage(command);

        message.validation();

        log.info(
                "[OrderService] sending message to Kafka: id={}, methodType={}, publishedTs={}",
                message.getId(),
                message.getMethodType(),
                message.getPublishedTimestamp());

        kafkaProducerService.sendToOrder(message);
    }
}
