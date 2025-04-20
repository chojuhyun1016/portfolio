package org.example.order.core.infra.dynamo.service;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.dynamo.entity.OrderDynamoEntity;
import org.example.order.core.infra.dynamo.repository.OrderDynamoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderDynamoService {

    private final OrderDynamoRepository orderDynamoRepository;

    public void save(OrderDynamoEntity entity) {
        orderDynamoRepository.save(entity);
    }

    public Optional<OrderDynamoEntity> get(String id) {
        return orderDynamoRepository.findById(id);
    }

    public List<OrderDynamoEntity> getAll() {
        return orderDynamoRepository.findAll();
    }

    public List<OrderDynamoEntity> getByUserId(Long userId) {
        return orderDynamoRepository.findByUserId(userId);
    }
}
