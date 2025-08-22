//package org.example.order.core.infra.security.jwt;
//
//import org.example.order.core.infra.security.jwt.config.JwtConfigurationProperties;
//import org.example.order.core.infra.security.jwt.config.JwtKeyManualConfig;
//import org.example.order.core.infra.security.jwt.provider.JwtTokenManager;
//import org.example.order.core.infra.security.jwt.resolver.JwtKeyProvider;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.InitializingBean;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.context.annotation.Import;
//import org.springframework.context.annotation.Bean;
//import org.springframework.test.context.TestPropertySource;
//
//import javax.crypto.KeyGenerator;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
///**
// * 인메모리 키 구성(@Import) + 부팅시 키 주입(InitializingBean)으로
// * JwtTokenManager end-to-end 검증
// */
//@SpringBootTest
//@TestPropertySource(properties = {
//        // JwtTokenManager 만료 시간 설정
//        "jwt.access-token-validity-in-seconds=600",
//        "jwt.refresh-token-validity-in-seconds=3600",
//        // ✅ 수동 키 구성 ON (JwtKeyManualConfig 활성)
//        "jwt.manual-key.enabled=true"
//})
//@Import(JwtKeyManualConfig.class) // ✅ 구성 클래스를 테스트 컨텍스트에 명시적 주입
//class JwtIntegrationTest {
//
//    @TestConfiguration
//    static class TestConfig {
//
//        // @ConfigurationProperties 타입을 빈으로 등록하면 자동 바인딩됨
//        @Bean
//        JwtConfigurationProperties jwtConfigurationProperties() {
//            return new JwtConfigurationProperties();
//        }
//
//        /**
//         * JwtKeyManualConfig 가 만든 JwtKeyProvider 빈을 받아
//         * 부팅 시 실제 256-bit HMAC 키를 세팅한다.
//         */
//        @Bean
//        InitializingBean initJwtKey(JwtKeyProvider provider) {
//            return () -> {
//                var kg = KeyGenerator.getInstance("HmacSHA256");
//                kg.init(256);
//                var keyBytes = kg.generateKey().getEncoded();
//                provider.setKey(keyBytes).setKid("it-hmac-v1");
//            };
//        }
//    }
//
//    private final JwtTokenManager jwtTokenManager;
//
//    JwtIntegrationTest(JwtTokenManager jwtTokenManager) {
//        this.jwtTokenManager = jwtTokenManager;
//    }
//
//    @Test
//    @DisplayName("JWT 생성/검증 end-to-end")
//    void create_and_validate_token() {
//        String userId = "user-123";
//        String jti = "jti-xyz";
//        var roles = List.of("ROLE_USER", "ROLE_ORDER");
//        var scopes = List.of("read", "write");
//        String device = "ios";
//        String ip = "127.0.0.1";
//
//        // 액세스 토큰 생성
//        String token = jwtTokenManager.createAccessToken(userId, roles, jti, device, ip, scopes);
//
//        // 기본 검증 + 클레임 추출
//        assertThat(jwtTokenManager.validateToken(token)).isTrue();
//        assertThat(jwtTokenManager.getUserId(token)).isEqualTo(userId);
//        assertThat(jwtTokenManager.getJti(token)).isEqualTo(jti);
//        assertThat(jwtTokenManager.getDevice(token)).isEqualTo(device);
//        assertThat(jwtTokenManager.getIp(token)).isEqualTo(ip);
//        assertThat(jwtTokenManager.getRoles(token))
//                .extracting("authority")
//                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ORDER");
//    }
//}
