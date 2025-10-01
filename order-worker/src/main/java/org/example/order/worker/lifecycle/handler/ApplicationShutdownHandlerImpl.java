package org.example.order.worker.lifecycle.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.s3.properties.S3Properties;
import org.example.order.worker.lifecycle.ApplicationShutdownHandler;
import org.example.order.worker.service.synchronize.S3LogSyncService;

import org.example.order.core.infra.common.secrets.manager.SecretsKeyResolver;
import org.example.order.core.infra.common.secrets.manager.SecretsLoader;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import java.io.IOException;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * ApplicationShutdownHandlerImpl
 * ------------------------------------------------------------------------
 * 목적
 * - 앱 종료 시 S3 로그 업로드(기존 로직 유지) 후,
 * Secrets/암호화 관련 리소스 정리(스케줄 취소, 클라이언트 close, 키 wipe).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(S3Properties.class)
@Profile({"local", "dev", "beta", "prod"})
public class ApplicationShutdownHandlerImpl implements ApplicationShutdownHandler, SmartLifecycle {

    private final S3Properties s3Properties;
    private final S3LogSyncService s3LogSyncEventService;

    // === 암호화/시크릿 정리용 의존성 (선택 주입) ===
    private final ObjectProvider<SecretsLoader> secretsLoaderProvider;
    private final ObjectProvider<SecretsManagerClient> secretsManagerClientProvider;
    private final ObjectProvider<SecretsKeyResolver> secretsKeyResolverProvider;

    private Boolean isRunning = false;

    @Value("${logging.file.path:logs}")
    private String LOG_DIRECTORY;

    /**
     * 애플리케이션 종료 시 S3로 로그 업로드 후 Crypto/Secrets 정리
     * - S3 기능 비활성 시 업로드 건너뜀
     * - 디렉터리 미존재 시 예외 없이 스킵
     * - 파일 단위 예외는 잡고 계속 진행
     */
    public void onShutdown() {
        // 1) S3: 디렉터리 존재 시 업로드
        if (s3Properties.getS3() == null || !s3Properties.getS3().isEnabled()) {
            log.info("application shutdown handler -> S3 비활성 상태로 동기화를 건너뜁니다.");
        } else {
            final String bucket = s3Properties.getS3().getBucket();
            final String folder = s3Properties.getS3().getDefaultFolder();
            final Path logDir = Paths.get(LOG_DIRECTORY);

            if (!Files.exists(logDir)) {
                log.warn("application shutdown handler -> 로그 디렉터리가 없어 업로드를 스킵합니다. file_path:{}, bucket_name:{}, bucket_path:{}",
                        LOG_DIRECTORY, bucket, folder);
            } else if (!Files.isDirectory(logDir)) {
                log.warn("application shutdown handler -> 지정 경로가 디렉터리가 아니어서 스킵합니다. file_path:{}, bucket_name:{}, bucket_path:{}",
                        LOG_DIRECTORY, bucket, folder);
            } else {
                AtomicLong success = new AtomicLong();
                AtomicLong failed = new AtomicLong();

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
        }

        // 2) Crypto/Secrets 정리 (빈이 있을 때만 수행)
        try {
            SecretsLoader loader = secretsLoaderProvider.getIfAvailable();

            if (loader != null) {
                loader.cancelSchedule();

                log.info("[Shutdown][Crypto] SecretsLoader schedule canceled.");
            } else {
                log.debug("[Shutdown][Crypto] SecretsLoader 없음 → 스킵");
            }
        } catch (Throwable t) {
            log.warn("[Shutdown][Crypto] cancelSchedule() 실패/스킵: {}", t.toString());
        }

        try {
            SecretsManagerClient client = secretsManagerClientProvider.getIfAvailable();
            if (client != null) {
                client.close();

                log.info("[Shutdown][Crypto] SecretsManagerClient closed.");
            } else {
                log.debug("[Shutdown][Crypto] SecretsManagerClient 없음 → 스킵");
            }
        } catch (Throwable t) {
            log.warn("[Shutdown][Crypto] SecretsManagerClient close 실패/스킵: {}", t.toString());
        }

        try {
            SecretsKeyResolver resolver = secretsKeyResolverProvider.getIfAvailable();

            if (resolver != null) {
                resolver.wipeAll();

                log.info("[Shutdown][Crypto] SecretsKeyResolver wiped & cleared.");
            } else {
                log.debug("[Shutdown][Crypto] SecretsKeyResolver 없음 → 스킵");
            }
        } catch (Throwable t) {
            log.warn("[Shutdown][Crypto] key wipe 실패/스킵: {}", t.toString());
        }
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
        return Integer.MAX_VALUE;
    }
}
