package org.example.order.api.master.dto.order;

import lombok.*;
import org.example.order.core.application.order.dto.sync.LocalOrderSync;

import java.time.LocalDateTime;

/**
 * 단건 조회 응답 DTO
 * - OrderSyncDto의 모든 값을 수용 (API 응답 전용)
 * - 외부 계약(Contract) DTO로 사용
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalOrderQueryResponse {

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

    public static LocalOrderQueryResponse from(LocalOrderSync s) {
        if (s == null) {
            return null;
        }

        return LocalOrderQueryResponse.builder()
                .id(s.getId())
                .userId(s.getUserId())
                .userNumber(s.getUserNumber())
                .orderId(s.getOrderId())
                .orderNumber(s.getOrderNumber())
                .orderPrice(s.getOrderPrice())
                .deleteYn(s.getDeleteYn())
                .version(s.getVersion())
                .createdUserId(s.getCreatedUserId())
                .createdUserType(s.getCreatedUserType())
                .createdDatetime(s.getCreatedDatetime())
                .modifiedUserId(s.getModifiedUserId())
                .modifiedUserType(s.getModifiedUserType())
                .modifiedDatetime(s.getModifiedDatetime())
                .publishedTimestamp(s.getPublishedTimestamp())
                .failure(s.getFailure())
                .build();
    }
}
