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
 * WebClient 사용 서비스
 * - ✨ WebClient 빈이 있을 때만 생성 → web-client.enabled=false면 이 구현체도 등록되지 않음
 * - 필요 시 NoOp 구현을 별도로 두고 @ConditionalOnMissingBean(WebClientService.class)로 대체 가능
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
        URI uri = UriComponentsBuilder
                .fromUriString(url)
                .queryParams(params)
                .build(true) // 인코딩 안전
                .toUri();

        log.info("web client - http method : GET, uri : {}", uri);

        return webClient.get()
                .uri(uri)
                .headers(httpHeaders -> {
                    if (headers != null) httpHeaders.setAll(headers);
                })
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(clz)
                .block();
    }
}
