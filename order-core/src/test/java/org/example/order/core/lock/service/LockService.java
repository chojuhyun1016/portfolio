package org.example.order.core.lock.service;

public interface LockService {
    void clear();
    String runWithLock(String key) throws InterruptedException;
    String runWithLockT(String key) throws InterruptedException;
}
