package org.example.order.api.common.infra;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 공통 인프라 프로퍼티:
 * - logging: 필터 순서, 요청/응답 헤더명
 * - security: 활성화, permitAll/authenticated 패턴, 게이트웨이 검증 헤더/시크릿
 * - format: 날짜/시간 직렬화 방식(문자열/타임스탬프)
 */
@Getter
@ConfigurationProperties(prefix = "order.api.infra")
public class ApiInfraProperties {

    private final Logging logging = new Logging();
    private final Security security = new Security();
    private final Format format = new Format();

    @Setter
    @Getter
    public static class Logging {
        private int filterOrder = 10;
        private int mdcFilterOrder = 5;
        private String incomingHeader = "X-Request-Id";
        private String responseHeader = "X-Request-Id";

    }

    @Getter
    @Setter
    public static class Security {
        private boolean enabled = true;
        private String[] permitAllPatterns = new String[]{"/actuator/health", "/actuator/info"};
        private String[] authenticatedPatterns = new String[]{"/api/**"};
        private final Gateway gateway = new Gateway();

        public String[] permitAllPatterns() {
            return permitAllPatterns;
        }

        public String[] authenticatedPatterns() {
            return authenticatedPatterns;
        }

        public Gateway gateway() {
            return gateway;
        }

        public static class Gateway {
            private String header = "X-Gateway-Secret";
            private String secret = "change-me";

            public String header() {
                return header;
            }

            public void setHeader(String header) {
                this.header = header;
            }

            public String secret() {
                return secret;
            }

            public void setSecret(String secret) {
                this.secret = secret;
            }
        }
    }

    @Setter
    @Getter
    public static class Format {
        private boolean writeDatesAsTimestamps = false;
    }
}
