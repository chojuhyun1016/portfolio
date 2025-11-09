package org.example.order.batch.config;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * BatchExitCodeConfig
 * ------------------------------------------------------------------------
 * 목적
 * - 배치 실행 결과를 OS 종료코드로 매핑.
 * - 성공(COMPLETED)  : 0
 * - 그 외(FAILED 등) : 1
 * 사용처
 * - OrderBatchApplication 에서 SpringApplication.exit(ctx) 호출 시
 * 등록된 ExitCodeGenerator 값을 반영해 종료코드를 결정.
 * 주의
 * - 배치 모듈에만 존재하므로 다른 서비스에 영향 없음.
 */
@Configuration
public class BatchExitCodeConfig {

    /**
     * 종료코드 저장소 (기본 0)
     */
    @Bean
    public AtomicInteger batchExitCodeHolder() {
        return new AtomicInteger(0);
    }

    /**
     * 잡 종료 시점에 종료코드 계산
     */
    @Bean
    public JobExecutionListenerSupport jobExitCodeListener(AtomicInteger batchExitCodeHolder) {
        return new JobExecutionListenerSupport() {
            @Override
            public void afterJob(JobExecution jobExecution) {
                BatchStatus status = jobExecution.getStatus();

                // COMPLETED 외엔 모두 비정상 종료코드 1
                int code = (status == BatchStatus.COMPLETED) ? 0 : 1;

                batchExitCodeHolder.set(code);
            }
        };
    }

    /**
     * SpringApplication.exit(ctx) 가 읽어갈 종료코드 Bean
     */
    @Bean
    public ExitCodeGenerator batchExitCodeGenerator(AtomicInteger batchExitCodeHolder) {
        return batchExitCodeHolder::get;
    }
}
