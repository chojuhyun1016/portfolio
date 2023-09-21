package com.example.mp.gw.kisa.service.impl;


import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.domain.RedisQueueDataWrapper;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_FIX_FROM_DATE;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_FIX_FROM_SEQ;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_REG_RD_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_REG_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_RETRY_FROM_DATE;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_RETRY_FROM_SEQ;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_RSLT;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_EADDR_RETRY_FROM_DATE;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_EADDR_RETRY_FROM_SEQ;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_EADDR_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_RETRY;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_SESSION;
import com.example.mp.gw.common.exception.KeepRunningToWorkerException;
import com.example.mp.gw.common.exception.StopRunningToWorkerException;
import com.example.mp.gw.common.service.RedisService;
import com.example.mp.gw.doc.domain.Document;
import com.example.mp.gw.kisa.domain.KisaAccessTokenResponse;
import com.example.mp.gw.kisa.service.KisaDocumentService;
import com.example.mp.gw.kisa.service.KisaMemberService;
import com.example.mp.gw.kisa.service.KisaTokenService;
import com.example.mp.gw.kisa.service.KisaWorker;
import com.example.mp.gw.maintain.service.MaintainSingleService;
import com.example.mp.gw.member.domain.Member;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service("KisaWorker")
public class KisaWorkerImpl implements KisaWorker
{
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger("com.uplus.mp.gw.kisa");

	@Autowired
	MaintainSingleService maintainSingleWorker;

	@Autowired
	KisaTokenService  kisaTokenService;

	@Autowired
	KisaDocumentService kisaDocumentService;

	@Autowired
	KisaMemberService kisaMemberService;

	@Autowired
	KisaRetryServiceImpl KisaRetryService;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Member>> redisSendEaddrToKisa;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Document>> redisUpdateDocumentRslt;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Document>> redisSendDocumentToKisa;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Document>> redisSendDocumentRdToKisa;

	@Autowired
	private RedisService<String, KisaAccessTokenResponse> redisKisaToken;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Document>> redisRetryToKisa;

	@Autowired
	private RedisService<String, String> redisRetryFromDateToKisa;

	@Autowired
	private RedisService<String, String> redisRetryEaddrFromDateToKisa;

	@Autowired
	private RedisService<String, String> redisRetryFromSeqToKisa;

	@Autowired
	private RedisService<String, String> redisRetryEaddrFromSeqToKisa;

	final private Integer KISA_EADDR_SEND_CNT        = 500;	// KISA 공인전자주소(등록/탈퇴/수정) 전송 건수
	final private Integer KISA_EADDR_RETRY_CNT       = 500;	// KISA 공인전자주소(등록/탈퇴/수정) 재처리 건수
	final private Integer KISA_EADDR_RETRY_DATE_CNT  = 500;	// KISA 공인전자주소(등록/탈퇴/수정) 재처리 건수(From 날짜)
	final private Integer KISA_EADDR_RETRY_SEQ_CNT   = 500;	// KISA 공인전자주소(등록/탈퇴/수정) 재처리 건수(From SEQ)

	final private Integer KISA_DCMNT_SEND_CNT        = 500;	// KISA 유통정보 등록(등록, 등록 + 열람일시) 전송 건수
	final private Integer KISA_DCMNT_RD_SEND_CNT     = 500;	// KISA 유통정보 등록(등록 + 열람일시) 전송 건수
	final private Integer KISA_DCMNT_RSLT_CNT        = 500;	// KISA 유통정보 등록 결과 처리(갱신) 건수
	final private Integer KISA_DCMNT_FIX_CNT         = 500;	// KISA 유통정보 보정(수신시간) 건수
	final private Integer KISA_DCMNT_FIX_DATE_CNT    = 500;	// KISA 유통정보 보정(수신시간) 건수(From 날짜)
	final private Integer KISA_DCMNT_FIX_SEQ_CNT     = 500;	// KISA 유통정보 보정(수신시간) 건수(From SEQ)
	final private Integer KISA_DCMNT_RETRY_CNT       = 500;	// KISA 유통정보 재처리 건수
	final private Integer KISA_DCMNT_RD_RETRY_CNT    = 500;	// KISA 유통정보 재처리 건수
	final private Integer KISA_DCMNT_RETRY_DATE_CNT  = 500;	// KISA 유통정보 재처리 건수(From 날짜)
	final private Integer KISA_DCMNT_RETRY_SEQ_CNT   = 500;	// KISA 유통정보 재처리 건수(From SEQ)


