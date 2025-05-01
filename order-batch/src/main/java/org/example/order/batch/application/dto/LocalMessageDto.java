package org.example.order.batch.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.batch.application.message.LocalMessage;
import org.example.order.common.code.type.MessageCategory;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LocalMessageDto {
    private String timestamp;
    private LocalMessage message;
    private MessageCategory category;
}
