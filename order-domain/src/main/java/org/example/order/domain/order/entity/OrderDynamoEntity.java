package org.example.order.domain.order.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

/**
 * Order DynamoDB 엔티티
 *
 * - DynamoDB V2 Enhanced Client용 엔티티
 * - @DynamoDbPartitionKey: 파티션 키 설정
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class OrderDynamoEntity {

    private String id;
    private Long userId;
    private String orderNumber;
    private Long orderPrice;

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }
}
