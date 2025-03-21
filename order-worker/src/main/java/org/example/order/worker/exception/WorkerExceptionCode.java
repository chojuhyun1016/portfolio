package org.example.order.worker.exception;

import lombok.Getter;
import org.example.order.common.code.ExceptionCodeEnum;
import org.springframework.http.HttpStatus;

@Getter
public enum WorkerExceptionCode implements ExceptionCodeEnum {
    EMPTY_PAYLOAD(5000, "Payload is empty", HttpStatus.BAD_REQUEST),
    EMPTY_MESSAGE(5001, "Message is empty", HttpStatus.BAD_REQUEST),
    MESSAGE_TRANSMISSION_FAILED(5002, "Message transmission failed", HttpStatus.INTERNAL_SERVER_ERROR),
    MESSAGE_POLLING_FAILED(5003, "Message polling failed", HttpStatus.INTERNAL_SERVER_ERROR),
    MESSAGE_GROUPING_FAILED(5004, "Message grouping failed", HttpStatus.INTERNAL_SERVER_ERROR),
    OMITTED_RESOURCE(5005, "Resource is omitted", HttpStatus.INSUFFICIENT_SPACE_ON_RESOURCE),
    MESSAGE_UPDATE_FAILED(5006, "Message update failed", HttpStatus.INTERNAL_SERVER_ERROR),
    POLLING_FAILED(5007, "Message polling failed", HttpStatus.INTERNAL_SERVER_ERROR),
    UNSUPPORTED_EVENT_CATEGORY(5008, "Unsupported event category", HttpStatus.INTERNAL_SERVER_ERROR),
    NOT_FOUND_LOCAL_RESOURCE(5009, "Not found local resource", HttpStatus.INTERNAL_SERVER_ERROR);

    private final int code;
    private final String msg;
    private final HttpStatus httpStatus;

    WorkerExceptionCode(int code, String msg, HttpStatus httpStatus) {
        this.code = code;
        this.msg = msg;
        this.httpStatus = httpStatus;
    }

}
