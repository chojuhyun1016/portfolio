package org.example.order.client.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import org.example.order.client.s3.config.S3Config;
import org.example.order.client.s3.config.property.S3Properties;
import org.example.order.client.s3.service.S3Client;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S3ClientIT (통합 테스트)
 * <p>
 * 주요 포인트:
 * - LocalStack 컨테이너로 S3 환경을 시뮬레이션
 * - @DynamicPropertySource 로 Spring Boot AWS 설정 주입
 * - AmazonS3 빈을 통해 버킷/객체 CRUD 동작 검증
 * - IDE의 @Autowired 경고는 오탐이므로 억제 처리
 */
@SpringBootTest(classes = S3Config.class)
@Testcontainers
@ExtendWith(SpringExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
class S3ClientIT {

    private static final DockerImageName LOCALSTACK_IMAGE =
            DockerImageName.parse("localstack/localstack:2.3.2");

    @Container
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(LocalStackContainer.Service.S3);

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("aws.region", LOCALSTACK::getRegion);
        r.add("aws.endpoint", () -> LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.S3).toString());
        r.add("aws.credential.enabled", () -> "true");
        r.add("aws.credential.access-key", LOCALSTACK::getAccessKey);
        r.add("aws.credential.secret-key", LOCALSTACK::getSecretKey);
        r.add("aws.s3.bucket", () -> "it-bucket-" + UUID.randomUUID());
        r.add("aws.s3.default-folder", () -> "it-folder");
    }

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    AmazonS3 amazonS3;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    S3Properties props;

    @Test
    @DisplayName("S3 put/get round-trip")
    void putGetObject() throws Exception {

        // 1) 버킷 생성
        String bucket = props.getS3().getBucket();
        if (!amazonS3.doesBucketExistV2(bucket)) {
            amazonS3.createBucket(bucket);
        }

        // 2) 임시 파일 생성 및 업로드
        String key = props.getS3().getDefaultFolder() + "/hello-" + UUID.randomUUID() + ".txt";
        File tmp = Files.createTempFile("s3-it-", ".txt").toFile();
        tmp.deleteOnExit();

        try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
            w.write("hello-s3");
        }

        // 3) 클라이언트로 업로드/다운로드 실행
        S3Client client = new S3Client(amazonS3);
        client.putObject(bucket, key, tmp);

        S3Object obj = client.getObject(bucket, key);
        assertNotNull(obj);

        // 4) 다운로드된 파일 내용 검증
        try (InputStream in = obj.getObjectContent()) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("hello-s3", text);
        }
    }
}
