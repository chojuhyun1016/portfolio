package org.example.order.api.common.config.module;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.common.exception.code.CommonExceptionCode;
import org.example.order.common.exception.CommonException;
import org.example.order.common.web.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> globalException(final Exception e) {
        log.error(e.getClass().getName(), e);
        return ApiResponse.error(CommonExceptionCode.UNKNOWN_SEVER_ERROR);
    }

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<?> commonException(final CommonException e) {
        return ApiResponse.error(e);
    }
}
