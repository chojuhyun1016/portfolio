package org.example.order.core.idgen.annotation;

import com.google.common.hash.Hashing;
import io.hypersistence.tsid.TSID;
import org.example.order.core.idgen.generator.CustomTsidGenerator;
import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Random;
import java.util.function.Supplier;

@IdGeneratorType(CustomTsidGenerator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface CustomTsid {
    Class<? extends Supplier<TSID.Factory>> value() default FactorySupplier.class;

    public static class FactorySupplier implements Supplier<TSID.Factory> {
        private TSID.Factory tsidFactory;
        public static final FactorySupplier INSTANCE = new FactorySupplier();

        private FactorySupplier() {
            this.tsidFactory = TSID.Factory.builder()
                    .withNodeBits(getNodeBits())
                    .withNode(getNodeId())
                    .withClock(Clock.system(System.getenv("ENV_TZ") == null ? ZoneId.systemDefault() : ZoneId.of(System.getenv("ENV_TZ"))))
                    .withRandom(new SecureRandom())
                    .build();
        }

        public TSID.Factory get() {
            return this.tsidFactory;
        }

        public synchronized Long generate() {
            return this.tsidFactory.generate().toLong();
        }

        private int getNodeBits() {
            return System.getenv("ENV_NODE_BITS") == null ? 10 : Integer.parseInt(System.getenv("ENV_NODE_BITS"));
        }

        private int getNodeId() throws RuntimeException {
            try {
                String instanceId = fetchInstanceId();
                String podName = System.getenv("HOSTNAME");

                System.out.println("CustomTsid instanceId : " + instanceId);
                System.out.println("CustomTsid podName : " + podName);

                // EC2 Instance ID를 가져오지 못한 경우 랜덤 값 사용
                if (instanceId == null || instanceId.isEmpty()) {
                    instanceId = generateRandomId();
                }

                // Pod 이름을 가져오지 못한 경우 랜덤 값 사용
                if (podName == null || podName.isEmpty()) {
                    podName = generateRandomId();
                }

                int instanceHash = getMurmurHash(instanceId) & 0x3FF;   // 상위 10비트 마스킹
                int podHash = getMurmurHash(podName) & 0x3FF;   // 하위 10비트 마스킹

                // 10비트 머신 ID 생성 : XOR 연산으로 독립성 유지하면서 비트 결합
                return (instanceHash ^ podHash) & 0x3FF;
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate node ID", e);
            }

        }

//        AWS EC2 메타데이터 API(AWS EC2 메타데이터 v1)를 통해 Instance ID 조회
//        private String fetchInstanceId() {
//            try {
//                String metadataUrl = "http://169.254.169.254/latest/meta-data/instance-id";
//                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(metadataUrl).openConnection();
//                connection.setRequestMethod("GET");
//                connection.setConnectTimeout(2000);
//                connection.setReadTimeout(2000);
//
//                if (connection.getResponseCode() == 200) {
//                    try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
//                        return reader.readLine();
//                    }
//                }
//            } catch (Exception e) {
//                // 실패시 null 반환
//            }
//
//            return null;
//        }

        // AWS EC2 메타데이터 API(AWS EC2 메타데이터 v2)를 통해 Instance ID 조회
        // Nitro 기반 인스턴스와 메타데이터 v2를 사용하는 경우, 세션 토큰 요청 단계 추가
        private String fetchInstanceId() {
            try {
                String baseUrl = "http://169.254.169.254/latest/meta-data/instance-id";
                String tokenUrl = "http://169.254.169.254/latest/api/token";

                // 1. 세션 토큰 요청
                java.net.HttpURLConnection tokenConnection = (java.net.HttpURLConnection) new java.net.URL(tokenUrl).openConnection();
                tokenConnection.setRequestMethod("PUT");
                tokenConnection.setRequestProperty("X-aws-ec2-metadata-token-ttl-seconds", "21600"); // TTL 설정
                tokenConnection.setConnectTimeout(2000);
                tokenConnection.setReadTimeout(2000);

                String token = null;

                if (tokenConnection.getResponseCode() == 200) {
                    try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(tokenConnection.getInputStream(), StandardCharsets.UTF_8))) {
                        token = reader.readLine();
                    }
                }

                // 2. 메타데이터 요청
                if (token != null) {
                    java.net.HttpURLConnection connection = (java.net.HttpURLConnection) new java.net.URL(baseUrl).openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("X-aws-ec2-metadata-token", token); // 세션 토큰 추가
                    connection.setConnectTimeout(2000);
                    connection.setReadTimeout(2000);

                    if (connection.getResponseCode() == 200) {
                        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                            return reader.readLine();
                        }
                    }
                }
            } catch (Exception e) {
                // 실패 시 null 반환
            }

            return null;
        }

        private String generateRandomId() {
            int length = 16;
            String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
            StringBuilder randomId = new StringBuilder(length);
            Random random = new SecureRandom();

            for (int i = 0; i < length; i++) {
                randomId.append(characters.charAt(random.nextInt(characters.length())));
            }

            return randomId.toString();
        }

//        # SHA-256 사용:
//        - 고유성이 가장 중요하고 해시 충돌 가능성을 극도로 줄여야 하는 환경에 적합
//	      - 계산 비용이 높으므로 고성능이 필요한 경우 부적합
//        private int getSha256Hash(String input) {
//            try {
//                MessageDigest digest = MessageDigest.getInstance("SHA-256");
//                byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
//                // 해시의 첫 4바이트를 정수로 변환
//                return ((hashBytes[0] & 0xFF) << 24)
//                        | ((hashBytes[1] & 0xFF) << 16)
//                        | ((hashBytes[2] & 0xFF) << 8)
//                        | (hashBytes[3] & 0xFF);
//            } catch (NoSuchAlgorithmException e) {
//                throw new RuntimeException("SHA-256 algorithm not available", e);
//            }
//        }

//        # MurmurHash 사용:
//        - 높은 성능과 적절한 고유성을 제공, 대부분의 TSID 생성 환경에 적합
//        -	EC2 인스턴스와 Pod 에서 동작하는 TSID 환경에 최적

        private int getMurmurHash(String input) {
            return Hashing.murmur3_32_fixed()
                    .hashString(input, StandardCharsets.UTF_8)
                    .asInt();
        }
    }
}