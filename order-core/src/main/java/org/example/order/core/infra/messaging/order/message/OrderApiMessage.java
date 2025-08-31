package org.example.order.core.infra.messaging.order.message;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.common.core.messaging.message.DlqMessage;
import org.example.order.core.infra.messaging.order.code.DlqOrderType;
import org.example.order.common.core.messaging.code.MessageMethodType;

@Getter
@NoArgsConstructor
@ToString
public class OrderApiMessage extends DlqMessage {
    // 이벤트 수신 대상 private key
    private Long id;

    // 이벤트 행위
    private MessageMethodType methodType;

    // 메시지 최초 생성 시간
    private Long publishedTimestamp;

    public OrderApiMessage(OrderLocalMessage orderLocalMessage) {
        super(DlqOrderType.ORDER_API);
        this.id = orderLocalMessage.getId();
        this.methodType = orderLocalMessage.getMethodType();
        this.publishedTimestamp = orderLocalMessage.getPublishedTimestamp();
    }

    public static OrderApiMessage toMessage(OrderLocalMessage orderLocalMessage) {
        return new OrderApiMessage(orderLocalMessage);
    }
}
