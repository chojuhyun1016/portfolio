package org.example.order.client.web.config;

import org.example.order.client.web.config.internal.WebClientConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * WebClientModuleConfig
 * <p>
 * - 외부에서는 이 모듈 하나만 @Import 하면 됨
 * - 내부 구성(WebClientConfig)은 web-client.enabled 스위치로 on/off
 */
@Configuration
@Import({WebClientConfig.class})
public class WebClientModuleConfig {
}
