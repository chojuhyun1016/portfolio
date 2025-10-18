package org.example.order.api.master.dto.order;

public record LocalOrderPublishResponse(
        Long orderId,
        String status // 예: "ACCEPTED"
) {
}
