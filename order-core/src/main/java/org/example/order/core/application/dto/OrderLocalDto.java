package org.example.order.core.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.common.utils.DateTimeUtils;
import org.example.order.core.domain.OrderEntity;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderLocalDto {
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
    @JsonIgnore
    private Boolean failure = false;

    public void postUpdate(Long publishedTimestamp) {
        this.publishedTimestamp = publishedTimestamp;
    }

    public void updateUserId(Long userId) {
        this.userId = userId;
    }

    public void fail() {
        this.failure = true;
    }

    public String getPublishedDateTimeStr() {
        return DateTimeUtils.longToLocalDateTime(this.publishedTimestamp).toString().replace("T", " ");
    }

    public static OrderLocalDto toDto(OrderEntity entity) {
        OrderLocalDto dto = new OrderLocalDto();
        dto.id = entity.getId();
        dto.userId = entity.getUserId();
        dto.userNumber = entity.getUserNumber();
        dto.orderId = entity.getOrderId();
        dto.orderNumber = entity.getOrderNumber();
        dto.orderPrice = entity.getOrderPrice();
        dto.createdUserId = entity.getCreatedUserId();
        dto.createdUserType = entity.getCreatedUserType();
        dto.createdDatetime = entity.getCreatedDatetime();
        dto.modifiedUserId = entity.getModifiedUserId();
        dto.modifiedUserType = entity.getModifiedUserType();
        dto.modifiedDatetime = entity.getModifiedDatetime();
        dto.publishedTimestamp = DateTimeUtils.localDateTimeToLong(entity.getPublishedDatetime());

        return dto;
    }
}
