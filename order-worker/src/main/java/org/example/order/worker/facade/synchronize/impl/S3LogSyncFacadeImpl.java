package org.example.order.worker.facade.synchronize.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.s3.config.property.S3Properties;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.worker.facade.synchronize.S3LogSyncFacade;
import org.example.order.worker.service.synchronize.S3LogSyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(S3Properties.class)
@Profile({"!local"})
public class S3LogSyncFacadeImpl implements S3LogSyncFacade {
    private final S3Properties s3Properties;

    private final S3LogSyncService s3LogSyncService;

    @Value("${logging.file.path:logs}")
    private String LOG_DIRECTORY;

    @Override
    public void

    run() {
        try (Stream<Path> paths = Files.walk(Paths.get(LOG_DIRECTORY))) {
            paths.filter(Files::isRegularFile).forEach(path -> s3LogSyncService.syncFileToS3(s3Properties.getS3().getBucket(), s3Properties.getS3().getDefaultFolder(), path));
        } catch (IOException e) {
            log.error("error : s3 log sync -> not found resource. file_path:{}, bucket_name:{}, bucket_path{}", LOG_DIRECTORY, s3Properties.getS3().getBucket(), s3Properties.getS3().getDefaultFolder());
            log.error(e.getMessage(), e);

            throw new CommonException(CommonExceptionCode.NOT_FOUND_RESOURCE);
        } catch (Exception e) {
            log.error("error : s3 log sync -> failed. file_path:{}, bucket_name:{}, bucket_path{}", LOG_DIRECTORY, s3Properties.getS3().getBucket(), s3Properties.getS3().getDefaultFolder());
            log.error(e.getMessage(), e);

            throw e;
        }
    }
}
