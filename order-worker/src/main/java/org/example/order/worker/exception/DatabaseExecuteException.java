package org.example.order.worker.exception;

import org.example.order.common.code.ExceptionCodeEnum;
import org.example.order.common.exception.CommonException;

public class DatabaseExecuteException extends CommonException {

    public DatabaseExecuteException(ExceptionCodeEnum exceptionCodeEnum) {
        super(exceptionCodeEnum);
    }
}
