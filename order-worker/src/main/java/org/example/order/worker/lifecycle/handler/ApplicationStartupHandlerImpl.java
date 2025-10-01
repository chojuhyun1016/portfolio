package org.example.order.worker.lifecycle.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.s3.properties.S3Properties;
import org.example.order.worker.lifecycle.ApplicationStartupHandler;
import org.example.order.worker.service.synchronize.S3LogSyncService;
import org.example.order.worker.crypto.selection.CryptoKeySelectionApplier;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
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
 * ------------------------------------------------------------------------
 * 목적
 * - 앱 시작 시 S3 초기 동기화(기존 로직 유지) 후,
 * Secrets 기반 암호화 키 시딩(CryptoKeySelectionApplier.applyAll(false), 선택 주입).
 * <p>
 * [핵심]
 * - 시작 순서: S3 초기화 -> (있다면) 암호화 키 시딩.
 * - ObjectProvider로 선택 주입: 비활성 환경에서도 안전.
 * - 운영 기본값: 자동 최신 금지(allowLatest=false), 설정된 kid/version만 주입.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(S3Properties.class)
@Profile({"local", "dev", "beta", "prod"})
public class ApplicationStartupHandlerImpl implements ApplicationStartupHandler, SmartLifecycle {

    private final S3Properties s3Properties;
    private final S3LogSyncService s3LogSyncEventService;

    private final ObjectProvider<CryptoKeySelectionApplier> cryptoKeySelectionApplierProvider;

    private volatile boolean running = false;

    @Value("${logging.file.path:logs}")
    private String LOG_DIRECTORY;

    /**
     * 애플리케이션 시작 시 S3 초기 동기화 후,
     * (있는 경우에만) Secrets -> EncryptorFactory 키 시딩(allowLatest=false) 수행.
     */
    public void onStartup() {
        // 1) S3 기능 비활성 시 동기화 스킵
        if (s3Properties.getS3() == null || !s3Properties.getS3().isEnabled()) {
            log.info("[Startup] S3 비활성 - 초기 동기화 스킵");
        } else {
            final String bucket = s3Properties.getS3().getBucket();
            final String folder = s3Properties.getS3().getDefaultFolder();
            final Path logDir = Paths.get(LOG_DIRECTORY);

            // 1-1) 로그 디렉터리 보장 (설정 경로만 사용, 폴백 없음)
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

            // 1-2) 기존 파일 업로드
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
        }

        // 2) 암호화 키 시딩(운영 기본: 자동 최신 금지) — 선택 주입
        try {
            CryptoKeySelectionApplier applier = cryptoKeySelectionApplierProvider.getIfAvailable();

            if (applier != null) {
                applier.applyAll(false);

                log.info("[Startup] 암호화 키 시딩 완료(allowLatest=false).");
            } else {
                log.info("[Startup] CryptoKeySelectionApplier 미제공 -> 키 시딩 스킵");
            }
        } catch (Exception e) {
            log.error("[Startup] 암호화 키 시딩 실패(부팅 계속).", e);
        }
    }

    @Override
    public void start() {
        onStartup();
        running = true;
    }

    @Override
    public void stop() {
        running = false;
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
