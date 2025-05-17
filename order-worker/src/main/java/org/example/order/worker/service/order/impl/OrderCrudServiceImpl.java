package org.example.order.worker.service.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.application.order.dto.command.OrderSyncCommandDto;
import org.example.order.core.application.order.mapper.OrderSyncCommandMapper;
import org.example.order.domain.order.entity.OrderEntity;
import org.example.order.domain.order.model.OrderUpdateCommand;
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
    private final OrderSyncCommandMapper orderSyncCommandMapper;

    @Override
    public List<OrderEntity> bulkInsert(List<OrderSyncCommandDto> dtoList) {
        try {
            List<OrderEntity> entities = dtoList.stream().map(orderSyncCommandMapper::toEntity).toList();
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
    public void bulkUpdate(List<OrderSyncCommandDto> dtoList) {
        List<OrderUpdateCommand> commandList = orderSyncCommandMapper.toUpdateCommands(dtoList);
        orderCommandRepository.bulkUpdate(commandList);
    }

    @Override
    public void deleteAll(List<OrderSyncCommandDto> dtoList) {
        List<Long> ids = dtoList.stream().map(OrderSyncCommandDto::getOrderId).toList();
        orderRepository.deleteByOrderIdIn(ids);
    }
}
