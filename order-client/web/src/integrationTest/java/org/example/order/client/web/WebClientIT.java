package org.example.order.client.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.example.order.client.web.config.WebClientConfig;
import org.example.order.client.web.service.WebClientService;
import org.example.order.client.web.service.impl.WebClientServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebClient 통합 테스트
 * <p>
 * 주요 흐름:
 * - JDK HttpServer 로 임시 API 서버 구동
 * - WebClientServiceImpl 호출 → JSON 응답 검증
 * - @SpringBootTest 로 WebClientConfig + ServiceImpl 컨텍스트 구성
 * - IDE Autowiring 경고는 억제
 */
@SpringBootTest(classes = {WebClientConfig.class, WebClientServiceImpl.class})
@TestPropertySource(properties = {
        "web-client.enabled=true",
        "web-client.timeout.connect-ms=2000",
        "web-client.timeout.read-ms=5000",
        "web-client.codec.max-bytes=2097152"
})
@ExtendWith(SpringExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
class WebClientIT {

    static HttpServer server;
    static int port;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    WebClientService webClientService;

    @BeforeAll
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        port = server.getAddress().getPort();
        server.createContext("/hello", new JsonHandler());
        server.setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor());
        server.start();
    }

    @AfterAll
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    @DisplayName("WebClientService → GET 호출/응답 JSON 파싱")
    void callLocalJsonEndpoint() {
        String url = "http://127.0.0.1:" + port + "/hello";
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("name", "neo");

        @SuppressWarnings("unchecked")
        Map<String, Object> res = (Map<String, Object>) webClientService.get(url, null, params, Map.class);

        assertEquals("ok", res.get("status"));
        assertEquals("neo", res.get("name"));
        assertEquals(3, res.get("len"));
    }

    /**
     * 단순 JSON 응답을 돌려주는 핸들러
     */
    static class JsonHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            URI uri = exchange.getRequestURI();
            String query = uri.getQuery();
            String name = (query != null && query.startsWith("name="))
                    ? query.substring("name=".length())
                    : "unknown";

            String body = String.format("{\"status\":\"ok\",\"name\":\"%s\",\"len\":%d}", name, name.length());
            exchange.getResponseHeaders().add("Content-Type", MediaType.APPLICATION_JSON_VALUE);

            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
