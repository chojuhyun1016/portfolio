package org.example.order.core.application.message.order;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.common.application.message.DlqMessage;
import org.example.order.common.code.DlqType;
import org.example.order.common.code.MessageMethodType;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class OrderRemoteMessage extends DlqMessage {
    private Long orderId;
    private MessageMethodType methodType;

    public OrderRemoteMessage(Long orderId, MessageMethodType methodType) {
        super(DlqType.ORDER_REMOTE);
        this.orderId = orderId;
        this.methodType = methodType;
    }

    public static OrderRemoteMessage toMessage(Long id, MessageMethodType methodType) {
        return new OrderRemoteMessage(id, methodType);
    }
}
