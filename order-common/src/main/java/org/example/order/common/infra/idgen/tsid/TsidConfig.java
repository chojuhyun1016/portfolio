package org.example.order.common.infra.idgen.tsid;

import com.github.f4b6a3.tsid.TsidFactory;
import com.google.common.hash.Hashing;
import lombok.extern.slf4j.Slf4j;
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

/**
 * TSID 설정 클래스 (공통)
 * - 순수 TsidFactory만 제공 (Hibernate 등 Infra 의존 없음)
 */
@Slf4j
@Configuration
public class TsidConfig {

    private static final int DEFAULT_NODE_BITS = 10;

    @Bean
    public TsidFactory tsidFactory() {
        int nodeBits = getNodeBits();
        int nodeId = getNodeId();
        ZoneId zone = getZone();

        log.info("Initializing TSID: nodeBits={}, nodeId={}, zone={}", nodeBits, nodeId, zone);

        return TsidFactory.builder()
                .withNodeBits(nodeBits)
                .withNode(nodeId)
                .withClock(Clock.system(zone))
                .withRandom(new SecureRandom())
                .build();
    }

    private int getNodeBits() {
        return Integer.parseInt(System.getenv().getOrDefault("ENV_NODE_BITS", String.valueOf(DEFAULT_NODE_BITS)));
    }

    private int getNodeId() {
        try {
            String instanceId = fetchInstanceId();
            String podName = System.getenv("HOSTNAME");

            if (instanceId == null || instanceId.isEmpty()) {
                instanceId = randomId();
            }
            if (podName == null || podName.isEmpty()) {
                podName = randomId();
            }

            log.info("Node ID source -> instanceId: {}, podName: {}", instanceId, podName);

            int instanceHash = murmurHash(instanceId) & 0x3FF;
            int podHash = murmurHash(podName) & 0x3FF;

            return (instanceHash ^ podHash) & 0x3FF;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Node ID", e);
        }
    }

    private ZoneId getZone() {
        String envTz = System.getenv("ENV_TZ");
        return envTz != null ? ZoneId.of(envTz) : ZoneId.systemDefault();
    }

    private String fetchInstanceId() {
        try {
            String tokenUrl = "http://169.254.169.254/latest/api/token";
            String instanceUrl = "http://169.254.169.254/latest/meta-data/instance-id";

            HttpURLConnection tokenConn = (HttpURLConnection) new java.net.URL(tokenUrl).openConnection();
            tokenConn.setRequestMethod("PUT");
            tokenConn.setRequestProperty("X-aws-ec2-metadata-token-ttl-seconds", "21600");
            tokenConn.setConnectTimeout(2000);
            tokenConn.setReadTimeout(2000);

            String token = null;
            if (tokenConn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(tokenConn.getInputStream(), StandardCharsets.UTF_8))) {
                    token = reader.readLine();
                }
            }

            if (token != null) {
                HttpURLConnection conn = (HttpURLConnection) new java.net.URL(instanceUrl).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("X-aws-ec2-metadata-token", token);
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);

                if (conn.getResponseCode() == 200) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        return reader.readLine();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch EC2 instance ID, using fallback random ID");
        }
        return null;
    }

    private String randomId() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new SecureRandom();
        StringBuilder builder = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            builder.append(chars.charAt(random.nextInt(chars.length())));
        }
        return builder.toString();
    }

    private int murmurHash(String input) {
        return Hashing.murmur3_32_fixed()
                .hashString(input, StandardCharsets.UTF_8)
                .asInt();
    }
}
