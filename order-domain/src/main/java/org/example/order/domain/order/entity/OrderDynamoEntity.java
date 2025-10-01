package org.example.order.domain.order.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.LocalDateTime;

/**
 * Order DynamoDB 엔티티
 * <p>
 * - DynamoDB V2 Enhanced Client용 엔티티
 * - PK: id (문자열, 일반적으로 orderId 기반)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamoDbBean
public class OrderDynamoEntity {

    /**
     * DynamoDB 파티션 키 (보통 orderId 문자열 사용)
     */
    private String id;

    private Long orderId;             // 주문 식별자
    private String orderNumber;       // 주문 번호
    private Long userId;              // 사용자 ID
    private String userNumber;        // 사용자 번호
    private String userName;          // 사용자 번호
    private Long orderPrice;          // 주문 금액
    private String orderPriceEnc;     // 주문 금액(암호화)
    private String deleteYn;          // 삭제 여부 (Y/N)
    private Long createdUserId;
    private String createdUserType;
    private LocalDateTime createdDatetime;
    private Long modifiedUserId;
    private String modifiedUserType;
    private LocalDateTime modifiedDatetime;
    private Long publishedTimestamp;  // 발행 시점 (epoch millis)

    @DynamoDbPartitionKey
    public String getId() {
        return id;
    }

    @DynamoDbSortKey
    public String getOrderNumber() {
        return orderNumber;
    }
}
