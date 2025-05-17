package org.example.order.core.messaging.order.message;

import lombok.Getter;
import lombok.ToString;
import org.example.order.common.core.messaging.message.DlqMessage;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.core.messaging.order.code.DlqOrderType;
import org.example.order.common.core.messaging.code.MessageMethodType;

import java.util.Objects;
import java.util.stream.Stream;

@Getter
@ToString
public class OrderLocalEvent extends DlqMessage {
    // 이벤트 수신 대상 private key
    private Long id;

    // 이벤트 행위
    private MessageMethodType methodType;

    // 메시지 최초 생성 시간
    private Long publishedTimestamp;

    public OrderLocalEvent() {
        super(DlqOrderType.ORDER_LOCAL);
    }

    public void validation() {
        if (Stream.of(id, methodType, publishedTimestamp).anyMatch(Objects::isNull)) {
            throw new CommonException(CommonExceptionCode.INVALID_REQUEST);
        }
    }
}
