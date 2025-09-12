package org.example.order.common.autoconfigure.logging;

import org.example.order.common.support.logging.CorrelationAspect;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * LoggingSupportAutoConfiguration
 * ------------------------------------------------------------------------
 * 목적
 * - @Async/스레드풀 경계에서 MDC(ThreadLocal)를 복제/복원하여 traceId 등 컨텍스트 전파.
 * 특징
 * - Boot 기본 실행기(TaskExecutorCustomizer)는 TaskDecorator 빈을 감지해 적용한다.
 * - 커스텀 ThreadPoolTaskExecutor/TaskScheduler에는 setTaskDecorator(...)로 직접 적용 필요.
 * - CorrelationAspect를 오토컨피그로 등록해 컴포넌트 스캔에 의존하지 않도록 한다.
 */
@AutoConfiguration
public class LoggingSupportAutoConfiguration {

    /**
     * MDC 컨텍스트 전파용 TaskDecorator
     */
    @Bean
    @ConditionalOnMissingBean(name = "mdcTaskDecorator")
    public TaskDecorator mdcTaskDecorator() {
        return (Runnable runnable) -> {
            Map<String, String> context = MDC.getCopyOfContextMap();

            return () -> {
                Map<String, String> prev = MDC.getCopyOfContextMap();

                if (context != null) {
                    MDC.setContextMap(context);
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

    /**
     * @Correlate AOP Aspect 등록(@Component 스캔에 의존하지 않음)
     */
    @Bean
    @ConditionalOnMissingBean(CorrelationAspect.class)
    public CorrelationAspect correlationAspect() {
        return new CorrelationAspect();
    }
}
