package org.example.order.core.lock.key;

import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.StringJoiner;

@Component("simple")
public class SimpleLockKeyGenerator implements LockKeyGenerator {

    @Override
    public String generate(String keyExpression, Method method, Object[] args) {
        // 단순한 문자열 조합 방식: keyExpression + args.toString()
        StringJoiner joiner = new StringJoiner("_", keyExpression + "_", "");
        for (Object arg : args) {
            joiner.add(arg == null ? "null" : arg.toString());
        }
        return joiner.toString();
    }
}
