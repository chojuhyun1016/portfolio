package org.example.order.common.core.messaging.message;

import lombok.*;
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
    private Throwable error;

    public DlqMessage(DlqType type) {
        this.type = type;
    }

    public void increaseFailedCount() {
        this.failedCount++;
    }

    public void fail(Throwable error) {
        this.error = error;
    }

    public boolean discard(int maxFailCount) {
        return this.failedCount > maxFailCount;
    }

    public boolean isErrorOfType(Class<? extends Throwable> errorType) {
        return errorType.isInstance(this.error);
    }

    public String getErrorMessage() {
        return error != null ? error.getMessage() : "No error";
    }

    public String getErrorType() {
        return error != null ? error.getClass().getSimpleName() : "No error";
    }
}