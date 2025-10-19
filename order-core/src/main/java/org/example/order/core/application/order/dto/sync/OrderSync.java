package org.example.order.core.application.order.dto.sync;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 주문 동기화 DTO (Application 계층 - 동기화/파이프라인 전용)
 * - 불변(record) + with-메서드로 상태 변형 시 새 인스턴스 반환
 * - 외부 계약(HTTP/토픽 스키마)과 분리 (API/Contracts에서 별도 정의)
 */
public record OrderSync(
        Long id,

        Long userId,
        String userNumber,

        Long orderId,
        String orderNumber,
        Long orderPrice,

        Boolean deleteYn,
        Long version,

        Long createdUserId,
        String createdUserType,
        LocalDateTime createdDatetime,

        Long modifiedUserId,
        String modifiedUserType,
        LocalDateTime modifiedDatetime,

        Long publishedTimestamp,

        @JsonIgnore
        boolean failure
) {
    /**
     * 생성 메타데이터를 덮어쓴 사본을 반환
     */
    public OrderSync withCreatedMeta(Long userId, String userType, LocalDateTime datetime) {
        return new OrderSync(
                this.id,
                this.userId,            // 기존 userId 유지 (생성자 userId 아님)
                this.userNumber,
                this.orderId,
                this.orderNumber,
                this.orderPrice,
                this.deleteYn,
                this.version,
                userId,                 // createdUserId
                userType,               // createdUserType
                datetime,               // createdDatetime
                this.modifiedUserId,
                this.modifiedUserType,
                this.modifiedDatetime,
                this.publishedTimestamp,
                this.failure
        );
    }

    /**
     * 수정 메타데이터를 덮어쓴 사본을 반환
     */
    public OrderSync withModifiedMeta(Long userId, String userType, LocalDateTime datetime) {
        return new OrderSync(
                this.id,
                this.userId,
                this.userNumber,
                this.orderId,
                this.orderNumber,
                this.orderPrice,
                this.deleteYn,
                this.version,
                this.createdUserId,
                this.createdUserType,
                this.createdDatetime,
                userId,                 // modifiedUserId
                userType,               // modifiedUserType
                datetime,               // modifiedDatetime
                this.publishedTimestamp,
                this.failure
        );
    }

    /**
     * failure 플래그를 true로 설정한 사본을 반환
     */
    public OrderSync withFailure() {
        if (this.failure) return this;
        return new OrderSync(
                this.id,
                this.userId,
                this.userNumber,
                this.orderId,
                this.orderNumber,
                this.orderPrice,
                this.deleteYn,
                this.version,
                this.createdUserId,
                this.createdUserType,
                this.createdDatetime,
                this.modifiedUserId,
                this.modifiedUserType,
                this.modifiedDatetime,
                this.publishedTimestamp,
                true
        );
    }

    /**
     * publishedTimestamp(밀리초) → "yyyy-MM-dd HH:mm:ss" 유사 포맷 문자열
     * (기존 getPublishedDateTimeStr 대체 / UTC 기준)
     */
    public String publishedDateTimeStr() {
        if (this.publishedTimestamp == null) return null;
        return LocalDateTime.ofEpochSecond(this.publishedTimestamp / 1000, 0, ZoneOffset.UTC)
                .toString()
                .replace("T", " ");
    }

    /**
     * publishedTimestamp를 덮어쓴 사본을 반환
     */
    public OrderSync withPublishedTimestamp(Long newTs) {
        return new OrderSync(
                this.id,
                this.userId,
                this.userNumber,
                this.orderId,
                this.orderNumber,
                this.orderPrice,
                this.deleteYn,
                this.version,
                this.createdUserId,
                this.createdUserType,
                this.createdDatetime,
                this.modifiedUserId,
                this.modifiedUserType,
                this.modifiedDatetime,
                newTs,
                this.failure
        );
    }

    /**
     * orderNumber, price, version 등 일부 필드만 바꾸고 싶을 때 사용할 수 있는 with-메서드 예시
     * 필요 시 추가 정의해서 쓰세요.
     */
    public OrderSync withOrderNumber(String newOrderNumber) {
        return new OrderSync(
                this.id,
                this.userId,
                this.userNumber,
                this.orderId,
                newOrderNumber,
                this.orderPrice,
                this.deleteYn,
                this.version,
                this.createdUserId,
                this.createdUserType,
                this.createdDatetime,
                this.modifiedUserId,
                this.modifiedUserType,
                this.modifiedDatetime,
                this.publishedTimestamp,
                this.failure
        );
    }

    public OrderSync withOrderPrice(Long newOrderPrice) {
        return new OrderSync(
                this.id,
                this.userId,
                this.userNumber,
                this.orderId,
                this.orderNumber,
                newOrderPrice,
                this.deleteYn,
                this.version,
                this.createdUserId,
                this.createdUserType,
                this.createdDatetime,
                this.modifiedUserId,
                this.modifiedUserType,
                this.modifiedDatetime,
                this.publishedTimestamp,
                this.failure
        );
    }

    public OrderSync withVersion(Long newVersion) {
        return new OrderSync(
                this.id,
                this.userId,
                this.userNumber,
                this.orderId,
                this.orderNumber,
                this.orderPrice,
                this.deleteYn,
                newVersion,
                this.createdUserId,
                this.createdUserType,
                this.createdDatetime,
                this.modifiedUserId,
                this.modifiedUserType,
                this.modifiedDatetime,
                this.publishedTimestamp,
                this.failure
        );
    }
}
