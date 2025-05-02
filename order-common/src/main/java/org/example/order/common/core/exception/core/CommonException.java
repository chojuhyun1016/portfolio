package org.example.order.common.core.exception.core;

import lombok.Getter;
import org.example.order.common.core.exception.code.ExceptionCodeEnum;
import org.springframework.http.HttpStatus;

/**
 * 공통 비즈니스 예외 클래스
 * - ExceptionCodeEnum 기반
 * - 커스텀 메시지 & 원인 예외(cause) 지원
 */
@Getter
public class CommonException extends RuntimeException {

    private final Integer code;
    private final String msg;
    private final HttpStatus httpStatus;

    /**
     * 기본 생성자 - ExceptionCodeEnum만 받음
     */
    public CommonException(ExceptionCodeEnum exceptionCodeEnum) {
        super(exceptionCodeEnum.getMsg());
        this.code = exceptionCodeEnum.getCode();
        this.msg = exceptionCodeEnum.getMsg();
        this.httpStatus = exceptionCodeEnum.getHttpStatus();
    }

    /**
     * 커스텀 메시지를 받는 생성자
     */
    public CommonException(ExceptionCodeEnum exceptionCodeEnum, String customMsg) {
        super(customMsg);
        this.code = exceptionCodeEnum.getCode();
        this.msg = customMsg;
        this.httpStatus = exceptionCodeEnum.getHttpStatus();
    }

    /**
     * 원인 예외를 포함하는 생성자
     */
    public CommonException(ExceptionCodeEnum exceptionCodeEnum, Throwable cause) {
        super(exceptionCodeEnum.getMsg(), cause);
        this.code = exceptionCodeEnum.getCode();
        this.msg = exceptionCodeEnum.getMsg();
        this.httpStatus = exceptionCodeEnum.getHttpStatus();
    }

    /**
     * 커스텀 메시지 + 원인 예외를 모두 포함하는 생성자
     */
    public CommonException(ExceptionCodeEnum exceptionCodeEnum, String customMsg, Throwable cause) {
        super(customMsg, cause);
        this.code = exceptionCodeEnum.getCode();
        this.msg = customMsg;
        this.httpStatus = exceptionCodeEnum.getHttpStatus();
    }
}
