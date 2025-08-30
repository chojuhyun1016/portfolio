package org.example.order.core.infra.common.idgen.tsid.config;

import org.example.order.core.infra.common.idgen.tsid.config.internal.TsidConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * TSID 모듈 구성
 * - 애플리케이션은 이 Config 하나만 @Import 하면 됨
 * - 내부 TsidConfig 는 tsid.enabled=true 일 때만 동작
 */
@Configuration
@Import({TsidConfig.class})
public class TsidInfraConfig {
}
