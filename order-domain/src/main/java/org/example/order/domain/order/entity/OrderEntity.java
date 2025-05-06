package org.example.order.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.order.common.helper.datetime.DateTimeUtils;
import org.example.order.core.application.order.command.OrderSyncCommand;
import org.example.order.core.infra.common.idgen.tsid.annotation.CustomTsid;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "order")
public class OrderEntity {

    @Id
    @CustomTsid
    private Long id;

    @Column(columnDefinition = "bigint COMMENT '사용자ID'")
    private Long userId;

    @Column(columnDefinition = "varchar(50) COMMENT '사용자번호'")
    private String userNumber;

    @Column(columnDefinition = "bigint COMMENT '주문ID'")
    private Long orderId;

    @Column(columnDefinition = "varchar(50) COMMENT '주문번호'")
    private String orderNumber;

    @Column(columnDefinition = "bigint COMMENT '주문가격'")
    private Long orderPrice;

    @Column(columnDefinition = "varchar(1) COMMENT '삭제여부'")
    private Boolean deleteYn;

    @Column(columnDefinition = "datetime COMMENT 'kafka published datetime'")
    private LocalDateTime publishedDatetime;

    @Column(columnDefinition = "bigint COMMENT '등록자'")
    private Long createdUserId;

    @Column(columnDefinition = "bigint COMMENT '등록자유형'")
    private String createdUserType;

    @Column(columnDefinition = "bigint COMMENT '등록일시'")
    private LocalDateTime createdDatetime;

    @Column(columnDefinition = "bigint COMMENT '수정자'")
    private Long modifiedUserId;

    @Column(columnDefinition = "bigint COMMENT '수정자유형'")
    private String modifiedUserType;

    @Column(columnDefinition = "bigint COMMENT '수정일시'")
    private LocalDateTime modifiedDatetime;

    @Version
    @Column(columnDefinition = "bigint COMMENT 'Data Version'")
    private Long version;

    public void updateTsid(Long tsid) {
        this.id = tsid;
    }

    public void update(OrderSyncCommand dto) {
        update(this, dto);
    }

    public static OrderEntity toEntity(OrderSyncCommand dto) {
        OrderEntity entity = new OrderEntity();
        update(entity, dto);
        return entity;
    }

    private static void update(OrderEntity entity, OrderSyncCommand dto) {
        entity.userId = dto.getUserId();
        entity.userNumber = dto.getUserNumber();
        entity.orderId = dto.getOrderId();
        entity.orderNumber = dto.getOrderNumber();
        entity.orderPrice = dto.getOrderPrice();
        entity.deleteYn = dto.getDeleteYn();
        entity.version = dto.getVersion();
        entity.publishedDatetime = DateTimeUtils.longToLocalDateTime(dto.getPublishedTimestamp());
        entity.createdUserId = dto.getCreatedUserId();
        entity.createdUserType = dto.getCreatedUserType();
        entity.createdDatetime = dto.getCreatedDatetime();
        entity.modifiedUserId = dto.getModifiedUserId();
        entity.modifiedUserType = dto.getModifiedUserType();
        entity.modifiedDatetime = dto.getModifiedDatetime();
    }
}
