package org.example.order.batch.service.retry.application.message;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.example.order.common.core.messaging.code.MessageMethodType;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class LocalMessage {
    private Long id;
    private Long publishedTimestamp;
    private MessageMethodType methodType;
}
