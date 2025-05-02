package org.example.order.common.core.code.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.order.common.core.code.CodeEnum;

@Getter
@RequiredArgsConstructor
public enum MessageChannelType implements CodeEnum {
    SQS("SQS"),
    SNS("SNS");

    private final String text;
}
