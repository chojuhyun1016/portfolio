package org.example.order.core.infra.dynamo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "dynamodb")
public class DynamoDbProperties {
    private String endpoint;
    private String region;
}
