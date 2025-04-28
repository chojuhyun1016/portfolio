package org.example.order.core.infra.common.idgen.tsid.annotation;

import io.hypersistence.tsid.TSID;
import org.example.order.core.infra.common.idgen.tsid.factory.TsidFactoryProvider;
import org.example.order.core.infra.common.idgen.tsid.generator.CustomTsidGenerator;
import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.*;
import java.util.function.Supplier;

/**
 * Hibernate용 Custom TSID Annotation
 *
 * - @IdGeneratorType을 통해 Hibernate의 PK 자동 생성기에 연결
 * - 기본 FactorySupplier를 사용하여 TSID.Factory를 제공
 */
@IdGeneratorType(CustomTsidGenerator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface CustomTsid {

    // Factory를 제공하는 Supplier 타입 (Default는 내부 FactorySupplier)
    Class<? extends Supplier<TSID.Factory>> value() default FactorySupplier.class;

    /**
     * TSID.Factory를 공급하는 Supplier
     * - Singleton 패턴으로 Factory 인스턴스 재사용
     */
    class FactorySupplier implements Supplier<TSID.Factory> {

        // 고정된 Singleton Factory 인스턴스
        public static final TSID.Factory factory = TsidFactoryProvider.getFactory();

        @Override
        public TSID.Factory get() {
            return factory;
        }
    }
}
