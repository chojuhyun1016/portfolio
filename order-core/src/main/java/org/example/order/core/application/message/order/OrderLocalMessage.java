package org.example.order.core.application.message.order;

import lombok.Getter;
import lombok.ToString;
import org.example.order.common.application.message.DlqMessage;
import org.example.order.common.code.CommonExceptionCode;
import org.example.order.common.code.MessageMethodType;
import org.example.order.common.code.DlqType;
import org.example.order.common.exception.CommonException;

import java.util.Objects;
import java.util.stream.Stream;

@Getter
@ToString
public class OrderLocalMessage extends DlqMessage {
    // 이벤트 수신 대상 private key
    private Long id;

    // 이벤트 행위
    private MessageMethodType methodType;

    // 메시지 최초 생성 시간
    private Long publishedTimestamp;

    public OrderLocalMessage() {
        super(DlqType.ORDER_LOCAL);
    }

    public void validation() {
        if (Stream.of(id, methodType, publishedTimestamp).anyMatch(Objects::isNull)) {
            throw new CommonException(CommonExceptionCode.INVALID_REQUEST);
        }
    }
}
