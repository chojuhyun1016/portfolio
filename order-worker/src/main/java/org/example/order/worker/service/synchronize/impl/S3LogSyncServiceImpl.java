package org.example.order.worker.service.synchronize.impl;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.s3.service.S3Client;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.worker.service.synchronize.S3LogSyncService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3LogSyncServiceImpl implements S3LogSyncService {

    private final S3Client s3Client;

    @Override
    public synchronized void syncFileToS3(String bucketName, String bucketPath, Path filePath) {
        try {
            File localFile = filePath.toFile();
            String fileName = localFile.getName();

            String key = bucketPath + "/" + filePath.getFileName().toString();

            // 본인 pod 로그 데이터만 동기화
            String instanceId = System.getenv("HOSTNAME");

            if (instanceId != null && !fileName.contains(instanceId)) {
                return;
            }

            // 로컬 파일 체크섬 계산
            String localChecksum = calculateChecksum(localFile);

            // S3 파일의 체크섬 가져오기
            String s3Checksum = getS3FileChecksum(bucketName, key);

            // 체크섬이 동일하면 동기화 생략
            if (localChecksum.equals(s3Checksum)) {
                return;
            }

            log.info("\"file\": \"{}\"", key);

            // S3에 파일 업로드
            s3Client.putObject(bucketName, key, localFile);
        }
        catch (Exception e) {
            log.error("error : syncFileToS3 -> failed. bucket_name:{}, bucket_path:{}, file_path:{}", bucketName, bucketPath, filePath);
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    private String calculateChecksum(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            try (var inputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[1024];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("error : calculateChecksum -> md5. file:{}", file);
            log.error(e.getMessage(), e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR);
        } catch (IOException e) {
            log.error("error : calculateChecksum -> read. file:{}", file);
            log.error(e.getMessage(), e);
            throw new CommonException(CommonExceptionCode.DATA_READ_ERROR);
        } catch (Exception e) {
            log.error("error : calculateChecksum -> failed. file:{}", file);
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    private String getS3FileChecksum(String bucketName, String key) {
        try {
            var s3Object = s3Client.getObject(bucketName, key);

            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = s3Object.getObjectContent().read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }

            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("error : getS3FileChecksum -> md5. bucket:{}, key:{}", bucketName, key);
            log.error(e.getMessage(), e);
            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR);
        } catch (IOException e) {
            log.error("error : getS3FileChecksum -> read. bucket:{}, key:{}", bucketName, key);
            log.error(e.getMessage(), e);
            throw new CommonException(CommonExceptionCode.DATA_READ_ERROR);
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404 || "NoSuchKey".equals(e.getErrorCode())) {
                // 키가 없을 경우 빈 문자열 반환
                return "";
            } else {
                log.error("S3 오류: bucket={}, key={}", bucketName, key, e);
                throw e; // 다른 오류는 다시 던짐
            }
        } catch (Exception e) {
            log.error("error : getS3FileChecksum -> failed. bucket:{}, key:{}", bucketName, key);
            log.error(e.getMessage(), e);
            throw e;
        }
    }
}
