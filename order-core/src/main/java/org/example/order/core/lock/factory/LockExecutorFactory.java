package org.example.order.core.lock.factory;

import lombok.RequiredArgsConstructor;
import org.example.order.core.lock.lock.LockExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class LockExecutorFactory {

    private final Map<String, LockExecutor> executors;

    public LockExecutor getExecutor(String type) {
        LockExecutor executor = executors.get(type);

        if (executor == null) {
            throw new IllegalArgumentException("Unknown lock type: " + type);
        }

        return executor;
    }
}
