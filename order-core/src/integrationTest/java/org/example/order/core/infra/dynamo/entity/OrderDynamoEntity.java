package org.example.order.core.infra.dynamo.entity;

/**
 * integrationTest 전용 DynamoDB 엔티티.
 * - 메인 코드에 같은 이름의 엔티티가 없어 컴파일이 깨지는 문제를 가리기 위해 테스트용 최소 필드만 둔다.
 * - DynamoDB Enhanced Client 어노테이션(@DynamoDbBean 등)으로 스키마를 선언한다.
 * - Lombok 미사용(불필요 의존성 없애기 위해 수동 getter/setter).
 *
 * 주의: integrationTest 소스셋에만 존재. main/test 소스셋에는 영향 없음.
 */

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
public class OrderDynamoEntity {

    private String id;       // 파티션 키
    private String payload;  // 테스트용 데이터

    @DynamoDbPartitionKey
    @DynamoDbAttribute("id")
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    @DynamoDbAttribute("payload")
    public String getPayload() {
        return payload;
    }
    public void setPayload(String payload) {
        this.payload = payload;
    }
}
