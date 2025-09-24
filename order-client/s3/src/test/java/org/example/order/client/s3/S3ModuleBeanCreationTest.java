package org.example.order.client.s3;

import com.amazonaws.services.s3.AmazonS3;
import org.example.order.client.s3.autoconfig.S3AutoConfiguration;
import org.example.order.client.s3.service.S3Client;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S3ModuleBeanCreationTest
 * - aws.s3.enabled=true 일 때 AmazonS3 / S3Client 빈 생성 여부만 검증(네트워크 호출 없음)
 */
@SpringBootTest
@ImportAutoConfiguration(S3AutoConfiguration.class)
@TestPropertySource(properties = {
        "aws.s3.enabled=true",
        "aws.region=us-east-1",
        "aws.endpoint=http://localhost:4566",
        "aws.credential.enabled=true",
        "aws.credential.access-key=dummy",
        "aws.credential.secret-key=dummy",
        "aws.s3.bucket=test-bucket",
        "aws.s3.default-folder=tmp"
})
class S3ModuleBeanCreationTest {

    @Autowired
    AmazonS3 amazonS3;

    @Autowired
    S3Client s3Client;

    @Test
    @DisplayName("aws.s3.enabled=true → AmazonS3 / S3Client 빈 생성")
    void beansCreated() {
        assertNotNull(amazonS3);
        assertNotNull(s3Client);
    }
}
