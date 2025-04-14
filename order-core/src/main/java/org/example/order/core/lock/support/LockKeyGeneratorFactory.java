package org.example.order.core.lock.support;

import lombok.RequiredArgsConstructor;
import org.example.order.core.lock.key.LockKeyGenerator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class LockKeyGeneratorFactory {

    private final Map<String, LockKeyGenerator> generators;

    public LockKeyGenerator getGenerator(String type) {
        LockKeyGenerator generator = generators.get(type);
        if (generator == null) {
            throw new IllegalArgumentException("Unknown key strategy: " + type);
        }
        return generator;
    }
}
