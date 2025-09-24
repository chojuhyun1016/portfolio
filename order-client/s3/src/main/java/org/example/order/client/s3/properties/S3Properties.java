package org.example.order.client.s3.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * S3Properties
 * - aws.s3.enabled=true 일 때 S3 빈 생성
 * - aws.endpoint: LocalStack/프록시 환경에서만 지정 (미지정 시 실제 AWS 사용)
 * - aws.credential.enabled=true 일 때 AccessKey/SecretKey 를 명시적으로 사용
 */
@Getter
@Setter
@ConfigurationProperties("aws")
public class S3Properties {

    /**
     * AWS 리전 (endpoint 미사용 시 필수)
     */
    private String region;

    /**
     * LocalStack/프록시 엔드포인트 (옵션)
     */
    private String endpoint;

    private Credential credential = new Credential();
    private S3 s3 = new S3();

    public String fullPath() {
        return String.format("%s/%s", s3.bucket, s3.defaultFolder);
    }

    @Getter
    @Setter
    public static class Credential {
        /**
         * true 면 AccessKey/SecretKey 사용, false 면 기본 자격증명 체인 사용(IAM Role 등)
         */
        private boolean enabled = true;
        private String accessKey;
        private String secretKey;
    }

    @Getter
    @Setter
    public static class S3 {
        /**
         * S3 모듈 전체 on/off 스위치
         */
        private boolean enabled = false;

        /**
         * 기본 버킷 및 폴더 (enabled=true 일 때 필수)
         */
        private String bucket;
        private String defaultFolder;
    }
}
