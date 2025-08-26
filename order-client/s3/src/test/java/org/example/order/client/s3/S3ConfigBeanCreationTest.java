package org.example.order.client.s3;

import com.amazonaws.services.s3.AmazonS3;
import org.example.order.client.s3.config.S3Config;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * S3ConfigBeanCreationTest
 * <p>
 * 주요 포인트:
 * - S3Config 가 AmazonS3 빈을 정상적으로 생성하는지 확인
 * - 실제 S3 연결/네트워크 호출은 하지 않음 (속도/안정성 확보)
 * - 단순히 Bean 존재 여부만 검증하는 "유닛 성격" 테스트
 */
@SpringBootTest(classes = S3Config.class)
@TestPropertySource(properties = {
        "aws.region=us-east-1",
        "aws.endpoint=http://localhost:4566", // LocalStack/프록시 가정
        "aws.credential.enabled=true",
        "aws.credential.access-key=dummy",
        "aws.credential.secret-key=dummy",
        "aws.s3.bucket=test-bucket",
        "aws.s3.default-folder=tmp"
})
class S3ConfigBeanCreationTest {

    @org.springframework.beans.factory.annotation.Autowired
    AmazonS3 amazonS3;

    @Test
    @DisplayName("S3Config → AmazonS3 빈 생성")
    void amazonS3BeanCreated() {
        assertNotNull(amazonS3);
    }
}
