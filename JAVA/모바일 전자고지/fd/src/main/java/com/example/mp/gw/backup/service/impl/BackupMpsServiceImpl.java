package com.example.mp.gw.backup.service.impl;


import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.mp.gw.backup.exception.NotMatchCountException;
import com.example.mp.gw.backup.mappers.altibase.BackupMapper;
import com.example.mp.gw.backup.service.BackupMpsService;
import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.domain.Const.FORMATTER;
import com.example.mp.gw.common.domain.Const.Q_BACKUP_RETRY_MP_DCMNT_INFO_FROM_DATE;
import com.example.mp.gw.common.domain.Const.Q_BACKUP_RETRY_PM_MSG_FROM_DATE;
import com.example.mp.gw.common.domain.Const.Q_BACKUP_RETRY_PM_REMIND_MSG_FROM_DATE;
import com.example.mp.gw.common.exception.DateFormatException;
import com.example.mp.gw.common.service.RedisService;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service("BackupMpsService")
public class BackupMpsServiceImpl implements BackupMpsService
{
	@Autowired
	private BackupMapper backupMapper;

	@Autowired
	private RedisService<String, String> redisRetryPmMsgFromDate;

	@Autowired
	private RedisService<String, String> redisRetryMpDcmntInfoFromDate;

	@Autowired
	private RedisService<String, String> redisRetryPmRemindMsgFromDate;

	final private String MPS_ACCOUNT         = "MPS";           // MPS DB 계정
	final private String MPS_BACKUP_ACCOUNT  = "MPS_BACKUP";    // MPS_BACKUP DB 계정

	final private String PM_MSG_TABLE        = "PM_MSG";        // PM_MSG 테이블
	final private String PM_REMIND_MSG_TABLE = "PM_REMIND_MSG"; // PM_REMIND_MSG 테이블
	final private String MP_DCMNT_INFO_TABLE = "MP_DCMNT_INFO"; // MP_DCMNT_INFO 테이블


