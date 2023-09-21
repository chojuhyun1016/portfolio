package com.example.mp.gw.common.config;
//package com.uplus.mp.gw.common.config;
//
//
//import java.util.concurrent.Executor;
//import java.util.concurrent.ThreadPoolExecutor;
//
//import org.springframework.beans.factory.config.BeanDefinition;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Role;
//import org.springframework.scheduling.annotation.AsyncConfigurerSupport;
//import org.springframework.scheduling.annotation.EnableAsync;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
//
///**
// * @Class Name : SchedulerAsyncConfig.java
// * @Description :
// *
// * @author 조주현
// * @since 2021.11.14
// * @version 1.0
// * @see
// *
// * <pre>
// * << 개정이력(Modification Information) >>
// *
// *   수정일			수정자          수정내용
// *  -----------  -------------    ---------------------------
// *  2021.11.14	    조주현          최초 생성
// *
// *  </pre>
// *
// */
//
//
//@Configuration
//@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
//@EnableAsync
//public class SchedulerAsyncConfig extends AsyncConfigurerSupport
//{
//	@Bean("msMakeRptPool")
//	public Executor msMakeRptPool()
//	{
//		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//		executor.setThreadNamePrefix("ms-make-rpt-pool-");
//		executor.setCorePoolSize(2);
//		executor.setMaxPoolSize(4);
//		executor.setQueueCapacity(0);
//		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
//		executor.setWaitForTasksToCompleteOnShutdown(true);
//		executor.setAwaitTerminationSeconds(10);
//		executor.initialize();
//
//		return executor;
//	}
//
//	@Bean("bcSendSndRptPool")
//	public Executor bcSendSndRptPool()
//	{
//		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//		executor.setThreadNamePrefix("bc-send-snd-rpt-pool-");
//		executor.setCorePoolSize(2);
//		executor.setMaxPoolSize(4);
//		executor.setQueueCapacity(0);
//		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
//		executor.setWaitForTasksToCompleteOnShutdown(true);
//		executor.setAwaitTerminationSeconds(10);
//		executor.initialize();
//
//		return executor;
//	}
//
//	@Bean("bcSendRsvRptPool")
//	public Executor bcSendRsvRptPool()
//	{
//		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//		executor.setThreadNamePrefix("bc-send-rsv-rpt-pool-");
//		executor.setCorePoolSize(2);
//		executor.setMaxPoolSize(4);
//		executor.setQueueCapacity(0);
//		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
//		executor.setWaitForTasksToCompleteOnShutdown(true);
//		executor.setAwaitTerminationSeconds(10);
//		executor.initialize();
//
//		return executor;
//	}
//}
