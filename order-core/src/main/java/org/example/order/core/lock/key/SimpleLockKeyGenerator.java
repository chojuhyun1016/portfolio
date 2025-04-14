package org.example.order.core.lock.key;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.StringJoiner;

@Slf4j
@Component("simple")
public class SimpleLockKeyGenerator implements LockKeyGenerator {

    @Override
    public String generate(String keyExpression, Method method, Object[] args) {
        System.out.println("==== SimpleLockKeyGenerator.generate() called ====");
        log.info("before:{}", keyExpression);

        StringJoiner joiner = new StringJoiner("_", keyExpression + "_", "");

        for (Object arg : args) {
            joiner.add(arg == null ? "null" : arg.toString());
        }

        log.info("after key:{}", joiner.toString());

        return joiner.toString();
    }
}
