package com.example.mp.gw.backup.service.impl;


import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.mp.gw.backup.service.BackupMpsService;
import com.example.mp.gw.backup.service.BackupWorker;
import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.domain.Const.Q_BACKUP_RETRY_MP_DCMNT_INFO_FROM_DATE;
import com.example.mp.gw.common.domain.Const.Q_BACKUP_RETRY_PM_MSG_FROM_DATE;
import com.example.mp.gw.common.domain.Const.Q_BACKUP_RETRY_PM_REMIND_MSG_FROM_DATE;
import com.example.mp.gw.common.service.RedisService;
import com.example.mp.gw.maintain.service.MaintainSingleService;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service("BackupWorker")
public class BackupWorkerImpl implements BackupWorker
{
	@Autowired
	BackupMpsService backupMpsService;

	@Autowired
	private MaintainSingleService maintainSingleWorker;

	@Autowired
	private RedisService<String, String> redisRetryPmMsgFromDate;

	@Autowired
	private RedisService<String, String> redisRetryMpDcmntInfoFromDate;

	@Autowired
	private RedisService<String, String> redisRetryPmRemindMsgFromDate;	


	/**
	 * PM_MSG(메시지) 테이블 백업
	 */
	@Override
	@Scheduled(cron = "${spring.backup.cron.pm_msg}")
	public void BackupPmMsg()
	{
		try
		{
			if (false == maintainSingleWorker.isRunning())
				return;

			backupMpsService.BackupPmMsg();
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

	/**
	 * PM_MSG(메시지) 테이블 백업(3일 전)
	 */
	@Override
	@Scheduled(cron = "${spring.backup.cron.pm_msg-3d-ago}")
	public void Backup3DaysAgoPmMsg()
	{
		try
		{
			if (false == maintainSingleWorker.isRunning())
				return;

			backupMpsService.Backup3DaysAgoPmMsg();
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

	/**
	 * PM_MSG(메시지) 테이블 백업(재처리)
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.backup.scheduler.retry.default-delay}")
	public void BackupRetryPmMsgFromDate()
	{
		try
		{
			if (false == maintainSingleWorker.isRunning())
				return;

			if (0 == redisRetryPmMsgFromDate.size(Q_BACKUP_RETRY_PM_MSG_FROM_DATE.STRUCTURE_NAME))
				return;

			backupMpsService.BackupRetryPmMsgFromDate();
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

	/**
	 * MP_DCMNT_INFO(유통정보) 테이블 백업
	 */
	@Override
	@Scheduled(cron = "${spring.backup.cron.mp_dcmnt_info}")
	public void BackupMpDcmntInfo()
	{
		try
		{
			if (false == maintainSingleWorker.isRunning())
				return;

			backupMpsService.BackupMpDcmntInfo();
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

	/**
	 * MP_DCMNT_INFO(유통정보) 테이블 백업(3일 전)
	 */
	@Override
	@Scheduled(cron = "${spring.backup.cron.mp_dcmnt_info-3d-ago}")
	public void Backup3DaysAgoMpDcmntInfo()
	{
		try
		{
			if (false == maintainSingleWorker.isRunning())
				return;

			backupMpsService.Backup3DaysAgoMpDcmntInfo();
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

	/**
	 * MP_DCMNT_INFO(유통정보) 테이블 백업[(재처리)
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.backup.scheduler.retry.default-delay}")
	public void BackupRetryMpDcmntInfoFromDate()
	{
		try
		{
			if (false == maintainSingleWorker.isRunning())
				return;

			if (0 == redisRetryMpDcmntInfoFromDate.size(Q_BACKUP_RETRY_MP_DCMNT_INFO_FROM_DATE.STRUCTURE_NAME))
				return;

			backupMpsService.BackupRetryMpDcmntInfoFromDate();
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

	/**
	 * PM_REDMIND_MSG(리마인드 메시지) 테이블 백업
	 */
	@Override
	@Scheduled(cron = "${spring.backup.cron.pm_remind_msg}")
	public void DeletePmRemindMsg()
	{
		try
		{
			if (false == maintainSingleWorker.isRunning())
				return;

			backupMpsService.DeletePmRemindMsg();
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

	/**
	 * PM_REDMIND_MSG(리마인드 메시지) 테이블 백업(3일 전)
	 */
	@Override
	@Scheduled(cron = "${spring.backup.cron.pm_remind_msg-3d-ago}")
	public void Delete3DaysAgoPmRemindMsg()
	{
		try
		{
			if (false == maintainSingleWorker.isRunning())
				return;

			backupMpsService.Delete3DaysAgoPmRemindMsg();
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

	/**
	 * PM_REDMIND_MSG(리마인드 메시지) 테이블 백업(재처리)
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.backup.scheduler.retry.default-delay}")
	public void DeleteRetryPmRemindMsgFromDate()
	{
		try
		{
			if (false == maintainSingleWorker.isRunning())
				return;

			if (0 == redisRetryPmRemindMsgFromDate.size(Q_BACKUP_RETRY_PM_REMIND_MSG_FROM_DATE.STRUCTURE_NAME))
				return;

			backupMpsService.DeleteRetryPmRemindMsgFromDate();
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
