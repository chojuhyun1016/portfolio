package org.example.order.core.infra.common.idgen.tsid.generator;

import org.example.order.core.infra.common.idgen.tsid.TsidFactoryHolder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

/**
 * Hibernate IdentifierGenerator 구현체
 * - 스프링 DI 없이도 동작하도록 TsidFactoryHolder 에서 팩토리를 가져와 사용
 * - @CustomTsid 와 함께 사용
 */
public class CustomTsidGenerator implements IdentifierGenerator {

    /**
     * 기본 생성자(리플렉션 생성 보장용)
     */
    public CustomTsidGenerator() {
    }

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        return TsidFactoryHolder.get().create().toLong();
    }
}
