package org.example.order.client.s3.config.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * S3Properties
 * <p>
 * - aws.s3.enabled=true 일 때만 S3 빈들이 생성됨
 * - aws.endpoint: LocalStack/프록시 환경에서만 지정 (미지정 시 실제 AWS 사용)
 * - aws.credential.enabled=true 일 때 AccessKey/SecretKey 를 명시적으로 사용
 * - 검증은 런타임에서 조건부 수행(설정 OFF일 땐 검증 X)
 * <p>
 * 예시:
 * aws:
 * region: ap-northeast-2
 * endpoint: http://localhost:4566        # LocalStack 용 (옵션)
 * credential:
 * enabled: true
 * access-key: test
 * secret-key: test
 * s3:
 * enabled: true
 * bucket: my-bucket
 * default-folder: tmp
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
