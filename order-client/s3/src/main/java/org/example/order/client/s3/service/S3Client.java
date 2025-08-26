package org.example.order.client.s3.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * S3Client
 * <p>
 * 주요 포인트:
 * - AmazonS3 빈을 주입받아 파일 업로드/다운로드 기능 제공
 * - Config(S3Config)에서 AmazonS3가 조건부로 생성될 때 함께 사용
 * - 예외 발생 시 로깅 후 그대로 전파
 */
@Slf4j
@RequiredArgsConstructor
public class S3Client {

    private final AmazonS3 amazonS3;

    /**
     * 파일 업로드
     */
    public void putObject(String bucketName, String key, File file) {
        try {
            amazonS3.putObject(new PutObjectRequest(bucketName, key, file));
        } catch (Exception e) {
            log.error("error : upload object failed", e);
            throw e;
        }
    }

    /**
     * 파일 다운로드
     */
    public S3Object getObject(String bucketName, String key) {
        return amazonS3.getObject(new GetObjectRequest(bucketName, key));
    }
}
