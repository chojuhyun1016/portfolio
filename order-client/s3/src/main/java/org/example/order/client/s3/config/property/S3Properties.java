package org.example.order.client.s3.config.property;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * S3Properties
 * <p>
 * 주요 포인트:
 * - aws.region: 필수 리전
 * - aws.credential: AccessKey/SecretKey (enabled=true일 때 필수)
 * - aws.endpoint: LocalStack/프록시 환경용 커스텀 엔드포인트 (옵션)
 * - aws.s3.bucket / default-folder: 기본 버킷 및 폴더 경로
 */
@Getter
@Setter
@Validated
@ConfigurationProperties("aws")
public class S3Properties {

    private Credential credential;

    @NotBlank(message = "aws.region must not be blank")
    private String region;

    private S3 s3;

    private String endpoint; // 로컬/프록시 환경용 엔드포인트

    public String fullPath() {
        return String.format("%s/%s", s3.bucket, s3.defaultFolder);
    }

    @Getter
    @Setter
    public static class Credential {
        private boolean enabled = true;

        @NotBlank(message = "aws.credential.access-key must not be blank when enabled=true")
        private String accessKey;

        @NotBlank(message = "aws.credential.secret-key must not be blank when enabled=true")
        private String secretKey;
    }

    @Getter
    @Setter
    public static class S3 {
        @NotBlank(message = "aws.s3.bucket must not be blank")
        private String bucket;

        @NotBlank(message = "aws.s3.default-folder must not be blank")
        private String defaultFolder;
    }
}
