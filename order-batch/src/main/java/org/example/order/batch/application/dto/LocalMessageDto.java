package org.example.order.batch.application.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.batch.application.message.LocalMessage;
import org.example.order.contract.shared.op.Operation;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LocalMessageDto {
    private String timestamp;
    private LocalMessage message;
    private Operation category;
}
