package org.example.order.api.master.advice;

import lombok.extern.slf4j.Slf4j;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.common.web.response.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "org.example.order.api.master")
public class MasterApiExceptionHandler {

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ApiResponse<Void>> handleCommon(CommonException e) {
        log.warn("[Master] CommonException: code={}, msg={}", e.getCode(), e.getMsg());
        return ApiResponse.error(e);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e) {
        log.error("[Master] Unknown error", e);
        return ApiResponse.error(CommonExceptionCode.UNKNOWN_SEVER_ERROR);
    }
}
