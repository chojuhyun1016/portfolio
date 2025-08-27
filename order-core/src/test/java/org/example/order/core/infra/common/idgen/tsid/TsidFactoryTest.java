package org.example.order.core.infra.common.idgen.tsid;

import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TsidFactoryTest {

    @Test
    @DisplayName("TsidFactory: create()로 TSID를 안정적으로 생성한다")
    void tsidFactoryCreatesIds() {
        TsidFactory factory = new TsidConfig().tsidFactory();

        Set<Long> ids = new HashSet<>();

        for (int i = 0; i < 1_000; i++) {
            ids.add(factory.create().toLong());
        }

        assertThat(ids).hasSize(1_000);
    }

    @Test
    @DisplayName("TsidFactory: time-based 단조 증가 특성(대략) 확인")
    void tsidFactoryIsRoughlyMonotonic() throws Exception {
        TsidFactory factory = new TsidConfig().tsidFactory();

        long prev = factory.create().toLong();
        Thread.sleep(2); // 타임스탬프 경계 넘기기
        long next = factory.create().toLong();

        assertThat(next).isGreaterThan(prev);
    }

    @Test
    @DisplayName("TsidFactory: 시스템 타임존에서도 정상 생성")
    void tsidFactoryWorksWithSystemZone() {
        TsidFactory factory = new TsidConfig().tsidFactory();
        Tsid tsid = factory.create();

        assertThat(tsid).isNotNull();
        assertThat(ZoneId.systemDefault()).isNotNull();
    }
}
