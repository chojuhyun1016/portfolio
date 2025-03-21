package org.example.order.core.application.vo.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.order.core.domain.order.OrderEntity;

@Getter
@AllArgsConstructor
public class OrderVo {
    private OrderEntity order;
}
