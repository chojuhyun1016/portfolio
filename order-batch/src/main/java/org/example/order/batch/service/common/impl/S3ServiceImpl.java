package org.example.order.batch.service.common.impl;

import com.amazonaws.services.s3.model.S3Object;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.batch.service.common.S3Service;
import org.example.order.client.s3.config.property.S3Properties;
import org.example.order.client.s3.service.S3Client;
import org.example.order.common.exception.code.CommonExceptionCode;
import org.example.order.common.exception.CommonException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.io.File;

@Slf4j
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(S3Properties.class)
public class S3ServiceImpl implements S3Service {
    private final S3Client s3Client;
    private final S3Properties s3Properties;

    @Override
    public void upload(String fileName, File file) {
        try {
            s3Client.putObject(s3Properties.getS3().getBucket(), String.format("%s/%s", s3Properties.getS3().getDefaultFolder(), fileName), file);
        } catch (Exception e) {
            log.error("error : fail to upload file", e);
            log.error(e.getMessage(), e);
            throw new CommonException(CommonExceptionCode.UPLOAD_FILE_TO_S3_ERROR);
        }
    }

    @Override
    public S3Object read(String key) {
        return s3Client.getObject(s3Properties.getS3().getBucket(), key);
    }
}
