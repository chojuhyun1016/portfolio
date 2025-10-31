package org.example.order.batch.service.synchronize.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CreateBucketRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.s3.properties.S3Properties;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.batch.service.synchronize.S3LogSyncService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * S3LogSyncServiceImpl
 * - @PostConstruct에서 S3 버킷/프리픽스 부트스트랩 수행 (AmazonS3 직접 사용)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true")
public class S3LogSyncServiceImpl implements S3LogSyncService {

    private final ObjectProvider<AmazonS3> amazonS3Provider;
    private final S3Properties s3Properties;
    private final Environment env;

    @PostConstruct
    void onReady() {
        AmazonS3 s3 = amazonS3Provider.getIfAvailable();

        if (s3 == null) {
            throw new IllegalStateException("aws.s3.enabled=true 인데 AmazonS3 빈이 없습니다. S3AutoConfiguration/프로퍼티를 확인하세요.");
        }

        final String bucket = s3Properties.getS3().getBucket();
        final boolean autoCreate = s3Properties.getS3().isAutoCreate();
        final boolean createPrefixPlaceholder = s3Properties.getS3().isCreatePrefixPlaceholder();
        final String defaultFolder = s3Properties.getS3().getDefaultFolder();

        final String configuredRegion = safeTrim(s3Properties.getRegion());

        if (configuredRegion == null || configuredRegion.isBlank()) {
            throw new IllegalStateException(
                    "[S3] region is required but not set. " +
                            "Set it in aws.yml (e.g. aws.region=ap-northeast-2)."
            );
        }

        if (!s3.doesBucketExistV2(bucket)) {
            if (autoCreate) {
                ensureBucketCreated(s3, bucket, configuredRegion);

                log.debug("[S3] bucket created: {}", bucket);
            } else {
                throw new IllegalStateException("[S3] bucket not found: " + bucket);
            }
        } else {
            log.debug("[S3] bucket exists: {}", bucket);
        }

        if (createPrefixPlaceholder && defaultFolder != null && !defaultFolder.isBlank()) {
            final String key = defaultFolder.endsWith("/") ? defaultFolder + ".keep" : defaultFolder + "/.keep";

            try {
                if (!s3.doesObjectExist(bucket, key)) {
                    s3.putObject(bucket, key, "placeholder");

                    log.debug("[S3] prefix placeholder created: s3://{}/{}", bucket, key);
                }
            } catch (Exception e) {
                log.warn("[S3] prefix placeholder create failed (skip). s3://{}/{}", bucket, key, e);
            }
        }

        log.info("[S3LogSyncService] initialized. region={}, activeProfiles={}",
                configuredRegion, String.join(",", env.getActiveProfiles()));
    }

    private void ensureBucketCreated(AmazonS3 s3, String bucket, String region) {
        final String normalized = region.trim();

        if ("us-east-1".equalsIgnoreCase(normalized)) {
            s3.createBucket(bucket);
        } else {
            CreateBucketRequest req = new CreateBucketRequest(bucket, normalized);
            s3.createBucket(req);
        }
    }

    private static String safeTrim(String v) {
        return v == null ? null : v.trim();
    }

    private AmazonS3 s3() {
        AmazonS3 s3 = amazonS3Provider.getIfAvailable();

        if (s3 == null) {
            throw new IllegalStateException("AmazonS3 not available.");
        }

        return s3;
    }

    @Override
    public synchronized void syncFileToS3(String bucketName, String bucketPath, Path filePath) {
        File source = filePath.toFile();
        String fileName = source.getName();
        String key = (bucketPath == null || bucketPath.isBlank()) ? fileName : (bucketPath + "/" + fileName);

        String instanceId = System.getenv("HOSTNAME");

        if (instanceId != null && !instanceId.isBlank() && !fileName.contains(instanceId)) {
            return;
        }

        Path snapshot = null;

        try {
            snapshot = createSnapshot(filePath);
            File snapFile = snapshot.toFile();

            String localChecksum = calculateChecksum(snapFile);
            String remoteChecksum = getRemoteChecksumByETag(bucketName, key);

            if (!remoteChecksum.isBlank() && remoteChecksum.equalsIgnoreCase(localChecksum)) {
                log.debug("skip upload (same checksum). key={}", key);

                return;
            }

            log.info("[S3LogSyncService] uploading snapshot -> s3://{}/{}", bucketName, key);

            s3().putObject(bucketName, key, snapFile);
        } catch (Exception e) {
            log.error("error : syncFileToS3 -> failed. bucket_name:{}, bucket_path:{}, file_path:{}",
                    bucketName, bucketPath, filePath, e);

            throw e;
        } finally {
            if (snapshot != null) {
                try {
                    Files.deleteIfExists(snapshot);
                } catch (IOException ignore) {
                }
            }
        }
    }

    private Path createSnapshot(Path source) {
        try {
            Path dir = source.getParent();
            Path uploadDir = dir.resolve(".upload");

            if (Files.notExists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String base = source.getFileName().toString();
            Path target = uploadDir.resolve(base + ".snapshot");
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

            return target;
        } catch (IOException io) {
            log.error("error : createSnapshot -> io. source={}", source, io);

            throw new CommonException(CommonExceptionCode.DATA_READ_ERROR);
        } catch (Exception e) {
            log.error("error : createSnapshot -> failed. source={}", source, e);

            throw e;
        }
    }

    private String calculateChecksum(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");

            try (var inputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
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
            log.error("error : calculateChecksum -> md5. file:{}", file, e);

            throw new CommonException(CommonExceptionCode.DATA_PARSING_ERROR);
        } catch (IOException e) {
            log.error("error : calculateChecksum -> read. file:{}", file, e);

            throw new CommonException(CommonExceptionCode.DATA_READ_ERROR);
        } catch (Exception e) {
            log.error("error : calculateChecksum -> failed. file:{}", file, e);

            throw e;
        }
    }

    private String getRemoteChecksumByETag(String bucket, String key) {
        try {
            if (!s3().doesObjectExist(bucket, key)) {
                return "";
            }

            ObjectMetadata meta = s3().getObjectMetadata(bucket, key);
            String etag = meta.getETag();

            if (etag == null || etag.isBlank() || etag.contains("-") || etag.length() != 32) {
                return "";
            }

            return etag;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404 || "NoSuchKey".equals(e.getErrorCode())) {
                return "";
            }

            log.error("error : getRemoteChecksumByETag -> meta. bucket:{}, key:{}", bucket, key, e);

            throw e;
        } catch (Exception e) {
            log.error("error : getRemoteChecksumByETag -> failed. bucket:{}, key:{}", bucket, key, e);

            throw e;
        }
    }
}
