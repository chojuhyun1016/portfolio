package org.example.order.domain.order.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import lombok.*;

/**
 * DynamoDB 엔티티 (V2 Enhanced Client용)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class OrderDynamoEntity {

    private String id;
    @Getter
    private Long userId;
    @Getter
    private String orderNumber;
    @Getter
    private Long orderPrice;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}
