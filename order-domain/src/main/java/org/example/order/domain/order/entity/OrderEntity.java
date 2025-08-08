package org.example.order.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 주문 엔티티 - Domain 순수 모델
 * - 비즈니스 로직만 포함
 * - 생성자는 PROTECTED로 막고, 정적 팩토리 메서드 제공
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "`order`")
public class OrderEntity {

    @Id
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

    @Column(columnDefinition = "varchar(20) COMMENT '등록자유형'")
    private String createdUserType;

    @Column(columnDefinition = "datetime COMMENT '등록일시'")
    private LocalDateTime createdDatetime;

    @Column(columnDefinition = "bigint COMMENT '수정자'")
    private Long modifiedUserId;

    @Column(columnDefinition = "varchar(20) COMMENT '수정자유형'")
    private String modifiedUserType;

    @Column(columnDefinition = "datetime COMMENT '수정일시'")
    private LocalDateTime modifiedDatetime;

    @Version
    @Column(columnDefinition = "bigint COMMENT 'Data Version'")
    private Long version;

    public static OrderEntity createEmpty() {
        return new OrderEntity();
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 전체 필드를 한 번에 업데이트 (순수 데이터만 받음)
     */
    public void updateAll(
            Long userId,
            String userNumber,
            Long orderId,
            String orderNumber,
            Long orderPrice,
            Boolean deleteYn,
            Long version,
            LocalDateTime publishedDatetime,
            Long createdUserId,
            String createdUserType,
            LocalDateTime createdDatetime,
            Long modifiedUserId,
            String modifiedUserType,
            LocalDateTime modifiedDatetime
    ) {
        this.userId = userId;
        this.userNumber = userNumber;
        this.orderId = orderId;
        this.orderNumber = orderNumber;
        this.orderPrice = orderPrice;
        this.deleteYn = deleteYn;
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
