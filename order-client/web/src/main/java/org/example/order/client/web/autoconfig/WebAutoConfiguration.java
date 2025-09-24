package org.example.order.client.web.autoconfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.example.order.client.web.properties.WebUrlProperties;
import org.example.order.client.web.service.WebService;
import org.example.order.client.web.service.impl.WebServiceImpl;
import org.example.order.common.core.context.AccessUserInfo;
import org.example.order.common.core.constant.HttpConstant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@AutoConfiguration
@ConditionalOnClass(WebClient.class)
@EnableConfigurationProperties(WebUrlProperties.class)
@ConditionalOnProperty(prefix = "web", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class WebAutoConfiguration {

    private final ObjectProvider<ObjectMapper> objectMapperProvider;
    private final WebUrlProperties props;

    @Bean
    @ConditionalOnMissingBean
    public WebClient webClient() {
        // 시스템 계정 헤더
        AccessUserInfo accessUser = AccessUserInfo.system();

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(props.getTimeout().getReadMs()))
                .compress(true)
                .followRedirect(true)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new io.netty.handler.timeout.ReadTimeoutHandler(
                                props.getTimeout().getReadMs(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new io.netty.handler.timeout.WriteTimeoutHandler(
                                props.getTimeout().getReadMs(), TimeUnit.MILLISECONDS)))
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getTimeout().getConnectMs());

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
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public WebService webService(WebClient webClient) {
        return new WebServiceImpl(webClient);
    }

    // ---- helpers ----
    private void customizeCodecs(ClientCodecConfigurer cfg) {
        ObjectMapper om = objectMapperProvider.getIfAvailable(this::fallbackObjectMapper);
        cfg.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(om, MediaType.APPLICATION_JSON));
        cfg.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(om, MediaType.APPLICATION_JSON));
        cfg.defaultCodecs().maxInMemorySize(props.getCodec().getMaxBytes());
    }

    private ObjectMapper fallbackObjectMapper() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .build();
    }
}
