package org.example.order.core.application.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.order.domain.order.entity.OrderEntity;

@Getter
@AllArgsConstructor
public class OrderVo {
    private OrderEntity order;
}
