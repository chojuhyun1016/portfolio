package org.example.order.core.infra.common.idgen.tsid.annotation;

import org.example.order.core.infra.common.idgen.tsid.generator.CustomTsidGenerator;
import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.*;

/**
 * Hibernate용 Custom TSID Annotation (Infra 전용)
 * - @IdGeneratorType을 통해 CustomTsidGenerator로 연결
 */
@IdGeneratorType(CustomTsidGenerator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface CustomTsid {
}