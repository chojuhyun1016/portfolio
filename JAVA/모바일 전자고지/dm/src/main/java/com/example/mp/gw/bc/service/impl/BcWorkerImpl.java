package com.example.mp.gw.bc.service.impl;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.mp.gw.bc.domain.BcAccessTokenResponse;
import com.example.mp.gw.bc.domain.MessageResult;
import com.example.mp.gw.bc.domain.VerifyTokenResult;
import com.example.mp.gw.bc.service.BcDocumentService;
import com.example.mp.gw.bc.service.BcMemberService;
import com.example.mp.gw.bc.service.BcMessageService;
import com.example.mp.gw.bc.service.BcRetryService;
import com.example.mp.gw.bc.service.BcTokenService;
import com.example.mp.gw.bc.service.BcWorker;
import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.domain.RedisQueueDataWrapper;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_AGREE_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_DOC_ISS_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_DOC_READ_DTM_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_DOC_STS_CFM_ISS_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_RETRY;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_RMD_RSV_RPT_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_RMD_SND_RPT_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_RSV_RPT_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_SESSION;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_SND_RPT_TO_SEND;
import com.example.mp.gw.common.exception.KeepRunningToWorkerException;
import com.example.mp.gw.common.exception.StopRunningToWorkerException;
import com.example.mp.gw.common.service.RedisService;
import com.example.mp.gw.doc.domain.DocumentIssue;
import com.example.mp.gw.doc.domain.DocumentIssueStatConfirmation;
import com.example.mp.gw.maintain.service.MaintainSingleService;
import com.example.mp.gw.member.domain.Agree;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service("BcWorker")
public class BcWorkerImpl implements BcWorker
{
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger("com.uplus.mp.gw.bc");

	@Autowired
	MaintainSingleService maintainSingleWorker;

	@Autowired
	private BcTokenService bcTokenService;

	@Autowired
	private BcMemberService bcMemberService;

	@Autowired
	private BcMessageService bcMessageService;

	@Autowired
	private BcDocumentService bcDocumentService;

	@Autowired
	private BcRetryService bcRetryService;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Agree>> redisSendAgreeInfoToBc;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<MessageResult>> redisSendSndRptToBc;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<MessageResult>> redisSendRsvRptToBc;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<VerifyTokenResult>> redisSendDocRdDtmToBc;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<DocumentIssue>> redisDocuIssSendToBc;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<DocumentIssueStatConfirmation>> redisDocIssStsCfmSendToBc;

	@Autowired
	private RedisService<String, BcAccessTokenResponse> redisBcToken;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<?>> redisRetryToBc;

	final private Integer BIZ_AGREE_SEND_CNT       = 200;	// BIZ-CENTER 수신동의(등록/해제) 전송 처리 건수
	final private Integer BIZ_SND_RPT_SEND_CNT     = 200;	// BIZ-CENTER 본문자 발송 결과 전송 처리 건수
	final private Integer BIZ_RSV_RPT_SEND_CNT     = 200;	// BIZ-CENTER 본문자 수신 결과 전송 처리 건수
	final private Integer BIZ_SND_RMD_RPT_SEND_CNT = 200;	// BIZ-CENTER 리마인드 문자 발송 결과 전송 처리 건수
	final private Integer BIZ_RSV_RMD_RPT_SEND_CNT = 200;	// BIZ-CENTER 리마인드 문자 수신 결과 전송 처리 건수
	final private Integer BIZ_DOC_ISS_SEND_CNT     = 100;	// BIZ-CENTER 발송/수신 결과 전송 처리 건수
	final private Integer BIZ_DOC_STS_ISS_SEND_CNT = 100;	// BIZ-CENTER 전자문서 유통정보 수치 확인서 발급 처리 결과 전송 처리 건수
	final private Integer BIZ_DOC_RD_DTM_CNT       = 200;	// BIZ-CENTER 열람일시 전송 처리 건수


