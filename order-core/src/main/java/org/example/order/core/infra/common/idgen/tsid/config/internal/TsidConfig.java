package org.example.order.core.infra.common.idgen.tsid.config.internal;

import com.github.f4b6a3.tsid.TsidFactory;
import com.google.common.hash.Hashing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.core.infra.common.idgen.tsid.TsidFactoryHolder;
import org.example.order.core.infra.common.idgen.tsid.config.property.TsidProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Random;

import org.example.order.domain.common.id.IdGenerator;

/**
 * TSID 설정 클래스 (내부)
 * - tsid.enabled=true 일 때만 동작(조건부)
 * - TsidFactory 를 생성하고, Hibernate 생성기에서 접근 가능하도록 정적 홀더에 등록
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(TsidProperties.class)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "tsid", name = "enabled", havingValue = "true")
public class TsidConfig {

    private final TsidProperties props;

    @Bean
    public TsidFactory tsidFactory() {
        int nodeBits = (props.getNodeBits() != null ? props.getNodeBits() : 10);
        int nodeId = resolveNodeId(nodeBits, props.isPreferEc2Meta());
        ZoneId zone = resolveZone(props.getZoneId());

        log.info("[TSID] Initialize TsidFactory: nodeBits={}, nodeId={}, zone={}", nodeBits, nodeId, zone);

        TsidFactory factory = TsidFactory.builder()
                .withNodeBits(nodeBits)
                .withNode(nodeId)
                .withClock(Clock.system(zone))
                .withRandom(new SecureRandom())
                .build();

        TsidFactoryHolder.set(factory);

        return factory;
    }

    @Bean
    public IdGenerator idGenerator(TsidFactory factory) {
        return () -> factory.create().toLong();
    }

    /**
     * 타임존 해석
     * - 잘못된 zoneId 가 들어오면 시스템 기본으로 대체
     */
    private ZoneId resolveZone(String zoneId) {
        if (zoneId == null || zoneId.isBlank()) return ZoneId.systemDefault();
        try {
            return ZoneId.of(zoneId);
        } catch (Exception e) {
            log.warn("[TSID] Invalid zone-id '{}' → fallback to system default", zoneId);

            return ZoneId.systemDefault();
        }
    }

    /**
     * 노드 ID 계산
     * - EC2 instance-id(옵션) + HOSTNAME 의 murmur32 해시값을 XOR → 하위 nodeBits 마스크 적용
     * - 모든 소스가 불가하면 랜덤 폴백
     */
    private int resolveNodeId(int nodeBits, boolean preferEc2) {
        final int mask = (nodeBits >= 31 ? -1 : ((1 << nodeBits) - 1));

        try {
            String instanceId = (preferEc2 ? fetchEc2InstanceId() : null);
            String podName = System.getenv("HOSTNAME");

            if (instanceId == null || instanceId.isEmpty()) {
                instanceId = randomId();
            }

            if (podName == null || podName.isEmpty()) {
                podName = randomId();
            }

            log.info("[TSID] Node ID sources → instanceId: {}, podName: {}", instanceId, podName);

            int instanceHash = murmur32(instanceId);
            int podHash = murmur32(podName);

            return (instanceHash ^ podHash) & mask;
        } catch (Exception e) {
            log.warn("[TSID] Failed to compute node-id, fallback to random", e);
            return murmur32(randomId()) & mask;
        }
    }

    /**
     * murmur3 32-bit 해시
     */
    private int murmur32(String input) {
        return Hashing.murmur3_32_fixed().hashString(input, StandardCharsets.UTF_8).asInt();
    }

    /**
     * 랜덤 ID(16자) 생성
     */
    private String randomId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new SecureRandom();
        StringBuilder b = new StringBuilder(16);

        for (int i = 0; i < 16; i++) {
            b.append(chars.charAt(random.nextInt(chars.length())));
        }

        return b.toString();
    }

    /**
     * EC2 IMDSv2 기반 instance-id 취득
     * - 로컬/비EC2 환경에서는 실패/무시가 정상
     */
    private String fetchEc2InstanceId() {
        try {
            String tokenUrl = "http://169.254.169.254/latest/api/token";
            String instanceUrl = "http://169.254.169.254/latest/meta-data/instance-id";

            HttpURLConnection tokenConn = (HttpURLConnection) new java.net.URL(tokenUrl).openConnection();
            tokenConn.setRequestMethod("PUT");
            tokenConn.setRequestProperty("X-aws-ec2-metadata-token-ttl-seconds", "21600");
            tokenConn.setConnectTimeout(1500);
            tokenConn.setReadTimeout(1500);

            String token = null;

            if (tokenConn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(tokenConn.getInputStream(), StandardCharsets.UTF_8))) {
                    token = reader.readLine();
                }
            }

            if (token != null && !token.isBlank()) {
                HttpURLConnection conn = (HttpURLConnection) new java.net.URL(instanceUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("X-aws-ec2-metadata-token", token);
                conn.setConnectTimeout(1500);
                conn.setReadTimeout(1500);

                if (conn.getResponseCode() == 200) {
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        return reader.readLine();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[TSID] EC2 metadata is not available (local/non-EC2 environment is OK): {}", e.toString());
        }

        return null;
    }
}
