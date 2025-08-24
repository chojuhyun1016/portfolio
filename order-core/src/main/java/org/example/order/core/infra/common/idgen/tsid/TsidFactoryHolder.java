package org.example.order.core.infra.common.idgen.tsid;

import com.github.f4b6a3.tsid.TsidFactory;

/**
 * TSID 팩토리 정적 홀더 (Hibernate 식별자 생성기에서 사용)
 *
 * - Hibernate는 @IdGeneratorType 대상 클래스를 스프링 빈으로 주입해주지 않고,
 *   리플렉션으로 "기본 생성자"로만 인스턴스화한다.
 * - 따라서 CustomTsidGenerator가 스프링 주입 없이도 TsidFactory에 접근할 수 있도록
 *   본 정적 홀더를 사용한다.
 *
 * ⚠️ 테스트/애플리케이션 부팅 시 TsidConfig에서 set(...)로 반드시 주입됨.
 */
public final class TsidFactoryHolder {

    private static volatile TsidFactory FACTORY;

    private TsidFactoryHolder() {}

    public static void set(TsidFactory factory) {
        FACTORY = factory;
    }

    public static TsidFactory get() {
        if (FACTORY == null) {
            throw new IllegalStateException("TsidFactoryHolder has not been initialized yet.");
        }
        return FACTORY;
    }
}
