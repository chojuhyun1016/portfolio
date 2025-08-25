package org.example.order.client.s3.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.RequiredArgsConstructor;
import org.example.order.client.s3.config.property.S3Properties;
import org.example.order.client.s3.service.S3Client;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * S3 구성
 * - ✨ aws.s3.enabled=true 일 때만 AmazonS3/S3Client 생성
 * - endpoint + pathStyle 로 LocalStack/MinIO 대응
 * - credential.enabled=true → BasicAWSCredentials 사용
 *   credential.enabled=false → AWS Default Credentials Provider Chain 사용
 */
@Configuration
@EnableConfigurationProperties(S3Properties.class)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true")
public class S3Config {

    private final S3Properties props;

    @Bean
    @ConditionalOnMissingBean(AmazonS3.class)
    public AmazonS3 amazonS3Client() {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();

        // 공통 클라이언트 설정(타임아웃/재시도 등 필요 시 확장)
        ClientConfiguration clientConf = new ClientConfiguration()
                .withProtocol(Protocol.HTTPS) // LocalStack가 http면 아래 endpoint에서 자동으로 http가 적용됨
                .withRetryPolicy(PredefinedRetryPolicies.getDefaultRetryPolicyWithCustomMaxRetries(3));
        builder.setClientConfiguration(clientConf);

        // 리전 (필수)
        builder.withRegion(props.getRegion());

        // LocalStack/MinIO 등 커스텀 엔드포인트
        if (props.getEndpoint() != null && !props.getEndpoint().isBlank()) {
            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(props.getEndpoint(), props.getRegion()));
            // 대부분 path-style을 요구
            if (props.isPathStyle()) {
                builder.enablePathStyleAccess();
            }
        } else {
            // 일반 AWS S3 (엔드포인트 지정 없음) → 필요시 path-style off
            if (props.isPathStyle()) {
                builder.enablePathStyleAccess();
            }
        }

        // 명시 자격증명(선택)
        if (props.getCredential() != null && props.getCredential().isEnabled()) {
            BasicAWSCredentials creds = new BasicAWSCredentials(
                    props.getCredential().getAccessKey(),
                    props.getCredential().getSecretKey()
            );
            builder.withCredentials(new AWSStaticCredentialsProvider(creds));
        }
        // else: Default Credentials Provider Chain (IAM Role, env, profile 등)

        return builder.build();
    }

    /**
     * ✨ AmazonS3 빈이 있을 때만 S3Client 생성
     * - 모듈 스위치/자격증명 오류 등으로 S3 빈이 없으면 클라이언트도 생성되지 않음
     */
    @Bean
    @ConditionalOnBean(AmazonS3.class)
    @ConditionalOnMissingBean(S3Client.class)
    public S3Client s3Client(AmazonS3 amazonS3) {
        return new S3Client(amazonS3);
    }
}
