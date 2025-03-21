package org.example.order.batch.service.retry;

public interface OrderDeadLetterService {
    void retry(Object message);
}
