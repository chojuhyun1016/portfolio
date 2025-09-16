package org.example.order.worker.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * CustomSchedulerConfig
 * ------------------------------------------------------------------------
 * 목적
 * - 스케줄러/비동기 경계에서 MDC(ThreadLocal) 전파 보장.
 * <p>
 * 구현
 * - ThreadPoolTaskScheduler 를 확장해 모든 schedule* 지점에서 Runnable 을 데코레이트(MDC 전파)
 * - Spring 6에서 deprecated 된 Date/long 기반 schedule* 오버라이드엔 @SuppressWarnings("deprecation")로 경고 억제
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
@EnableAsync
@EnableScheduling
public class CustomSchedulerConfig implements SchedulingConfigurer {

    private final ObjectProvider<TaskDecorator> taskDecoratorProvider;

    /**
     * MDC TaskDecorator 를 적용하는 스케줄러
     * - Runnable 을 실행 직전에 데코레이터로 감싸 MDC 컨텍스트를 전파/복원.
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        TaskDecorator decorator = taskDecoratorProvider.getIfAvailable(this::defaultMdcTaskDecorator);

        MdcThreadPoolTaskScheduler scheduler = new MdcThreadPoolTaskScheduler(decorator);
        scheduler.setPoolSize(2);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setThreadNamePrefix("sched-");
        scheduler.initialize();

        return scheduler;
    }

    @Override
    public void configureTasks(org.springframework.scheduling.config.ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler());
    }

    /**
     * 로컬 기본 MDC TaskDecorator
     * - 현재 스레드의 MDC 맵을 복제하여 실행 스레드에서 설정/복원
     */
    private TaskDecorator defaultMdcTaskDecorator() {
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

    /**
     * ThreadPoolTaskScheduler 확장: 모든 schedule* 지점에서 Runnable 데코레이션 적용
     * - Spring 6에서 deprecate 된 Date/long 기반 시그니처는 경고 억제
     * - Instant/Duration 오버로드도 지원(프로젝트 Spring 버전에 따라 자동 연결)
     */
    static final class MdcThreadPoolTaskScheduler extends ThreadPoolTaskScheduler {
        private final TaskDecorator decorator;

        MdcThreadPoolTaskScheduler(TaskDecorator decorator) {
            this.decorator = decorator;
        }

        private Runnable decorate(Runnable task) {
            return (decorator != null) ? decorator.decorate(task) : task;
        }

        // ---- 공통 오버로드 ----
        @Override
        public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
            return super.schedule(decorate(task), trigger);
        }

        // ---- Spring 6에서 deprecated 된 Date/long 기반 시그니처 (경고 억제) ----
        @SuppressWarnings("deprecation")
        @Override
        public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
            return super.schedule(decorate(task), startTime);
        }

        @SuppressWarnings("deprecation")
        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
            return super.scheduleAtFixedRate(decorate(task), startTime, period);
        }

        @SuppressWarnings("deprecation")
        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
            return super.scheduleAtFixedRate(decorate(task), period);
        }

        @SuppressWarnings("deprecation")
        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
            return super.scheduleWithFixedDelay(decorate(task), startTime, delay);
        }

        @SuppressWarnings("deprecation")
        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
            return super.scheduleWithFixedDelay(decorate(task), delay);
        }

        // ---- Spring 6.x Instant/Duration 오버로드 (존재하면 컴파일 시 자동 바인딩) ----
        @Override
        public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
            return super.schedule(decorate(task), startTime);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
            return super.scheduleAtFixedRate(decorate(task), startTime, period);
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
            return super.scheduleAtFixedRate(decorate(task), period);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
            return super.scheduleWithFixedDelay(decorate(task), startTime, delay);
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
            return super.scheduleWithFixedDelay(decorate(task), delay);
        }
    }
}
