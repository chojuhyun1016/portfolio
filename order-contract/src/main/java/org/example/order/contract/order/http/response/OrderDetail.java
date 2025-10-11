package org.example.order.contract.order.http.response;

/**
 * 주문 상세 응답 DTO (외부 노출용 View)
 * - publishedDatetime은 포맷 문자열(예: "2025-10-09 19:24:10")로 반환하는 걸 권장
 */
public record OrderDetail(
        Long id,
        Long userId,
        String userNumber,
        Long orderId,
        String orderNumber,
        Long orderPrice,
        Boolean deleteYn,
        String publishedDatetime,
        Long version
) {
}
