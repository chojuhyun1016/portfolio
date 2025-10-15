package org.example.order.batch.application.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.contract.shared.op.Operation;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LocalMessage {
    private Long id;
    private Long publishedTimestamp;
    private Operation methodType;
}
