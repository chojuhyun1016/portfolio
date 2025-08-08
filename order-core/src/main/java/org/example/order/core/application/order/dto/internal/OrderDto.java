package org.example.order.core.application.order.dto.internal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Local Order 데이터 모델 (Application 계층)
 * - 순수 래퍼 DTO: 매핑 책임 없음
 * - LocalOrderDto만 감싼다
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OrderDto {

    private LocalOrderDto order;

    public void updatePublishedTimestamp(Long publishedTimestamp) {
        if (this.order != null) {
            this.order.updatePublishedTimestamp(publishedTimestamp);
        }
    }

    /**
     * 내부 표준 DTO를 그대로 감싸는 팩토리
     */
    public static OrderDto fromInternal(LocalOrderDto order) {
        return new OrderDto(order);
    }
}
