package org.example.order.core.infra.lock.factory;

import lombok.RequiredArgsConstructor;
import org.example.order.core.infra.lock.key.LockKeyGenerator;

import java.util.Map;

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
