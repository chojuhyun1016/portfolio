package org.example.order.client.web.service;

import org.springframework.util.MultiValueMap;

import java.util.Map;

public interface WebService {
    <T> Object get(String url, Map<String, String> headers, MultiValueMap<String, String> params, Class<T> clz);
}
