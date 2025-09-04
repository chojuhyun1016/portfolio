package com.example.order.api.web;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * 단위/슬라이스(WebMvc/RestDocs) 테스트 전용 경량 부트
 * 공용 테스트 Bean을 필요 시 이곳에 정의
 */
@TestConfiguration(proxyBeanMethods = false)
@ComponentScan(basePackages = {
})
public class TestBoot {
}
