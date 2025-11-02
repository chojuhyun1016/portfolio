package org.example.order.batch.job.tasklet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.batch.facade.retry.OrderDeadLetterFacade;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

/**
 * OrderDeadLetterRetryTasklet
 * - 파라미터가 필요해지면 @StepScope 로 전환하고 JobParametersAccessor 주입.
 */
@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class OrderDeadLetterRetryTasklet implements Tasklet {

    private final OrderDeadLetterFacade facade;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("OrderDeadLetterJob start");

        facade.retry();

        return RepeatStatus.FINISHED;
    }
}
