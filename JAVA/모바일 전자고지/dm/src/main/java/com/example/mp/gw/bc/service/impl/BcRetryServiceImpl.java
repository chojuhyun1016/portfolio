package com.example.mp.gw.bc.service.impl;


import java.util.HashMap;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.mp.gw.bc.service.BcRetryService;
import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.domain.RedisQueueDataWrapper;
import com.example.mp.gw.common.domain.Const.DOCUMENT;
import com.example.mp.gw.common.domain.Const.FORMATTER;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_AGREE_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_DOC_ISS_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_DOC_READ_DTM_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_DOC_STS_CFM_ISS_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_RETRY;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_RMD_RSV_RPT_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_RMD_SND_RPT_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_RSV_RPT_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_SND_RPT_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_PM_RPT_RESULT;
import com.example.mp.gw.common.domain.RedisStructure.Q_PM_RPT_RESULT_RMD;
import com.example.mp.gw.common.exception.InternalServerException;
import com.example.mp.gw.common.exception.KeepRunningToWorkerException;
import com.example.mp.gw.common.exception.StopRunningToWorkerException;
import com.example.mp.gw.common.exception.UnknownStructureNameException;
import com.example.mp.gw.common.service.RedisService;
import com.example.mp.gw.doc.domain.Document;
import com.example.mp.gw.doc.domain.DocumentIssue;
import com.example.mp.gw.doc.domain.DocumentIssueIsd;
import com.example.mp.gw.doc.domain.DocumentIssueStatConfirmation;
import com.example.mp.gw.doc.domain.DocumentIssueStatConfirmationIsd;
import com.example.mp.gw.doc.mappers.altibase.DocumentMapper;
import com.example.mp.gw.member.domain.Agree;
import com.example.mp.gw.ms.domain.PmRpt;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service("BcRetryService")
public class BcRetryServiceImpl implements BcRetryService
{
	@Autowired
	BcRetryService bcRetryService;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<?>> redisRetryToBc;

	@Autowired
	private RedisService<String, Map<String, Object>> redisSendReportResultToBc;

	@Autowired
	private DocumentMapper documentMapper;

	@Value("${spring.bc.scheduler.retry.send-cnt}")
	private String RETRY_SND_CNT;

	@Value("${spring.bc.scheduler.retry.send-interval}")
	private String RETRY_SND_INTERVAL;


