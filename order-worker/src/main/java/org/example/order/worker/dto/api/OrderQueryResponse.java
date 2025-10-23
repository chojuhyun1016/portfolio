package org.example.order.worker.dto.api;

import lombok.*;
import org.example.order.core.application.order.dto.sync.LocalOrderSync;

import java.time.LocalDateTime;

/**
 * order-api-master /api/v1/local-orders/query 응답의 data 스키마
 * - 워커 내부에서는 LocalOrderSync 로 변환하여 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderQueryResponse {

    private Long id;

    private Long userId;
    private String userNumber;

    private Long orderId;
    private String orderNumber;
    private Long orderPrice;

    private Boolean deleteYn;
    private Long version;

    private Long createdUserId;
    private String createdUserType;
    private LocalDateTime createdDatetime;

    private Long modifiedUserId;
    private String modifiedUserType;
    private LocalDateTime modifiedDatetime;

    private Long publishedTimestamp;

    private Boolean failure;

    /**
     * 응답 View -> 애플리케이션 동기화 DTO
     */
    public LocalOrderSync toLocalOrderSync() {
        return new LocalOrderSync(
                id,
                userId,
                userNumber,
                orderId,
                orderNumber,
                orderPrice,
                deleteYn,
                version,
                createdUserId,
                createdUserType,
                createdDatetime,
                modifiedUserId,
                modifiedUserType,
                modifiedDatetime,
                publishedTimestamp,
                Boolean.TRUE.equals(failure)
        );
    }
}
