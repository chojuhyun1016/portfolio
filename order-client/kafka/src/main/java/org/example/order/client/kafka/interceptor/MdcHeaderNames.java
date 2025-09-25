package org.example.order.client.kafka.interceptor;

public final class MdcHeaderNames {

    private MdcHeaderNames() {
    }

    // Kafka headers
    public static final String TRACE_ID = "traceId";
    public static final String ORDER_ID = "orderId";

    // MDC keys (동일 키로 사용)
    public static final String MDC_TRACE_ID = TRACE_ID;
    public static final String MDC_ORDER_ID = ORDER_ID;
}
