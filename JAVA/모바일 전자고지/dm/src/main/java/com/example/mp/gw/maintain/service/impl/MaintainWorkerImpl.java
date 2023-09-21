package com.example.mp.gw.maintain.service.impl;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.maintain.service.MaintainSingleService;
import com.example.mp.gw.maintain.service.MaintainWorker;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service("MaintainWorker")
public class MaintainWorkerImpl implements MaintainWorker
{
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger("com.uplus.mp.gw.mt");

	@Autowired
	private MaintainSingleService maintainSingleWorker;


	/**
	 * 단일 수행 서비스 정보 업데이트 스케줄러
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.mt.scheduler.default-delay}")
	public void updateWorker()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			maintainSingleWorker.updateWorker();
		}
		catch(Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());
		}
		finally
		{
			MDC.clear();
		}
	}
}