	/**
	 * PM_MSG(메시지) 테이블 백업 서비스
	 * @return void
	 * @throws NotMatchCountException  
	 * @throws DataAccessException 
	 * @throws Exception
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void BackupPmMsg()
	{
		int insert = 0;
		int delete = 0;

		Map<String, String> target = new HashMap<String, String>();

		try
		{
			insert = 0;
			delete = 0;

			Date     now = new Date();
			Calendar cal = Calendar.getInstance();

			// 1. 날짜 설정(-1년)
			cal.setTime(now);
			cal.add(Calendar.YEAR, -1);

			// 2. 대상 데이터 설정
			//    백업 : MPS_BACKUP.PM_MSG_0X (X:0-9)
			//    삭제 : MPS.PM_MSG
			target.put("insertTable", MPS_BACKUP_ACCOUNT + "." + PM_MSG_TABLE + "_" + "0" + FORMATTER.yyyy.val().format(cal.getTimeInMillis()).substring(3, 4));
			target.put("deleteTable", MPS_ACCOUNT        + "." + PM_MSG_TABLE);
			target.put("partMm"     , FORMATTER.MM.val().format(cal.getTimeInMillis()));
			target.put("regDt"      , FORMATTER.yyyyMMdd.val().format(cal.getTimeInMillis()));

			log.info("{ ■ PM_MSG 테이블 백업 - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class));

			// 3. 백업
			insert = backupMapper.insertBackupPmMsg(target);

			// 4. 삭제
			delete = backupMapper.deletePmMsg(target);

			if (insert != delete)
				throw new NotMatchCountException();

			log.info("{ ■ PM_MSG 테이블 백업 - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"insert\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), insert, delete);

			// 5. 윤년 처리
			//    처리 월 = 02월 + 처리 일 = 28일 + 처리 월 마지막 일 = 29일(윤일)
			if ("02".equals(FORMATTER.MM.val().format(cal.getTimeInMillis()))
			 && "28".equals(FORMATTER.dd.val().format(cal.getTimeInMillis()))
			 && cal.getActualMaximum(Calendar.DAY_OF_MONTH) == 29)
			{
				insert = 0;
				delete = 0;

				// 5.1 처리일을 29일로 설정
				cal.set(Calendar.DATE, 29);

				target.replace("regDt", FORMATTER.yyyyMMdd.val().format(cal.getTimeInMillis()));

				log.info("{ ■ PM_MSG [윤년] 테이블 백업 - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class));

				// 5.2 백업
				insert = backupMapper.insertBackupPmMsg(target);

				// 5.3 삭제
				delete = backupMapper.deletePmMsg(target);

				if (insert != delete)
					throw new NotMatchCountException();

				log.info("{ ■ PM_MSG [윤년] 테이블 백업 - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"insert\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), insert, delete);
			}
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ ■ PM_MSG 테이블 백업 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw e;
		}
		catch (NotMatchCountException e)
		{
			log.error("{ ■ PM_MSG 테이블 백업 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"insert\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class), insert, delete);

			throw e;
		}
		catch (Exception e)
		{
			log.error("{ ■ PM_MSG 테이블 백업 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), new Gson().toJson(target, Map.class));

			throw e;
		}
	}

	/**
	 * PM_MSG(메시지) 테이블 백업 서비스(3일 전)
	 * @return void
	 * @throws NotMatchCountException  
	 * @throws DataAccessException 
	 * @throws Exception
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void Backup3DaysAgoPmMsg()
	{
		int insert = 0;
		int delete = 0;

		Map<String, String> target = new HashMap<String, String>();

		try
		{
			insert = 0;
			delete = 0;

			Date     now = new Date();
			Calendar cal = Calendar.getInstance();

			// 1. 날짜 설정(-1년 -3일)
			cal.setTime(now);
			cal.add(Calendar.YEAR, -1);
			cal.add(Calendar.DATE, -3);

			// 2. 대상 데이터 설정
			//    백업 : MPS_BACKUP.PM_MSG_0X (X:0-9)
			//    삭제 : MPS.PM_MSG
			target.put("insertTable", MPS_BACKUP_ACCOUNT + "." + PM_MSG_TABLE + "_" + "0" + FORMATTER.yyyy.val().format(cal.getTimeInMillis()).substring(3, 4));
			target.put("deleteTable", MPS_ACCOUNT        + "." + PM_MSG_TABLE);
			target.put("partMm"     , FORMATTER.MM.val().format(cal.getTimeInMillis()));
			target.put("regDt"      , FORMATTER.yyyyMMdd.val().format(cal.getTimeInMillis()));

			log.info("{ ■ PM_MSG 테이블 백업 [-3일] - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class));

			// 3. 백업
			insert = backupMapper.insertBackupPmMsg(target);

			// 4. 삭제
			delete = backupMapper.deletePmMsg(target);

			if (insert != delete)
				throw new NotMatchCountException();

			log.info("{ ■ PM_MSG 테이블 백업 [-3일] - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"insert\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), insert, delete);

			// 5. 윤년 처리
			//    처리 월 = 02월 + 처리 일 = 28일 + 처리 월 마지막 일 = 29일(윤일)
			if ("02".equals(FORMATTER.MM.val().format(cal.getTimeInMillis()))
			 && "28".equals(FORMATTER.dd.val().format(cal.getTimeInMillis()))
			 && cal.getActualMaximum(Calendar.DAY_OF_MONTH) == 29)
			{
				insert = 0;
				delete = 0;

				// 5.1 처리일을 29일로 설정
				cal.set(Calendar.DATE, 29);

				target.replace("regDt", FORMATTER.yyyyMMdd.val().format(cal.getTimeInMillis()));

				log.info("{ ■ PM_MSG [윤년] 테이블 백업 [-3일] - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class));

				// 5.2 백업
				insert = backupMapper.insertBackupPmMsg(target);

				// 5.3 삭제
				delete = backupMapper.deletePmMsg(target);

				if (insert != delete)
					throw new NotMatchCountException();

				log.info("{ ■ PM_MSG [윤년] 테이블 백업 [-3일] - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"insert\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), insert, delete);
			}
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ ■ PM_MSG 테이블 백업 [-3일] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw e;
		}
		catch (NotMatchCountException e)
		{
			log.error("{ ■ PM_MSG 테이블 백업 [-3일] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"insert\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class), insert, delete);

			throw e;
		}
		catch (Exception e)
		{
			log.error("{ ■ PM_MSG 테이블 백업 [-3일] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), new Gson().toJson(target, Map.class));

			throw e;
		}
	}

	/**
	 * PM_MSG(메시지) 테이블 백업 서비스(재처리)
	 * @return void
	 * @throws NotMatchCountException  
	 * @throws DataAccessException 
	 * @throws Exception
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void BackupRetryPmMsgFromDate()
	{
		int insert = 0;
		int delete = 0;

		String request = null;

		Map<String, String> target = new HashMap<String, String>();

		try
		{
			insert = 0;
			delete = 0;

			// 1. 요청(YYYYMMDD) 읽기
			request = redisRetryPmMsgFromDate.rpop(Q_BACKUP_RETRY_PM_MSG_FROM_DATE.STRUCTURE_NAME);

			// 2. 요청일 정합성 체크
			//    null 체크 + 길이(8) 체크 + 정수 체크 + 월(1-12월) 체크
			if (null == request
			|| 8 != request.length()
			|| false == request.chars().allMatch(Character::isDigit)
			|| false == (Integer.parseInt(request.substring(4, 6)) > 0 && Integer.parseInt(request.substring(4, 6)) <= 12)
			   )
			{
				throw new DateFormatException(request);
			}

			// 3. 대상 데이터 설정
			//    백업 : MPS_BACKUP.PM_MSG_0X (X:0-9)
			//    삭제 : MPS.PM_MSG
			target.put("insertTable", MPS_BACKUP_ACCOUNT + "." + PM_MSG_TABLE + "_" + "0" + request.substring(3, 4));
			target.put("deleteTable", MPS_ACCOUNT        + "." + PM_MSG_TABLE);
			target.put("partMm"     , request.substring(4, 6));
			target.put("regDt"      , request.substring(0, 8));

			log.info("{ ■ PM_MSG 테이블 백업 [재처리] - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class));

			// 4. 백업
			insert = backupMapper.insertBackupPmMsg(target);

			// 5. 삭제
			delete = backupMapper.deletePmMsg(target);

			if (insert != delete)
				throw new NotMatchCountException();

			log.info("{ ■ PM_MSG 테이블 백업 [재처리] - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"insert\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), insert, delete);
		}
		catch (DateFormatException e)
		{
			log.error("{ ■ PM_MSG 테이블 백업 [재처리] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"request\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), request);

			throw e;
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ ■ PM_MSG 테이블 백업 [재처리] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw e;
		}
		catch (NotMatchCountException e)
		{
			log.error("{ ■ PM_MSG 테이블 백업 [재처리] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"insert\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class), insert, delete);

			throw e;
		}
		catch (Exception e)
		{
			log.error("{ ■ PM_MSG 테이블 백업 [재처리] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), new Gson().toJson(target, Map.class));

			throw e;
		}
	}

	/**
	 * MP_DCMNT_INFO(유통증명서) 테이블 백업 서비스
	 * @return void
	 * @throws NotMatchCountException  
	 * @throws DataAccessException 
	 * @throws Exception
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void BackupMpDcmntInfo()
	{
		int insert = 0;
		int delete = 0;

		Map<String, String> target = new HashMap<String, String>();

		try
		{
			insert = 0;
			delete = 0;

			Date        now = new Date();
			Calendar    cal = Calendar.getInstance();

			// 1. 날짜 설정(-1년)
			cal.setTime(now);
			cal.add(Calendar.YEAR, -1);

			// 2. 대상 데이터 설정
			//    백업 : MPS_BACKUP.MP_DCMNT_INFO_0X (X:0-9)
			//    삭제 : MPS.MP_DCMNT_INFO
			target.put("insertTable", MPS_BACKUP_ACCOUNT + "." + MP_DCMNT_INFO_TABLE + "_" + "0" + FORMATTER.yyyy.val().format(cal.getTimeInMillis()).substring(3, 4));
			target.put("deleteTable", MPS_ACCOUNT        + "." + MP_DCMNT_INFO_TABLE);
			target.put("partMm"     , FORMATTER.MM.val().format(cal.getTimeInMillis()));
			target.put("regDt"      , FORMATTER.yyyyMMdd.val().format(cal.getTimeInMillis()));

			log.info("{ ■ MP_DCMNT_INFO 테이블 백업 - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class));

			// 3. 백업
			insert = backupMapper.insertBackupMpDcmntInfo(target);

			// 4. 삭제
			delete = backupMapper.deleteMpDcmntInfo(target);

			if (insert != delete)
				throw new NotMatchCountException();

			log.info("{ ■ MP_DCMNT_INFO 테이블 백업 - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"insert\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), insert, delete);

			// 5. 윤년 처리
			//    처리 월 = 02월 + 처리 일 = 28일 + 처리 월 마지막 일 = 29일(윤일)
			if ("02".equals(FORMATTER.MM.val().format(cal.getTimeInMillis()))
			 && "28".equals(FORMATTER.dd.val().format(cal.getTimeInMillis()))
			 && cal.getActualMaximum(Calendar.DAY_OF_MONTH) == 29)
			{
				insert = 0;
				delete = 0;

				// 5.1 처리일을 29일로 설정
				cal.set(Calendar.DATE, 29);

				target.replace("regDt", FORMATTER.yyyyMMdd.val().format(cal.getTimeInMillis()));

				log.info("{ ■ MP_DCMNT_INFO [윤년] 테이블 백업 - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class));

				// 5.2 백업
				insert = backupMapper.insertBackupMpDcmntInfo(target);

				// 5.3 삭제
				delete = backupMapper.deleteMpDcmntInfo(target);

				if (insert != delete)
					throw new NotMatchCountException();

				log.info("{ ■ MP_DCMNT_INFO [윤년] 테이블 백업 - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"insert\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), insert, delete);
			}
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ ■ MP_DCMNT_INFO 테이블 백업 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw e;
		}
		catch (NotMatchCountException e)
		{
			log.error("{ ■ MP_DCMNT_INFO 테이블 백업 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"insert\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class), insert, delete);

			throw e;
		}
		catch (Exception e)
		{
			log.error("{ ■ MP_DCMNT_INFO 테이블 백업 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), new Gson().toJson(target, Map.class));

			throw e;
		}
	}

	/**
	 * MP_DCMNT_INFO(유통증명서) 테이블 백업 서비스(3일 전)
	 * @return void
	 * @throws NotMatchCountException  
	 * @throws DataAccessException 
	 * @throws Exception
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void Backup3DaysAgoMpDcmntInfo()
	{
		int insert = 0;
		int delete = 0;

		Map<String, String> target = new HashMap<String, String>();

		try
		{
			insert = 0;
			delete = 0;

			Date        now = new Date();
			Calendar    cal = Calendar.getInstance();

			// 1. 날짜 설정(-1년 -3일)
			cal.setTime(now);
			cal.add(Calendar.YEAR, -1);
			cal.add(Calendar.DATE, -3);

			// 2. 대상 데이터 설정
			//    백업 : MPS_BACKUP.MP_DCMNT_INFO_0X (X:0-9)
			//    삭제 : MPS.MP_DCMNT_INFO
			target.put("insertTable", MPS_BACKUP_ACCOUNT + "." + MP_DCMNT_INFO_TABLE + "_" + "0" + FORMATTER.yyyy.val().format(cal.getTimeInMillis()).substring(3, 4));
			target.put("deleteTable", MPS_ACCOUNT        + "." + MP_DCMNT_INFO_TABLE);
			target.put("partMm"     , FORMATTER.MM.val().format(cal.getTimeInMillis()));
			target.put("regDt"      , FORMATTER.yyyyMMdd.val().format(cal.getTimeInMillis()));

			log.info("{ ■ MP_DCMNT_INFO 테이블 백업 [-3일] - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class));

			// 3. 백업
			insert = backupMapper.insertBackupMpDcmntInfo(target);

			// 4. 삭제
			delete = backupMapper.deleteMpDcmntInfo(target);

			if (insert != delete)
				throw new NotMatchCountException();

			log.info("{ ■ MP_DCMNT_INFO 테이블 백업 [-3일] - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"insert\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), insert, delete);

			// 5. 윤년 처리
			//    처리 월 = 02월 + 처리 일 = 28일 + 처리 월 마지막 일 = 29일(윤일)
			if ("02".equals(FORMATTER.MM.val().format(cal.getTimeInMillis()))
			 && "28".equals(FORMATTER.dd.val().format(cal.getTimeInMillis()))
			 && cal.getActualMaximum(Calendar.DAY_OF_MONTH) == 29)
			{
				insert = 0;
				delete = 0;

				// 5.1 처리일을 29일로 설정
				cal.set(Calendar.DATE, 29);

				target.replace("regDt", FORMATTER.yyyyMMdd.val().format(cal.getTimeInMillis()));

				log.info("{ ■ MP_DCMNT_INFO [윤년] 테이블 백업 [-3일] - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class));

				// 5.2 백업
				insert = backupMapper.insertBackupMpDcmntInfo(target);

				// 5.3 삭제
				delete = backupMapper.deleteMpDcmntInfo(target);

				if (insert != delete)
					throw new NotMatchCountException();

				log.info("{ ■ MP_DCMNT_INFO [윤년] 테이블 백업 [-3일] - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"insert\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), insert, delete);
			}
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ ■ MP_DCMNT_INFO 테이블 백업 [-3일] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw e;
		}
		catch (NotMatchCountException e)
		{
			log.error("{ ■ MP_DCMNT_INFO 테이블 백업 [-3일] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"insert\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class), insert, delete);

			throw e;
		}
		catch (Exception e)
		{
			log.error("{ ■ MP_DCMNT_INFO 테이블 백업 [-3일] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), new Gson().toJson(target, Map.class));

			throw e;
		}
	}

	/**
	 * MP_DCMNT_INFO(유통증명서) 테이블 백업 서비스(재처리)
	 * @return void
	 * @throws NotMatchCountException  
	 * @throws DataAccessException 
	 * @throws Exception
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void BackupRetryMpDcmntInfoFromDate()
	{
		int insert = 0;
		int delete = 0;

		String request = null;

		Map<String, String> target = new HashMap<String, String>();

		try
		{
			insert = 0;
			delete = 0;

			// 1. 요청(YYYYMMDD) 읽기
			request = redisRetryMpDcmntInfoFromDate.rpop(Q_BACKUP_RETRY_MP_DCMNT_INFO_FROM_DATE.STRUCTURE_NAME);

			// 2. 요청일 정합성 체크
			//    null 체크 + 길이(8) 체크 + 정수 체크 + 월(1-12월) 체크
			if (null == request
			|| 8 != request.length()
			|| false == request.chars().allMatch(Character::isDigit)
			|| false == (Integer.parseInt(request.substring(4, 6)) > 0 && Integer.parseInt(request.substring(4, 6)) <= 12)
			   )
			{
				throw new DateFormatException(request);
			}

			// 3. 대상 데이터 설정
			//    백업 : MPS_BACKUP.MP_DCMNT_INFO_0X (X:0-9)
			//    삭제 : MPS.MP_DCMNT_INFO
			target.put("insertTable", MPS_BACKUP_ACCOUNT + "." + MP_DCMNT_INFO_TABLE + "_" + "0" + request.substring(3, 4));
			target.put("deleteTable", MPS_ACCOUNT        + "." + MP_DCMNT_INFO_TABLE);
			target.put("partMm"     , request.substring(4, 6));
			target.put("regDt"      , request.substring(0, 8));

			log.info("{ ■ MP_DCMNT_INFO 테이블 백업 [재처리] - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class));

			// 4. 백업
			insert = backupMapper.insertBackupMpDcmntInfo(target);

			// 5. 삭제
			delete = backupMapper.deleteMpDcmntInfo(target);

			if (insert != delete)
				throw new NotMatchCountException();

			log.info("{ ■ MP_DCMNT_INFO 테이블 백업 [재처리] - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"insert\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), insert, delete);
		}
		catch (DateFormatException e)
		{
			log.error("{ ■ MP_DCMNT_INFO 테이블 백업 [재처리] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"request\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), request);

			throw e;
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ ■ MP_DCMNT_INFO 테이블 백업 [재처리] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw e;
		}
		catch (NotMatchCountException e)
		{
			log.error("{ ■ MP_DCMNT_INFO 테이블 백업 [재처리] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"insert\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class), insert, delete);

			throw e;
		}
		catch (Exception e)
		{
			log.error("{ ■ MP_DCMNT_INFO 테이블 백업 [재처리] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), new Gson().toJson(target, Map.class));

			throw e;
		}
	}

	/**
	 * PM_REMIND_MSG(리마인드 메시지) 테이블 백업 서비스
	 * @return void
	 * @throws NotMatchCountException  
	 * @throws DataAccessException 
	 * @throws Exception
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void DeletePmRemindMsg()
	{
		int delete = 0;

		Map<String, String> target = new HashMap<String, String>();

		try
		{
			delete = 0;

			Date        now = new Date();
			Calendar    cal = Calendar.getInstance();

			// 1. 날짜 설정(-1년)
			cal.setTime(now);
			cal.add(Calendar.YEAR, -1);

			// 2. 대상 데이터 설정
			//    삭제 : MPS.PM_REMIND_MSG
			target.put("deleteTable", MPS_ACCOUNT + "." + PM_REMIND_MSG_TABLE);
			target.put("partMm"     , FORMATTER.MM.val().format(cal.getTimeInMillis()));
			target.put("regDt"      , FORMATTER.yyyyMMdd.val().format(cal.getTimeInMillis()));

			log.info("{ ■ PM_REMIND_MSG 테이블 삭제 - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class));

			// 3. 삭제
			delete = backupMapper.deletePmRemindMsg(target);

			log.info("{ ■ PM_REMIND_MSG 테이블 삭제 - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), delete);

			// 4. 윤년 처리
			//    처리 월 = 02월 + 처리 일 = 28일 + 처리 월 마지막 일 = 29일(윤일)
			if ("02".equals(FORMATTER.MM.val().format(cal.getTimeInMillis()))
			 && "28".equals(FORMATTER.dd.val().format(cal.getTimeInMillis()))
			 && cal.getActualMaximum(Calendar.DAY_OF_MONTH) == 29)
			{
				delete = 0;

				// 4.1 처리일을 29일로 설정
				cal.set(Calendar.DATE, 29);

				target.replace("regDt", FORMATTER.yyyyMMdd.val().format(cal.getTimeInMillis()));

				log.info("{ ■ PM_REMIND_MSG [윤년] 테이블 삭제 - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class));

				// 4.2 삭제
				delete = backupMapper.deletePmRemindMsg(target);

				log.info("{ ■ PM_REMIND_MSG [윤년] 테이블 삭제 - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), delete);
			}
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ ■ PM_REMIND_MSG 테이블 삭제 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw e;
		}
		catch (NotMatchCountException e)
		{
			log.error("{ ■ PM_REMIND_MSG 테이블 삭제 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class), delete);

			throw e;
		}
		catch (Exception e)
		{
			log.error("{ ■ PM_REMIND_MSG 테이블 삭제 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), new Gson().toJson(target, Map.class));

			throw e;
		}
	}

	/**
	 * PM_REMIND_MSG(리마인드 메시지) 테이블 백업 서비스(3일 전)
	 * @return void
	 * @throws NotMatchCountException  
	 * @throws DataAccessException 
	 * @throws Exception
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void Delete3DaysAgoPmRemindMsg()
	{
		int delete = 0;

		Map<String, String> target = new HashMap<String, String>();

		try
		{
			delete = 0;

			Date        now = new Date();
			Calendar    cal = Calendar.getInstance();

			// 1. 날짜 설정(-1년 -3일)
			cal.setTime(now);
			cal.add(Calendar.YEAR, -1);
			cal.add(Calendar.DATE, -3);

			// 2. 대상 데이터 설정
			//    삭제 : MPS.PM_REMIND_MSG
			target.put("deleteTable", MPS_ACCOUNT + "." + PM_REMIND_MSG_TABLE);
			target.put("partMm"     , FORMATTER.MM.val().format(cal.getTimeInMillis()));
			target.put("regDt"      , FORMATTER.yyyyMMdd.val().format(cal.getTimeInMillis()));

			log.info("{ ■ PM_REMIND_MSG 테이블 삭제 [-3일] - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class));

			// 3. 삭제
			delete = backupMapper.deletePmRemindMsg(target);

			log.info("{ ■ PM_REMIND_MSG 테이블 삭제 [-3일] - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), delete);

			// 4. 윤년 처리
			//    처리 월 = 02월 + 처리 일 = 28일 + 처리 월 마지막 일 = 29일(윤일)
			if ("02".equals(FORMATTER.MM.val().format(cal.getTimeInMillis()))
			 && "28".equals(FORMATTER.dd.val().format(cal.getTimeInMillis()))
			 && cal.getActualMaximum(Calendar.DAY_OF_MONTH) == 29)
			{
				delete = 0;

				// 4.1 처리일을 29일로 설정
				cal.set(Calendar.DATE, 29);

				target.replace("regDt", FORMATTER.yyyyMMdd.val().format(cal.getTimeInMillis()));

				log.info("{ ■ PM_REMIND_MSG [윤년] 테이블 삭제 [-3일] - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class));

				// 4.2 삭제
				delete = backupMapper.deletePmRemindMsg(target);

				log.info("{ ■ PM_REMIND_MSG [윤년] 테이블 삭제 [-3일] - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), delete);
			}
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ ■ PM_REMIND_MSG 테이블 삭제 [-3일] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw e;
		}
		catch (NotMatchCountException e)
		{
			log.error("{ ■ PM_REMIND_MSG 테이블 삭제 [-3일] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class), delete);

			throw e;
		}
		catch (Exception e)
		{
			log.error("{ ■ PM_REMIND_MSG 테이블 삭제 [-3일] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), new Gson().toJson(target, Map.class));

			throw e;
		}
	}

	/**
	 * PM_REMIND_MSG(리마인드 메시지) 테이블 백업 서비스(재처리)
	 * @return void
	 * @throws NotMatchCountException  
	 * @throws DataAccessException 
	 * @throws Exception
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void DeleteRetryPmRemindMsgFromDate()
	{
		int delete = 0;

		String request = null;

		Map<String, String> target = new HashMap<String, String>();

		try
		{
			delete = 0;

			// 1. 요청(YYYYMMDD) 읽기
			request = redisRetryPmRemindMsgFromDate.rpop(Q_BACKUP_RETRY_PM_REMIND_MSG_FROM_DATE.STRUCTURE_NAME);

			// 2. 요청일 정합성 체크
			//    null 체크 + 길이(8) 체크 + 정수 체크 + 월(1-12월) 체크
			if (null == request
			|| 8 != request.length()
			|| false == request.chars().allMatch(Character::isDigit)
			|| false == (Integer.parseInt(request.substring(4, 6)) > 0 && Integer.parseInt(request.substring(4, 6)) <= 12)
			   )
			{
				throw new DateFormatException(request);
			}

			// 3. 대상 데이터 설정
			//    삭제 : MPS.PM_REMIND_MSG
			target.put("deleteTable", MPS_ACCOUNT + "." + PM_REMIND_MSG_TABLE);
			target.put("partMm"     , request.substring(4, 6));
			target.put("regDt"      , request.substring(0, 8));

			log.info("{ ■ PM_REMIND_MSG 테이블 삭제 [재처리] - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class));

			// 4. 삭제
			delete = backupMapper.deletePmRemindMsg(target);

			log.info("{ ■ PM_REMIND_MSG 테이블 삭제 [재처리] - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), delete);
		}
		catch (DateFormatException e)
		{
			log.error("{ ■ PM_REMIND_MSG 테이블 삭제 [재처리] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"request\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), request);

			throw e;
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ ■ PM_REMIND_MSG 테이블 삭제 [재처리] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw e;
		}
		catch (NotMatchCountException e)
		{
			log.error("{ ■ PM_REMIND_MSG 테이블 삭제 [재처리] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"delete\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(target, Map.class), delete);

			throw e;
		}
		catch (Exception e)
		{
			log.error("{ ■ PM_REMIND_MSG 테이블 삭제 [재처리] - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), new Gson().toJson(target, Map.class));

			throw e;
		}
	}
}
