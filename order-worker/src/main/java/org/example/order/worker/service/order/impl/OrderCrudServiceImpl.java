package org.example.order.worker.service.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.application.order.dto.internal.LocalOrderDto;
import org.example.order.core.application.order.mapper.OrderMapper;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.model.OrderUpdate;
import org.example.order.domain.order.repository.OrderCommandRepository;
import org.example.order.domain.order.repository.OrderRepository;
import org.example.order.worker.service.order.OrderCrudService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderCrudServiceImpl implements OrderCrudService {
    private final OrderRepository orderRepository;
    private final OrderCommandRepository orderCommandRepository;
    private final OrderMapper orderMapper;

    @Override
    public List<OrderEntity> bulkInsert(List<LocalOrderDto> dtoList) {
        try {
            List<OrderEntity> entities = dtoList.stream().map(orderMapper::toEntity).toList();

            orderCommandRepository.bulkInsert(entities);

            return entities;
        } catch (DataAccessException e) {
            log.error("error : OrderCrudEntity bulkInsert failed - msg : {}, cause : {}", e.getMessage(), e.getCause(), e);

            throw e;
        } catch (Exception e) {
            log.error("error : OrderCrudEntity bulkInsert failed", e);

            throw e;
        }
    }

    @Override
    public void bulkUpdate(List<LocalOrderDto> dtoList) {
        List<OrderUpdate> commandList = orderMapper.toUpdateCommands(dtoList);
        orderCommandRepository.bulkUpdate(commandList);
    }

    @Override
    public void deleteAll(List<LocalOrderDto> dtoList) {
        List<Long> ids = dtoList.stream().map(LocalOrderDto::getOrderId).toList();
        orderRepository.deleteByOrderIdIn(ids);
    }
}
