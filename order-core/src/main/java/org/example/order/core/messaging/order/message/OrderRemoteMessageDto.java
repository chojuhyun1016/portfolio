package org.example.order.core.messaging.order.message;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.order.core.messaging.order.code.MessageMethodType;

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
