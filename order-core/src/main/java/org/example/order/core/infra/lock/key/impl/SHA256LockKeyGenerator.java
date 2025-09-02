package org.example.order.core.infra.lock.key.impl;

import org.example.order.common.helper.hash.SecureHashUtils;
import org.example.order.core.infra.lock.key.LockKeyGenerator;

import java.lang.reflect.Method;

public class SHA256LockKeyGenerator implements LockKeyGenerator {

    @Override
    public String generate(String keyExpression, Method method, Object[] args) {
        StringBuilder sb = new StringBuilder();

        for (Object arg : args) {
            sb.append(arg == null ? "null" : arg.toString());
        }

        String generatedKey = sb.toString();

        return SecureHashUtils.getSHA256Hash(generatedKey);
    }
}
