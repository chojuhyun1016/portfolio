package org.example.order.contract.common.messaging;

import com.fasterxml.jackson.annotation.JsonFormat;

public enum MessageMethodType {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    POST,
    PUT,
    DELETE
}
