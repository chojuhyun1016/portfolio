package org.example.order.batch.facade.retry;

public interface OrderDeadLetterFacade {
    /**
     * DLQ에서 1건을 가져와 재처리한다
     */
    void retry();
}
