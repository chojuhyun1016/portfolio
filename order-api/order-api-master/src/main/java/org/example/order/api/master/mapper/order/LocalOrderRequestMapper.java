package org.example.order.api.master.mapper.order;

import org.example.order.api.master.dto.order.LocalOrderPublishRequest;
import org.example.order.api.master.dto.order.LocalOrderQueryRequest;
import org.example.order.contract.shared.op.Operation;
import org.example.order.core.application.order.dto.command.LocalOrderCommand;
import org.example.order.core.application.order.dto.query.LocalOrderQuery;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * API Request -> Application Command/Query 변환 (수동 매퍼)
 * 레이어: adapter(api) -> application
 * <p>
 * 리팩토링 포인트:
 * - 입력 null-safe 처리
 * - operation: 최종적으로 Operation enum을 전달
 * · 입력이 Operation이면 그대로
 * · 입력이 String이면 trim+upper 후 Operation.valueOf 로 변환
 * - 변환만 수행(검증은 상위 계층)
 */
@Component
public final class LocalOrderRequestMapper {

    /**
     * LocalOrderPublishRequest -> LocalOrderCommand
     * - LocalOrderPublishRequest.operation() 타입이 Operation(권장) 또는 String일 수 있음
     * - 최종적으로 LocalOrderCommand에는 Operation enum을 전달
     */
    @Nullable
    public LocalOrderCommand toCommand(@Nullable LocalOrderPublishRequest req) {
        if (req == null) {
            return null;
        }

        Operation op = normalizeOperation(req.operation());

        return new LocalOrderCommand(req.orderId(), op);
    }

    /**
     * LocalOrderQueryRequest -> LocalOrderQuery
     */
    @Nullable
    public LocalOrderQuery toQuery(@Nullable LocalOrderQueryRequest req) {
        if (req == null) {
            return null;
        }

        return new LocalOrderQuery(req.getOrderId());
    }

    /* ------------------------ normalize helpers ------------------------ */

    /**
     * 입력이 이미 Operation enum인 경우 그대로 반환
     */
    @Nullable
    private static Operation normalizeOperation(@Nullable Operation op) {
        return op;
    }

    /**
     * 입력이 String인 경우: trim + upper 후 Operation.valueOf 로 변환
     * - null/blank는 null 반환 (검증은 상위 레이어 책임)
     */
    @Nullable
    private static Operation normalizeOperation(@Nullable String op) {
        if (op == null) {
            return null;
        }

        String t = op.trim();

        if (t.isEmpty()) {
            return null;
        }

        return Operation.valueOf(t.toUpperCase(Locale.ROOT));
    }

    /**
     * 그 외 타입이 들어와도 컴파일 단계에서 이 오버로드는 선택되지 않음.
     * (toCommand에서 컴파일러가 Operation 또는 String 중 하나의 오버로드를 고릅니다)
     */
}
