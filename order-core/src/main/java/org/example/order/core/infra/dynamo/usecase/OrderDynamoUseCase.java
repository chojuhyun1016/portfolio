package org.example.order.core.infra.dynamo.usecase;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.dynamo.model.OrderDynamoEntity;
import org.example.order.core.infra.dynamo.port.out.OrderDynamoPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderDynamoUseCase {

    private final OrderDynamoPort orderDynamoPort;

    public void save(OrderDynamoEntity entity) {
        orderDynamoPort.save(entity);
    }

    public Optional<OrderDynamoEntity> get(String id) {
        return orderDynamoPort.findById(id);
    }

    public List<OrderDynamoEntity> getAll() {
        return orderDynamoPort.findAll();
    }

    public List<OrderDynamoEntity> getByUserId(Long userId) {
        return orderDynamoPort.findByUserId(userId);
    }
}
