package org.example.order.api.master.service.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.api.master.service.common.KafkaProducerService;
import org.example.order.api.master.service.order.OrderService;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.core.application.order.dto.command.LocalOrderCommand;
import org.example.order.core.application.order.dto.internal.OrderDto;
import org.example.order.core.application.order.mapper.OrderMapper;
import org.example.order.core.messaging.order.message.OrderLocalMessage;
import org.example.order.domain.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final KafkaProducerService kafkaProducerService;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    public OrderDto findById(Long id) {
        return orderRepository
                .findById(id)
                .map(orderMapper::toDto)
                .map(OrderDto::fromInternal)
                .orElseThrow(
                        () -> {
                            String msg = "Order not found. id=" + id;
                            log.warn("[OrderService] {}", msg);
                            return new CommonException(CommonExceptionCode.NOT_FOUND_RESOURCE, msg);
                        });
    }

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
