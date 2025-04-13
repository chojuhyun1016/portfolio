package org.example.order.core.lock.lock;

public interface LockExecutor {
    Object execute(String key, long waitTime, long leaseTime, LockCallback callback) throws Throwable;
}
