package org.example.order.worker.lifecycle.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.s3.properties.S3Properties;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(S3Properties.class)
@Profile({"local", "dev", "beta", "prod"})
public class ApplicationStartupHandlerImpl implements ApplicationStartupHandler, SmartLifecycle {

    private final S3Properties s3Properties;
    private final S3LogSyncService s3LogSyncEventService;

    private Boolean isRunning = false;

    @Value("${logging.file.path:logs}")
    private String LOG_DIRECTORY;

    /**
     * 애플리케이션 시작 시 기존 로그를 S3로 초기 동기화
     * - S3 기능 비활성 시 동작 건너뜀
     * - 로그 디렉터리가 없으면 생성 후(쓰기 경로 확보) 동기화는 스킵
     * - 파일 단위 예외는 잡고 계속 진행
     */
    public void onStartup() {
        if (s3Properties.getS3() == null || !s3Properties.getS3().isEnabled()) {
            log.info("application startup handler -> S3 비활성 상태로 동기화를 건너뜁니다.");

            return;
        }

        final String bucket = s3Properties.getS3().getBucket();
        final String folder = s3Properties.getS3().getDefaultFolder();

        final Path logDir = Paths.get(LOG_DIRECTORY);

        // 2) 로그 디렉터리 보장(없으면 생성)
        try {
            if (Files.notExists(logDir)) {
                Files.createDirectories(logDir);

                log.info("application startup handler -> 로그 디렉터리를 생성했습니다: {}", logDir.toAbsolutePath());
            } else if (!Files.isDirectory(logDir)) {
                log.warn("application startup handler -> 지정 경로가 디렉터리가 아닙니다. 스킵합니다. path: {}",
                        logDir.toAbsolutePath());

                return;
            }
        } catch (IOException e) {
            log.error("error : application startup handler -> 로그 디렉터리 준비 실패. file_path:{}, bucket_name:{}, bucket_path:{}",
                    LOG_DIRECTORY, bucket, folder, e);

            return;
        }

        // 3) 디렉터리 내 기존 파일이 있다면 업로드
        AtomicLong success = new AtomicLong();
        AtomicLong failed = new AtomicLong();

        try (Stream<Path> paths = Files.walk(logDir)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                try {
                    s3LogSyncEventService.syncFileToS3(bucket, folder, path);
                    success.incrementAndGet();
                } catch (Exception ex) {
                    failed.incrementAndGet();

                    log.error("application startup handler -> 업로드 실패. path:{}, bucket:{}, folder:{}",
                            path, bucket, folder, ex);
                }
            });

            log.info("Startup completed");
        } catch (IOException e) {
            log.error("error : application startup handler -> 파일 순회 실패. file_path:{}, bucket_name:{}, bucket_path:{}",
                    LOG_DIRECTORY, bucket, folder, e);
        } catch (Exception e) {
            log.error("error : application startup handler -> 알 수 없는 실패. file_path:{}, bucket_name:{}, bucket_path:{}",
                    LOG_DIRECTORY, bucket, folder, e);
        }

        log.info("application startup handler -> S3 초기 동기화 완료. 성공:{}건, 실패:{}건, 디렉터리:{}",
                success.get(), failed.get(), logDir.toAbsolutePath());
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
