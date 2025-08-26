package org.example.order.client.web.config.property;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * WebClientUrlProperties
 * <p>
 * 주요 구성 포인트:
 * - web-client.enabled : 모듈 on/off 스위치
 * - client : 호출자 ID 및 대상 API URL
 * - timeout : connect/read 타임아웃(ms)
 * - codec : 직렬화/역직렬화 시 최대 메모리 크기
 */
@Getter
@Setter
@Validated
@ConfigurationProperties("web-client")
public class WebClientUrlProperties {

    private boolean enabled = false;
    private Client client = new Client();
    private Timeout timeout = new Timeout();
    private Codec codec = new Codec();

    @Getter
    @Setter
    public static class Client {
        private String clientId;
        private Url url = new Url();
    }

    @Getter
    @Setter
    public static class Url {
        private String order; // 주문 API Base URL
        private String user;  // 사용자 API Base URL

        public String getWithPathVariable(String url, Object path) {
            return String.format("%s/%s", url, path);
        }
    }

    @Getter
    @Setter
    public static class Timeout {
        @Positive
        private int connectMs = 3_000;
        @Positive
        private int readMs = 10_000;
    }

    @Getter
    @Setter
    public static class Codec {
        @Positive
        private int maxBytes = 2 * 1024 * 1024;
    }
}
