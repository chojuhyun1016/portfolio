package org.example.order.client.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.order.common.context.AccessUserInfo;
import org.example.order.common.constant.HttpConstant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class WebClientConfig {
    private final ObjectMapper objectMapper;

    @Bean
    public WebClient webClient() {
        AccessUserInfo accessUser = AccessUserInfo.system();

        return WebClient.builder()
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(clientDefaultCodecsConfigurer -> {
                            clientDefaultCodecsConfigurer.defaultCodecs()
                                    .jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
                            clientDefaultCodecsConfigurer.defaultCodecs()
                                    .jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
                        })
                        .build())
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.add(HttpConstant.X_USER_ID, accessUser.userId().toString());
                    httpHeaders.add(HttpConstant.X_LOGIN_ID, accessUser.loginId());
                    httpHeaders.add(HttpConstant.X_USER_TYPE, accessUser.userType());
                }).build();
    }
}
