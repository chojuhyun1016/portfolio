package org.example.order.core.infra.dynamo.support;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.time.Duration;

@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
public class LocalStackDynamoSupport {

    private static final DockerImageName LOCALSTACK_IMAGE =
            DockerImageName.parse("localstack/localstack:2.3.2");

    @Container
    public static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer(LOCALSTACK_IMAGE)
                    .withServices(LocalStackContainer.Service.DYNAMODB)
                    .withStartupTimeout(Duration.ofSeconds(90));

    @BeforeAll
    void setAwsSystemProperties() {
        String accessKey = LOCALSTACK.getAccessKey();
        String secretKey = LOCALSTACK.getSecretKey();
        String region = LOCALSTACK.getRegion();

        System.setProperty("aws.accessKeyId", accessKey);
        System.setProperty("aws.secretAccessKey", secretKey);
        System.setProperty("aws.region", region);
    }

    @DynamicPropertySource
    static void registerAwsProps(DynamicPropertyRegistry registry) {
        registry.add("aws.region", LOCALSTACK::getRegion);
        registry.add("aws.dynamodb.endpoint", () ->
                LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.DYNAMODB).toString());
    }

    public static DynamoDbClient dynamoDbClient() {
        URI endpoint = LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.DYNAMODB);
        Region region = Region.of(LOCALSTACK.getRegion());

        return DynamoDbClient.builder()
                .endpointOverride(endpoint)
                .region(region)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        LOCALSTACK.getAccessKey(),
                                        LOCALSTACK.getSecretKey()
                                )
                        )
                )
                .build();
    }
}
