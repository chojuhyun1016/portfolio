package org.example.order.batch.exception;

import lombok.Getter;
import org.example.order.common.core.exception.code.ExceptionCodeEnum;
import org.springframework.http.HttpStatus;

@Getter
public enum BatchExceptionCode implements ExceptionCodeEnum {
    EMPTY_MESSAGE(6001, "Message is empty", HttpStatus.BAD_REQUEST),
    MESSAGE_TRANSMISSION_FAILED(6002, "Message transmission failed", HttpStatus.INTERNAL_SERVER_ERROR),
    POLLING_FAILED(6003, "Message polling failed", HttpStatus.INTERNAL_SERVER_ERROR),
    UNSUPPORTED_EVENT_CATEGORY(6004, "Unsupported event category", HttpStatus.INTERNAL_SERVER_ERROR),
    UNSUPPORTED_DLQ_TYPE(6005, "Dlq Type is not unregistered", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String msg;
    private final HttpStatus httpStatus;

    BatchExceptionCode(int code, String msg, HttpStatus httpStatus) {
        this.code = code;
        this.msg = msg;
        this.httpStatus = httpStatus;
    }
}
