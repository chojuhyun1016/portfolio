package org.example.order.common.core.code.dto;

import org.example.order.common.core.code.type.CodeEnum;

public record CodeEnumDto(
        String text,
        String code
) {
    public static CodeEnumDto toDto(CodeEnum codeEnum) {
        return new CodeEnumDto(codeEnum.getText(), codeEnum.getCode());
    }
}
