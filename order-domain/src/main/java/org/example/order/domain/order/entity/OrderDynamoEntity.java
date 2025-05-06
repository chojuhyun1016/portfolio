package org.example.order.domain.order.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName = "order_dynamo")
public class OrderDynamoEntity {

    @DynamoDBHashKey(attributeName = "id")
    private String id;

    @DynamoDBAttribute(attributeName = "userId")
    private Long userId;

    @DynamoDBAttribute(attributeName = "orderNumber")
    private String orderNumber;

    @DynamoDBAttribute(attributeName = "orderPrice")
    private Long orderPrice;
}
