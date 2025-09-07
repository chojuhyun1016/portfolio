package org.example.order.core.infra.jpa.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Boolean <-> "Y"/"N" 매핑 컨버터
 * - DB: VARCHAR(1) 'Y' / 'N'
 * - Java: Boolean TRUE / FALSE / null
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