	/**
	 * Biz-center 수신동의(등록/해제) 전송 스케줄러 (G/W -> 비즈센터)
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.bc.scheduler.default-delay}")
	public void popAndSendAgree()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisSendAgreeInfoToBc.size(Q_BC_AGREE_TO_SEND.STRUCTURE_NAME))
				return;

			if (0 != redisBcToken.size(Q_BC_SESSION.STRUCTURE_NAME))
			{
				Thread.sleep(1000);

				return;
			}

			bcMemberService.popAndSendAgree(BIZ_AGREE_SEND_CNT);
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
	 * Biz-center 본문자 발송 결과 전송 스케줄러 (G/W -> 비즈센터)
	 */
	@Override
	@Async("bcSendSndRptPool")
	@Scheduled(fixedRateString = "${spring.bc.scheduler.report.send-snd-rate}")
	public void popAndSendSndRpt()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisSendSndRptToBc.size(Q_BC_SND_RPT_TO_SEND.STRUCTURE_NAME))
				return;

			if (0 != redisBcToken.size(Q_BC_SESSION.STRUCTURE_NAME))
			{
				Thread.sleep(1000);

				return;
			}

			bcMessageService.popAndSendSndRpt(BIZ_SND_RPT_SEND_CNT);
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
	 * Biz-center 본문자 수신 결과 전송 스케줄러 (G/W -> 비즈센터)
	 */
	@Override
	@Async("bcSendRsvRptPool")
	@Scheduled(fixedRateString = "${spring.bc.scheduler.report.send-rpt-rate}")
	public void popAndSendRsvRpt()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisSendRsvRptToBc.size(Q_BC_RSV_RPT_TO_SEND.STRUCTURE_NAME))
				return;

			if (0 != redisBcToken.size(Q_BC_SESSION.STRUCTURE_NAME))
			{
				Thread.sleep(1000);

				return;
			}

			bcMessageService.popAndSendRsvRpt(BIZ_RSV_RPT_SEND_CNT);
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());
		}
		finally
		{
			MDC.clear();
		}
	}

	/**
	 * Biz-center 리마인드 문자 발송 결과 전송 스케줄러 (G/W -> 비즈센터)
	 */
	@Override
	@Async("bcSendRmdSndRptPool")
	@Scheduled(fixedRateString = "${spring.bc.scheduler.report.send-rmd-snd-rate}")
	public void popAndSendRmdSndRpt()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisSendSndRptToBc.size(Q_BC_RMD_SND_RPT_TO_SEND.STRUCTURE_NAME))
				return;

			if (0 != redisBcToken.size(Q_BC_SESSION.STRUCTURE_NAME))
			{
				Thread.sleep(1000);

				return;
			}

			bcMessageService.popAndSendRmdSndRpt(BIZ_SND_RMD_RPT_SEND_CNT);
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
	 * Biz-center 리마인드 문자 수신 결과 전송 스케줄러 (G/W -> 비즈센터)
	 */
	@Override
	@Async("bcSendRmdRsvRptPool")
	@Scheduled(fixedRateString = "${spring.bc.scheduler.report.send-rmd-rpt-rate}")
	public void popAndSendRmdRsvRpt()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisSendRsvRptToBc.size(Q_BC_RMD_RSV_RPT_TO_SEND.STRUCTURE_NAME))
				return;

			if (0 != redisBcToken.size(Q_BC_SESSION.STRUCTURE_NAME))
			{
				Thread.sleep(1000);

				return;
			}

			bcMessageService.popAndSendRmdRsvRpt(BIZ_RSV_RMD_RPT_SEND_CNT);
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());
		}
		finally
		{
			MDC.clear();
		}
	}

	/**
	 * BC 열람확인결과 수신 스케줄러 (G/W -> 비즈센터)
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.bc.scheduler.default-delay}")
	public void popAndSendRdDtm()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisSendDocRdDtmToBc.size(Q_BC_DOC_READ_DTM_TO_SEND.STRUCTURE_NAME))
				return;

			if (0 != redisBcToken.size(Q_BC_SESSION.STRUCTURE_NAME))
			{
				Thread.sleep(1000);

				return;
			}

			bcMessageService.popAndSendRdDtm(BIZ_DOC_RD_DTM_CNT);
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());
		}
		finally
		{
			MDC.clear();
		}
	}

	/**
	 * BC 유통증명서발급 결과 수신 처리 전송 스케줄러 (G/W -> 비즈센터)
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.bc.scheduler.default-delay}")
	public void popAndSendIssDoc()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisDocuIssSendToBc.size(Q_BC_DOC_ISS_TO_SEND.STRUCTURE_NAME))
				return;

			if (0 != redisBcToken.size(Q_BC_SESSION.STRUCTURE_NAME))
			{
				Thread.sleep(1000);
				
				return;
			}

			for (int i = 0; i < BIZ_DOC_ISS_SEND_CNT; i++)
			{
				try
				{
					bcDocumentService.popAndSendIssDoc();
				}
				catch (KeepRunningToWorkerException e)
				{
					continue;
				}
				catch (StopRunningToWorkerException e)
				{
					throw e;
				}
				catch (Exception e)
				{
					throw e;
				}
			}
		}
		catch(Exception e )
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());
		}
		finally
		{
			MDC.clear();
		}
	}

	/**
	 * BC 전자문서 유통정보 수치 확인서 발급 결과 수신 처리 전송 스케줄러 (G/W -> 비즈센터)
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.bc.scheduler.default-delay}")
	public void popAndSendIssDocStsCfm()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisDocIssStsCfmSendToBc.size(Q_BC_DOC_STS_CFM_ISS_TO_SEND.STRUCTURE_NAME))
				return;

			if (0 != redisBcToken.size(Q_BC_SESSION.STRUCTURE_NAME))
			{
				Thread.sleep(1000);
				
				return;
			}

			for (int i = 0; i < BIZ_DOC_STS_ISS_SEND_CNT; i++)
			{
				try
				{
					bcDocumentService.popAndSendIssDocStsCfm();
				}
				catch (KeepRunningToWorkerException e)
				{
					continue;
				}
				catch (StopRunningToWorkerException e)
				{
					throw e;
				}
				catch (Exception e)
				{
					throw e;
				}
			}
		}
		catch(Exception e )
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());
		}
		finally
		{
			MDC.clear();
		}
	}

	/**
	 * BC ACCESS TOKEN 발급 스케줄러 (G/W -> 비즈센터) 3달 기준
	 */
	@Override
	@Scheduled(cron = "${spring.bc.cron.issue-token}")
	public void issueBcToken()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (false == maintainSingleWorker.isRunning())
				return;

			bcTokenService.issueBcToken();
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());
		}
		finally
		{
			MDC.clear();
		}
	}

	/**
	 * BC ACCESS TOKEN 발급 스케줄러 (G/W -> 비즈센터)
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.bc.scheduler.token.issue-token-delay}")
	public void popAndIssueBcToken()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisBcToken.size(Q_BC_SESSION.STRUCTURE_NAME))
				return;

			if (false == maintainSingleWorker.isRunning())
				return;
			
			bcTokenService.popAndIssueBcToken();
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());
		}
		finally
		{
			MDC.clear();
		}
	}
	
	/**
	// BC 실패 건 재처리 스케줄러 (G/W -> 비즈센터)
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.bc.scheduler.retry.check-delay}")
	public void popAndRetryToBc()
	{
		try
		{
			long cnt = 0;

			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());
			
			if (0 == (cnt = redisRetryToBc.size(Q_BC_RETRY.STRUCTURE_NAME)))
				return;

			if (false == maintainSingleWorker.isRunning())
				return;

			for (long i = 0; i < cnt; i++)
			{
				try
				{
					bcRetryService.popAndRetryToBc();
				}
				catch (KeepRunningToWorkerException e)
				{
					continue;
				}
				catch (StopRunningToWorkerException e)
				{
					throw e;
				}
				catch (Exception e)
				{
					throw e;
				}
			}
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());
		}
		finally
		{
			MDC.clear();
		}
	}
}
