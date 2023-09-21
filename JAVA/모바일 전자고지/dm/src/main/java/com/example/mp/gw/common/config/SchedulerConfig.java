package com.example.mp.gw.common.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * @Class Name : SchedulerConfig.java
 * @Description : 
 * 
 * @author 조주현
 * @since 2021.05.29
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.05.29	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


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

		if( PROFILES.equals("dev") )
		{
			POOL_SIZE = 128;
		}
		else
		{
			POOL_SIZE = 1024;
		}

		// threadPoolTaskScheduler.setPoolSize(Runtime.getRuntime().availableProcessors() * 2);
		threadPoolTaskScheduler.setPoolSize(POOL_SIZE);
		threadPoolTaskScheduler.setThreadNamePrefix("worker-pool-");
		threadPoolTaskScheduler.initialize();

		taskRegistrar.setTaskScheduler(threadPoolTaskScheduler);
	}
}
