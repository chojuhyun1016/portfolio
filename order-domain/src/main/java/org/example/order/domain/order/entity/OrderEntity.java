package org.example.order.domain.order.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.order.common.support.jpa.converter.BooleanToYNConverter;

import java.time.LocalDateTime;

/**
 * 주문 엔티티
 * - delete_yn: 공용 컨버터(Boolean <-> 'Y'/'N') 적용
 */
@Entity
@Table(name = "`order`")
@Getter
@Setter
@NoArgsConstructor
public class OrderEntity {

    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_number", length = 50, nullable = false)
    private String userNumber;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Column(name = "order_number", length = 50, nullable = false)
    private String orderNumber;

    @Column(name = "order_price", nullable = false)
    private Long orderPrice;

    @Convert(converter = BooleanToYNConverter.class)
    @Column(name = "delete_yn", columnDefinition = "varchar(1) not null")
    private Boolean deleteYn = false;

    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "published_datetime", nullable = false)
    private LocalDateTime publishedDatetime;

    @Column(name = "created_user_id", nullable = false)
    private Long createdUserId;

    @Column(name = "created_user_type", length = 50, nullable = false)
    private String createdUserType;

    @Column(name = "created_datetime", nullable = false)
    private LocalDateTime createdDatetime;

    @Column(name = "modified_user_id", nullable = false)
    private Long modifiedUserId;

    @Column(name = "modified_user_type", length = 50, nullable = false)
    private String modifiedUserType;

    @Column(name = "modified_datetime", nullable = false)
    private LocalDateTime modifiedDatetime;

    public static OrderEntity createEmpty() {
        return new OrderEntity();
    }

    public void updateAll(
            Long userId, String userNumber,
            Long orderId, String orderNumber,
            Long orderPrice, Boolean deleteYn, Long version,
            LocalDateTime publishedDatetime,
            Long createdUserId, String createdUserType, LocalDateTime createdDatetime,
            Long modifiedUserId, String modifiedUserType, LocalDateTime modifiedDatetime
    ) {
        this.userId = userId;
        this.userNumber = userNumber;
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.orderPrice = orderPrice;
        this.deleteYn = (deleteYn != null ? deleteYn : Boolean.FALSE);
        this.version = version;
        this.publishedDatetime = publishedDatetime;
        this.createdUserId = createdUserId;
        this.createdUserType = createdUserType;
        this.createdDatetime = createdDatetime;
        this.modifiedUserId = modifiedUserId;
        this.modifiedUserType = modifiedUserType;
        this.modifiedDatetime = modifiedDatetime;
    }
}