	/**
	 * KISA 공인전자주소(등록/탈퇴/수정) 전송 스케줄러 (G/W -> KISA) 
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.kisa.scheduler.default-delay}")
	public void popAndSendEaddrToKisa()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisSendEaddrToKisa.size(Q_KISA_EADDR_TO_SEND.STRUCTURE_NAME))
				return;

			if (0 != redisKisaToken.size(Q_KISA_SESSION.STRUCTURE_NAME))
			{
				Thread.sleep(1000);

				return;
			}

			if (false == maintainSingleWorker.isRunning())
			{
				Thread.sleep(1000);

				return;
			}

			for (int i = 0; i < KISA_EADDR_SEND_CNT; i++)
			{
				try
				{
					kisaMemberService.popAndSendEaddrToKisa();
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
	 * KISA 유통정보 등록 전송 스케줄러 (G/W -> KISA) 
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.kisa.scheduler.snd-doc-reg-delay}")
	public void popAndSendDocToKisa()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisSendDocumentToKisa.size(Q_KISA_DOC_REG_TO_SEND.STRUCTURE_NAME))
				return;

			if (0 != redisKisaToken.size(Q_KISA_SESSION.STRUCTURE_NAME))
			{
				Thread.sleep(1000);

				return;
			}

			kisaDocumentService.popAndSendDocToKisa(KISA_DCMNT_SEND_CNT);
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
	 * KISA 유통정보 열람일시 등록 전송 스케줄러 (G/W -> KISA) 
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.kisa.scheduler.snd-doc-reg-rd-delay}")
	public void popAndSendDocRdToKisa()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisSendDocumentRdToKisa.size(Q_KISA_DOC_REG_RD_TO_SEND.STRUCTURE_NAME))
				return;

			if (0 != redisKisaToken.size(Q_KISA_SESSION.STRUCTURE_NAME))
			{
				Thread.sleep(1000);

				return;
			}

			kisaDocumentService.popAndSendDocRdToKisa(KISA_DCMNT_RD_SEND_CNT);
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
	 * KISA 유통증명정보 전송(등록/갱신) 결과 업데이트 스케줄러 
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.kisa.scheduler.default-delay}")
	public void popAndUpdateDocRslt()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisUpdateDocumentRslt.size(Q_KISA_DOC_RSLT.STRUCTURE_NAME))
				return;

			if (false == maintainSingleWorker.isRunning())
			{
				Thread.sleep(1000);

				return;
			}

			for (int i = 0; i < KISA_DCMNT_RSLT_CNT; i++)
			{
				try
				{
					kisaDocumentService.popAndUpdateDocRslt();
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
	// KISA 전송 실패 건 재처리 스케줄러 (G/W -> KISA)
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.kisa.scheduler.retry.check-delay}")
	public void popAndRetryToKisa()
	{
		try
		{
			long cnt = 0;

			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == (cnt = redisRetryToKisa.size(Q_KISA_RETRY.STRUCTURE_NAME)))
				return;

			if (false == maintainSingleWorker.isRunning())
				return;

			for (long i = 0; i < cnt; i++)
			{
				try
				{
					KisaRetryService.popAndRetryToKisa();
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

	/**
	// KISA 공인전자주소(등록, 탈퇴, 수정) 실패건 재처리(날짜) 스케줄러 (G/W -> G/W, G/W -> KISA)
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.kisa.scheduler.retry.check-delay}")
	public void popAndRetryEaddrFromDateToKisa()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisRetryEaddrFromDateToKisa.size(Q_KISA_EADDR_RETRY_FROM_DATE.STRUCTURE_NAME))
				return;

			KisaRetryService.popAndRetryEaddrFromDateToKisa(KISA_EADDR_RETRY_DATE_CNT);
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

	/**
	// KISA 공인전자주소(등록, 탈퇴, 수정) 실패건 재처리(SEQ) 스케줄러 (G/W -> G/W, G/W -> KISA)
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.kisa.scheduler.retry.check-delay}")
	public void popAndRetryEaddrFromSeqToKisa()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisRetryEaddrFromSeqToKisa.size(Q_KISA_EADDR_RETRY_FROM_SEQ.STRUCTURE_NAME))
				return;

			KisaRetryService.popAndRetryEaddrFromSeqToKisa(KISA_EADDR_RETRY_SEQ_CNT);
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

	/**
	// KISA 공인전자주소(등록, 탈퇴, 수정) 실패건 재처리(하루전) 크론 (G/W -> KISA)
	 */
	@Override
	@Scheduled(cron = "${spring.kisa.cron.retry-eaddr}")
	public void selectAndSendEaddrToKisa()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			KisaRetryService.selectAndSendEaddrToKisa(KISA_EADDR_RETRY_CNT);
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
	// KISA 유통증명서 수신시간 보정(날짜) 스케줄러 (G/W)
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.kisa.scheduler.retry.check-delay}")
	public void popAndFixDocFromDate()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisRetryFromDateToKisa.size(Q_KISA_DOC_FIX_FROM_DATE.STRUCTURE_NAME))
				return;

			KisaRetryService.popAndFixDocFromDate(KISA_DCMNT_FIX_DATE_CNT);
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

	/**
	// KISA 유통증명서 수신시간 보정(MP_DCMNT_REQ_FAIL.FAIL_SEQ) 스케쥴러 (G/W)
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.kisa.scheduler.retry.check-delay}")
	public void popAndFixDocFromSeq()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisRetryFromSeqToKisa.size(Q_KISA_DOC_FIX_FROM_SEQ.STRUCTURE_NAME))
				return;

			KisaRetryService.popAndFixDocFromSeq(KISA_DCMNT_FIX_SEQ_CNT);
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

	/**
	// KISA 유통증명서 전송 실패 건 재처리(날짜) 스케쥴러 (G/W -> G/W, G/W -> KISA)
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.kisa.scheduler.retry.check-delay}")
	public void popAndRetryDocFromDateToKisa()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisRetryFromDateToKisa.size(Q_KISA_DOC_RETRY_FROM_DATE.STRUCTURE_NAME))
				return;

			KisaRetryService.popAndRetryDocFromDateToKisa(KISA_DCMNT_RETRY_DATE_CNT);
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

	/**
	// KISA 유통증명서 전송 실패 건 재처리(SEQ) 스케줄러 (G/W -> G/W, G/W -> KISA)
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.kisa.scheduler.retry.check-delay}")
	public void popAndRetryDocFromSeqToKisa()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisRetryFromSeqToKisa.size(Q_KISA_DOC_RETRY_FROM_SEQ.STRUCTURE_NAME))
				return;

			KisaRetryService.popAndRetryDocFromSeqToKisa(KISA_DCMNT_RETRY_SEQ_CNT);
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

	/**
	// KISA 유통정보(등록, 등록 + 열람일시) 보정 크론 (G/W)
	 */
	@Override
	@Scheduled(cron = "${spring.kisa.cron.fix-doc-reg}")
	public void selectAndFixDoc()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			KisaRetryService.selectAndFixDoc(KISA_DCMNT_FIX_CNT);
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
	// KISA 유통정보(등록, 등록 + 열람일시) 재전송 크론 (G/W -> KISA)
	 */
	@Override
	@Scheduled(cron = "${spring.kisa.cron.retry-doc-reg}")
	public void selectAndSendDocToKisa()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			KisaRetryService.selectAndSendDocToKisa(KISA_DCMNT_RETRY_CNT);
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
	// KISA 유통정보(열람일시) 재전송 크론 (G/W -> KISA)
	 */
	@Override
	@Scheduled(cron = "${spring.kisa.cron.retry-doc-reg-rd}")
	public void selectAndSendDocRdToKisa()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			KisaRetryService.selectAndSendDocRdToKisa(KISA_DCMNT_RD_RETRY_CNT);
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
	 * KISA ACCESS TOKEN 발급 크론 (G/W -> KISA) 엑세스 토큰은 30일
	 */
	@Override
	@Scheduled(cron = "${spring.kisa.cron.issue-token}")
	public void issueKisaToken()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (false == maintainSingleWorker.isRunning())
				return;

			kisaTokenService.issueKisaToken();
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

	/**
	// KISA ACCESS TOKEN 발급(유효기간 만료) 스케줄러 (G/W -> KISA)
	 */
	@Override
	@Scheduled(fixedDelayString = "${spring.kisa.scheduler.token.issue-delay}")
	public void popAndIssueKisaToken()
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (0 == redisKisaToken.size(Q_KISA_SESSION.STRUCTURE_NAME))
				return;

			if (false == maintainSingleWorker.isRunning())
				return;

			kisaTokenService.popAndIssueKisaToken();
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
