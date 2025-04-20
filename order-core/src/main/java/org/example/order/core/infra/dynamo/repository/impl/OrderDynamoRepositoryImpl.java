package org.example.order.core.infra.dynamo.repository.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.dynamo.entity.OrderDynamoEntity;
import org.example.order.core.infra.dynamo.repository.OrderDynamoRepository;
import org.example.order.core.infra.common.nosql.DynamoQuerySupport;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class OrderDynamoRepositoryImpl implements OrderDynamoRepository {
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
