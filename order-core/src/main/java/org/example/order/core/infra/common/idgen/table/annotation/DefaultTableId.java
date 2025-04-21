package org.example.order.core.infra.common.idgen.table.annotation;

import java.lang.annotation.*;

/**
 * 테이블 기반 ID 생성을 의미하는 마커 어노테이션.
 * <p>실제 생성 전략은 필드에 직접 @GeneratedValue, @TableGenerator 로 명시합니다.</p>
 *
 * CREATE TABLE id_sequence (
 *     seq_name VARCHAR(100) NOT NULL PRIMARY KEY,
 *     next_val BIGINT NOT NULL
 * );
 * <p>
 * entity example
 * package org.example.order.core.domain.entity;
 * <p>
 * import jakarta.persistence.*;
 * import org.example.order.core.infra.common.idgen.table.annotation.DefaultTableId;
 *
 * @Entity
 * @Table(name = "sample_entity")
 * @TableGenerator(
 *     name = "DefaultTableIdGenerator",
 *     table = "id_sequence",
 *     pkColumnName = "seq_name",
 *     valueColumnName = "next_val",
 *     pkColumnValue = "sample_entity_id", // 테이블마다 고유 식별자 설정
 *     allocationSize = 50
 * )
 * public class SampleEntity {
 *
 *     @Id
 *     @GeneratedValue(strategy = GenerationType.TABLE, generator = "DefaultTableIdGenerator")
 *     @DefaultTableId // 마커 용도 (코드 가독성 및 룰 적용 용도)
 *     private Long id;
 * <p>
 *     private String name;
 * }
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DefaultTableId {
}
