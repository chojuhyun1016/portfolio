package org.example.order.batch.service.common.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.batch.service.common.FileService;
import org.example.order.batch.service.common.S3Service;
import org.example.order.common.support.json.ObjectMapperUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final S3Service s3Service;

    // 객체를 파일로 변환 후 S3 업로드
    @Override
    public void upload(String fileName, String suffix, Object object) {
        try {
            File file = convert(suffix, object);

            s3Service.upload(fileName, file);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    // 객체 → JSON 직렬화 → 임시 파일 변환
    private File convert(String suffix, Object object) throws IOException {
        File tempFile = File.createTempFile("tmp", suffix);

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            ObjectMapperUtils.writeValue(fos, object);
        }

        return tempFile;
    }
}
