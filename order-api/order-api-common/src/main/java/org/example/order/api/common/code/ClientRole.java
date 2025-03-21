package org.example.order.api.common.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.example.order.common.code.CodeEnum;

@Getter
@RequiredArgsConstructor
public enum ClientRole implements CodeEnum {
    ROLE_CLIENT("ROLE_CLIENT");

    private final String text;
}
