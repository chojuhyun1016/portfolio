package org.example.order.client.s3.config.property;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * AWS S3 설정 Properties
 *
 * - aws.credential.enabled: true 인 경우 AccessKey/SecretKey 필수
 * - endpoint: 로컬(LocalStack) 또는 프록시용 커스텀 엔드포인트 (옵션)
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

    /** ✨ 신규: 로컬/프록시 환경을 위한 커스텀 엔드포인트 */
    private String endpoint;

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
