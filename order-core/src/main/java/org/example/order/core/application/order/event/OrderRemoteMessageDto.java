package org.example.order.core.application.order.event;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.order.common.code.type.MessageMethodType;
import org.example.order.core.application.order.event.message.OrderRemoteEvent;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRemoteMessageDto {
    @NotNull
    private Long orderId;

    @NotNull
    private MessageMethodType methodType;

    public OrderRemoteEvent toMessage() {
        return OrderRemoteEvent.toMessage(this.orderId, this.methodType);
    }
}
