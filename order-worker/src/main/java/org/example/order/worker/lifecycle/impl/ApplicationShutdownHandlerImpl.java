package org.example.order.worker.lifecycle.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.s3.properties.S3Properties;
import org.example.order.worker.lifecycle.ApplicationShutdownHandler;
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
public class ApplicationShutdownHandlerImpl implements ApplicationShutdownHandler, SmartLifecycle {

    private final S3Properties s3Properties;
    private final S3LogSyncService s3LogSyncEventService;

    private Boolean isRunning = false;

    @Value("${logging.file.path:logs}")
    private String LOG_DIRECTORY;

    /**
     * 애플리케이션 종료 시 S3로 로그 업로드
     * - S3 기능 비활성 시 동작 건너뜀
     * - 로그 디렉터리 미존재 시 예외 없이 스킵
     * - 파일 단위 예외는 잡고 계속 진행
     */
    public void onShutdown() {
        // 1) S3 사용 여부 확인 (enabled 가 boolean → Lombok 게터는 isEnabled())
        if (s3Properties.getS3() == null || !s3Properties.getS3().isEnabled()) {
            log.info("application shutdown handler -> S3 비활성 상태로 동기화를 건너뜁니다.");

            return;
        }

        final String bucket = s3Properties.getS3().getBucket();
        final String folder = s3Properties.getS3().getDefaultFolder();

        final Path logDir = Paths.get(LOG_DIRECTORY);

        if (!Files.exists(logDir)) {
            log.warn("application shutdown handler -> 로그 디렉터리가 없어 업로드를 스킵합니다. file_path:{}, bucket_name:{}, bucket_path:{}",
                    LOG_DIRECTORY, bucket, folder);

            return;
        }
        if (!Files.isDirectory(logDir)) {
            log.warn("application shutdown handler -> 지정 경로가 디렉터리가 아니어서 스킵합니다. file_path:{}, bucket_name:{}, bucket_path:{}",
                    LOG_DIRECTORY, bucket, folder);

            return;
        }

        AtomicLong success = new AtomicLong();
        AtomicLong failed = new AtomicLong();

        // 2) 디렉터리 순회 및 업로드
        try (Stream<Path> paths = Files.walk(logDir)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                try {
                    s3LogSyncEventService.syncFileToS3(bucket, folder, path);
                    success.incrementAndGet();
                } catch (Exception ex) {
                    failed.incrementAndGet();

                    log.error("application shutdown handler -> 업로드 실패. path:{}, bucket:{}, folder:{}",
                            path, bucket, folder, ex);
                }
            });
        } catch (IOException e) {
            log.error("error : application shutdown handler -> 파일 순회 실패. file_path:{}, bucket_name:{}, bucket_path:{}",
                    LOG_DIRECTORY, bucket, folder, e);
        } catch (Exception e) {
            log.error("error : application shutdown handler -> 알 수 없는 실패. file_path:{}, bucket_name:{}, bucket_path:{}",
                    LOG_DIRECTORY, bucket, folder, e);
        }

        log.info("application shutdown handler -> S3 동기화 완료. 성공:{}건, 실패:{}건, 디렉터리:{}",
                success.get(), failed.get(), logDir.toAbsolutePath());
    }

    @Override
    public void start() {
        this.isRunning = true;
    }

    @Override
    public void stop() {
        onShutdown();
        this.isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    @Override
    public int getPhase() {
        // 기존 정책 유지
        return Integer.MIN_VALUE;
    }
}
