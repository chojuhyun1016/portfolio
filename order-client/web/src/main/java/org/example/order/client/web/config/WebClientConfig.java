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
 * WebClientConfig
 * <p>
 * 주요 구성 포인트:
 * - web-client.enabled=true 일 때만 활성화됨
 * - WebClient 기본 설정: 공통 헤더(X-USER-*) 주입
 * - Timeout 설정 (connect/read)
 * - ObjectMapper: 스프링 컨텍스트에서 주입되면 사용, 없으면 fallback 생성
 */
@Configuration
@EnableConfigurationProperties(WebClientUrlProperties.class)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "web-client", name = "enabled", havingValue = "true")
public class WebClientConfig {

    private final ObjectProvider<ObjectMapper> objectMapperProvider;
    private final WebClientUrlProperties props;

    @Bean
    public WebClient webClient() {
        // 시스템 계정 기본 헤더 세팅
        AccessUserInfo accessUser = AccessUserInfo.system();

        // Reactor Netty 기반 HttpClient 구성 (timeout + 압축 + redirect)
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(props.getTimeout().getReadMs()))
                .compress(true)
                .followRedirect(true)
                .resolver(spec -> {
                })
                .doOnConnected(conn -> conn
                        .addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(
                                props.getTimeout().getReadMs(), java.util.concurrent.TimeUnit.MILLISECONDS))
                        .addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(
                                props.getTimeout().getReadMs(), java.util.concurrent.TimeUnit.MILLISECONDS)))
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getTimeout().getConnectMs());

        // Codec 전략 (ObjectMapper 기반 JSON 인코더/디코더, maxInMemorySize 적용)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(this::customizeCodecs)
                .build();

        // WebClient Bean 정의
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.add(HttpConstant.X_USER_ID, accessUser.userId().toString());
                    httpHeaders.add(HttpConstant.X_LOGIN_ID, accessUser.loginId());
                    httpHeaders.add(HttpConstant.X_USER_TYPE, accessUser.userType());
                })
                .build();
    }

    /**
     * JSON 직렬화/역직렬화 Codec 구성
     */
    private void customizeCodecs(ClientCodecConfigurer clientDefaultCodecsConfigurer) {
        ObjectMapper om = objectMapperProvider.getIfAvailable(this::fallbackObjectMapper);

        clientDefaultCodecsConfigurer.defaultCodecs()
                .jackson2JsonEncoder(new Jackson2JsonEncoder(om, MediaType.APPLICATION_JSON));
        clientDefaultCodecsConfigurer.defaultCodecs()
                .jackson2JsonDecoder(new Jackson2JsonDecoder(om, MediaType.APPLICATION_JSON));
        clientDefaultCodecsConfigurer.defaultCodecs()
                .maxInMemorySize(props.getCodec().getMaxBytes());
    }

    /**
     * ObjectMapper fallback (JavaTimeModule 포함)
     */
    private ObjectMapper fallbackObjectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
    }
}
