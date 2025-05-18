// 📦 package org.example.order.core.application.order.model;

package org.example.order.core.application.order.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.order.domain.order.entity.OrderEntity;

/**
 * Local Order 엔티티 래퍼 모델 (Application 계층)
 */
@Getter
@AllArgsConstructor
public class OrderEntityDto {
    private OrderEntity order;
}
