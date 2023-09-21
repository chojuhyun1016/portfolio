package com.example.mp.gw.bc.service.impl;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.mp.gw.bc.domain.BcAccessTokenResponse;
import com.example.mp.gw.bc.domain.MessageResult;
import com.example.mp.gw.bc.domain.MessageResultsRequest;
import com.example.mp.gw.bc.domain.ResponseResult;
import com.example.mp.gw.bc.domain.VerifyTokenResult;
import com.example.mp.gw.bc.domain.VerifyTokensResultRequest;
import com.example.mp.gw.bc.exception.FailRequestDcmntReadDtmToBcException;
import com.example.mp.gw.bc.exception.FailRequestReportToBcException;
import com.example.mp.gw.bc.exception.FailRequestToBcException;
import com.example.mp.gw.bc.exception.FailResponseDcmntReadDtmToBcException;
import com.example.mp.gw.bc.exception.FailResponseReportToBcException;
import com.example.mp.gw.bc.exception.FailResponseToBcException;
import com.example.mp.gw.bc.exception.NotExistAccessTokenToBcException;
import com.example.mp.gw.bc.service.BcMessageService;
import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.domain.RedisQueueDataWrapper;
import com.example.mp.gw.common.domain.Const.FORMATTER;
import com.example.mp.gw.common.domain.Const.LGUPLUS;
import com.example.mp.gw.common.domain.RedisStructure.H_BC_SESSION;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_DOC_READ_DTM_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_RETRY;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_RMD_RSV_RPT_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_RMD_SND_RPT_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_RSV_RPT_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_SESSION;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_SND_RPT_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_PM_RPT_RESULT;
import com.example.mp.gw.common.domain.RedisStructure.Q_PM_RPT_RESULT_RMD;
import com.example.mp.gw.common.service.RedisService;
import com.example.mp.gw.doc.domain.Document;
import com.example.mp.gw.ms.domain.PmRpt;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

/**
 * @Class Name : BcMessageServiceImpl.java
 * @Description : BizCenter 메시지 서비스 구현 객체 
 * @author 조주현
 * @since 2021.07.13
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.07.13	    조주현           최초 생성
 * 
 *  </pre>
 * 
 */


@Slf4j
@Service("BcMessageService")
public class BcMessageServiceImpl implements BcMessageService
{
	@Autowired
	private BcMessageService bcMessageService;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<PmRpt>> redisSendSndReportsToBc;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<PmRpt>> redisSendRptReportsToBc;

	@Autowired
	private RedisService<String, Map<String, Object>> redisSendReportResultToBc;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Document>> redisSendReadDtmToBc;

	@Autowired
	private RedisService<String, BcAccessTokenResponse> redisBcToken;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<?>> redisRetryToBc;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	@Qualifier("bizCenterHeaders")
	private HttpHeaders bizCenterHeaders;

	@Value("${spring.bc.hostname}")
	private String HOSTNAME;

	@Value("${spring.bc.url.message-read}")
	private String MESSAGE_READ_URL;

	@Value("${spring.bc.url.message-result}")
	private String MESSAGE_RESULT_URL;


