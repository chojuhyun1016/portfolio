package org.example.order.contract.order.messaging.type;

/**
 * 메시지 카테고리 (계약 관점)
 * - 서비스 간 합의용 (모든 서비스가 이 enum만 보면 됨)
 */
public enum MessageOrderType {
    ORDER_LOCAL,
    ORDER_API,
    ORDER_CRUD,
    ORDER_REMOTE,
    ORDER_DLQ,
    ORDER_ALARM
}
