package org.example.order.common.support.jpa.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Boolean <-> "Y"/"N" 매핑 컨버터 (공용 레이어)
 * - DB: VARCHAR(1) 'Y' / 'N'
 * - Java: Boolean TRUE / FALSE / null
 * <p>
 * 도메인/인프라 어디서든 안전하게 사용 가능하도록 order-common에 둔다.
 */
@Converter(autoApply = false)
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
