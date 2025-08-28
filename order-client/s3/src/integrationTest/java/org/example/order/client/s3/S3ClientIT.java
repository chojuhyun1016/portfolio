package org.example.order.client.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import org.example.order.client.s3.config.S3ModuleConfig;
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
 * - 대표 모듈 S3ModuleConfig 만 로드
 * - LocalStack 컨테이너는 static 블록에서 선기동
 * - @DynamicPropertySource에서는 컨테이너 API를 다시 호출하지 않고, 캐시된 상수만 사용
 */
@SpringBootTest(classes = S3ModuleConfig.class)
@Testcontainers
@ExtendWith(SpringExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
class S3ClientIT {

    private static final DockerImageName LOCALSTACK_IMAGE =
            DockerImageName.parse("localstack/localstack:2.3.2");

    @Container
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(LocalStackContainer.Service.S3);

    // ---- 컨테이너 정보를 미리 확보해 두는 캐시 상수들 ----
    static final String REGION;
    static final String ENDPOINT;
    static final String ACCESS_KEY;
    static final String SECRET_KEY;
    static final String BUCKET;
    static final String DEFAULT_FOLDER;

    static {
        // 컨테이너를 선제적으로 시작하여 포트 매핑/엔드포인트가 유효하도록 보장
        LOCALSTACK.start();

        REGION = LOCALSTACK.getRegion();
        ENDPOINT = LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.S3).toString();
        ACCESS_KEY = LOCALSTACK.getAccessKey();
        SECRET_KEY = LOCALSTACK.getSecretKey();
        BUCKET = "it-bucket-" + UUID.randomUUID();
        DEFAULT_FOLDER = "it-folder";
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        // 스위치 ON (미설정이면 모듈이 비활성화되어 빈이 생성되지 않음)
        r.add("aws.s3.enabled", () -> "true");
        // Supplier 내부에서 컨테이너 API를 호출하지 않고, 캐싱된 상수만 사용
        r.add("aws.region", () -> REGION);
        r.add("aws.endpoint", () -> ENDPOINT);
        r.add("aws.credential.enabled", () -> "true");
        r.add("aws.credential.access-key", () -> ACCESS_KEY);
        r.add("aws.credential.secret-key", () -> SECRET_KEY);
        r.add("aws.s3.bucket", () -> BUCKET);
        r.add("aws.s3.default-folder", () -> DEFAULT_FOLDER);
    }

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    AmazonS3 amazonS3; // 버킷 생성 용도

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    S3Client s3Client;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    S3Properties props;

    @Test
    @DisplayName("S3 put/get round-trip (LocalStack)")
    void putGetObject() throws Exception {

        // 0) 버킷 준비(없으면 생성)
        String bucket = props.getS3().getBucket();

        if (!amazonS3.doesBucketExistV2(bucket)) {
            amazonS3.createBucket(bucket);
        }

        // 1) 업로드할 임시 파일 준비
        String key = props.getS3().getDefaultFolder() + "/hello-" + UUID.randomUUID() + ".txt";
        File tmp = Files.createTempFile("s3-it-", ".txt").toFile();
        tmp.deleteOnExit();

        try (Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8)) {
            w.write("hello-s3");
        }

        // 2) 업로드
        s3Client.putObject(bucket, key, tmp);

        // 3) 다운로드 & 내용 검증
        S3Object obj = s3Client.getObject(bucket, key);
        assertNotNull(obj);

        try (InputStream in = obj.getObjectContent()) {
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertEquals("hello-s3", text);
        }
    }
}
