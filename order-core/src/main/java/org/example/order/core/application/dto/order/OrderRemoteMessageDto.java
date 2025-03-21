package org.example.order.core.application.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.order.common.code.MessageMethodType;
import org.example.order.core.application.message.order.OrderRemoteMessage;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRemoteMessageDto {
    @NotNull
    private Long orderId;

    @NotNull
    private MessageMethodType methodType;

    public OrderRemoteMessage toMessage() {
        return OrderRemoteMessage.toMessage(this.orderId, this.methodType);
    }
}
