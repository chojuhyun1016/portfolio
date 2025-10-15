package org.example.order.core.application.order.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 주문 동기화 커맨드 DTO (Application 계층)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderSyncDto {

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

    public void updatePublishedTimestamp(Long publishedTimestamp) {
        this.publishedTimestamp = publishedTimestamp;
    }

    public void updateUserId(Long userId) {
        this.userId = userId;
    }

    public void updateUserNumber(String userNumber) {
        this.userNumber = userNumber;
    }

    public void updateOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public void updateOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void updateOrderPrice(Long orderPrice) {
        this.orderPrice = orderPrice;
    }

    public void markAsFailed() {
        this.failure = true;
    }

    public String getPublishedDateTimeStr() {
        if (this.publishedTimestamp == null) {
            return null;
        }

        return LocalDateTime.ofEpochSecond(this.publishedTimestamp / 1000, 0, java.time.ZoneOffset.UTC)
                .toString()
                .replace("T", " ");
    }
}
