package org.example.order.core.infra.dynamo.adapter;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.dynamo.model.OrderDynamoEntity;
import org.example.order.core.infra.dynamo.port.out.OrderDynamoPort;
import org.example.order.core.infra.dynamo.support.DynamoQuerySupport;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class OrderDynamoAdapter implements OrderDynamoPort {

    private final DynamoDBMapper dynamoDBMapper;

    @Override
    public void save(OrderDynamoEntity entity) {
        dynamoDBMapper.save(entity);
    }

    @Override
    public Optional<OrderDynamoEntity> findById(String id) {
        return Optional.ofNullable(dynamoDBMapper.load(OrderDynamoEntity.class, id));
    }

    @Override
    public List<OrderDynamoEntity> findAll() {
        return dynamoDBMapper.scan(OrderDynamoEntity.class, new DynamoDBScanExpression());
    }

    @Override
    public List<OrderDynamoEntity> findByUserId(Long userId) {
        return DynamoQuerySupport.scanByNumber(
                dynamoDBMapper,
                OrderDynamoEntity.class,
                "userId",
                userId
        );
    }
}
