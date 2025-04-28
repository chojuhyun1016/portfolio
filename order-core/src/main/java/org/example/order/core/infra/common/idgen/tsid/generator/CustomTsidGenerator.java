package org.example.order.core.infra.common.idgen.tsid.generator;

import org.example.order.core.infra.common.idgen.tsid.annotation.CustomTsid;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

/**
 * Hibernate IdentifierGenerator
 *
 * - 엔티티의 ID 생성시 TSID를 생성하여 반환
 */
public class CustomTsidGenerator implements IdentifierGenerator {

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        return CustomTsid.FactorySupplier.factory.generate().toLong();
    }
}
