package org.example.order.core.application.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.order.core.domain.OrderEntity;

@Getter
@AllArgsConstructor
public class OrderVo {
    private OrderEntity order;
}
