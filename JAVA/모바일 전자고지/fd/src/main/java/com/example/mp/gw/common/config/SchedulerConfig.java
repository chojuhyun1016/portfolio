package com.example.mp.gw.common.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
public class SchedulerConfig implements SchedulingConfigurer
{
	@Value("${spring.profiles}")
	private String PROFILES;

	private int POOL_SIZE = 0;

	/**
	 * @param taskRegistrar
	 */
	@Override
	public void configureTasks(ScheduledTaskRegistrar taskRegistrar)
	{
		ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();

		if( PROFILES.equals("dev") || PROFILES.equals("local"))
		{
			POOL_SIZE = 128;
		}
		else
		{
			POOL_SIZE = 1024;
		}

		threadPoolTaskScheduler.setPoolSize(POOL_SIZE);
		threadPoolTaskScheduler.setThreadNamePrefix("worker-pool-");
		threadPoolTaskScheduler.initialize();

		taskRegistrar.setTaskScheduler(threadPoolTaskScheduler);
	}
}
