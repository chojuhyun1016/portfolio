package org.example.order.core.lock.key;

import lombok.extern.slf4j.Slf4j;
import org.example.order.common.utils.hash.HashUtil;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Slf4j
@Component("sha256")
public class SHA256LockKeyGenerator implements LockKeyGenerator {

    @Override
    public String generate(String keyExpression, Method method, Object[] args) {
        StringBuilder sb = new StringBuilder();

        for (Object arg : args) {
            sb.append(arg == null ? "null" : arg.toString());
        }

        String generatedKey = sb.toString();
        return HashUtil.getSHA256Hash(generatedKey);
    }
}
