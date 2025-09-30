package org.example.order.core.infra.common.secrets.manager;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.support.json.ObjectMapperUtils;
import org.example.order.core.infra.common.secrets.listener.SecretKeyRefreshListener;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpec;
import org.example.order.core.infra.common.secrets.model.CryptoKeySpecEntry;
import org.example.order.core.infra.common.secrets.config.properties.SecretsManagerProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import software.amazon.awssdk.core.exception.SdkServiceException;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.CreateSecretRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.PutSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.ResourceExistsException;
import software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * [요약]
 * - SecretsManager에서 JSON을 읽어 alias -> (객체/배열) 모두 수용.
 * - 각 항목을 CryptoKeySpecEntry로 정규화하여 Resolver에 스냅샷 반영.
 * - 로딩 후 SecretKeyRefreshListener들에게 통지(Initializer가 핀 정책 적용).
 * <p>
 * [핵심 포인트]
 * - LocalStack 엔드포인트일 때 Secret 없으면 "{}"로 부트스트랩.
 * - 실패 처리: 운영은 fail-fast, 로컬은 완화.
 */
@Slf4j
public class SecretsLoader {

    private final SecretsManagerProperties props;
    private final SecretsKeyResolver resolver;
    private final SecretsManagerClient client;
    private final List<SecretKeyRefreshListener> listeners;

    @Nullable
    private final TaskScheduler scheduler;

    private final AtomicBoolean scheduled = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> future;

    public SecretsLoader(SecretsManagerProperties props, SecretsKeyResolver resolver,
                         SecretsManagerClient client, List<SecretKeyRefreshListener> listeners,
                         @Nullable TaskScheduler scheduler) {
        this.props = props;
        this.resolver = resolver;
        this.client = client;
        this.listeners = listeners;
        this.scheduler = scheduler;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            refreshOnce();

            log.info("SecretsLoader 초기 로드 완료");
        } catch (Exception e) {
            log.error("SecretsLoader 초기 로드 실패", e);

            if (props.getSecretsManager().isFailFast() && !isLocalstack()) {
                throw new IllegalStateException("SecretsLoader 초기 로드 실패", e);
            } else {
                log.warn("LocalStack으로 간주, 기동 지속");
            }
        }

        if (props.getSecretsManager().isSchedulerEnabled() && scheduler != null && scheduled.compareAndSet(false, true)) {
            long d = Math.max(1_000L, props.getSecretsManager().getRefreshIntervalMillis());

            future = scheduler.scheduleWithFixedDelay(this::safeRefresh, Duration.ofMillis(d));

            log.info("SecretsLoader 주기 갱신 등록: {} ms", d);
        }
    }

    public void refreshNowForTest() {
        try {
            refreshOnce();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void safeRefresh() {
        try {
            refreshOnce();
        } catch (Exception e) {
            log.error("주기 갱신 실패", e);
        }
    }

    private void refreshOnce() throws Exception {
        final String secretName = props.getSecretsManager().getSecretName();

        if (secretName == null || secretName.isBlank()) {
            String msg = "aws.secrets-manager.secret-name 비어있음";

            if (props.getSecretsManager().isFailFast() && !isLocalstack()) {
                throw new IllegalStateException(msg);
            }

            log.warn("{} -> 건너뜀", msg);

            return;
        }

        String secretJson;

        try {
            GetSecretValueResponse r = client.getSecretValue(GetSecretValueRequest.builder().secretId(secretName).build());
            secretJson = r.secretString();
        } catch (ResourceNotFoundException rnfe) {
            if (isLocalstack()) {
                bootstrapSecretIfMissing(secretName);
                secretJson = client.getSecretValue(GetSecretValueRequest.builder().secretId(secretName).build()).secretString();
            } else {
                throw rnfe;
            }
        }

        if (secretJson == null || secretJson.isBlank()) {
            log.warn("Secret 빈 문자열 -> 파싱 생략");

            return;
        }

        // 1) JSON -> Map<String, Object> (alias → object|array)
        Map<String, Object> raw = ObjectMapperUtils.readValue(secretJson, new TypeReference<>() {
        });

        for (Map.Entry<String, Object> e : raw.entrySet()) {
            String alias = e.getKey();
            Object val = e.getValue();

            // 객체/배열 모두 수용
            List<CryptoKeySpec> specs;

            if (val instanceof Map) {
                specs = List.of(ObjectMapperUtils.valueToObject(val, CryptoKeySpec.class));
            } else if (val instanceof List) {
                specs = new ArrayList<>();

                for (Object o : (List<?>) val) {
                    specs.add(ObjectMapperUtils.valueToObject(o, CryptoKeySpec.class));
                }
            } else {
                log.warn("알 수 없는 secret value 타입: alias={}, type={}", alias, val == null ? "null" : val.getClass());

                continue;
            }

            // 2) 정규화
            List<CryptoKeySpecEntry> entries = new ArrayList<>();

            for (CryptoKeySpec s : specs) {
                entries.add(new CryptoKeySpecEntry(
                        alias,
                        s.getKid(),
                        s.getVersion(),
                        s.getAlgorithm(),
                        s.decodeKey()
                ));
            }

            // 3) 스냅샷 적용
            resolver.setSnapshot(alias, entries);
        }

        // 4) 리스너 통지 -> Initializer가 핀 정책 적용
        if (listeners != null) {
            for (SecretKeyRefreshListener l : listeners) {
                try {
                    l.onSecretKeyRefreshed();
                } catch (Exception ex) {
                    log.error("리스너 실패: {}", l.getClass().getSimpleName(), ex);
                }
            }
        }

        log.info("SecretsLoader 로드 성공: alias 수={}", raw.size());
    }

    private void bootstrapSecretIfMissing(String secretName) {
        final String initial = "{}";

        try {
            client.createSecret(CreateSecretRequest.builder().name(secretName).secretString(initial).build());

            log.info("Secret 생성(LocalStack): {}", secretName);
        } catch (ResourceExistsException exists) {
            client.putSecretValue(PutSecretValueRequest.builder().secretId(secretName).secretString(initial).build());

            log.info("Secret 초기화(LocalStack put): {}", secretName);
        } catch (SdkServiceException sse) {
            log.warn("Secret 생성 경고(LocalStack): {}", sse.getMessage());
        }
    }

    private boolean isLocalstack() {
        String ep = props.getEndpoint();

        if (ep == null || ep.isBlank()) {
            return false;
        }

        try {
            URI u = URI.create(ep);
            String h = u.getHost() == null ? "" : u.getHost();

            return h.equals("localhost") || h.equals("127.0.0.1");
        } catch (Exception ignore) {
            return false;
        }
    }

    public void cancelSchedule() {
        if (future != null) {
            future.cancel(false);
        }
    }
}
