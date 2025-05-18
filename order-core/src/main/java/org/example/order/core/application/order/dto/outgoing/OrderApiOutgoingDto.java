package org.example.order.core.application.order.dto.outgoing;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.order.common.core.messaging.code.MessageMethodType;
import org.example.order.core.messaging.order.message.OrderCloseMessage;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderApiOutgoingDto {
    @NotNull
    private Long orderId;

    @NotNull
    private MessageMethodType methodType;

    public OrderCloseMessage toMessage() {
        return OrderCloseMessage.toMessage(this.orderId, this.methodType);
    }
}
