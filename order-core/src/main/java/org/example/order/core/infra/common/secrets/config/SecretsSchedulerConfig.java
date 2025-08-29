package org.example.order.core.infra.common.secrets.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * SecretsLoader의 프로그램적 스케줄링을 위한 TaskScheduler 제공
 * - 다른 TaskScheduler가 이미 있다면 그 빈을 사용
 * - 없을 때만 1스레드 스케줄러를 제공(중복 실행 자체 방지)
 */
@Configuration
public class SecretsSchedulerConfig {

    @Bean
    @ConditionalOnMissingBean(TaskScheduler.class)
    public TaskScheduler secretsTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);                  // 단일 스레드
        scheduler.setThreadNamePrefix("secrets-"); // 디버깅 편의
        scheduler.initialize();
        return scheduler;
    }
}
