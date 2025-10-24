package com.example.order.api.web.web.advice;

import lombok.extern.slf4j.Slf4j;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.common.web.response.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Web 모듈 전용 예외 핸들러
 * - 전역 정책은 common(GlobalExceptionHandler)이 담당
 * - 이 핸들러는 Web 모듈 전용 로그 태깅 등 최소한의 차별화만 수행
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.example.order.api.web")
public class WebApiExceptionHandler {

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ApiResponse<Void>> handleCommon(CommonException e) {
        log.warn("[Web] CommonException: code={}, msg={}", e.getCode(), e.getMsg());

        return ApiResponse.error(e);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e) {
        log.error("[Web] Unknown error", e);

        return ApiResponse.error(CommonExceptionCode.UNKNOWN_SERVER_ERROR);
    }
}
