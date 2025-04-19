package org.example.order.core.domain.order.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.order.core.domain.order.enums.OrderStatus;

@Converter(autoApply = true)
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {

    @Override
    public String convertToDatabaseColumn(OrderStatus attribute) {
        return attribute != null ? attribute.getCode() : null;
    }

    @Override
    public OrderStatus convertToEntityAttribute(String dbData) {
        return OrderStatus.fromCode(dbData);
    }
}
