package org.example.order.core.infra.lock.key.impl;

import org.example.order.core.infra.lock.key.LockKeyGenerator;

import java.lang.reflect.Method;
import java.util.StringJoiner;

public class SimpleLockKeyGenerator implements LockKeyGenerator {

    @Override
    public String generate(String keyExpression, Method method, Object[] args) {
        StringJoiner joiner = new StringJoiner("_", keyExpression + "_", "");

        for (Object arg : args) {
            joiner.add(arg == null ? "null" : arg.toString());
        }

        return joiner.toString();
    }
}
