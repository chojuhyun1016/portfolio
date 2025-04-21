package org.example.order.core.infra.common.idgen.table;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @Description TableGenerator에서 사용할 Generator 이름 정의
 * 엔티티별 @TableGenerator(name = ...) 선언 없이 사용할 경우 여기서 상수 참조
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SequenceDefine {

    public static final String SEQUENCE_TABLE = "id_sequence";

    // 예: 판매
    public static final String GENERATOR_SALES_ORDER = "SalesOrderIdGenerator";
    public static final String GENERATOR_SALES_PAYMENT = "SalesPaymentIdGenerator";

    // 정산
    public static final String GENERATOR_SETTLE_BASE_MONTHLY = "SettleBaseMonthlyIdGenerator";
    public static final String GENERATOR_SETTLE_COMMISSION = "SettleCommissionIdGenerator";

    // 환수
    public static final String GENERATOR_REFUND_COMMISSION = "RefundCommissionIdGenerator";

    // 기타
    public static final String GENERATOR_CUSTOM_USER = "CustomUserIdGenerator";

    // 필요 시 추가 정의
}
