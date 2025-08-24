package org.example.order.core.infra.common.idgen.tsid.generator;

import org.example.order.core.infra.common.idgen.tsid.TsidFactoryHolder;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
//import org.springframework.stereotype.Component; // ❌ 더 이상 스프링 컴포넌트로 등록하지 않음
//import lombok.RequiredArgsConstructor;        // ❌ 생성자 주입 제거

import java.io.Serializable;

/**
 * Hibernate IdentifierGenerator 구현 (Infra 전용)
 *
 * - 기존: 스프링 DI(@Component, 생성자 주입) → Hibernate가 기본생성자 요구하여 실패
 * - 변경: 기본생성자 유지 + TsidFactoryHolder에서 TsidFactory를 꺼내 사용
 *
 * ※ @IdGeneratorType(CustomTsidGenerator.class)로 사용될 때
 *    Hibernate가 new CustomTsidGenerator()로 인스턴스를 만들고,
 *    generate(...) 호출 시 정적 홀더에 미리 세팅된 팩토리를 사용한다.
 */
public class CustomTsidGenerator implements IdentifierGenerator {

    // ✅ 기본 생성자(명시X여도 되지만, 오해 방지용으로 남김)
    public CustomTsidGenerator() {}

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) {
        // 정적 홀더에서 가져온 팩토리로 ID 생성
        return TsidFactoryHolder.get().create().toLong();
    }
}