	/**
	 * Biz-center 리마인드 문자 발송결과 전송 스케줄러 (G/W -> 비즈센터)
	 * @param Integer
	 * @return void  
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void popAndSendRmdSndRpt(Integer BIZ_RMD_SND_RPT_SEND_CNT)
	{
		RedisQueueDataWrapper<PmRpt>   request  = null;
		List<RedisQueueDataWrapper<?>> requests = new ArrayList<RedisQueueDataWrapper<?>>();

		List<MessageResult>   reports        = new ArrayList<MessageResult>();
		MessageResultsRequest reportsRequest = null;

		try
		{
			// 1. 큐에서 리마인드 문자 발송 결과 전송 요청 데이터 읽기
			for (int i = 0; i < BIZ_RMD_SND_RPT_SEND_CNT; i++)
			{
				request = redisSendSndReportsToBc.rpop(Q_BC_RMD_SND_RPT_TO_SEND.STRUCTURE_NAME);

				if (null == request)
					break;

				PmRpt result = (PmRpt) request.getData();

				if (null == result)
					break;

				log.info("{ ■ BC 리마인드 문자 발송결과 전송 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, result.getSndn_mgnt_seq() + "_" + result.getSndn_seq_no(), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(result, PmRpt.class));

				requests.add(request);

				reports.add(
							MessageResult.builder()
												   .sndn_mgnt_seq(result.getSndn_mgnt_seq())
												   .sndn_seq_no(result.getSndn_seq_no())
												   .mms_sndg_rslt_sqno(result.getMms_sndg_rslt_sqno())
												   .rl_mms_sndg_telno(result.getRl_mms_sndg_telno())
												   .mms_rslt_dvcd(result.getMms_rslt_dvcd())
												   .mms_rslt_tmst(result.getMms_rslt_tmst())
												   .msg_type(result.getMsg_type())
												   .prev_approve_yn(result.getPrev_approve_yn())
												   .rcv_npost(result.getRcv_npost())
												   .rcv_plfm_id(result.getRcv_plfm_id())
												   .message_id(result.getMessage_id())
												   .msg_key(result.getMsg_key())
												   .multi_mbl_prc_type(result.getMulti_mbl_prc_type())
												   .click_dt(result.getClick_dt())
												   .approve_dt(result.getApprove_dt())
												   .part_mm(result.getPart_mm())
												   .rcs_yn(result.getRcs_yn())
										 .build()
						   );
			}

			// 2. Biz-center 리마인드 문자 발송 결과 전송
			if (reports.size() > 0)
			{
				reportsRequest = MessageResultsRequest.builder()
															    .mbl_bzowr_dvcd(LGUPLUS.LG_UPLUS_TYPE)
															    .reqs(reports)
													  .build();

				bcMessageService.sendMsgResult(reportsRequest);
			}
		}
		catch (FailRequestReportToBcException e)
		{
			// 재처리 레디스(Q_BC_RETRY) 적재
			if (false == requests.isEmpty())
			{
				String nowDtm = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());

				for (RedisQueueDataWrapper<?> req : requests)
				{
					req.setSend_dtm(nowDtm);
				}

				redisRetryToBc.lpushT(Q_BC_RETRY.STRUCTURE_NAME, requests);
			}
				
			throw new RuntimeException(e);
		}
		catch (FailResponseReportToBcException e)
		{
			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ BC 리마인드 문자 발송결과 전송 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(requests, List.class), e.getClass().getName(), e.getMessage());

			// 재처리 레디스(Q_BC_RETRY) 적재
			if (false == requests.isEmpty())
			{
				String nowDtm = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());

				for (RedisQueueDataWrapper<?> req : requests)
				{
					req.setSend_dtm(nowDtm);
				}

				redisRetryToBc.lpushT(Q_BC_RETRY.STRUCTURE_NAME, requests);
			}

			throw e;
		}
		finally
		{
			requests.clear();
			reports.clear();
		}
	}

	/**
	 * Biz-center 리마인드 문자 수신결과 전송 스케줄러 (G/W -> 비즈센터)
	 * @param Integer
	 * @return void 
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void popAndSendRmdRsvRpt(Integer BIZ_RMD_RSV_RPT_SEND_CNT)
	{
		RedisQueueDataWrapper<PmRpt>   request  = null;
		List<RedisQueueDataWrapper<?>> requests = new ArrayList<RedisQueueDataWrapper<?>>();

		List<MessageResult>   reports        = new ArrayList<MessageResult>();
		MessageResultsRequest reportsRequest = null;

		List<Map<String, Object>> reportsResult = new ArrayList<Map<String, Object>>();

		try
		{
			// 1. 큐에서 리마인드 문자 수신 결과 전송 요청 데이터 읽기
			for (int i = 0; i < BIZ_RMD_RSV_RPT_SEND_CNT; i++)
			{
				request = redisSendRptReportsToBc.rpop(Q_BC_RMD_RSV_RPT_TO_SEND.STRUCTURE_NAME);
				
				if (null == request)
					break;

				PmRpt result = (PmRpt) request.getData();

				if (null == result)
					break;

				log.info("{ ■ BC 리마인드 수신결과 전송 ■ \"{}\": \"{}\", \"type\": \"worker\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, result.getSndn_mgnt_seq() + "_" + result.getSndn_seq_no(), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(result, PmRpt.class));

				requests.add(request);

				reports.add(
							MessageResult.builder()
												   .sndn_mgnt_seq(result.getSndn_mgnt_seq())
												   .sndn_seq_no(result.getSndn_seq_no())
												   .mms_sndg_rslt_sqno(result.getMms_sndg_rslt_sqno())
												   .rl_mms_sndg_telno(result.getRl_mms_sndg_telno())
												   .mms_rslt_dvcd(result.getMms_rslt_dvcd())
												   .mms_rslt_tmst(result.getMms_rslt_tmst())
												   .msg_type(result.getMsg_type())
												   .prev_approve_yn(result.getPrev_approve_yn())
												   .rcv_npost(result.getRcv_npost())
												   .rcv_plfm_id(result.getRcv_plfm_id())
												   .message_id(result.getMessage_id())
												   .msg_key(result.getMsg_key())
												   .multi_mbl_prc_type(result.getMulti_mbl_prc_type())
												   .click_dt(result.getClick_dt())
												   .approve_dt(result.getApprove_dt())
												   .part_mm(result.getPart_mm())
												   .rcs_yn(result.getRcs_yn())
										 .build()
						   );

				Map<String, Object> map = new HashMap<String, Object>();

				map.put(Q_PM_RPT_RESULT.RPT_TYPE.name()			 	 , result.getRpt_type());
				map.put(Q_PM_RPT_RESULT.RPT_RSLT_CD.name()		 	 , result.getMms_rslt_dvcd());
				map.put(Q_PM_RPT_RESULT.RCS_YN.name()			 	 , result.getRcs_yn());
				map.put(Q_PM_RPT_RESULT_RMD.MSGKEY.name()			 , result.getMsg_key());
				map.put(Q_PM_RPT_RESULT_RMD.PART_MM.name()			 , result.getPart_mm());
				map.put(Q_PM_RPT_RESULT_RMD.MESSAGE_ID.name()		 , result.getMessage_id());
				map.put(Q_PM_RPT_RESULT_RMD.MULTI_MBL_PRC_TYPE.name(), result.getMulti_mbl_prc_type());
				map.put(Q_PM_RPT_RESULT_RMD.SEND_STAT.name()		 , Q_PM_RPT_RESULT_RMD.FIELD.SEND_STAT_CD.SUCCESS.val());
				map.put(Q_PM_RPT_RESULT_RMD.END_DT.name()			 , FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));

				reportsResult.add(map);
			}

			// 2. Biz-center 리마인드 문자 수신 결과 전송
			if (reports.size() > 0)
			{
				reportsRequest = MessageResultsRequest.builder()
															    .mbl_bzowr_dvcd(LGUPLUS.LG_UPLUS_TYPE)
															    .reqs(reports)
													  .build();

				bcMessageService.sendMsgResult(reportsRequest);
			}

			// 3. Biz-center 레포트 전송 결과 TS 전송
			if (reportsResult.size() > 0)
			{
				log.error("{ ■ TS 리마인드 수신결과 전송 ■ \"{}\": \"{}\", \"type\": \"worker\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(reportsResult, List.class));

				redisSendReportResultToBc.lpushT(Q_PM_RPT_RESULT_RMD.STRUCTURE_NAME, reportsResult);
			}
		}
		catch (FailRequestReportToBcException e)
		{
			// 재처리 레디스(Q_BC_RETRY) 적재
			if (false == requests.isEmpty())
			{
				String nowDtm = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());

				for (RedisQueueDataWrapper<?> req : requests)
				{
					req.setSend_dtm(nowDtm);
				}

				redisRetryToBc.lpushT(Q_BC_RETRY.STRUCTURE_NAME, requests);
			}

			throw new RuntimeException(e);
		}
		catch (FailResponseReportToBcException e)
		{
			// Biz-center 레포트 전송 응답 결과가 실패인 경우, 결과 TS 전송
			if (reportsResult.size() > 0)
			{
				log.error("{ ■ TS 본문자 리마인드 전송 - ERROR ■ \"{}\": \"{}\", \"type\": \"worker\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(reportsResult, List.class), e.getClass().getName(), e.getMessage());

				redisSendReportResultToBc.lpushT(Q_PM_RPT_RESULT_RMD.STRUCTURE_NAME, reportsResult);
			}

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ BC 리마인드 문자 수신결과 전송 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(requests, List.class), e.getClass().getName(), e.getMessage());

			// 재처리 레디스(Q_BC_RETRY) 적재
			if (false == requests.isEmpty())
			{
				String nowDtm = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());

				for (RedisQueueDataWrapper<?> req : requests)
				{
					req.setSend_dtm(nowDtm);
				}

				redisRetryToBc.lpushT(Q_BC_RETRY.STRUCTURE_NAME, requests);
			}

			throw e;
		}
		finally
		{
			requests.clear();
			reports.clear();
		}
	}

	/**
	 * Biz-center 본문자 발송결과 전송 스케줄러 (G/W -> 비즈센터)
	 * @param Integer
	 * @return void  
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void popAndSendSndRpt(Integer BIZ_SND_RPT_SEND_CNT)
	{
		RedisQueueDataWrapper<PmRpt>   request  = null;
		List<RedisQueueDataWrapper<?>> requests = new ArrayList<RedisQueueDataWrapper<?>>();	// 재처리용

		List<MessageResult>   reports        = new ArrayList<MessageResult>();
		MessageResultsRequest reportsRequest = null;

		List<Map<String, Object>> reportsResult = new ArrayList<Map<String, Object>>();

		try
		{
			// 1. 큐에서 본문자 발송 결과 전송 요청 데이터 읽기
			for (int i = 0; i < BIZ_SND_RPT_SEND_CNT; i++)
			{
				request = redisSendSndReportsToBc.rpop(Q_BC_SND_RPT_TO_SEND.STRUCTURE_NAME);

				if (null == request)
					break;

				PmRpt result = (PmRpt) request.getData();

				if (null == result)
					break;

				log.info("{ ■ BC 본문자 발송결과 전송 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, result.getSndn_mgnt_seq() + "_" + result.getSndn_seq_no(), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(result, PmRpt.class));

				requests.add(request);

				reports.add(
							MessageResult.builder()
												   .sndn_mgnt_seq(result.getSndn_mgnt_seq())
												   .sndn_seq_no(result.getSndn_seq_no())
												   .mms_sndg_rslt_sqno(result.getMms_sndg_rslt_sqno())
												   .rl_mms_sndg_telno(result.getRl_mms_sndg_telno())
												   .mms_rslt_dvcd(result.getMms_rslt_dvcd())
												   .mms_rslt_tmst(result.getMms_rslt_tmst())
												   .msg_type(result.getMsg_type())
												   .prev_approve_yn(result.getPrev_approve_yn())
												   .rcv_npost(result.getRcv_npost())
												   .rcv_plfm_id(result.getRcv_plfm_id())
												   .message_id(result.getMessage_id())
												   .msg_key(result.getMsg_key())
												   .multi_mbl_prc_type(result.getMulti_mbl_prc_type())
												   .click_dt(result.getClick_dt())
												   .approve_dt(result.getApprove_dt())
												   .part_mm(result.getPart_mm())
												   .rcs_yn(result.getRcs_yn())
										 .build()
						   );

				Map<String, Object> map = new HashMap<String, Object>();

				map.put(Q_PM_RPT_RESULT.RPT_TYPE.name()			 , result.getRpt_type());
				map.put(Q_PM_RPT_RESULT.RPT_RSLT_CD.name()		 , result.getMms_rslt_dvcd());
				map.put(Q_PM_RPT_RESULT.RCS_YN.name()			 , result.getRcs_yn());
				map.put(Q_PM_RPT_RESULT.MSGKEY.name()			 , result.getMsg_key());
				map.put(Q_PM_RPT_RESULT.PART_MM.name()			 , result.getPart_mm());
				map.put(Q_PM_RPT_RESULT.MESSAGE_ID.name()		 , result.getMessage_id());
				map.put(Q_PM_RPT_RESULT.MULTI_MBL_PRC_TYPE.name(), result.getMulti_mbl_prc_type());
				map.put(Q_PM_RPT_RESULT.SEND_STAT.name()		 , Q_PM_RPT_RESULT.FIELD.SEND_STAT_CD.SUCCESS.val());
				map.put(Q_PM_RPT_RESULT.END_DT.name()			 , FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));

				reportsResult.add(map);
			}

			// 2. Biz-center 본문자 발송 결과 전송
			if (reports.size() > 0)
			{
				reportsRequest = MessageResultsRequest.builder()
															    .mbl_bzowr_dvcd(LGUPLUS.LG_UPLUS_TYPE)
															    .reqs(reports)
													  .build();

				bcMessageService.sendMsgResult(reportsRequest);
			}

			// 3. Biz-center 레포트 전송 결과 TS 전송
			if (reportsResult.size() > 0)
			{
				log.info("{ ■ TS 본문자 발송결과 전송 ■ \"{}\": \"{}\", \"type\": \"worker\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(reportsResult, List.class));

				redisSendReportResultToBc.lpushT(Q_PM_RPT_RESULT.STRUCTURE_NAME, reportsResult);
			}
		}
		catch (FailRequestReportToBcException e)
		{
			// 재처리 레디스(Q_BC_RETRY) 적재
			if (false == requests.isEmpty())
			{
				String nowDtm = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());

				for (RedisQueueDataWrapper<?> req : requests)
				{
					req.setSend_dtm(nowDtm);
				}

				redisRetryToBc.lpushT(Q_BC_RETRY.STRUCTURE_NAME, requests);
			}
				
			throw new RuntimeException(e);
		}
		catch (FailResponseReportToBcException e)
		{
			// Biz-center 레포트 전송 응답 결과가 실패인 경우, 결과 TS 전송
			if (reportsResult.size() > 0)
			{
				log.error("{ ■ TS 본문자 발송결과 전송 - ERROR ■ \"{}\": \"{}\", \"type\": \"worker\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(reportsResult, List.class), e.getClass().getName(), e.getMessage());

				redisSendReportResultToBc.lpushT(Q_PM_RPT_RESULT.STRUCTURE_NAME, reportsResult);
			}

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ BC 본문자 발송결과 전송 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(requests, List.class), e.getClass().getName(), e.getMessage());

			// 재처리 레디스(Q_BC_RETRY) 적재
			if (false == requests.isEmpty())
			{
				String nowDtm = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());

				for (RedisQueueDataWrapper<?> req : requests)
				{
					req.setSend_dtm(nowDtm);
				}

				redisRetryToBc.lpushT(Q_BC_RETRY.STRUCTURE_NAME, requests);
			}

			throw e;
		}
		finally
		{
			requests.clear();
			reports.clear();
		}
	}

	/**
	 * Biz-center 본문자 수신결과 전송 스케줄러 (G/W -> 비즈센터)
	 * @param Integer
	 * @return void 
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void popAndSendRsvRpt(Integer BIZ_RSV_RPT_SEND_CNT)
	{
		RedisQueueDataWrapper<PmRpt>   request  = null;
		List<RedisQueueDataWrapper<?>> requests = new ArrayList<RedisQueueDataWrapper<?>>();	// 재처리용

		List<MessageResult>   reports        = new ArrayList<MessageResult>();
		MessageResultsRequest reportsRequest = null;

		List<Map<String, Object>> reportsResult = new ArrayList<Map<String, Object>>();

		try
		{
			// 1. 큐에서 본문자 수신 결과 전송 요청 데이터 읽기
			for (int i = 0; i < BIZ_RSV_RPT_SEND_CNT; i++)
			{
				request = redisSendRptReportsToBc.rpop(Q_BC_RSV_RPT_TO_SEND.STRUCTURE_NAME);
				
				if (null == request)
					break;

				PmRpt result = (PmRpt) request.getData();

				if (null == result)
					break;

				log.info("{ ■ BC 본문자 수신결과 전송 ■ \"{}\": \"{}\", \"type\": \"worker\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, result.getSndn_mgnt_seq() + "_" + result.getSndn_seq_no(), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(result, PmRpt.class));

				requests.add(request);

				reports.add(
							MessageResult.builder()
												   .sndn_mgnt_seq(result.getSndn_mgnt_seq())
												   .sndn_seq_no(result.getSndn_seq_no())
												   .mms_sndg_rslt_sqno(result.getMms_sndg_rslt_sqno())
												   .rl_mms_sndg_telno(result.getRl_mms_sndg_telno())
												   .mms_rslt_dvcd(result.getMms_rslt_dvcd())
												   .mms_rslt_tmst(result.getMms_rslt_tmst())
												   .msg_type(result.getMsg_type())
												   .prev_approve_yn(result.getPrev_approve_yn())
												   .rcv_npost(result.getRcv_npost())
												   .rcv_plfm_id(result.getRcv_plfm_id())
												   .message_id(result.getMessage_id())
												   .msg_key(result.getMsg_key())
												   .multi_mbl_prc_type(result.getMulti_mbl_prc_type())
												   .click_dt(result.getClick_dt())
												   .approve_dt(result.getApprove_dt())
												   .part_mm(result.getPart_mm())
												   .rcs_yn(result.getRcs_yn())
										 .build()
						   );

				Map<String, Object> map = new HashMap<String, Object>();

				map.put(Q_PM_RPT_RESULT.RPT_TYPE.name()			 , result.getRpt_type());
				map.put(Q_PM_RPT_RESULT.RPT_RSLT_CD.name()		 , result.getMms_rslt_dvcd());
				map.put(Q_PM_RPT_RESULT.RCS_YN.name()			 , result.getRcs_yn());
				map.put(Q_PM_RPT_RESULT.MSGKEY.name()			 , result.getMsg_key());
				map.put(Q_PM_RPT_RESULT.PART_MM.name()			 , result.getPart_mm());
				map.put(Q_PM_RPT_RESULT.MESSAGE_ID.name()		 , result.getMessage_id());
				map.put(Q_PM_RPT_RESULT.MULTI_MBL_PRC_TYPE.name(), result.getMulti_mbl_prc_type());
				map.put(Q_PM_RPT_RESULT.SEND_STAT.name()		 , Q_PM_RPT_RESULT.FIELD.SEND_STAT_CD.SUCCESS.val());
				map.put(Q_PM_RPT_RESULT.END_DT.name()			 , FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));

				reportsResult.add(map);
			}

			// 2. Biz-center 본문자 수신 결과 전송
			if (reports.size() > 0)
			{
				reportsRequest = MessageResultsRequest.builder()
															    .mbl_bzowr_dvcd(LGUPLUS.LG_UPLUS_TYPE)
															    .reqs(reports)
													  .build();

				bcMessageService.sendMsgResult(reportsRequest);
			}

			// 3. Biz-center 레포트 전송 결과 TS 전송
			if (reportsResult.size() > 0)
			{
				log.info("{ ■ TS 본문자 수신결과 전송 ■ \"{}\": \"{}\", \"type\": \"worker\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(reportsResult, List.class));

				redisSendReportResultToBc.lpushT(Q_PM_RPT_RESULT.STRUCTURE_NAME, reportsResult);
			}
		}
		catch (FailRequestReportToBcException e)
		{
			// 재처리 레디스(Q_BC_RETRY) 적재
			if (false == requests.isEmpty())
			{
				String nowDtm = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());

				for (RedisQueueDataWrapper<?> req : requests)
				{
					req.setSend_dtm(nowDtm);
				}

				redisRetryToBc.lpushT(Q_BC_RETRY.STRUCTURE_NAME, requests);
			}

			throw new RuntimeException(e);
		}
		catch (FailResponseReportToBcException e)
		{
			// Biz-center 레포트 전송 응답 결과가 실패인 경우, 결과 TS 전송
			if (reportsResult.size() > 0)
			{
				log.error("{ ■ TS 본문자 수신결과 전송 - ERROR ■ \"{}\": \"{}\", \"type\": \"worker\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(reportsResult, List.class), e.getClass().getName(), e.getMessage());

				redisSendReportResultToBc.lpushT(Q_PM_RPT_RESULT.STRUCTURE_NAME, reportsResult);
			}

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ BC 본문자 수신결과 전송 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(requests, List.class), e.getClass().getName(), e.getMessage());

			// 재처리 레디스(Q_BC_RETRY) 적재
			if (false == requests.isEmpty())
			{
				String nowDtm = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());

				for (RedisQueueDataWrapper<?> req : requests)
				{
					req.setSend_dtm(nowDtm);
				}

				redisRetryToBc.lpushT(Q_BC_RETRY.STRUCTURE_NAME, requests);
			}

			throw e;
		}
		finally
		{
			requests.clear();
			reports.clear();
		}
	}

	/**
	 * BC 열람일시 전송(G/W -> 비즈센터)
	 * @param Integer
	 * @return void
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void popAndSendRdDtm(Integer BIZ_DOC_RD_DTM_CNT)
	{
		RedisQueueDataWrapper<Document> request  = null;
		List<RedisQueueDataWrapper<?>>  requests = new ArrayList<RedisQueueDataWrapper<?>>();

		List<VerifyTokenResult>   reports        = new ArrayList<>();
		VerifyTokensResultRequest reportsRequest = null;

		try
		{
			// 1. 큐에서 열람확인결과수신처리 전송 요청 데이터 읽기
			for (int i = 0; i < BIZ_DOC_RD_DTM_CNT; i++)
			{
				request = redisSendReadDtmToBc.rpop(Q_BC_DOC_READ_DTM_TO_SEND.STRUCTURE_NAME);

				if (null == request)
					break;

				Document doc = (Document) request.getData();

				if (null == doc)
					break;

				requests.add(request);

				log.info("{ ■ BC 열람일시 전송 ■ \"{}\": \"{}\", \"type\": \"worker\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, doc.getMessageId(), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(doc, Document.class));

				reports.add(VerifyTokenResult.builder()
													   .sndn_mgnt_seq(Integer.parseInt(doc.getMessageId().split("_")[0]))
													   .sndn_seq_no(Integer.parseInt(doc.getMessageId().split("_")[1]))
													   .mms_rdg_tmst(doc.getRdDtm())
													   .rcv_npost(doc.getRsvCea())
													   .rcv_plfm_id(doc.getRsvPlfmId())
											 .build()
						);
			}

			if (true == reports.isEmpty())
				return;

			reportsRequest = VerifyTokensResultRequest.builder().reqs(reports).build();

			// 2. Biz-center 열람확인결과수신처리 전송
			bcMessageService.sendDocumentReadDtm(reportsRequest);
		}
		catch (FailRequestDcmntReadDtmToBcException e)
		{
			// 재처리 레디스(Q_BC_RETRY) 적재
			if (false == requests.isEmpty())
			{
				String nowDtm = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());

				for (RedisQueueDataWrapper<?> req : requests)
				{
					req.setSend_dtm(nowDtm);
				}

				redisRetryToBc.lpushT(Q_BC_RETRY.STRUCTURE_NAME, requests);
			}

			throw new RuntimeException(e);
		}
		catch (FailResponseDcmntReadDtmToBcException e)
		{
			throw new RuntimeException(e);
		}
		catch(Exception e)
		{
			log.error("{ ■ BC 열람일시 전송 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(requests, List.class), e.getClass().getName(), e.getMessage());

			// 재처리 레디스(Q_BC_RETRY) 적재
			if (false == requests.isEmpty())
			{
				String nowDtm = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());

				for (RedisQueueDataWrapper<?> req : requests)
				{
					req.setSend_dtm(nowDtm);
				}

				redisRetryToBc.lpushT(Q_BC_RETRY.STRUCTURE_NAME, requests);
			}

			throw e;
		}
	}

	/**
	 * 발송결과/수신결과 전송 (G/W -> 비즈센터)
	 * @param MessageResultsRequest
	 * @return void
	 * @throws FailRequestReportToBcException
	 * @throws FailResponseReportToBcException
	 */
	@Override
	public void sendMsgResult(MessageResultsRequest messageResultsRequest) throws FailRequestReportToBcException, FailResponseReportToBcException
	{
		ResponseEntity<ResponseResult> response = null;

		try
		{
			BcAccessTokenResponse accessToken = redisBcToken.hmget(H_BC_SESSION.STRUCTURE_NAME, H_BC_SESSION.STRUCTURE_NAME);

			if (null == accessToken)
				throw new NotExistAccessTokenToBcException();

			bizCenterHeaders.setBearerAuth(accessToken.getAccess_token());

			HttpEntity<MessageResultsRequest> request = new HttpEntity<>(messageResultsRequest, bizCenterHeaders);

			log.info("{ ■ BC ■ <발송결과/수신결과 요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + MESSAGE_RESULT_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), MessageResultsRequest.class)
					);

			response = restTemplate.exchange(HOSTNAME + MESSAGE_RESULT_URL, HttpMethod.POST, request, ResponseResult.class);
			
			log.info("{ ■ BC ■ <발송결과/수신결과 응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"responseResult\": \"{}\" }"
					, Const.TRACING_ID,MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), ResponseResult.class)
					);

