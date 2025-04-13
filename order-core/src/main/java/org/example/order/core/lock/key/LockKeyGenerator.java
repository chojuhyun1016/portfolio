package org.example.order.core.lock.key;

import java.lang.reflect.Method;

public interface LockKeyGenerator {
    String generate(String keyExpression, Method method, Object[] args);
}
