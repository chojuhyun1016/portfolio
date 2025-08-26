package org.example.order.client.web.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.web.service.WebClientService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * WebClientServiceImpl
 * <p>
 * 주요 포인트:
 * - web-client.enabled=true 일 때만 활성화됨 (@ConditionalOnBean(WebClient))
 * - GET 요청을 수행하고 JSON 응답을 지정한 타입으로 반환
 * - 필요 시 NoOp 구현을 통해 교체 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(WebClient.class)
public class WebClientServiceImpl implements WebClientService {

    private final WebClient webClient;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String url, Map<String, String> headers, MultiValueMap<String, String> params, Class<T> clz) {
        URI uri = UriComponentsBuilder.fromUriString(url)
                .queryParams(params)
                .build(true)
                .toUri();

        log.info("WebClient GET → uri: {}", uri);

        return webClient.get()
                .uri(uri)
                .headers(httpHeaders -> {
                    if (headers != null) {
                        httpHeaders.setAll(headers);
                    }
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(clz)
                .block();
    }
}
