package org.example.order.worker.service.order.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.application.dto.OrderLocalDto;
import org.example.order.core.domain.OrderEntity;
import org.example.order.core.repository.OrderRepository;
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
    private final OrderRepository repository;

    @Override
    public List<OrderEntity> bulkInsert(List<OrderLocalDto> dtoList) {
        try {
            List<OrderEntity> entities = dtoList.stream().map(OrderEntity::toEntity).toList();
            repository.bulkInsert(entities);
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
    public void bulkUpdate(List<OrderLocalDto> dtoList) {
        repository.bulkUpdate(dtoList);
    }

    @Override
    public void deleteAll(List<OrderLocalDto> dtoList) {
        List<Long> ids = dtoList.stream().map(OrderLocalDto::getOrderId).toList();
        repository.deleteByOrderIdIn(ids);
    }
}
