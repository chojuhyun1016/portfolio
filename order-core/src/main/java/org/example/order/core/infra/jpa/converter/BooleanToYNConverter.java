package org.example.order.core.infra.jpa.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Boolean <-> "Y"/"N" 매핑 컨버터
 * - DB: VARCHAR(1) 'Y' / 'N'
 * - Java: Boolean TRUE / FALSE / null
 * <p>
 * [중요]
 * - autoApply=true 로 설정하여, 엔티티에서 @Convert를 제거해도 전역 적용되도록 함
 * - 위치는 infra-persistence(order-core) 모듈에 두어 도메인이 인프라를 참조하지 않게 함
 */
@Converter(autoApply = true)
public class BooleanToYNConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean attribute) {
        if (attribute == null) {
            return null;
        }

        return attribute ? "Y" : "N";
    }

    @Override
    public Boolean convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        return "Y".equalsIgnoreCase(dbData);
    }
}
