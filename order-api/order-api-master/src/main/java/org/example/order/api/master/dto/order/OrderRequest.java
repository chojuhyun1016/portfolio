package org.example.order.api.master.dto.order;

import jakarta.validation.constraints.NotNull;

public record OrderRequest(

        @NotNull(message = "orderId 는 필수입니다.")
        Long orderId
) {
}
