package org.example.order.core.infra.lock.lock;

@FunctionalInterface
public interface LockCallback<T> {
    T call() throws Throwable;
}