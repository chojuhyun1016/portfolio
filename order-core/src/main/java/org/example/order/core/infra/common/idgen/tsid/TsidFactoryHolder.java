package org.example.order.core.infra.common.idgen.tsid;

import com.github.f4b6a3.tsid.TsidFactory;

/**
 * TSID 팩토리 정적 홀더
 * - Hibernate 에서는 스프링 DI가 불가 → 정적 접근 필요
 * - TsidConfig 부팅 시 set(...) 호출로 초기화
 */
public final class TsidFactoryHolder {

    private static volatile TsidFactory FACTORY;

    private TsidFactoryHolder() {
    }

    /**
     * 스프링 구성에서 팩토리를 주입(1회성)
     */
    public static void set(TsidFactory factory) {
        FACTORY = factory;
    }

    /**
     * Hibernate IdentifierGenerator 등에서 정적 취득
     */
    public static TsidFactory get() {
        if (FACTORY == null) {
            throw new IllegalStateException("TsidFactoryHolder is not initialized. Check tsid.enabled=true and configuration.");
        }

        return FACTORY;
    }
}
