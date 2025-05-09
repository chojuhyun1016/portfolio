package org.example.order.core.messaging.order.message;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.common.core.messaging.message.DlqMessage;
import org.example.order.core.messaging.order.code.DlqOrderType;
import org.example.order.core.messaging.order.code.MessageMethodType;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class OrderRemoteEvent extends DlqMessage {
    private Long orderId;
    private MessageMethodType methodType;

    public OrderRemoteEvent(Long orderId, MessageMethodType methodType) {
        super(DlqOrderType.ORDER_REMOTE);
        this.orderId = orderId;
        this.methodType = methodType;
    }

    public static OrderRemoteEvent toMessage(Long id, MessageMethodType methodType) {
        return new OrderRemoteEvent(id, methodType);
    }
}
