package org.example.order.worker.listener.synchronize.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.order.worker.facade.synchronize.S3LogSyncFacade;
import org.example.order.worker.listener.synchronize.S3LogSyncScheduler;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"!local"})
public class S3LogSyncSchedulerImpl implements S3LogSyncScheduler {

    private final S3LogSyncFacade facade;

    @Override
    @Scheduled(fixedDelay = 10000, initialDelay = 10000)
    public void event() {
        try {
            facade.run();
        } catch (Exception e) {
            log.error("error : s3 log sync event scheduling");
            log.error(e.getMessage(), e);
        }
    }
}
