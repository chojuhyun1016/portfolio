package org.example.order.client.s3.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Client {
    private final AmazonS3 amazonS3;

    public void putObject(String bucketName, String key, File file) {
        try {
            amazonS3.putObject(new PutObjectRequest(bucketName, key, file));
        } catch (Exception e) {
            log.error("error : upload object failed", e);
            throw e;
        }
    }

    public S3Object getObject(String bucketName, String key) {
        return amazonS3.getObject(new GetObjectRequest(bucketName, key));
    }
}
