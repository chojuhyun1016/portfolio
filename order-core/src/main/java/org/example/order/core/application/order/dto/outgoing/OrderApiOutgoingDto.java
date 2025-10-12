package org.example.order.core.application.order.dto.outgoing;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.order.contract.order.messaging.type.MessageMethodType;
import org.example.order.contract.order.messaging.event.OrderCloseMessage;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderApiOutgoingDto {
    @NotNull
    private Long orderId;

    @NotNull
    private MessageMethodType methodType;

    public OrderCloseMessage toMessage() {
        return OrderCloseMessage.of(this.orderId, this.methodType);
    }
}
