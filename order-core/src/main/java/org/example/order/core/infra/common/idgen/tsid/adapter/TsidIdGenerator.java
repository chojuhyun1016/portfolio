package org.example.order.core.infra.common.idgen.tsid.adapter;

import com.github.f4b6a3.tsid.TsidFactory;
import lombok.RequiredArgsConstructor;
import org.example.order.domain.common.id.IdGenerator;

/**
 * 인프라 어댑터: TSID 기반 IdGenerator 구현
 */
@RequiredArgsConstructor
public class TsidIdGenerator implements IdGenerator {

    private final TsidFactory factory;

    @Override
    public long nextId() {
        return factory.create().toLong();
    }
}
