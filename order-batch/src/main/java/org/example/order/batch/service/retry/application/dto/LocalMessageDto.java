package org.example.order.batch.service.retry.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.batch.service.retry.application.message.LocalMessage;
import org.example.order.core.messaging.order.code.MessageCategory;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LocalMessageDto {
    private String timestamp;
    private LocalMessage message;
    private MessageCategory category;
}
