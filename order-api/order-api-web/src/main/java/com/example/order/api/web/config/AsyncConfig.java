package com.example.order.api.web.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * @Async 경로 MDC 전파 보장.
 * - 비동기 경계에서 MDC(ThreadLocal)를 복제/복원한다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(8);
        ex.setMaxPoolSize(32);
        ex.setQueueCapacity(1000);
        ex.setThreadNamePrefix("async-");
        ex.setTaskDecorator(mdcTaskDecorator());
        ex.initialize();

        return ex;
    }

    @Bean
    public TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> context = MDC.getCopyOfContextMap();

            return () -> {
                Map<String, String> prev = MDC.getCopyOfContextMap();

                if (context != null) {
                    MDC.setContextMap(context);
                } else {
                    MDC.clear();
                }

                try {
                    runnable.run();
                } finally {
                    if (prev != null) {
                        MDC.setContextMap(prev);
                    } else {
                        MDC.clear();
                    }
                }
            };
        };
    }
}
