package org.example.order.common.exception;

import lombok.Getter;
import org.example.order.common.code.ExceptionCodeEnum;
import org.springframework.http.HttpStatus;

@Getter
public class CommonException extends RuntimeException {
    private Integer code;
    private String msg;
    private HttpStatus httpStatus;

    public CommonException(ExceptionCodeEnum exceptionCodeEnum) {
        super(exceptionCodeEnum.getMsg());
        this.code = exceptionCodeEnum.getCode();
        this.msg = exceptionCodeEnum.getMsg();
        this.httpStatus = exceptionCodeEnum.getHttpStatus();
    }
}