	/**
	 * BC 연동 실패건 재처리
	 * @return void
	 * @throws KeepRunningToWorkerException
	 * @throws StopRunningToWorkerException   
	 */
	@Override
	public void popAndRetryToBc() throws KeepRunningToWorkerException, StopRunningToWorkerException
	{
		RedisQueueDataWrapper<?> request = null;

		try
		{
			boolean IS_RETRY_DTM = false;
			long    nowTime      = System.currentTimeMillis() - Long.parseLong(RETRY_SND_INTERVAL); 
			String	nowDtm       = FORMATTER.yyyyMMddHHmmss.val().format(nowTime);

			// 1. 데이터 추출
			request = redisRetryToBc.rpop(Q_BC_RETRY.STRUCTURE_NAME);

			if (null == request)
				return;

			log.debug("{ ■ BC 재처리 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class));

			// 2. 재처리 대상 여부 판별(재처리 시간)
			IS_RETRY_DTM = Long.parseLong(request.getSend_dtm()) <= Long.parseLong(nowDtm);

			// 3. 재처리 기준 시간 미만이면 재삽입
			if (!IS_RETRY_DTM)
			{
				redisRetryToBc.lpush(Q_BC_RETRY.STRUCTURE_NAME, request);

				return;
			}

			// 4. 요청 Queue 에 따른 분기 처리
			switch(request.getStructure_name())
			{
				// 수신동의상태전송(등록/해제) 재전송
				case Q_BC_AGREE_TO_SEND.STRUCTURE_NAME :
	
					bcRetryService.caseApproveToSend(request);
	
					break;

				// 리마인드 문자 발송 결과 조회 재전송
				case Q_BC_RMD_SND_RPT_TO_SEND.STRUCTURE_NAME :
	
					bcRetryService.caseRmdRptSndToSend(request);
	
					break;
	
				// 리마인드 문자 수신 결과 조회 재전송
				case Q_BC_RMD_RSV_RPT_TO_SEND.STRUCTURE_NAME :
	
					bcRetryService.caseRmdRptRsvToSend(request);
	
					break;

				// 본문자 발송 결과 조회 재전송
				case Q_BC_SND_RPT_TO_SEND.STRUCTURE_NAME :

					bcRetryService.caseRptSndToSend(request);

					break;

				// 본문자 수신 결과 조회 재전송
				case Q_BC_RSV_RPT_TO_SEND.STRUCTURE_NAME :

					bcRetryService.caseRptRsvToSend(request);

					break;

				// (열람확인대체시)열람확인결과수신 처리 재전송
				case Q_BC_DOC_READ_DTM_TO_SEND.STRUCTURE_NAME :

					bcRetryService.caseDocReadToSend(request);

					break;					

				// 유통증명서발급 결과 수신 처리 재전송
				case Q_BC_DOC_ISS_TO_SEND.STRUCTURE_NAME :

					bcRetryService.caseDocIssSndToSend(request);

					break;

				// 전자문서 유통정보 수치 확인서 발급 결과 수신 처리 재전송
				case Q_BC_DOC_STS_CFM_ISS_TO_SEND.STRUCTURE_NAME :

					bcRetryService.caseDocStsCfmIssSndToSend(request);

					break;

				default :

					throw new UnknownStructureNameException();
			}
		}
		catch (UnknownStructureNameException e)
		{
			log.error("{ ■ BC 재처리 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class), e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ BC 재처리 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class), e.getClass().getName(), e.getMessage());

			if (null != request)
				redisRetryToBc.lpush(Q_BC_RETRY.STRUCTURE_NAME, request);

			throw new StopRunningToWorkerException(e);
		}
	}

	/**
	 * BC 수신동의(등록/해제) 정보
	 * @return void
	 */
	public void caseApproveToSend(RedisQueueDataWrapper<?> request)
	{
		try
		{
			Agree agree = (Agree) request.getData();

			boolean IS_RETRY_CNT = request.getSend_count() < Integer.parseInt(RETRY_SND_CNT);

			MDC.put(Const.TRACING_ID, agree.getPhone() + (StringUtils.hasText(agree.getMessageId()) ? ":" + agree.getMessageId() : ""));

			// 재처리 횟수 초과 시 폐기
			if (!IS_RETRY_CNT)
			{
				log.error("{ ■ BC 수신동의(등록/해제) - EXPIRED ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class));

				return;
			}

			log.debug("{ ■ BC 수신동의(등록/해제) - 재요청 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(agree, Agree.class));

			// 재전송 카운트 증가
			request.incSndCnt();

			// 재전송
			redisRetryToBc.lpush(request.getStructure_name(), request);
		}
		catch (Exception e)
		{
			throw e;
		}

		return;
	}

	/**
	 * BC 리마인드 문자 발송 결과
	 * @return void
	 */
	public void caseRmdRptSndToSend(RedisQueueDataWrapper<?> request)
	{
		try
		{
			PmRpt report = (PmRpt) request.getData();

			boolean IS_RETRY_CNT = request.getSend_count() < Integer.parseInt(RETRY_SND_CNT);

			MDC.put(Const.TRACING_ID, report.getMessage_id());

			// 재처리 횟수 초과 시 폐기
			if (!IS_RETRY_CNT)
			{
				log.error("{ ■ BC 리마인드 문자 발송 결과 - EXPIRED ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class));

				// Biz-center 발송 레포트 전송 실패, TS 전송 
				Map<String, Object> result = new HashMap<String, Object>();

				result.put(Q_PM_RPT_RESULT_RMD.RPT_TYPE.name()			, report.getRpt_type());
				result.put(Q_PM_RPT_RESULT_RMD.RPT_RSLT_CD.name()		, report.getMms_rslt_dvcd());
				result.put(Q_PM_RPT_RESULT_RMD.RCS_YN.name()			, report.getRcs_yn());
				result.put(Q_PM_RPT_RESULT_RMD.PART_MM.name()			, report.getPart_mm());
				result.put(Q_PM_RPT_RESULT_RMD.MSGKEY.name()			, report.getMsg_key());
				result.put(Q_PM_RPT_RESULT_RMD.MESSAGE_ID.name()		, report.getMessage_id());
				result.put(Q_PM_RPT_RESULT_RMD.MULTI_MBL_PRC_TYPE.name(), report.getMulti_mbl_prc_type());
				result.put(Q_PM_RPT_RESULT_RMD.SEND_STAT.name()			, Q_PM_RPT_RESULT_RMD.FIELD.SEND_STAT_CD.FAIL.val());
				result.put(Q_PM_RPT_RESULT_RMD.END_DT.name()			, FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));

				redisSendReportResultToBc.lpush(Q_PM_RPT_RESULT_RMD.STRUCTURE_NAME, result);

				return;
			}

			log.debug("{ ■ BC 리마인드 발송 결과 - 재요청 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(report, PmRpt.class));
			
			// 재전송 카운트 증가
			request.incSndCnt();

			// 재전송
			redisRetryToBc.lpush(request.getStructure_name(), request);
		}
		catch (Exception e)
		{
			throw e;
		}

		return;
	}

	/**
	 * BC 리마인드 문자 수신 결과
	 * @return void
	 */
	public void caseRmdRptRsvToSend(RedisQueueDataWrapper<?> request)
	{
		try
		{
			PmRpt report = (PmRpt) request.getData();

			boolean IS_RETRY_CNT = request.getSend_count() < Integer.parseInt(RETRY_SND_CNT);

			MDC.put(Const.TRACING_ID, report.getMessage_id());

			// 재처리 횟수 초과 시 폐기
			if (!IS_RETRY_CNT)
			{
				log.error("{ ■ BC 리마인드 문자 수신 결과 - EXPIRED ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class));

				// Biz-center 수신 레포트 전송 실패, TS 전송 
				Map<String, Object> result = new HashMap<String, Object>();

				result.put(Q_PM_RPT_RESULT_RMD.RPT_TYPE.name()			, report.getRpt_type());
				result.put(Q_PM_RPT_RESULT_RMD.RPT_RSLT_CD.name()		, report.getMms_rslt_dvcd());
				result.put(Q_PM_RPT_RESULT_RMD.RCS_YN.name()			, report.getRcs_yn());
				result.put(Q_PM_RPT_RESULT_RMD.PART_MM.name()			, report.getPart_mm());
				result.put(Q_PM_RPT_RESULT_RMD.MSGKEY.name()			, report.getMsg_key());
				result.put(Q_PM_RPT_RESULT_RMD.MESSAGE_ID.name()		, report.getMessage_id());
				result.put(Q_PM_RPT_RESULT_RMD.MULTI_MBL_PRC_TYPE.name(), report.getMulti_mbl_prc_type());
				result.put(Q_PM_RPT_RESULT_RMD.SEND_STAT.name()			, Q_PM_RPT_RESULT_RMD.FIELD.SEND_STAT_CD.FAIL.val());
				result.put(Q_PM_RPT_RESULT_RMD.END_DT.name()			, FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));

				redisSendReportResultToBc.lpush(Q_PM_RPT_RESULT_RMD.STRUCTURE_NAME, result);

				return;
			}

			log.debug("{ ■ BC 리마인드 문자 수신 결과 - 재요청 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(report, PmRpt.class));

			// 재전송 카운트 증가
			request.incSndCnt();

			// 재전송
			redisRetryToBc.lpush(request.getStructure_name(), request);
		}
		catch (Exception e)
		{
			throw e;
		}

		return;
	}

	/**
	 * BC 본문자 발송 결과
	 * @return void
	 */
	public void caseRptSndToSend(RedisQueueDataWrapper<?> request)
	{
		try
		{
			PmRpt report = (PmRpt) request.getData();

			boolean IS_RETRY_CNT = request.getSend_count() < Integer.parseInt(RETRY_SND_CNT);

			MDC.put(Const.TRACING_ID, report.getMessage_id());

			// 재처리 횟수 초과 시 폐기
			if (!IS_RETRY_CNT)
			{
				log.error("{ ■ BC 본문자 발송 결과 - EXPIRED ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class));

				// Biz-center 발송 레포트 전송 실패, TS 전송 
				Map<String, Object> result = new HashMap<String, Object>();

				result.put(Q_PM_RPT_RESULT.RPT_TYPE.name()			, report.getRpt_type());
				result.put(Q_PM_RPT_RESULT.RPT_RSLT_CD.name()		, report.getMms_rslt_dvcd());
				result.put(Q_PM_RPT_RESULT.RCS_YN.name()			, report.getRcs_yn());
				result.put(Q_PM_RPT_RESULT.PART_MM.name()			, report.getPart_mm());
				result.put(Q_PM_RPT_RESULT.MSGKEY.name()			, report.getMsg_key());
				result.put(Q_PM_RPT_RESULT.MESSAGE_ID.name()		, report.getMessage_id());
				result.put(Q_PM_RPT_RESULT.MULTI_MBL_PRC_TYPE.name(), report.getMulti_mbl_prc_type());
				result.put(Q_PM_RPT_RESULT.SEND_STAT.name()			, Q_PM_RPT_RESULT.FIELD.SEND_STAT_CD.FAIL.val());
				result.put(Q_PM_RPT_RESULT.END_DT.name()			, FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));

				redisSendReportResultToBc.lpush(Q_PM_RPT_RESULT.STRUCTURE_NAME, result);

				return;
			}

			log.debug("{ ■ BC 본문자 발송 결과 - 재요청 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(report, PmRpt.class));
			
			// 재전송 카운트 증가
			request.incSndCnt();

			// 재전송
			redisRetryToBc.lpush(request.getStructure_name(), request);
		}
		catch (Exception e)
		{
			throw e;
		}

		return;
	}

	/**
	 * BC 본문자 수신 결과
	 * @return void
	 */
	public void caseRptRsvToSend(RedisQueueDataWrapper<?> request)
	{
		try
		{
			PmRpt report = (PmRpt) request.getData();

			boolean IS_RETRY_CNT = request.getSend_count() < Integer.parseInt(RETRY_SND_CNT);

			MDC.put(Const.TRACING_ID, report.getMessage_id());

			// 재처리 횟수 초과 시 폐기
			if (!IS_RETRY_CNT)
			{
				log.error("{ ■ BC 본문자 수신 결과 - EXPIRED ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class));

				// // Biz-center 수신 레포트 전송 실패, TS 전송 
				Map<String, Object> result = new HashMap<String, Object>();

				result.put(Q_PM_RPT_RESULT.RPT_TYPE.name()			, report.getRpt_type());
				result.put(Q_PM_RPT_RESULT.RPT_RSLT_CD.name()		, report.getMms_rslt_dvcd());
				result.put(Q_PM_RPT_RESULT.RCS_YN.name()			, report.getRcs_yn());
				result.put(Q_PM_RPT_RESULT.PART_MM.name()			, report.getPart_mm());
				result.put(Q_PM_RPT_RESULT.MSGKEY.name()			, report.getMsg_key());
				result.put(Q_PM_RPT_RESULT.MESSAGE_ID.name()		, report.getMessage_id());
				result.put(Q_PM_RPT_RESULT.MULTI_MBL_PRC_TYPE.name(), report.getMulti_mbl_prc_type());
				result.put(Q_PM_RPT_RESULT.SEND_STAT.name()			, Q_PM_RPT_RESULT.FIELD.SEND_STAT_CD.FAIL.val());
				result.put(Q_PM_RPT_RESULT.END_DT.name()			, FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));

				redisSendReportResultToBc.lpush(Q_PM_RPT_RESULT.STRUCTURE_NAME, result);

				return;
			}

			log.debug("{ ■ BC 본문자 수신 결과 - 재요청 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(report, PmRpt.class));

			// 재전송 카운트 증가
			request.incSndCnt();

			// 재전송
			redisRetryToBc.lpush(request.getStructure_name(), request);
		}
		catch (Exception e)
		{
			throw e;
		}

		return;
	}

	/**
	 * BC 메시지 열람일시
	 * @return void
	 */
	public void caseDocReadToSend(RedisQueueDataWrapper<?> request)
	{
		try
		{
			Document doc = (Document) request.getData();

			boolean IS_RETRY_CNT = request.getSend_count() < Integer.parseInt(RETRY_SND_CNT);

			MDC.put(Const.TRACING_ID, doc.getMessageId());

			// 재처리 횟수 초과 시 폐기
			if (!IS_RETRY_CNT)
			{
				log.error("{ ■ BC 열람일시 - EXPIRED ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class));

				return;
			}

			log.debug("{ ■ BC 열람일시 - 재요청 ■ \"{}\": \"{}\", \"type\": \"worker\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(doc, Document.class));

			// 재전송 카운트 증가
			request.incSndCnt();

			// 재전송
			redisRetryToBc.lpush(request.getStructure_name(), request);
		}
		catch (Exception e)
		{
			throw e;
		}

		return;
	}

	/**
	 * BC 유통증명서발급 결과
	 * @return void
	 */
	@Transactional(rollbackFor = Exception.class)
	public void caseDocIssSndToSend(RedisQueueDataWrapper<?> request)
	{
		try
		{
			DocumentIssue doc = (DocumentIssue) request.getData();

			boolean IS_RETRY_CNT = request.getSend_count() < Integer.parseInt(RETRY_SND_CNT);

			MDC.put(Const.TRACING_ID, doc.getSndn_mgnt_seq() + "_" + doc.getSndn_seq_no());

			// 재처리 횟수 초과 시 폐기
			if (!IS_RETRY_CNT)
			{
				log.error("{ ■ BC 유통증명서발급(유통증명서) 결과 - EXPIRED ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class));

				// 유통증명서(법인/비즈) 발급(실패) 내역 기록
				documentMapper.updateDocumentIssueIsd(DocumentIssueIsd.builder()
														.isdStatus(DOCUMENT.ISSUE.ISD_STATUS.SND.val())
														.sndRsltCd(DOCUMENT.ISSUE.SND_RSLT_CD.FAIL.val())
														.sndRsltMsg(StringUtils.hasText(doc.getSnd_result_msg())
																	? doc.getSnd_result_msg()
																	: new InternalServerException().getMessage())
														.sndRsltDtm(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
														.dcmntIsdId(doc.getDcmnt_isd_id())
													  .build()
													 );

				return;
			}

			log.debug("{ ■ BC 유통증명서발급(유통증명서) 결과 - 재요청 ■ \"{}\": \"{}\", \"type\": \"worker\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(doc, DocumentIssue.class));

			// 재전송 카운트 증가
			request.incSndCnt();

			// 재전송
			redisRetryToBc.lpush(request.getStructure_name(), request);
		}
		catch (Exception e)
		{
			throw e;
		}

		return;
	}

	/**
	 * BC 유통정보 수치 확인서 발급 결과
	 * @return void
	 */
	@Transactional(rollbackFor = Exception.class)
	public void caseDocStsCfmIssSndToSend(RedisQueueDataWrapper<?> request)
	{
		try
		{
			DocumentIssueStatConfirmation docStsCfm = (DocumentIssueStatConfirmation) request.getData();

			boolean IS_RETRY_CNT = request.getSend_count() < Integer.parseInt(RETRY_SND_CNT);

			MDC.put(Const.TRACING_ID, docStsCfm.getEaddr());

			// 재처리 횟수 초과 시 폐기
			if (!IS_RETRY_CNT)
			{
				log.error("{ ■ BC 유통정보 수치 확인서 발급 결과 - EXPIRED ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class));

				// 유통증명서(법인/비즈) 발급(실패) 내역 기록
				documentMapper.updatetDocumentStatConfirmationIssueIsd(DocumentIssueStatConfirmationIsd.builder()
																			.isdStatus(DOCUMENT.ISSUE.ISD_STATUS.SND.val())
																			.sndRsltCd(DOCUMENT.ISSUE.SND_RSLT_CD.FAIL.val())
																			.sndRsltMsg(StringUtils.hasText(docStsCfm.getSnd_result_msg())
																				? docStsCfm.getSnd_result_msg()
																				: new InternalServerException().getMessage())
																			.sndRsltDtm(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
																			.dcmntStsIsdSeq(docStsCfm.getDcmnt_sts_isd_seq())
																	   .build()
																	  );

				return;
			}

			log.debug("{ ■ BC 유통정보 수치 확인서 발급 결과 - 재요청 ■ \"{}\": \"{}\", \"type\": \"worker\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(docStsCfm, DocumentIssueStatConfirmation.class));

			// 재전송 카운트 증가
			request.incSndCnt();

			// 재전송
			redisRetryToBc.lpush(request.getStructure_name(), request);
		}
		catch (Exception e)
		{
			throw e;
		}

		return;
	}
}
