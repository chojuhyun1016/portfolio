package org.example.order.core.infra.dynamo.support;

/**
 * 통합테스트 전용 LocalStack(DynamoDB) 지원 유틸.
 *
 * - Testcontainers 로 LocalStack(DynamoDB) 컨테이너를 띄우고,
 *   AWS SDK v2 가 참조하는 JVM 시스템 프로퍼티(aws.accessKeyId, aws.secretAccessKey, aws.region)를
 *   컨테이너 기동 이후에 주입한다.
 * - 통합테스트 각 클래스는 이 클래스를 상속(extends)해서 컨테이너를 공유할 수 있다.
 * - DynamoDbClient 팩토리를 제공한다.
 *
 * 주의: integrationTest 소스셋에만 존재. main/test 코드에는 영향 없음.
 */

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

    // LocalStack 이미지 태그 고정. 필요 시 버전 업그레이드 가능.
    private static final DockerImageName LOCALSTACK_IMAGE =
            DockerImageName.parse("localstack/localstack:2.3.2");

    // DynamoDB 서비스만 사용. (필요 시 다른 서비스 추가)
    @Container
    public static final LocalStackContainer LOCALSTACK =
            new LocalStackContainer(LOCALSTACK_IMAGE)
                    .withServices(LocalStackContainer.Service.DYNAMODB)
                    .withStartupTimeout(Duration.ofSeconds(90));

    /**
     * 컨테이너 기동 이후, AWS SDK가 참고할 JVM 시스템 프로퍼티를 주입한다.
     * DefaultCredentialsProvider가 System properties → Env → Profile 순으로 조회한다.
     */
    @BeforeAll
    void setAwsSystemProperties() {
        String accessKey = LOCALSTACK.getAccessKey();
        String secretKey = LOCALSTACK.getSecretKey();
        String region = LOCALSTACK.getRegion();

        System.setProperty("aws.accessKeyId", accessKey);
        System.setProperty("aws.secretAccessKey", secretKey);
        System.setProperty("aws.region", region);
    }

    /**
     * (선택) Spring 쪽으로도 동적 프로퍼티를 흘려보낸다.
     * main 코드가 Environment/property로 endpoint/region을 읽는 경우 대비.
     */
    @DynamicPropertySource
    static void registerAwsProps(DynamicPropertyRegistry registry) {
        registry.add("aws.region", LOCALSTACK::getRegion);
        registry.add("aws.dynamodb.endpoint", () ->
                LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.DYNAMODB).toString());
    }

    /**
     * LocalStack용 DynamoDbClient 생성기.
     * - endpointOverride: 컨테이너 엔드포인트
     * - region: 컨테이너 리전
     * - credentials: 컨테이너가 노출하는 더미 AK/SK (정적 크레덴셜)
     */
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
