package org.example.order.client.s3.config;

import org.example.order.client.s3.config.internal.S3ClientConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * S3ModuleConfig
 * - 모듈 외부에 노출되는 단 하나의 진입점(조립 클래스)
 * - 내부 조건부 설정(S3ClientConfig)만 Import
 * - 외부에서는 @Import(S3ModuleConfig.class) 만 사용하면 됨
 */
@Configuration
@Import({S3ClientConfig.class})
public class S3ModuleConfig {
}
