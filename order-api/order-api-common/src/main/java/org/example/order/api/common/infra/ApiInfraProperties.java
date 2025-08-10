package org.example.order.api.common.infra;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
    public static class Security {
        @Setter
        private boolean enabled = true;

        private String[] permitAllPatterns = new String[]{"/actuator/health", "/actuator/info"};
        private String[] authenticatedPatterns = new String[]{"/api/**"};

        private final Gateway gateway = new Gateway();

        // 세터에서 null 방지
        public void setPermitAllPatterns(String[] v) {
            this.permitAllPatterns = (v == null) ? new String[0] : v;
        }

        public void setAuthenticatedPatterns(String[] v) {
            this.authenticatedPatterns = (v == null) ? new String[0] : v;
        }

        // 커스텀 접근자(코드 가독성 용도)
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
