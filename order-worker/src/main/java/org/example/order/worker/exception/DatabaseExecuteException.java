package org.example.order.worker.exception;

import org.example.order.common.core.exception.code.ExceptionCodeEnum;
import org.example.order.common.core.exception.core.CommonException;

public class DatabaseExecuteException extends CommonException {

    public DatabaseExecuteException(ExceptionCodeEnum exceptionCodeEnum) {
        super(exceptionCodeEnum);
    }
}
