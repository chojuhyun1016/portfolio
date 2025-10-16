package org.example.order.core.application.order.dto.internal;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 주문 동기화 DTO (Application 계층)
 */
@Getter
@Setter
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
    @Setter(AccessLevel.NONE)
    private Boolean failure = false;

    public void updateCreatedMeta(Long userId, String userType, LocalDateTime datetime) {
        this.createdUserId = userId;
        this.createdUserType = userType;
        this.createdDatetime = datetime;
    }

    public void updateModifiedMeta(Long userId, String userType, LocalDateTime datetime) {
        this.modifiedUserId = userId;
        this.modifiedUserType = userType;
        this.modifiedDatetime = datetime;
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
