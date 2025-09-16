package org.example.order.client.s3.config.internal;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.RequiredArgsConstructor;
import org.example.order.client.s3.config.property.S3Properties;
import org.example.order.client.s3.service.S3Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * S3ClientConfig
 * <p>
 * - aws.s3.enabled=true 일 때만 활성화(@ConditionalOnProperty)
 * - AmazonS3 + S3Client 를 설정 기반으로 @Bean 등록
 * - 필수값/조합 검증은 런타임에서 조건부 수행
 */
@Configuration
@EnableConfigurationProperties(S3Properties.class)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true")
public class S3ClientConfig {

    private final S3Properties props;

    @Bean
    public AmazonS3 amazonS3Client() {
        validateRequiredWhenEnabled();

        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .enablePathStyleAccess();

        if (isNotBlank(props.getEndpoint())) {
            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(props.getEndpoint(), coalesce(props.getRegion(), "us-east-1"))
            );
        } else {
            builder.withRegion(props.getRegion());
        }

        if (props.getCredential() != null && props.getCredential().isEnabled()) {
            String ak = props.getCredential().getAccessKey();
            String sk = props.getCredential().getSecretKey();

            if (!isNotBlank(ak) || !isNotBlank(sk)) {
                throw new IllegalStateException("aws.credential.enabled=true requires both access-key and secret-key.");
            }

            builder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(ak, sk)));
        }

        return builder.build();
    }

    @Bean
    public S3Client s3Client(AmazonS3 amazonS3) {
        return new S3Client(amazonS3);
    }

    private void validateRequiredWhenEnabled() {
        if (props.getS3() == null || !isNotBlank(props.getS3().getBucket()) || !isNotBlank(props.getS3().getDefaultFolder())) {
            throw new IllegalStateException("aws.s3.enabled=true requires aws.s3.bucket and aws.s3.default-folder.");
        }

        if (!isNotBlank(props.getEndpoint()) && !isNotBlank(props.getRegion())) {
            throw new IllegalStateException("aws.s3.enabled=true without 'aws.endpoint' requires 'aws.region'.");
        }
    }

    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String coalesce(String val, String fallback) {
        return isNotBlank(val) ? val : fallback;
    }
}
