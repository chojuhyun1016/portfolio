package org.example.order.client.s3.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * S3Client
 * <p>
 * 주요 포인트:
 * - AmazonS3 빈을 주입받아 파일 업로드/다운로드/메타데이터 조회 기능 제공
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
     * 파일 업로드 (메타데이터 포함) — 멀티파트/체크섬 비교 등 확장 시 활용
     */
    public void putObject(String bucketName, String key, File file, ObjectMetadata metadata) {
        try {
            PutObjectRequest req = new PutObjectRequest(bucketName, key, file);

            if (metadata != null) {
                req.setMetadata(metadata);
            }

            amazonS3.putObject(req);
        } catch (Exception e) {
            log.error("error : upload object with metadata failed", e);

            throw e;
        }
    }

    /**
     * 파일 다운로드
     */
    public S3Object getObject(String bucketName, String key) {
        return amazonS3.getObject(new GetObjectRequest(bucketName, key));
    }

    /**
     * 존재 여부
     */
    public boolean doesObjectExist(String bucketName, String key) {
        return amazonS3.doesObjectExist(bucketName, key);
    }

    /**
     * 메타데이터 조회(ETag 등)
     */
    public ObjectMetadata getObjectMetadata(String bucketName, String key) {
        return amazonS3.getObjectMetadata(bucketName, key);
    }
}
