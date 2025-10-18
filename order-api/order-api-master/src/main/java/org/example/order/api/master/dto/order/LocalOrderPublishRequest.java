package org.example.order.api.master.dto.order;

import jakarta.validation.constraints.NotNull;
import org.example.order.contract.shared.op.Operation;

public record LocalOrderPublishRequest(

        @NotNull(message = "orderId 는 필수입니다.")
        Long orderId,

        @NotNull(message = "operation 은 필수입니다.")
        Operation operation
) {
}
