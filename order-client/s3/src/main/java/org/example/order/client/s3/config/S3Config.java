package org.example.order.client.s3.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.RequiredArgsConstructor;
import org.example.order.client.s3.config.property.S3Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * S3Config
 * <p>
 * 주요 포인트:
 * - aws.endpoint 가 지정된 경우 → EndpointConfiguration만 설정 (LocalStack/프록시)
 * - endpoint 없으면 → Region 기반 설정 (실제 AWS)
 * - credential.enabled=true → AccessKey/SecretKey 직접 사용
 * - credential.enabled=false → IAM Role 등 기본 자격 증명 체인 사용
 */
@Configuration
@EnableConfigurationProperties(S3Properties.class)
@RequiredArgsConstructor
public class S3Config {
    private final S3Properties props;

    @Bean
    public AmazonS3 amazonS3Client() {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .enablePathStyleAccess(); // LocalStack 호환

        String endpoint = props.getEndpoint();
        if (endpoint != null && !endpoint.isBlank()) {
            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(endpoint, props.getRegion())
            );
        } else {
            builder.withRegion(props.getRegion());
        }

        if (props.getCredential() != null && props.getCredential().isEnabled()) {
            BasicAWSCredentials awsCredentials =
                    new BasicAWSCredentials(
                            props.getCredential().getAccessKey(),
                            props.getCredential().getSecretKey()
                    );

            builder.withCredentials(new AWSStaticCredentialsProvider(awsCredentials));
        }

        return builder.build();
    }
}
