package com.example.service;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


@Service
public class SchedulerService
{
    @Scheduled( fixedDelay = 1000 )
    public void schedulerTask1() throws InterruptedException
    {
        System.out.println( "task1 - " + System.currentTimeMillis() / 1000 );
    }

    @Scheduled( fixedDelay = 1000 )
    public void schedulerTask2()
    {
        System.out.println( "task2 - " + System.currentTimeMillis() / 1000 );
    }
}
