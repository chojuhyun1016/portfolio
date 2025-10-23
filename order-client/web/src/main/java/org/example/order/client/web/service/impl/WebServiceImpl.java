package org.example.order.client.web.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.web.service.WebService;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * WebServiceImpl
 * <p>
 * - 어노테이션(예: @Service) 없음 → Config(@Bean)에서 명시적으로 등록
 * - web.enabled=true 인 경우에만 WebAutoConfiguration에서 @Bean으로 노출됨
 */
@Slf4j
@RequiredArgsConstructor
public class WebServiceImpl implements WebService {

    private final WebClient webClient;

    @Override
    @SuppressWarnings("unchecked")
    public <T> Object get(String url, Map<String, String> headers, MultiValueMap<String, String> params, Class<T> clz) {
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

    @Override
    @SuppressWarnings("unchecked")
    public <T> Object post(String url, Map<String, String> headers, Object body, Class<T> clz) {
        log.info("WebClient POST → url: {}", url);

        return webClient.post()
                .uri(url)
                .headers(httpHeaders -> {
                    if (headers != null) {
                        httpHeaders.setAll(headers);
                    }
                })
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body == null ? new Object() : body)
                .retrieve()
                .bodyToMono(clz)
                .block();
    }
}
