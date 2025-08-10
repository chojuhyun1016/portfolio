package com.example.order.api.web.service.order.impl;

import com.example.order.api.web.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.core.application.order.dto.internal.OrderDto;
import org.example.order.core.application.order.mapper.OrderMapper;
import org.example.order.domain.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    @Transactional(readOnly = true)
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
}
