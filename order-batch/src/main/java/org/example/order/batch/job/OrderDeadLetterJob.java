package org.example.order.batch.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.batch.facade.retry.OrderDeadLetterFacade;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class OrderDeadLetterJob {

    private final OrderDeadLetterFacade facade;
    public static final String JOB_NAME = "ORDER_DEAD_LETTER_JOB";

    @Bean(name = JOB_NAME)
    public Job job(JobRepository jobRepository, Step orderDeadLetterStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(orderDeadLetterStep)
                .preventRestart()
                .build();
    }

    @Bean
    @JobScope
    public Step orderDeadLetterStep(JobRepository jobRepository, Tasklet orderDeadLetterTasklet, PlatformTransactionManager platformTransactionManager) {
        return new StepBuilder(String.format("%s.%s", JOB_NAME, "retry"), jobRepository)
                .tasklet(orderDeadLetterTasklet, platformTransactionManager)
                .build();
    }

    @Bean
    @JobScope
    public Tasklet orderDeadLetterTasklet() {
        return ((contribution, chunkContext) -> {
            log.info("OrderDeadLetterJob start");
            facade.retry();
            return RepeatStatus.FINISHED;
        });
    }
}
