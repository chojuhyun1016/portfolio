package org.example.order.core.application.event;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.common.event.DlqMessage;
import org.example.order.common.code.DlqType;
import org.example.order.common.code.MessageMethodType;

@Getter
@NoArgsConstructor
@ToString
public class OrderApiEvent extends DlqMessage {
    // 이벤트 수신 대상 private key
    private Long id;

    // 이벤트 행위
    private MessageMethodType methodType;

    // 메시지 최초 생성 시간
    private Long publishedTimestamp;

    public OrderApiEvent(OrderLocalEvent orderLocalEvent) {
        super(DlqType.ORDER_API);
        this.id = orderLocalEvent.getId();
        this.methodType = orderLocalEvent.getMethodType();
        this.publishedTimestamp = orderLocalEvent.getPublishedTimestamp();
    }

    public static OrderApiEvent toMessage(OrderLocalEvent orderLocalEvent) {
        return new OrderApiEvent(orderLocalEvent);
    }
}
