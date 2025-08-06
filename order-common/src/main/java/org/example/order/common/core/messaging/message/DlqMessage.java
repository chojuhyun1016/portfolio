package org.example.order.common.core.messaging.message;

import lombok.*;
import org.example.order.common.core.exception.message.CustomErrorMessage;
import org.example.order.common.core.messaging.code.DlqType;

/**
 * 공통 Dead Letter Queue 메시지 구조체
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@ToString
public class DlqMessage {
    private DlqType type;
    private int failedCount = 0;
    private CustomErrorMessage error;

    public DlqMessage(DlqType type) {
        this.type = type;
    }

    public void increaseFailedCount() {
        this.failedCount++;
    }

    public void fail(CustomErrorMessage error) {
        this.error = error;
    }

    public boolean discard(int maxFailCount) {
        return this.failedCount > maxFailCount;
    }
}
