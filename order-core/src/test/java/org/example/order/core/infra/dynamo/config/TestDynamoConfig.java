package org.example.order.core.infra.dynamo.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class TestDynamoConfig {

    @Bean
    public DynamoDbProperties dynamoDbProperties() {
        DynamoDbProperties props = new DynamoDbProperties();
        props.setEndpoint("http://localhost:4566"); // LocalStack endpoint
        props.setRegion("ap-northeast-2"); // 아무 리전이나 가능
        return props;
    }

    @Bean
    public AmazonDynamoDB amazonDynamoDB(DynamoDbProperties properties) {
        return AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        properties.getEndpoint(), properties.getRegion()))
                .withCredentials(new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials("dummy-access-key", "dummy-secret-key")))
                .build();
    }

    @Bean
    public DynamoDBMapper dynamoDBMapper(AmazonDynamoDB amazonDynamoDB) {
        return new DynamoDBMapper(amazonDynamoDB);
    }
}
