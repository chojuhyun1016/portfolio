package org.example.order.core.application.order.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.order.domain.order.entity.OrderEntity;

import java.time.LocalDateTime;

/**
 * Local Order 내부 전송용 DTO (Application 내부에서만 사용)
 * - Entity 참조 제거
 * - 필요한 필드만 보관
 * - 외부 노출 없음 (internal 패키지)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntityDto {

    private Long id;
    private Long userId;
    private String userNumber;
    private Long orderId;
    private String orderNumber;
    private Long orderPrice;
    private Boolean deleteYn;
    private LocalDateTime publishedDatetime;
    private Long createdUserId;
    private String createdUserType;
    private LocalDateTime createdDatetime;
    private Long modifiedUserId;
    private String modifiedUserType;
    private LocalDateTime modifiedDatetime;
    private Long version;

    /**
     * Entity → DTO 변환 메서드
     */
    public static OrderEntityDto fromEntity(OrderEntity entity) {
        if (entity == null) {
            return null;
        }

        return OrderEntityDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .userNumber(entity.getUserNumber())
                .orderId(entity.getOrderId())
                .orderNumber(entity.getOrderNumber())
                .orderPrice(entity.getOrderPrice())
                .deleteYn(entity.getDeleteYn())
                .publishedDatetime(entity.getPublishedDatetime())
                .createdUserId(entity.getCreatedUserId())
                .createdUserType(entity.getCreatedUserType())
                .createdDatetime(entity.getCreatedDatetime())
                .modifiedUserId(entity.getModifiedUserId())
                .modifiedUserType(entity.getModifiedUserType())
                .modifiedDatetime(entity.getModifiedDatetime())
                .version(entity.getVersion())
                .build();
    }
}
