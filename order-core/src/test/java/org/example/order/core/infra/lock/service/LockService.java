package org.example.order.core.infra.lock.service;

public interface LockService {
    void clear();
    String runWithLock(String key) throws InterruptedException;
    String runWithLockT(String key) throws InterruptedException;
}
