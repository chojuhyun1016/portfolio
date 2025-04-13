package org.example.order.core.lock.lock;

@FunctionalInterface
public interface LockCallback<T> {
    T call() throws Throwable;
}