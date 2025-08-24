package org.example.order.core.infra.common.idgen.tsid;

import com.github.f4b6a3.tsid.Tsid;
import com.github.f4b6a3.tsid.TsidFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 유닛 테스트: TsidConfig 가 만드는 TsidFactory 동작 검증
 *
 * - 여기서는 Hibernate용 CustomTsidGenerator 를 직접 new 하지 않는다.
 *   (Hibernate IdentifierGenerator 는 프레임워크가 무인자 생성자로 리플렉션 생성)
 * - 제너레이터 자체의 JPA 연동 검증은 통합 테스트(TsidHibernateGeneratorIT)에서 수행한다.
 */
class TsidFactoryTest {

    @Test
    @DisplayName("TsidFactory: create()로 TSID를 안정적으로 생성한다")
    void tsidFactoryCreatesIds() {
        // given: 실제 애플리케이션과 동일한 방식으로 TsidFactory 생성
        TsidFactory factory = new TsidConfig().tsidFactory();

        // when: 여러 개 생성
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 1_000; i++) {
            ids.add(factory.create().toLong());
        }

        // then: 전부 유니크
        assertThat(ids).hasSize(1_000);
    }

    @Test
    @DisplayName("TsidFactory: time-based 단조 증가 특성(대략) 확인")
    void tsidFactoryIsRoughlyMonotonic() throws Exception {
        // given
        TsidFactory factory = new TsidConfig().tsidFactory();

        long prev = factory.create().toLong();

        // 타임스탬프 경계 넘기기(환경 잡음 줄이기 위해 잠깐 대기)
        Thread.sleep(2);

        long next = factory.create().toLong();

        // then
        assertThat(next).isGreaterThan(prev);
    }

    @Test
    @DisplayName("TsidFactory: 시스템 타임존에서도 정상 생성")
    void tsidFactoryWorksWithSystemZone() {
        // given
        TsidFactory factory = new TsidConfig().tsidFactory();

        // when
        Tsid tsid = factory.create();

        // then
        assertThat(tsid).isNotNull();
        assertThat(ZoneId.systemDefault()).isNotNull(); // 단순 sanity check
    }
}
