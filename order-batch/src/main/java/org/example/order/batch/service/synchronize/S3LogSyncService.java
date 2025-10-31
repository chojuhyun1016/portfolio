package org.example.order.batch.service.synchronize;

import java.nio.file.Path;

public interface S3LogSyncService {
    void syncFileToS3(String bucketName, String bucketPath, Path filePath);
}
