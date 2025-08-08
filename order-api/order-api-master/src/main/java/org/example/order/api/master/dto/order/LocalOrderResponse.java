package org.example.order.api.master.dto.order;

public record LocalOrderResponse(
        Long orderId,
        String status // 예: "ACCEPTED"
) {
}
