package org.example.order.batch.service.retry.service.common;

import com.amazonaws.services.s3.model.S3Object;

import java.io.File;

public interface S3Service {
    void upload(String fileName, File file);
    S3Object read(String fileName);
}
