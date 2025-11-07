package org.example.order.batch.job.deadletter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.batch.facade.retry.OrderDeadLetterFacade;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * OrderDeadLetterJobConfig
 * ------------------------------------------------------------------------
 * 목적
 * - DLQ 재처리 잡 구성(기존 로직 유지)
 * <p>
 * 변경사항
 * - 패키지 경로를 job.deadletter 로 정리.
 * - Tasklet 이 잡 파라미터를 직접 주입받지 않으므로 @StepScope 필수는 아님.
 * (향후 JobParametersAccessor 를 주입받아 파라미터 사용 시 @StepScope 권장)
 * - [중요/권장] RunIdIncrementer 적용: 매 실행마다 고유 run.id를 JobParameters에 추가하여
 * "JobInstance already exists and is not restartable" 문제를 근본적으로 방지.
 * (운영/개발에서 가장 널리 쓰이는 방식)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderDeadLetterJobConfig {

    private final OrderDeadLetterFacade facade;

    public static final String JOB_NAME = "ORDER_DEAD_LETTER_JOB";

    @Bean(name = JOB_NAME)
    public Job job(JobRepository jobRepository, Step orderDeadLetterStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(orderDeadLetterStep)
                .preventRestart()
                .build();
    }

    @Bean
    @JobScope
    public Step orderDeadLetterStep(JobRepository jobRepository,
                                    Tasklet orderDeadLetterTasklet,
                                    PlatformTransactionManager tx) {
        return new StepBuilder(JOB_NAME + ".retry", jobRepository)
                .tasklet(orderDeadLetterTasklet, tx)
                .build();
    }

    /**
     * DLQ 재처리 Tasklet
     * - 현재는 파라미터를 참조하지 않으므로 스코프는 기본.
     * - 만약 JobParametersAccessor 등을 주입해서 파라미터 사용 시 @StepScope 권장.
     */
    @Bean
    public Tasklet orderDeadLetterTasklet() {
        return (contribution, chunkContext) -> {
            log.info("OrderDeadLetterJob start");

            try {
                facade.retry();
            } catch (Exception e) {
                log.error("dead-letter job failed", e);

                throw e;
            }

            return RepeatStatus.FINISHED;
        };
    }
}
