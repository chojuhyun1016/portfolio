package org.example.order.client.web;

import org.example.order.client.web.config.WebClientConfig;
import org.example.order.client.web.service.impl.WebClientServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebClientConfigEnabledTest
 * <p>
 * 주요 포인트:
 * - web-client.enabled=true → WebClient 및 WebClientServiceImpl 빈이 정상 생성되어야 함
 * - 외부 서버 호출은 하지 않고, 빈 존재 여부만 검증
 */
@SpringBootTest(classes = {WebClientConfig.class, WebClientServiceImpl.class})
@TestPropertySource(properties = {
        "web-client.enabled=true",
        "web-client.timeout.connect-ms=1000",
        "web-client.timeout.read-ms=2000",
        "web-client.codec.max-bytes=2097152" // 2MiB
})
class WebClientConfigEnabledTest {

    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.context.ApplicationContext ctx;

    @Test
    @DisplayName("web-client.enabled=true → WebClient/Service 빈 생성")
    void webClientBeansPresent() {
        WebClient wc = ctx.getBean(WebClient.class);
        assertNotNull(wc);

        WebClientServiceImpl svc = ctx.getBean(WebClientServiceImpl.class);
        assertNotNull(svc);
    }
}
