package org.example.order.client.s3.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.s3.config.property.S3Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(S3Properties.class)
@RequiredArgsConstructor
public class S3Config {
    private final S3Properties s3Properties;

    @Bean
    public AmazonS3 amazonS3Client() {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withRegion(s3Properties.getRegion())
                .enablePathStyleAccess();

        if (s3Properties.getCredential().isEnabled()) {
            BasicAWSCredentials awsCredentials = new BasicAWSCredentials(s3Properties.getCredential().getAccessKey(), s3Properties.getCredential().getSecretKey());
            builder.withCredentials(new AWSStaticCredentialsProvider(awsCredentials));
        }

        return builder.build();
    }
}
