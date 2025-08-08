package org.example.order.common.core.exception.code;

import org.springframework.http.HttpStatus;

public interface ExceptionCodeEnum {
    int getCode();

    String getMsg();

    HttpStatus getHttpStatus();
}
