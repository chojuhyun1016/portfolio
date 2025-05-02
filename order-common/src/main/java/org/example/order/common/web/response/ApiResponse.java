package org.example.order.common.web.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.order.common.core.exception.code.CommonExceptionCode;
import org.example.order.common.core.exception.core.CommonException;
import org.example.order.common.web.ResponseMeta;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class ApiResponse<T> {
    private T data;
    private ResponseMeta metadata;

    public static <T> ResponseEntity<ApiResponse<T>> of(final HttpStatus status, final T data, final ResponseMeta metadata) {
        return ResponseEntity.status(status).body(new ApiResponse<>(data, metadata));
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(final T data) {
        return of(HttpStatus.OK, data, ResponseMeta.ok());
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(CommonException e) {
        return of(e.getHttpStatus(), null, ResponseMeta.of(e.getCode(), e.getMessage()));
    }

    public static <T> ResponseEntity<ApiResponse<T>> error(CommonExceptionCode code) {
        return of(code.getHttpStatus(), null, ResponseMeta.of(code.getCode(), code.getMsg()));
    }
}
