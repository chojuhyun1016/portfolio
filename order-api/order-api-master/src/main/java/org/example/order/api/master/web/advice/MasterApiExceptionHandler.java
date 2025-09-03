package org.example.order.api.master.web.advice;

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
 * 모듈 전용 핸들러는 얇게 유지합니다.
 * - 전역 정책은 common(GlobalExceptionHandler)이 담당
 * - 이 핸들러는 마스터 모듈 전용 로그 태깅 등 최소한의 차별화만 수행
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "org.example.order.api.master")
public class MasterApiExceptionHandler {

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ApiResponse<Void>> handleCommonForMaster(CommonException e) {
        log.warn("[Master] CommonException: code={}, msg={}", e.getCode(), e.getMsg());

        return ApiResponse.error(e);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknownForMaster(Exception e) {
        log.error("[Master] Unknown error", e);

        return ApiResponse.error(CommonExceptionCode.UNKNOWN_SERVER_ERROR);
    }
}
