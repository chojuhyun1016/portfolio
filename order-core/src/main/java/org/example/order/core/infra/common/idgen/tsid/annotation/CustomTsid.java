package org.example.order.core.infra.common.idgen.tsid.annotation;

import org.example.order.core.infra.common.idgen.tsid.generator.CustomTsidGenerator;
import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.*;

/**
 * Hibernate용 Custom TSID 어노테이션
 * - @IdGeneratorType 를 통해 CustomTsidGenerator로 연결
 * - 엔티티 ID 필드/게터에 부착하여 사용
 */
@IdGeneratorType(CustomTsidGenerator.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface CustomTsid {
}
