package org.example.order.common.code.dto;

import org.example.order.common.code.enums.CodeEnum;

public record CodeEnumDto(
        String text,
        String code
) {
    public static CodeEnumDto toDto(CodeEnum codeEnum) {
        return new CodeEnumDto(codeEnum.getText(), codeEnum.getCode());
    }
}
