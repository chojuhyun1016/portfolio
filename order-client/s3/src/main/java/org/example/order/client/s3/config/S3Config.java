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
 * AmazonS3 Client 설정
 *
 * - aws.endpoint 가 지정된 경우: LocalStack/프록시 환경
 * - aws.credential.enabled=true 인 경우: accessKey/secretKey 강제 주입
 * - 기본은 IAM Role 기반 인증 (EC2/EKS/ECS)
 */
@Configuration
@EnableConfigurationProperties(S3Properties.class)
@RequiredArgsConstructor
public class S3Config {
    private final S3Properties props;

    @Bean
    public AmazonS3 amazonS3Client() {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withRegion(props.getRegion())
                .enablePathStyleAccess();

        // 로컬/프록시 환경일 경우 endpoint 설정
        if (props.getEndpoint() != null && !props.getEndpoint().isBlank()) {
            builder.withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(
                            props.getEndpoint(),
                            props.getRegion()
                    )
            );
        }

        // AccessKey/SecretKey 인증 사용
        if (props.getCredential().isEnabled()) {
            BasicAWSCredentials awsCredentials =
                    new BasicAWSCredentials(props.getCredential().getAccessKey(),
                            props.getCredential().getSecretKey());
            builder.withCredentials(new AWSStaticCredentialsProvider(awsCredentials));
        }

        return builder.build();
    }
}
