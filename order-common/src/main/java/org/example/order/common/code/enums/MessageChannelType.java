package org.example.order.common.code.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageChannelType implements CodeEnum {
    SQS("SQS"),
    SNS("SNS");

    private final String text;
}
