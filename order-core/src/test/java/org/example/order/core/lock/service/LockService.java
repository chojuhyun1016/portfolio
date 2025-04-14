package org.example.order.core.lock.service;

public interface LockService {
    String runWithLock(String key) throws InterruptedException;
}
