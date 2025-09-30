package org.example.order.core.infra.common.idgen.tsid.config;

import org.example.order.core.infra.common.idgen.tsid.config.fallback.FallbackIdGeneratorConfig;
import org.example.order.core.infra.common.idgen.tsid.config.internal.TsidConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * TSID 모듈 구성의 엔트리포인트
 * ------------------------------------------------------------------
 * - tsid.enabled=true  : TsidConfig 가 TsidFactory/IdGenerator 등록 + Holder 세팅
 * - 그 외(=false/미설정): FallbackIdGeneratorConfig 가 안전 폴백 IdGenerator 등록
 * <p>
 * 애플리케이션에서는 이 클래스 하나만 @Import 하면 충분합니다.
 */
@Configuration
@Import({
        TsidConfig.class,                 // 조건부: tsid.enabled=true 일 때만 동작
        FallbackIdGeneratorConfig.class   // 조건부: IdGenerator 미존재 시 폴백 등록
})
public class TsidInfraConfig {
}
