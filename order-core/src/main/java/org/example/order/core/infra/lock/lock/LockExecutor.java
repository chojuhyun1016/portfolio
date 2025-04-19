package org.example.order.core.infra.lock.lock;

public interface LockExecutor {
    Object execute(String key, long waitTime, long leaseTime, LockCallback callback) throws Throwable;
}
