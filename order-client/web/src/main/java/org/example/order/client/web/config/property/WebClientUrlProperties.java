package org.example.order.client.web.config.property;

import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * WebClient 모듈 프로퍼티
 *
 * - web-client.enabled            : 모듈 on/off 스위치 (기본 false → 설정 없으면 비활성)
 * - web-client.client.client-id   : 호출자 식별자(선택)
 * - web-client.client.url.order   : 주문 API Base URL (enabled=true일 때 권장)
 * - web-client.client.url.user    : 사용자 API Base URL (enabled=true일 때 권장)
 * - web-client.timeout.connect-ms : 커넥트 타임아웃(ms)
 * - web-client.timeout.read-ms    : 읽기 타임아웃(ms)
 * - web-client.codec.max-bytes    : 디코딩 시 최대 메모리 바이트 (기본 2MiB)
 */
@Getter
@Setter
@Validated
@ConfigurationProperties("web-client")
public class WebClientUrlProperties {

    /** ✨ 스위치: true일 때만 WebClient 빈 생성 */
    private boolean enabled = false;

    private Client client = new Client();
    private Timeout timeout = new Timeout();
    private Codec codec = new Codec();

    @Getter @Setter
    public static class Client {
        private String clientId; // 선택
        private Url url = new Url();
    }

    @Getter @Setter
    public static class Url {
        /** 주문 API */
        private String order;
        /** 사용자 API */
        private String user;

        /** 단순 path variable 붙이기 유틸 */
        public String getWithPathVariable(String url, Object path) {
            return String.format("%s/%s", url, path);
        }
    }

    @Getter @Setter
    public static class Timeout {
        /** 연결 타임아웃(ms) */
        @Positive
        private int connectMs = 3_000;
        /** 읽기 타임아웃(ms) */
        @Positive
        private int readMs = 10_000;
    }

    @Getter @Setter
    public static class Codec {
        /** 디코더/인코더 maxInMemorySize (바이트). 기본 2MiB */
        @Positive
        private int maxBytes = 2 * 1024 * 1024;
    }
}
