package org.example.order.core.infra.messaging.order.message;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.common.core.messaging.message.DlqMessage;
import org.example.order.core.infra.messaging.order.code.DlqOrderType;
import org.example.order.common.core.messaging.code.MessageMethodType;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class OrderCloseMessage extends DlqMessage {
    private Long orderId;
    private MessageMethodType methodType;

    public OrderCloseMessage(Long orderId, MessageMethodType methodType) {
        super(DlqOrderType.ORDER_REMOTE);
        this.orderId = orderId;
        this.methodType = methodType;
    }

    public static OrderCloseMessage toMessage(Long id, MessageMethodType methodType) {
        return new OrderCloseMessage(id, methodType);
    }
}
