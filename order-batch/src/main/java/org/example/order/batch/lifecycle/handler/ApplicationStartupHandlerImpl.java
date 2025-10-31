package org.example.order.batch.lifecycle.handler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.s3.properties.S3Properties;
import org.example.order.batch.service.synchronize.S3LogSyncService;
import org.example.order.batch.lifecycle.ApplicationStartupHandler;
import org.example.order.batch.crypto.selection.CryptoKeySelectionApplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * ApplicationStartupHandlerImpl
 * - S3 부트스트랩은 S3LogSyncServiceImpl @PostConstruct 에서 이미 완료됨
 * - 여기서는 SmartLifecycle로 시작 시점 동기화만 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(S3Properties.class)
@Profile({"local", "dev", "beta", "prod"})
@ConditionalOnProperty(prefix = "aws.s3", name = "enabled", havingValue = "true")
@ConditionalOnBean(S3LogSyncService.class)
public class ApplicationStartupHandlerImpl implements ApplicationStartupHandler, SmartLifecycle {

    private final S3Properties s3Properties;
    private final S3LogSyncService s3LogSyncEventService;
    private final ObjectProvider<CryptoKeySelectionApplier> cryptoKeySelectionApplierProvider;

    private volatile boolean running = false;

    @Value("${logging.file.path:logs}")
    private String LOG_DIRECTORY;

    @PostConstruct
    void onReady() {
        log.info("[StartupLifecycle] prepared (aws.s3.enabled=true).");
    }

    public void onStartup() {
        final String bucket = s3Properties.getS3().getBucket();
        final String folder = s3Properties.getS3().getDefaultFolder();
        final Path logDir = Paths.get(LOG_DIRECTORY);

        log.info("[Startup] begin. bucket={}, folder={}, logDir={}", bucket, folder, logDir.toAbsolutePath());

        try {
            if (Files.notExists(logDir)) {
                Files.createDirectories(logDir);

                log.info("[Startup] 로그 디렉터리 생성: {}", logDir.toAbsolutePath());
            } else if (!Files.isDirectory(logDir)) {
                log.warn("[Startup] 지정 경로가 디렉터리가 아님. 스킵. path: {}", logDir.toAbsolutePath());

                return;
            }
        } catch (FileSystemException fse) {
            log.warn("[Startup] 로그 디렉터리 생성 불가(읽기 전용/권한 등). 업로드 스킵. path:{}, cause={}",
                    logDir.toAbsolutePath(), fse.toString());

            return;
        } catch (IOException ioe) {
            log.warn("[Startup] 로그 디렉터리 준비 실패. 업로드 스킵. path:{}, cause={}",
                    logDir.toAbsolutePath(), ioe.toString());

            return;
        }

        AtomicLong success = new AtomicLong();
        AtomicLong failed = new AtomicLong();

        try (Stream<Path> paths = Files.walk(logDir)) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                try {
                    s3LogSyncEventService.syncFileToS3(bucket, folder, path);

                    success.incrementAndGet();
                } catch (Exception ex) {
                    failed.incrementAndGet();

                    log.error("[Startup] 업로드 실패. path:{}, bucket:{}, folder:{}", path, bucket, folder, ex);
                }
            });

            log.info("[Startup] S3 초기 동기화 완료. 성공:{}건, 실패:{}건, 디렉터리:{}",
                    success.get(), failed.get(), logDir.toAbsolutePath());
        } catch (IOException e) {
            log.error("[Startup] 파일 순회 실패. file_path:{}, bucket:{}, folder:{}",
                    logDir.toAbsolutePath(), bucket, folder, e);
        } catch (Exception e) {
            log.error("[Startup] 알 수 없는 실패. file_path:{}, bucket:{}, folder:{}",
                    logDir.toAbsolutePath(), bucket, folder, e);
        }

        try {
            CryptoKeySelectionApplier applier = cryptoKeySelectionApplierProvider.getIfAvailable();

            if (applier != null) {
                log.info("[Startup] 암호화 키 시딩은 리스너 경유로 처리(allowLatest=false).");
            } else {
                log.info("[Startup] CryptoKeySelectionApplier 미제공 -> 키 시딩 스킵");
            }
        } catch (Exception e) {
            log.error("[Startup] 암호화 키 시딩 실패(부팅 계속).", e);
        }

        log.info("[Startup] done.");
    }

    @Override
    public void start() {
        if (running) {
            return;
        }

        log.info("[Startup] SmartLifecycle.start() called.");

        try {
            onStartup();
        } finally {
            running = true;

            log.info("[Startup] SmartLifecycle.start() finished.");
        }
    }

    @Override
    public void stop() {
        running = false;

        log.info("[Startup] SmartLifecycle.stop() called.");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }
}
