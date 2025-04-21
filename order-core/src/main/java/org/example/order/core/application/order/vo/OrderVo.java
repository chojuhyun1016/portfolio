package org.example.order.core.application.order.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.order.core.domain.order.entity.OrderEntity;

@Getter
@AllArgsConstructor
public class OrderVo {
    private OrderEntity order;
}
