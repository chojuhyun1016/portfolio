package org.example.order.core.infra.dynamo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DynamoDB 프로퍼티 구성 클래스
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "dynamodb")
public class DynamoDbProperties {
    private String endpoint;
    private String region;
    private String accessKey;   // 옵션: 로컬/운영 구분용
    private String secretKey;   // 옵션: 로컬/운영 구분용
}
