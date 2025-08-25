package org.example.order.client.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.example.order.client.web.config.property.WebClientUrlProperties;
import org.example.order.common.core.context.AccessUserInfo;
import org.example.order.common.core.constant.HttpConstant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * WebClient 구성
 *
 * - ✨ web-client.enabled=true 일 때만 WebClient 빈 생성(@ConditionalOnProperty)
 * - Jackson 인코더/디코더를 ObjectMapper로 맞추고 maxInMemorySize 조절
 * - 기본 헤더(X-USER-*)를 시스템 계정으로 셋업(필요 시 교체 가능)
 * - connect/read 타임아웃을 프로퍼티로 튜닝
 *
 * ⚠️ 변경 사항:
 * - ObjectMapper를 강제 주입하지 않고 ObjectProvider로 "선택 주입" 받음.
 *   → 실행 컨텍스트에 ObjectMapper 빈이 없을 경우에도 fallback ObjectMapper를 생성하여 사용.
 *   → 라이브러리 모듈/단위 테스트에서 'Could not autowire ObjectMapper' 문제 방지.
 */
@Configuration
@EnableConfigurationProperties(WebClientUrlProperties.class)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "web-client", name = "enabled", havingValue = "true")
public class WebClientConfig {

    // ⚠️ 기존: private final ObjectMapper objectMapper;
    // ✨ 변경: 선택 주입으로 전환 (없으면 fallback 사용)
    private final ObjectProvider<ObjectMapper> objectMapperProvider;

    private final WebClientUrlProperties props;

    @Bean
    public WebClient webClient() {
        // 시스템 계정 헤더(서비스 공통 상수) — 필요 시 Request마다 덮어쓸 수 있음
        AccessUserInfo accessUser = AccessUserInfo.system();

        // Netty 타임아웃
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(props.getTimeout().getReadMs()))
                .compress(true)
                .followRedirect(true)
                .resolver(spec -> {}) // 필요 시 DNS/Proxy 커스터마이징
                .doOnConnected(conn -> conn
                        .addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(
                                props.getTimeout().getReadMs(), java.util.concurrent.TimeUnit.MILLISECONDS))
                        .addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(
                                props.getTimeout().getReadMs(), java.util.concurrent.TimeUnit.MILLISECONDS)))
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getTimeout().getConnectMs());

        // Jackson + maxInMemorySize
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(this::customizeCodecs)
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.add(HttpConstant.X_USER_ID, accessUser.userId().toString());
                    httpHeaders.add(HttpConstant.X_LOGIN_ID, accessUser.loginId());
                    httpHeaders.add(HttpConstant.X_USER_TYPE, accessUser.userType());
                })
                // .baseUrl(...) 는 고정 엔드포인트가 하나일 때만. 여기선 동적으로 URL을 쓰므로 생략
                .build();
    }

    private void customizeCodecs(ClientCodecConfigurer clientDefaultCodecsConfigurer) {
        // ✨ ObjectMapper가 컨텍스트에 없으면 fallback 생성
        ObjectMapper om = objectMapperProvider.getIfAvailable(this::fallbackObjectMapper);

        clientDefaultCodecsConfigurer.defaultCodecs()
                .jackson2JsonEncoder(new Jackson2JsonEncoder(om, MediaType.APPLICATION_JSON));
        clientDefaultCodecsConfigurer.defaultCodecs()
                .jackson2JsonDecoder(new Jackson2JsonDecoder(om, MediaType.APPLICATION_JSON));
        clientDefaultCodecsConfigurer.defaultCodecs()
                .maxInMemorySize(props.getCodec().getMaxBytes()); // ✨ 응답 사이즈 제한
    }

    // ✨ 부트 자동구성이 없어도 안전하게 동작하도록 하는 기본 ObjectMapper
    //    - JavaTimeModule 등록 (LocalDateTime 등 직렬화/역직렬화)
    private ObjectMapper fallbackObjectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
    }
}
