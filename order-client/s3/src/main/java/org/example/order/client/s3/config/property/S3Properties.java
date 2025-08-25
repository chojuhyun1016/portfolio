package org.example.order.client.s3.config.property;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * S3 모듈 프로퍼티 (Kafka 스타일 on/off 지원)
 *
 * - aws.s3.enabled            : 모듈 전체 스위치 (true일 때만 빈 생성)
 * - aws.s3.region             : 리전 (enabled=true일 때 필수)
 * - aws.s3.endpoint           : LocalStack/MinIO 등 커스텀 엔드포인트 (선택)
 * - aws.s3.path-style         : Path-Style 접근 강제 (LocalStack/MinIO는 보통 true)
 * - aws.s3.credential.enabled : true면 access/secret를 명시적으로 사용 (테스트/온프렘 등)
 *                               false면 AWS Default Credentials Provider Chain 사용(IAM Role 등)
 */
@Getter @Setter
@Validated
@ConfigurationProperties("aws.s3")
public class S3Properties {

    /** ✨ 모듈 on/off 스위치 (기본 false → 설정 없으면 비활성) */
    private boolean enabled = false;

    /** ✨ LocalStack/MinIO용 커스텀 엔드포인트 (예: http://localhost:4566) */
    private String endpoint;

    /** ✨ Path-Style 접근 (LocalStack/MinIO는 주로 true), 기본 true */
    private boolean pathStyle = true;

    /** ✨ S3 리전 (enabled=true일 때 필수) */
    @NotBlank(message = "aws.s3.region must not be blank when aws.s3.enabled=true")
    private String region;

    /** ✨ 명시 자격증명 설정(선택) */
    private Credential credential = new Credential();

    /** ✨ 기본 업로드 대상 (선택) */
    private S3 s3 = new S3();

    /** 편의: bucket/defaultFolder가 지정되면 'bucket/folder' 반환 */
    public String fullPath() {
        if (s3 == null) return null;
        return String.format("%s/%s", s3.getBucket(), s3.getDefaultFolder());
    }

    @Getter @Setter
    public static class Credential {
        /** ✨ true면 accessKey/secretKey 사용, false면 Default Provider Chain 사용 */
        private boolean enabled = false;

        /** credential.enabled=true일 때만 사실상 의미가 있음 */
        private String accessKey;
        private String secretKey;
    }

    @Getter @Setter
    public static class S3 {
        private String bucket;
        private String defaultFolder = ""; // 기본 빈 문자열
    }
}
