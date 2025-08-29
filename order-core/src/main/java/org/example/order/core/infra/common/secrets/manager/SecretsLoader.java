package org.example.order.core.infra.common.secrets.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.support.json.ObjectMapperUtils;
import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.example.order.core.infra.common.secrets.props.SecretsManagerProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AWS Secrets Manager에서 주기적으로 JSON 키셋을 가져와 파싱 + Resolver에 등록.
 * - @Component 금지(설정 파일에서 조건부로만 등록)
 * <p>
 * 변경 사항:
 * - 초기 1회 로드는 ApplicationReadyEvent에서 동기 실행
 * - 이후 반복 로드는 TaskScheduler.scheduleWithFixedDelay(...) 사용
 * (작업이 끝난 뒤 지정 지연 후 다음 실행 ⇒ "fixedDelay")
 */
@Slf4j
public class SecretsLoader {

    private final SecretsManagerProperties properties;
    private final SecretsKeyResolver secretsKeyResolver;
    private final SecretsManagerClient secretsManagerClient;
    private final List<SecretKeyRefreshListener> refreshListeners;
    private final TaskScheduler taskScheduler;

    // 스케줄 중복 등록 방지
    private final AtomicBoolean scheduled = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> scheduledFuture;

    public SecretsLoader(SecretsManagerProperties properties,
                         SecretsKeyResolver secretsKeyResolver,
                         SecretsManagerClient secretsManagerClient,
                         List<SecretKeyRefreshListener> refreshListeners,
                         TaskScheduler taskScheduler) {
        this.properties = properties;
        this.secretsKeyResolver = secretsKeyResolver;
        this.secretsManagerClient = secretsManagerClient;
        this.refreshListeners = refreshListeners;
        this.taskScheduler = taskScheduler;
    }

    /**
     * 애플리케이션 준비 완료 시점에:
     * 1) 초기 1회 로드(동기)
     * 2) 이후 fixedDelay 스케줄을 1회만 등록
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        // 1) 초기 1회 로드
        try {
            refreshOnce();
            log.info("[SecretsLoader] Initial secrets loaded on ApplicationReadyEvent.");
        } catch (Exception e) {
            log.error("[SecretsLoader] Initial secret loading failed.", e);
            if (properties.isFailFast()) {
                throw new IllegalStateException("Initial secret loading failed. Aborting startup.", e);
            }
        }

        // 2) 이후 반복 스케줄(중복 등록 방지)
        if (scheduled.compareAndSet(false, true)) {
            long delay = Math.max(1_000L, properties.getRefreshIntervalMillis());
            scheduledFuture = taskScheduler.scheduleWithFixedDelay(this::safeRefresh, Duration.ofMillis(delay));
            log.info("[SecretsLoader] Periodic refresh scheduled with fixedDelay={}ms.", delay);
        }
    }

    /**
     * 반복 스케줄의 안전 래퍼(예외가 스케줄 자체를 깨지 않도록)
     */
    private void safeRefresh() {
        try {
            refreshOnce();
        } catch (Exception e) {
            log.error("[SecretsLoader] Periodic secret refresh failed.", e);
        }
    }

    /**
     * 단발(1회) 갱신 로직
     */
    private void refreshOnce() throws Exception {
        var request = GetSecretValueRequest.builder()
                .secretId(properties.getSecretName())
                .build();

        var response = secretsManagerClient.getSecretValue(request);
        String secretJson = response.secretString();

        Map<String, CryptoKeySpec> parsedKeys = ObjectMapperUtils.readValue(
                secretJson,
                new TypeReference<Map<String, CryptoKeySpec>>() {
                }
        );

        parsedKeys.forEach((keyName, spec) -> {
            byte[] decoded = spec.decodeKey();
            int expectedBytes = spec.getKeySize() / 8;

            if (decoded.length != expectedBytes) {
                throw new IllegalArgumentException(
                        String.format("Invalid key size for [%s]: expected %d bytes, got %d",
                                keyName, expectedBytes, decoded.length)
                );
            }

            secretsKeyResolver.updateKey(keyName, spec);
        });

        if (refreshListeners != null) {
            for (SecretKeyRefreshListener listener : refreshListeners) {
                try {
                    listener.onSecretKeyRefreshed();

                    log.info("[SecretsLoader] SecretKeyRefreshListener [{}] notified.",
                            listener.getClass().getSimpleName());
                } catch (Exception ex) {
                    log.error("[SecretsLoader] Listener {} failed during onSecretKeyRefreshed: {}",
                            listener.getClass().getSimpleName(), ex.getMessage(), ex);
                }
            }
        }

        log.info("[SecretsLoader] Keys refreshed successfully. Total: {}", parsedKeys.size());
    }

    /**
     * (테스트/종료 훅에서 사용) 스케줄 해제 도우미
     */
    public void cancelSchedule() {
        ScheduledFuture<?> f = this.scheduledFuture;

        if (f != null) {
            f.cancel(false);
        }
    }
}
