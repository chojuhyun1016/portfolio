package org.example.order.client.s3.autoconfig;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.s3.properties.S3Properties;
import org.example.order.client.s3.service.S3Client;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * S3AutoConfiguration
 * - AmazonS3, S3Client 빈만 생성한다. (부트스트랩 빈 없음)
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(S3Properties.class)
@ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true")
public class S3AutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AmazonS3 amazonS3(S3Properties props) {
        final String endpoint = props.getEndpoint();
        final String region = props.getRegion();
        final boolean credEnabled = props.getCredential().isEnabled();
        final String accessKey = props.getCredential().getAccessKey();
        final String secretKey = props.getCredential().getSecretKey();

        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();

        if (endpoint != null && !endpoint.isBlank()) {
            builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                    .withPathStyleAccessEnabled(true);
        } else {
            builder.withRegion(region);
        }

        if (credEnabled) {
            builder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)));
        }

        builder.withClientConfiguration(new ClientConfiguration());

        AmazonS3 s3 = builder.build();

        log.debug("[S3 AutoConfig] AmazonS3 bean created. endpoint={}, region={}", endpoint, region);

        return s3;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AmazonS3.class)
    public S3Client s3Client(AmazonS3 amazonS3) {
        S3Client client = new S3Client(amazonS3);

        log.debug("[S3 AutoConfig] S3Client wrapper bean created.");

        return client;
    }
}
