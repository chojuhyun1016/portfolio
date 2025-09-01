package org.example.order.worker.lifecycle.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.s3.config.property.S3Properties;
import org.example.order.worker.lifecycle.ApplicationStartupHandler;
import org.example.order.worker.service.synchronize.S3LogSyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
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
public class ApplicationStartupHandlerImpl implements ApplicationStartupHandler, SmartLifecycle {

    private final S3Properties s3Properties;
    private final S3LogSyncService s3LogSyncEventService;

    private Boolean isRunning = false;

    @Value("${logging.file.path:logs}")
    private String LOG_DIRECTORY;

    public void onStartup() {
        try (Stream<Path> paths = Files.walk(Paths.get(LOG_DIRECTORY))) {
            paths.filter(Files::isRegularFile).forEach(path -> s3LogSyncEventService.syncFileToS3(s3Properties.getS3().getBucket(), s3Properties.getS3().getDefaultFolder(), path));
            log.info("Startup completed");
        } catch (IOException e) {
            log.error("error : application startup handler -> not found resource. file_path:{}, bucket_name:{}, bucket_path{}", LOG_DIRECTORY, s3Properties.getS3().getBucket(), s3Properties.getS3().getDefaultFolder());
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error("error : application startup handler -> failed. file_path:{}, bucket_name:{}, bucket_path{}", LOG_DIRECTORY, s3Properties.getS3().getBucket(), s3Properties.getS3().getDefaultFolder());
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void start() {
        onStartup();
        this.isRunning = true;
    }

    @Override
    public void stop() {
        this.isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }
}
