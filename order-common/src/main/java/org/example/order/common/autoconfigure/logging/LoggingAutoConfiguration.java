package org.example.order.common.autoconfigure.logging;

import org.example.order.common.support.logging.CorrelationAspect;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * LoggingAutoConfiguration (최종본)
 * - MDC TaskDecorator 전파(기존 기능 유지)
 * - @Correlate AOP 자동 등록 + AspectJ 프록시 활성화
 */
@AutoConfiguration
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = false)
public class LoggingAutoConfiguration {

    /**
     * MDC 컨텍스트 전파용 TaskDecorator (기존 유지)
     */
    @Bean
    @ConditionalOnMissingBean(name = "mdcTaskDecorator")
    public TaskDecorator mdcTaskDecorator() {
        return runnable -> {
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
     * @Correlate AOP Aspect 등록
     */
    @Bean
    @ConditionalOnMissingBean(CorrelationAspect.class)
    public CorrelationAspect correlationAspect() {
        return new CorrelationAspect();
    }
}
