package org.example.order.core.infra.common.idgen.tsid.factory;

import com.google.common.hash.Hashing;
import io.hypersistence.tsid.TSID;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Random;

/**
 * TSID Factory 제공자 클래스
 *
 * - Node ID를 EC2 Instance ID와 Kubernetes Pod 이름을 해시하여 자동 생성
 * - SecureRandom을 사용하여 충돌 최소화
 * - TimeZone 설정 지원 (ENV_TZ)
 */
@Slf4j
public class TsidFactoryProvider {

    // 기본 NodeBits (환경변수 ENV_NODE_BITS 없으면 10 사용)
    private static final int NODE_BITS = Integer.parseInt(System.getenv().getOrDefault("ENV_NODE_BITS", "10"));

    // 최초 한번 Factory 인스턴스 생성
    private static final TSID.Factory FACTORY = TSID.Factory.builder()
            .withNodeBits(NODE_BITS)
            .withNode(getNodeId()) // Node ID 동적 계산
            .withClock(Clock.system(getZone())) // TimeZone 적용
            .withRandom(new SecureRandom()) // SecureRandom 사용
            .build();

    private TsidFactoryProvider() {}

    /**
     * TSID Factory 반환
     */
    public static TSID.Factory getFactory() {
        return FACTORY;
    }

    /**
     * Node ID를 생성
     * - EC2 Instance ID + PodName을 murmurHash 후 XOR
     */
    private static int getNodeId() {
        try {
            String instanceId = fetchInstanceId();
            String podName = System.getenv("HOSTNAME");

            if (instanceId == null || instanceId.isEmpty()) instanceId = randomId();
            if (podName == null || podName.isEmpty()) podName = randomId();

            int instanceHash = murmurHash(instanceId) & 0x3FF; // 10bit
            int podHash = murmurHash(podName) & 0x3FF;

            return (instanceHash ^ podHash) & 0x3FF;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Node ID", e);
        }
    }

    /**
     * 시스템 TimeZone 반환
     */
    private static ZoneId getZone() {
        String envTz = System.getenv("ENV_TZ");
        return envTz != null ? ZoneId.of(envTz) : ZoneId.systemDefault();
    }

    /**
     * AWS EC2 인스턴스 ID 조회 (Metadata API v2)
     */
    private static String fetchInstanceId() {
        try {
            var tokenConnection = new java.net.URL("http://169.254.169.254/latest/api/token").openConnection();
            tokenConnection.setRequestProperty("X-aws-ec2-metadata-token-ttl-seconds", "21600");
            tokenConnection.setRequestProperty("Method", "PUT");
            tokenConnection.setConnectTimeout(2000);
            tokenConnection.setReadTimeout(2000);

            String token = new String(tokenConnection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            var connection = new java.net.URL("http://169.254.169.254/latest/meta-data/instance-id").openConnection();
            connection.setRequestProperty("X-aws-ec2-metadata-token", token);
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);

            return new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 랜덤 ID 생성 (16자)
     */
    private static String randomId() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        Random random = new SecureRandom();
        StringBuilder builder = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            builder.append(characters.charAt(random.nextInt(characters.length())));
        }
        return builder.toString();
    }

    /**
     * murmur3 해시 함수
     */
    private static int murmurHash(String input) {
        return Hashing.murmur3_32_fixed()
                .hashString(input, StandardCharsets.UTF_8)
                .asInt();
    }
}
