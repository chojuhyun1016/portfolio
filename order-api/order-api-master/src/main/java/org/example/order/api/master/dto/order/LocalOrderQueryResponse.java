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
                .id(s.id())
                .userId(s.userId())
                .userNumber(s.userNumber())
                .orderId(s.orderId())
                .orderNumber(s.orderNumber())
                .orderPrice(s.orderPrice())
                .deleteYn(s.deleteYn())
                .version(s.version())
                .createdUserId(s.createdUserId())
                .createdUserType(s.createdUserType())
                .createdDatetime(s.createdDatetime())
                .modifiedUserId(s.modifiedUserId())
                .modifiedUserType(s.modifiedUserType())
                .modifiedDatetime(s.modifiedDatetime())
                .publishedTimestamp(s.publishedTimestamp())
                .failure(s.failure())
                .build();
    }
}
