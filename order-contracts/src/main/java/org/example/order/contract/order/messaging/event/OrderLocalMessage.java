package org.example.order.contract.order.messaging.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import org.example.order.contract.order.messaging.type.MessageOrderType;
import org.example.order.contract.shared.op.Operation;

import java.util.Locale;

/**
 * 주문 로컬 메시지 (Contract)
 * ------------------------------------------------------------------------
 * 목적
 * - Producer/Consumer 공용 스키마
 * - 필수값 검증 메서드 제공: validation()
 * - operation은 계약 유연성(후방 호환)을 위해 String 유지
 * <p>
 * Jackson 역직렬화 대응
 * - JsonDeserializer에서 바로 OrderLocalMessage로 역직렬화할 수 있도록
 *
 * @JsonCreator + @JsonProperty 기반의 생성자를 명시적으로 제공한다.
 * - 필드명(id, operation, orderType, publishedTimestamp)과
 * JSON 필드명이 정확히 일치해야 한다.
 */
@Getter
@Builder
public class OrderLocalMessage {

    private final Long id;                    // 주문 ID (필수)
    private final String operation;           // 동작(예: CREATE/UPDATE/DELETE 등) (필수, String 유지)
    private final MessageOrderType orderType; // 메시지 타입 (예: ORDER_LOCAL) (필수)
    private final Long publishedTimestamp;    // 발행 시각(epoch millis) (필수)

    /**
     * Jackson용 생성자
     * - JsonDeserializer가 이 생성자를 사용해 OrderLocalMessage를 생성한다.
     * - @JsonProperty 이름은 JSON 필드명과 정확히 일치해야 한다.
     */
    @JsonCreator
    public OrderLocalMessage(@JsonProperty("id") Long id, @JsonProperty("operation") String operation, @JsonProperty("orderType") MessageOrderType orderType, @JsonProperty("publishedTimestamp") Long publishedTimestamp) {
        this.id = id;
        this.operation = operation;
        this.orderType = orderType;
        this.publishedTimestamp = publishedTimestamp;
    }

    /**
     * 필수값 + Operation 유효성 검증
     */
    public void validation() throws IllegalArgumentException {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("OrderLocalMessage.id is required and must be positive.");
        }

        if (operation == null || operation.isBlank()) {
            throw new IllegalArgumentException("OrderLocalMessage.operation is required.");
        } else {
            String norm = operation.trim().toUpperCase(Locale.ROOT);

            try {
                Operation.valueOf(norm);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("OrderLocalMessage.operation unsupported value: '" + operation + "'. Allowed: " + java.util.Arrays.toString(Operation.values()));
            }
        }

        if (orderType == null) {
            throw new IllegalArgumentException("OrderLocalMessage.orderType is required.");
        }

        if (publishedTimestamp == null || publishedTimestamp <= 0) {
            throw new IllegalArgumentException("OrderLocalMessage.publishedTimestamp is required and must be positive.");
        }
    }
}
