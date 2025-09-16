package org.example.order.core.infra.common.secrets.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.support.json.ObjectMapperUtils;
import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.example.order.core.infra.common.secrets.props.SecretsManagerProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AWS Secrets Manager 에서 키 셋을 조회하여 Resolver 에 반영하는 구성요소.
 * aws.secrets-manager.enabled 가 true 일 때만 빈으로 등록된다.
 * scheduler 옵션이 켜져 있고 TaskScheduler 가 주입되면 주기 갱신을 등록한다.
 * <p>
 * ★ 변경 요약:
 * - SecretsManagerProperties 이너 블록(secretsManager) 게터 사용으로 변경
 * (failFast, schedulerEnabled, refreshIntervalMillis, secretName)
 */
@Slf4j
public class SecretsLoader {

    private final SecretsManagerProperties properties;
    private final SecretsKeyResolver secretsKeyResolver;
    private final SecretsManagerClient secretsManagerClient;
    private final List<SecretKeyRefreshListener> refreshListeners;

    /**
     * 선택적 스케줄러.
     * null 이면 초기 1회 로드만 수행한다.
     */
    @Nullable
    private final TaskScheduler taskScheduler;

    /**
     * 스케줄 중복 등록 방지용 플래그.
     */
    private final AtomicBoolean scheduled = new AtomicBoolean(false);

    /**
     * 등록된 스케줄 핸들.
     */
    private volatile ScheduledFuture<?> scheduledFuture;

    public SecretsLoader(SecretsManagerProperties properties,
                         SecretsKeyResolver secretsKeyResolver,
                         SecretsManagerClient secretsManagerClient,
                         List<SecretKeyRefreshListener> refreshListeners,
                         @Nullable TaskScheduler taskScheduler) {
        this.properties = properties;
        this.secretsKeyResolver = secretsKeyResolver;
        this.secretsManagerClient = secretsManagerClient;
        this.refreshListeners = refreshListeners;
        this.taskScheduler = taskScheduler;
    }

    /**
     * 애플리케이션 준비 완료 시 초기 1회 로드를 수행한다.
     * 이후 scheduler 설정과 스케줄러 주입 여부에 따라 주기 갱신을 등록한다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            refreshOnce();

            log.info("SecretsLoader 초기 로드 완료");
        } catch (Exception e) {
            log.error("SecretsLoader 초기 로드 실패", e);

            if (properties.getSecretsManager().isFailFast()) {
                throw new IllegalStateException("SecretsLoader 초기 로드 실패", e);
            }
        }

        if (properties.getSecretsManager().isSchedulerEnabled() && taskScheduler != null && scheduled.compareAndSet(false, true)) {
            long delay = Math.max(1_000L, properties.getSecretsManager().getRefreshIntervalMillis());

            scheduledFuture = taskScheduler.scheduleWithFixedDelay(this::safeRefresh, Duration.ofMillis(delay));

            log.info("SecretsLoader 주기 갱신 등록됨. 지연 밀리초 {}", delay);
        } else {
            log.info("SecretsLoader 주기 갱신 미등록. scheduler 비활성 또는 스케줄러 미제공");
        }
    }

    /**
     * 단위 테스트에서 이벤트 없이 직접 로드를 트리거하기 위한 메서드.
     * 스케줄 등록과는 무관하게 한 번만 수행한다.
     */
    public void refreshNowForTest() {
        try {
            refreshOnce();
        } catch (Exception e) {
            throw new IllegalStateException("SecretsLoader 수동 갱신 실패", e);
        }
    }

    /**
     * 주기 갱신용 안전 래퍼. 예외가 발생해도 스케줄 자체는 유지한다.
     */
    private void safeRefresh() {
        try {
            refreshOnce();
        } catch (Exception e) {
            log.error("SecretsLoader 주기 갱신 실패", e);
        }
    }

    /**
     * 실제 1회 로드 로직.
     * AWS Secrets Manager 에서 JSON 을 가져와 파싱하고 키 길이를 검증한 뒤 Resolver 에 반영한다.
     * 리스너가 있다면 알림을 보낸다.
     * <p>
     * ★ 변경: secretName 접근을 properties.getSecretsManager().getSecretName()으로 변경
     */
    private void refreshOnce() throws Exception {
        var request = GetSecretValueRequest.builder()
                .secretId(properties.getSecretsManager().getSecretName())
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
                        String.format("키 길이 오류. 이름 %s. 기대 바이트 %d. 실제 바이트 %d",
                                keyName, expectedBytes, decoded.length)
                );
            }

            secretsKeyResolver.updateKey(keyName, spec);
        });

        if (refreshListeners != null) {
            for (SecretKeyRefreshListener listener : refreshListeners) {
                try {
                    listener.onSecretKeyRefreshed();

                    log.info("SecretsLoader 리스너 알림 완료. 리스너 {}", listener.getClass().getSimpleName());
                } catch (Exception ex) {
                    log.error("SecretsLoader 리스너 알림 실패. 리스너 {}", listener.getClass().getSimpleName(), ex);
                }
            }
        }

        log.info("SecretsLoader 로드 성공. 총 개수 {}", parsedKeys.size());
    }

    /**
     * 등록된 스케줄이 있으면 해제한다.
     */
    public void cancelSchedule() {
        ScheduledFuture<?> f = this.scheduledFuture;

        if (f != null) {
            f.cancel(false);
        }
    }
}
