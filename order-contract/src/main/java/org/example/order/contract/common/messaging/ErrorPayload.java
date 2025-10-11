package org.example.order.contract.common.messaging;

public record ErrorPayload(String code, String message, String detail) {
}
