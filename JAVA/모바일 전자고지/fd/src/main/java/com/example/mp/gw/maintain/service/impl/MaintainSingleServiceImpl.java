package com.example.mp.gw.maintain.service.impl;


import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.domain.Const.FORMATTER;
import com.example.mp.gw.common.domain.RedisStructure.H_SINGLE_WORKER;
import com.example.mp.gw.common.service.RedisService;
import com.example.mp.gw.maintain.domain.WorkerInfo;
import com.example.mp.gw.maintain.service.MaintainSingleService;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

/**
 * @Class Name : MaintainSingleServiceImpl.java
 * @Description : MaintainSingleService 구현 
 * 
 * @author 조주현
 * @since 2023.03.14
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.03.14	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Slf4j
@Service("MaintainSingleService")
public class MaintainSingleServiceImpl implements MaintainSingleService
{
	@Autowired
	private MaintainSingleService maintainSingleWorker;

	@Autowired
	private RedisService<String, WorkerInfo> redisWorkerInfo;

	@Value("${spring.prefix}")
	private String SERVICE_PREFIX;

	@Value("${spring.mt.scheduler.update-limit}")
	private String UPDATE_LIMIT_TIME;


	/**
	 * 서버 기동 시 단일 수행 서비스 정보 설정 (Primary or Secondary)
	 * @return void
	 * @throws Exception 
	 */
	@Override
	public void startWorker()
	{
		WorkerInfo info = null;

		try
		{
			info = redisWorkerInfo.hmget(H_SINGLE_WORKER.STRUCTURE_NAME, H_SINGLE_WORKER.STRUCTURE_NAME);

			maintainSingleWorker.setWorker(info);
		}
		catch(Exception e)
		{
			log.error("{ ■ startWorker - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(info, WorkerInfo.class), e.getClass().getName(), e.getMessage());

			throw e;
		}

		return;
	}

	/**
	 * 서버 수행 중 단일 수행 서비스 정보 갱신 (Primary or Secondary)
	 * @return void
	 * @throws Exception
	 */
	@Override
	public void updateWorker()
	{
		WorkerInfo info = null;

		try
		{
			info = redisWorkerInfo.hmget(H_SINGLE_WORKER.STRUCTURE_NAME, H_SINGLE_WORKER.STRUCTURE_NAME);

			maintainSingleWorker.setWorker(info);
		}
		catch(Exception e)
		{
			log.error("{ ■ updateWorker - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(info, WorkerInfo.class), e.getClass().getName(), e.getMessage());

			throw e;
		}

		return;
	}

	/**
	 * 서버 종료 시 단일 수행 서비스 정보 변경 (Primary <-> Secondary)
	 * @return void
	 * @throws Exception
	 */
	@Override
	public void endWorker()
	{
		WorkerInfo info = null;

		try
		{
			info = redisWorkerInfo.hmget(H_SINGLE_WORKER.STRUCTURE_NAME, H_SINGLE_WORKER.STRUCTURE_NAME);

			if (SERVICE_PREFIX.equals(info.getPrefix()))
				maintainSingleWorker.switchWorker(info);
		}
		catch(Exception e)
		{
			log.error("{ ■ endWorker - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(info, WorkerInfo.class), e.getClass().getName(), e.getMessage());

			throw e;
		}

		return;
	}

	/**
	 * 단일 수행 서비스 정보 설정 (유지/변경)
	 * @param WorkerInfo
	 * @return void
	 * @throws Exception
	 */
	@Override
	public void setWorker(WorkerInfo info)
	{
		try
		{
			long    now     = System.currentTimeMillis();
			long    std     = System.currentTimeMillis()- Long.parseLong(UPDATE_LIMIT_TIME);

			String	nowDtm  = FORMATTER.yyyyMMddHHmmss.val().format(now);
			String	stdDtm  = FORMATTER.yyyyMMddHHmmss.val().format(std);

			// 1. 서비스 정보가 존재하는 경우
			if (null != info)
			{
				// 1. 수행 서비스 정보의 서비스 Prefix 가 현재 서버의 Prefix 와 동일한 경우 (수행서버 = 현서버)  
				if (SERVICE_PREFIX.equals(info.getPrefix()))
				{
					// 1. 서비스 수행(running='Y') 상태가 아닌 경우 수행 상태로 변경
					if (!Const.Y.equals(info.getRunning().get(Integer.parseInt(info.getPrefix()) - 1)))
					{
						info.getRunning().set(Integer.parseInt(info.getPrefix()) - 1, Const.Y);
						info.setUpdatedDtm(nowDtm);

						redisWorkerInfo.hmset(H_SINGLE_WORKER.STRUCTURE_NAME, H_SINGLE_WORKER.STRUCTURE_NAME, info);

						log.info("{ ■ setWorker - set ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(info, WorkerInfo.class));
					}
				}
				// 2. 수행 서비스 정보의 서비스 prefix 가 현재 서버의 prefix 가 다른 경우 (수행서버 != 현서버)  
				else
				{
					// 1. 현재 서버가 아닌 이중화 서버의 Prefix 설정
					String prefix = SERVICE_PREFIX.equals(H_SINGLE_WORKER.PRIMARY.val()) ?
									H_SINGLE_WORKER.SECONDARY.val() : H_SINGLE_WORKER.PRIMARY.val();

					// 2. 이중화 서버의 상태가 미수행(running="N") 이며 마지막 갱신 시각이 임계 시간을 초과한 경우
					//     -> 수행 서비스 정보의 서비스 정보를 현재 서버의 상태로 갱신
					if ((Const.N.equals(info.getRunning().get(Integer.parseInt(prefix) - 1)))
					&& (Long.parseLong(stdDtm) > Long.parseLong(info.getUpdatedDtm())))
					{
						info.setPrefix(SERVICE_PREFIX);
						info.getRunning().set(Integer.parseInt(info.getPrefix()) - 1, Const.Y);
						info.setUpdatedDtm(nowDtm);

						redisWorkerInfo.hmset(H_SINGLE_WORKER.STRUCTURE_NAME, H_SINGLE_WORKER.STRUCTURE_NAME, info);

						log.info("{ ■ setWorker - switch ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(info, WorkerInfo.class));
					}
				}
			}
			//2. 서비스 정보가 존재하지 않는 경우
			else
			{
				// 1. 수행 서비스 정보 객체 생성
				info = WorkerInfo.builder().prefix(SERVICE_PREFIX).build();

				// 2. Primary, Secondary 조회
				boolean primary = SERVICE_PREFIX.equals(H_SINGLE_WORKER.PRIMARY.val()) ? true : false;

				// 3. 수행 서비스 정보 객체 설정(Primary, Second 정보를 동시에 설정)
				info.setPrefix(SERVICE_PREFIX);
				info.getRunning().add(Integer.parseInt(H_SINGLE_WORKER.PRIMARY.val())   - 1, primary ? Const.Y : Const.N);
				info.getRunning().add(Integer.parseInt(H_SINGLE_WORKER.SECONDARY.val()) - 1, primary ? Const.N : Const.Y);
				info.setUpdatedDtm(nowDtm);

				// 4. Redis 수행 서비스 정보 생성
				redisWorkerInfo.hmset(H_SINGLE_WORKER.STRUCTURE_NAME, H_SINGLE_WORKER.STRUCTURE_NAME, info);

				log.info("{ ■ setWorker - new ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(info, WorkerInfo.class));
			}
			
			return;
		}
		catch(Exception e)
		{
			log.error("{ ■ setWorker - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(info, WorkerInfo.class), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * 단일 수행 서비스 정보 변경 (Primary <-> Secondary)
	 * @param WorkerInfo
	 * @return void
	 * @throws Exception
	 */
	@Override
	public void switchWorker(WorkerInfo info)
	{
		try
		{
			// 1. 현재 서버가 아닌 이중화 서버의 Prefix 설정
			String prefix = SERVICE_PREFIX.equals(H_SINGLE_WORKER.PRIMARY.val()) ?
							H_SINGLE_WORKER.SECONDARY.val() : H_SINGLE_WORKER.PRIMARY.val();

			info.getRunning().set(Integer.parseInt(info.getPrefix()) - 1, Const.N);
			info.setPrefix(prefix);
			info.setUpdatedDtm(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));

			// 2. 이중화 서버의 정보를 Redis 에 서비스 서버로 등록
			redisWorkerInfo.hmset(H_SINGLE_WORKER.STRUCTURE_NAME, H_SINGLE_WORKER.STRUCTURE_NAME, info);

			log.info("{ ■ switchWorker - switch ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(info, WorkerInfo.class));
		}
		catch(Exception e)
		{
			log.error("{ ■ switchWorker - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(info, WorkerInfo.class), e.getClass().getName(), e.getMessage());

			throw e;
		}

		return;
	}

	/**
	 * 단일 수행 상태 조회
	 * @return boolean
	 * @throws Exception
	 */
	@Override
	public boolean isRunning()
	{
		WorkerInfo info = null;

		try
		{
			// 1. Redis 등록 기동 서버 조회
			info = redisWorkerInfo.hmget(H_SINGLE_WORKER.STRUCTURE_NAME, H_SINGLE_WORKER.STRUCTURE_NAME);

			// 2. 기동 서버 유무 반환
			return  SERVICE_PREFIX.equals(info.getPrefix()) && Const.Y.equals(info.getRunning().get(Integer.parseInt(info.getPrefix()) - 1 )); 
		}
		catch(Exception e)
		{
			log.error("{ ■ isRunning - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(info, WorkerInfo.class), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}
}
