package org.example.order.core.infra.common.idgen.tsid.generator;

import com.github.f4b6a3.tsid.TsidFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Hibernate IdentifierGenerator 구현 (Infra 전용)
 * - 주입받은 TsidFactory를 사용하여 TSID 생성
 */
@Component
@RequiredArgsConstructor
public class CustomTsidGenerator implements IdentifierGenerator {

    private final TsidFactory tsidFactory;

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        return tsidFactory.create().toLong();
    }
}