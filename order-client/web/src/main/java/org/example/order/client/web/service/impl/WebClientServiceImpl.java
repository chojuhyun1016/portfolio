package org.example.order.client.web.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.client.web.service.WebClientService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebClientServiceImpl implements WebClientService {
    private final WebClient webClient;

    @Override
    public <T> T get(String url, Map<String, String> headers, MultiValueMap<String, String> params, Class<T> clz) {
        URI uri = UriComponentsBuilder
                .fromUriString(url)
                .queryParams(params)
                .build()
                .toUri();

        log.info("web client - http method : GET, uri : {}", uri);

        return webClient.get()
                .uri(uri)
                .headers(httpHeaders -> httpHeaders.setAll(headers))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(clz).block();
    }
}