			if (true == "01".equals(response.getBody().getResult_cd()))
				throw new FailResponseToBcException();
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ BC ■ <발송결과/수신결과 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"statusCode\": \"{}\",\"errorName\": \"{}\", \"message\": \"{}\", \"errors\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestReportToBcException(e);
		}
		catch (NotExistAccessTokenToBcException e)
		{
			log.error("{ ■ BC ■ <발송결과/수신결과 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisBcToken.lpush(
							   Q_BC_SESSION.STRUCTURE_NAME
							 , BcAccessTokenResponse.builder()
							 		.error_location(new Object() {}.getClass().getEnclosingMethod().getName())
							 		.error_description(e.getMessage())
							 		.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
							   .build()
							  );

			throw new FailRequestReportToBcException(e);
		}
		catch (FailRequestToBcException e)
		{
			log.error("{ ■ BC ■ <발송결과/수신결과 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestReportToBcException(e);
		}
		catch (FailResponseToBcException e)
		{
			log.error("{ ■ BC ■ <발송결과/수신결과 - FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailResponseReportToBcException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ BC ■ <발송결과/수신결과 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * 열람확인결과수신 (G/W -> 비즈센터)
	 * @param member
	 * @return
	 */
	@Override
	public void sendDocumentReadDtm(VerifyTokensResultRequest verifyTokensResultRequest) throws FailRequestDcmntReadDtmToBcException, FailResponseDcmntReadDtmToBcException
	{
		HttpEntity<VerifyTokensResultRequest> request  = null;
		ResponseEntity<ResponseResult>        response = null;

		try
		{
			BcAccessTokenResponse accessToken = redisBcToken.hmget(H_BC_SESSION.STRUCTURE_NAME, H_BC_SESSION.STRUCTURE_NAME);

			if (null == accessToken)
				throw new NotExistAccessTokenToBcException();

			bizCenterHeaders.setBearerAuth(accessToken.getAccess_token());

			request = new HttpEntity<>(verifyTokensResultRequest, bizCenterHeaders);

			log.info("{ ■ BC ■ <열람확인결과수신 요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + MESSAGE_READ_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), VerifyTokensResultRequest.class)
					);
			
			response = restTemplate.exchange(HOSTNAME + MESSAGE_READ_URL, HttpMethod.POST, request, ResponseResult.class);
			
			log.info("{ ■ BC ■ <열람확인결과수신 응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"responseResult\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), ResponseResult.class)
					);

			if (true == "01".equals(response.getBody().getResult_cd()))
				throw new FailResponseToBcException();
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ BC ■ <열람확인결과수신 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"statusCode\": \"{}\",\"errorName\": \"{}\", \"message\": \"{}\", \"errors\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestDcmntReadDtmToBcException(e);
		}
		catch (NotExistAccessTokenToBcException e)
		{
			log.error("{ ■ BC ■ <열람확인결과수신 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisBcToken.lpush(
							   Q_BC_SESSION.STRUCTURE_NAME
							 , BcAccessTokenResponse.builder()
							 		.error_location(new Object() {}.getClass().getEnclosingMethod().getName())
							 		.error_description(e.getMessage())
							 		.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
							   .build()
							  );

			throw new FailRequestDcmntReadDtmToBcException(e);
		}
		catch (FailRequestToBcException e)
		{
			log.error("{ ■ BC ■ <열람확인결과수신 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestDcmntReadDtmToBcException(e);
		}
		catch (FailResponseToBcException e)
		{
			log.error("{ ■ BC ■ <열람확인결과수신 - FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailResponseDcmntReadDtmToBcException(e);
		}
		catch (Exception e)
		{
			log.error("{■ BC ■ <열람확인결과수신 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}
}
