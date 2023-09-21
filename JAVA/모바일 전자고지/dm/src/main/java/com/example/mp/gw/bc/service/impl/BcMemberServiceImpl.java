package com.example.mp.gw.bc.service.impl;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.mp.gw.bc.domain.Approve;
import com.example.mp.gw.bc.domain.ApprovesRequest;
import com.example.mp.gw.bc.domain.BcAccessTokenResponse;
import com.example.mp.gw.bc.domain.RegisterRejection;
import com.example.mp.gw.bc.domain.RegisterRejectionsRequest;
import com.example.mp.gw.bc.domain.RegisterWhitelist;
import com.example.mp.gw.bc.domain.RegisterWhitelistsRequest;
import com.example.mp.gw.bc.domain.ResponseResult;
import com.example.mp.gw.bc.domain.ResponseResults;
import com.example.mp.gw.bc.domain.StatusRejection;
import com.example.mp.gw.bc.domain.StatusRejectionRequest;
import com.example.mp.gw.bc.exception.FailRequestApproveToBcException;
import com.example.mp.gw.bc.exception.FailRequestRejectionToBcException;
import com.example.mp.gw.bc.exception.FailRequestReportToBcException;
import com.example.mp.gw.bc.exception.FailRequestStatusRejectionToBcException;
import com.example.mp.gw.bc.exception.FailRequestToBcException;
import com.example.mp.gw.bc.exception.FailRequestWhitelistToBcException;
import com.example.mp.gw.bc.exception.FailResponseApproveToBcException;
import com.example.mp.gw.bc.exception.FailResponseRejectionToBcException;
import com.example.mp.gw.bc.exception.FailResponseReportToBcException;
import com.example.mp.gw.bc.exception.FailResponseStatusRejectionToBcException;
import com.example.mp.gw.bc.exception.FailResponseToBcException;
import com.example.mp.gw.bc.exception.FailResponseWhitelistToBcException;
import com.example.mp.gw.bc.exception.NotExistAccessTokenToBcException;
import com.example.mp.gw.bc.service.BcMemberService;
import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.domain.RedisQueueDataWrapper;
import com.example.mp.gw.common.domain.Const.FORMATTER;
import com.example.mp.gw.common.domain.RedisStructure.H_BC_SESSION;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_AGREE_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_RETRY;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_SESSION;
import com.example.mp.gw.common.service.RedisService;
import com.example.mp.gw.member.domain.Agree;
import com.example.mp.gw.member.domain.Member;
import com.example.mp.gw.member.domain.Rejection;
import com.example.mp.gw.member.domain.Whitelist;
import com.example.mp.gw.ncas.service.NCasService;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

/**
 * @Class Name : BcMemberServiceImpl.java
 * @Description : BizCenter 회원 서비스 구현체
 * 
 * @author 조주현
 * @since 2021.03.27
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.03.27	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Slf4j
@Service("BcMemberService")
public class BcMemberServiceImpl implements BcMemberService
{
	@Autowired
	private NCasService nCasService;

	@Autowired
	private BcMemberService bcMemberService;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Agree>> redisSendAgreeInfoToBc;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<?>> redisRetryToBc;

	@Autowired
	private RedisService<String,BcAccessTokenResponse> redisBcToken;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	@Qualifier("bizCenterHeaders")
	private HttpHeaders bizCenterHeaders;

	@Value("${spring.bc.hostname}")
	private String HOSTNAME;

	@Value("${spring.bc.url.agree-reg}")
	private String AGREE_REG_URL;

	@Value("${spring.bc.url.blacklist-get}")
	private String BLACKLIST_GET_URL;

	@Value("${spring.bc.url.blacklist-reg}")
	private String BLACKLIST_REG_URL;

	@Value("${spring.bc.url.whitelist-reg}")
	private String WHITELIST_REG_URL;


	/**
	 * Biz-center 수신동의(등록/해제) 요청 전송 스케줄러 (G/W -> 비즈센터)
	 * @param Integer
	 * @return void  
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void popAndSendAgree(Integer BIZ_AGREE_SEND_CNT)
	{
		RedisQueueDataWrapper<Agree>   request  = null;
		List<RedisQueueDataWrapper<?>> requests = new ArrayList<RedisQueueDataWrapper<?>>();	// 재처리용

		List<Approve>   reports        = new ArrayList<Approve>();
		ApprovesRequest reportsRequest = null;

		try
		{
			// 1. 큐에서 수신동의(등록/해제) 요청 데이터 읽기
			for (int i = 0; i < BIZ_AGREE_SEND_CNT; i++)
			{
				request = redisSendAgreeInfoToBc.rpop(Q_BC_AGREE_TO_SEND.STRUCTURE_NAME);

				if (null == request)
					break;

				Agree result = (Agree) request.getData();

				if (null == result)
					break;

				log.info("{ ■ BC 수신동의 정보 전송 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, result.getMessageId(), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(result, Agree.class));

				requests.add(request);

				reports.add(
							Approve.builder()
											 .apct_acct_cls(result.getGubun())
											 .phone(result.getPhone())
											 .ci(result.getCi())
											 .service_cd(result.getSvcOrgCd())
											 .apct_dt(result.getAgreeDtm())
											 .message_id(result.getMessageId())
								   .build()
						   );
			}

			// 2. Biz-center 수신동의 정보 전송
			if (reports.size() > 0)
			{
				reportsRequest = ApprovesRequest.builder()
														  .mbl_bzowr_dvcd(Const.LGUPLUS.LG_UPLUS_TYPE)
														  .reqs(reports)
												.build();

				bcMemberService.transferApproves(reportsRequest);
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
			log.error("{ ■ BC 수신동의 정보 전송 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(requests, List.class), e.getClass().getName(), e.getMessage());

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
	 * 수신동의(등록/해제) 정보 전송 (G/W->비즈센터)
	 * @param ApprovesRequest 
	 * @return void
	 * @throws FailRequestApproveToBcException
	 * @throws FailResponseApproveToBcException
	 */
	@Override
	public void transferApproves(ApprovesRequest approvesRequest) throws FailRequestApproveToBcException, FailResponseApproveToBcException
	{
		ResponseEntity<ResponseResult> response = null;

		try
		{
			BcAccessTokenResponse accessToken = redisBcToken.hmget(H_BC_SESSION.STRUCTURE_NAME, H_BC_SESSION.STRUCTURE_NAME);

			if (null == accessToken)
				throw new NotExistAccessTokenToBcException();

			bizCenterHeaders.setBearerAuth(accessToken.getAccess_token());

			HttpEntity<ApprovesRequest> request = new HttpEntity<>(approvesRequest, bizCenterHeaders);

			log.info("{ ■ BC ■ <수신동의 등록/해제 요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + AGREE_REG_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), ApprovesRequest.class)
			);

			response = restTemplate.exchange(HOSTNAME + AGREE_REG_URL, HttpMethod.POST, request, ResponseResult.class);

			log.info("{ ■ BC ■ <수신동의 등록/해제 응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"responseResult\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), ResponseResult.class)
			);

			if (true == "01".equals(response.getBody().getResult_cd()))
				throw new FailResponseToBcException();
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ BC ■ <수신동의 등록/해제 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"statusCode\": \"{}\",\"errorName\": \"{}\", \"message\": \"{}\", \"errors\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestApproveToBcException(e);
		}
		catch (NotExistAccessTokenToBcException e)
		{
			log.error("{ ■ BC ■ <수신동의 등록/해제 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisBcToken.lpush(
							   Q_BC_SESSION.STRUCTURE_NAME
							 , BcAccessTokenResponse.builder()
							 		.error_location(new Object() {}.getClass().getEnclosingMethod().getName())
							 		.error_description(e.getMessage())
							 		.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
							   .build()
							  );

			throw new FailRequestApproveToBcException(e);
		}
		catch (FailRequestToBcException e)
		{
			log.error("{ ■ BC ■ <수신동의 등록/해제 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestApproveToBcException(e);
		}
		catch (FailResponseToBcException e)
		{
			log.error("{ ■ BC ■ <수신동의 등록/해제 - FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailResponseApproveToBcException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ BC ■ <수신동의 등록/해제 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * 요청 법인에 대한 수신거부 신청 요청 (G/W->비즈센터)
	 * @param Rejection 
	 * @return void
	 * @throws FailRequestRejectionToBcException
	 * @throws FailResponseRejectionToBcException
	 */
	@Override
	public void transferRejections(Rejection rejectionRequest) throws FailRequestRejectionToBcException, FailResponseRejectionToBcException
	{
		ResponseEntity<ResponseResult> response = null;

		try
		{
			RegisterRejectionsRequest requestRejectionsRequest = RegisterRejectionsRequest.builder().mbl_bzowr_dvcd(Const.LGUPLUS.LG_UPLUS_TYPE)
																 .reqs(
																	   rejectionRequest.getRejections().stream().map(r->
										 									RegisterRejection.builder()
														 						.service_cd(r)
														 						.ci(rejectionRequest.getCi())
														 						.apct_acct_cls("1")
														 						.apct_tm(rejectionRequest.getApctDtm())
														 					.build()
																	   ).collect(Collectors.toList())
																	  )
																 .build();

			BcAccessTokenResponse accessToken = redisBcToken.hmget(H_BC_SESSION.STRUCTURE_NAME, H_BC_SESSION.STRUCTURE_NAME);

			if (null == accessToken)
				throw new NotExistAccessTokenToBcException();

			bizCenterHeaders.setBearerAuth(accessToken.getAccess_token());

			HttpEntity<RegisterRejectionsRequest> request = new HttpEntity<>(requestRejectionsRequest, bizCenterHeaders);

			log.info("{ ■ BC ■ <(요청 법인)수신거부 신청 요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + BLACKLIST_REG_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), RegisterRejectionsRequest.class)
					);

			response = restTemplate.exchange(HOSTNAME + BLACKLIST_REG_URL, HttpMethod.POST, request, ResponseResult.class);

			log.info("{ ■ BC ■ <(요청 법인)수신거부 신청 응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"responseResult\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), ResponseResult.class)
					);

			if (true == "01".equals(response.getBody().getResult_cd()))
				throw new FailResponseToBcException();
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ BC ■ <(요청 법인)수신거부 신청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"statusCode\": \"{}\",\"errorName\": \"{}\", \"message\": \"{}\", \"errors\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestRejectionToBcException(e);
		}
		catch (NotExistAccessTokenToBcException e)
		{
			log.error("{ ■ BC ■ <(요청 법인)수신거부 신청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisBcToken.lpush(
							   Q_BC_SESSION.STRUCTURE_NAME
							 , BcAccessTokenResponse.builder()
							 		.error_location(new Object() {}.getClass().getEnclosingMethod().getName())
							 		.error_description(e.getMessage())
							 		.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
							   .build()
							  );

			throw new FailRequestRejectionToBcException(e);
		}
		catch (FailRequestToBcException e)
		{
			log.error("{ ■ BC ■ <(요청 법인)수신거부 신청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestRejectionToBcException(e);
		}
		catch (FailResponseToBcException e)
		{
			log.error("{ ■ BC ■ <(요청 법인)수신거부 신청 - FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailResponseRejectionToBcException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ BC ■ <(요청 법인)수신거부 신청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}	

	/**
	 * 모든 법인에 대한 수신거부 신청 요청 (G/W->비즈센터)
	 * @param Member 
	 * @return void
	 * @throws FailRequestRejectionToBcException
	 * @throws FailResponseRejectionToBcException
	 */
	@Override
	public void transferAllRejections(Member rejectionRequest) throws FailRequestRejectionToBcException, FailResponseRejectionToBcException
	{
		ResponseEntity<ResponseResult> response = null;

		try
		{
			String ci = nCasService.toCIFrom(rejectionRequest.getPhone());

			RegisterRejectionsRequest requestRejectionsRequest = RegisterRejectionsRequest.builder()
																	 .mbl_bzowr_dvcd(Const.LGUPLUS.LG_UPLUS_TYPE)
																	 .reqs(
																		   Arrays.asList(RegisterRejection.builder()
																						 	.service_cd("ALL")
																						 	.ci(ci)
																				 			.apct_acct_cls("1") // 0:해지, 1:신청
																			 			 .build()
																			 			)
																		  )
																 .build();

			BcAccessTokenResponse accessToken = redisBcToken.hmget(H_BC_SESSION.STRUCTURE_NAME, H_BC_SESSION.STRUCTURE_NAME);

			if (null == accessToken)
				throw new NotExistAccessTokenToBcException();

			bizCenterHeaders.setBearerAuth(accessToken.getAccess_token());

			HttpEntity<RegisterRejectionsRequest> request = new HttpEntity<>(requestRejectionsRequest, bizCenterHeaders);

			log.info("{ ■ BC ■ <(모든 법인)수신거부 신청 요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + BLACKLIST_REG_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), RegisterRejectionsRequest.class)
					);

			response = restTemplate.exchange(HOSTNAME + BLACKLIST_REG_URL, HttpMethod.POST, request, ResponseResult.class);

			log.info("{ ■ BC ■ <(모든 법인)수신거부 신청 응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"responseResult\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), ResponseResult.class)
					);
			
			if (true == "01".equals(response.getBody().getResult_cd()))
				throw new FailResponseToBcException();
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ BC ■ <(모든 법인)수신거부 신청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"statusCode\": \"{}\",\"errorName\": \"{}\", \"message\": \"{}\", \"errors\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestRejectionToBcException(e);
		}
		catch (NotExistAccessTokenToBcException e)
		{
			log.error("{ ■ BC ■ <(모든 법인)수신거부 신청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisBcToken.lpush(
							   Q_BC_SESSION.STRUCTURE_NAME
							 , BcAccessTokenResponse.builder()
							 		.error_location(new Object() {}.getClass().getEnclosingMethod().getName())
							 		.error_description(e.getMessage())
							 		.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
							   .build()
							  );

			throw new FailRequestRejectionToBcException(e);
		}
		catch (FailRequestToBcException e)
		{
			log.error("{ ■ BC ■ <(모든 법인)수신거부 신청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestRejectionToBcException(e);
		}
		catch (FailResponseToBcException e)
		{
			log.error("{ ■ BC ■ <(모든 법인)수신거부 신청 - FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailResponseRejectionToBcException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ BC ■ <(모든 법인)수신거부 신청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * 요청 법인에 대한 수신거부 해지 요청 (G/W->비즈센터)
	 * @param Rejection
	 * @return void 
	 * @throws FailRequestRejectionToBcException
	 * @throws FailResponseRejectionToBcException
	 */
	@Override
	public void transferCencelRejections(Rejection rejectionRequest) throws FailRequestRejectionToBcException, FailResponseRejectionToBcException
	{
		ResponseEntity<ResponseResult> response = null;

		try
		{
			RegisterRejectionsRequest requestRejectionsRequest = RegisterRejectionsRequest.builder()
																 .mbl_bzowr_dvcd(Const.LGUPLUS.LG_UPLUS_TYPE)
																 .reqs(
																	   rejectionRequest.getRejections().stream().map(r->
																		 	RegisterRejection.builder()
														 						.service_cd(r)
																				.ci(rejectionRequest.getCi())
																				.apct_acct_cls("0")
																				.apct_tm(rejectionRequest.getApctDtm())
														 					.build()
																	   ).collect(Collectors.toList())
																	  )
																 .build();

			BcAccessTokenResponse accessToken = redisBcToken.hmget(H_BC_SESSION.STRUCTURE_NAME, H_BC_SESSION.STRUCTURE_NAME);

			if (null == accessToken)
				throw new NotExistAccessTokenToBcException();

			bizCenterHeaders.setBearerAuth(accessToken.getAccess_token());

			HttpEntity<RegisterRejectionsRequest> request = new HttpEntity<>(requestRejectionsRequest, bizCenterHeaders);

			log.info("{ ■ BC ■ <(요청 법인)수신거부 해지 요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + BLACKLIST_REG_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), RegisterRejectionsRequest.class)
					);

			response = restTemplate.exchange(HOSTNAME + BLACKLIST_REG_URL, HttpMethod.POST, request, ResponseResult.class);
			
			log.info("{ ■ BC ■ <(요청 법인)수신거부 해지 응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"responseResult\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), ResponseResult.class)
					);
			
			if (true == "01".equals(response.getBody().getResult_cd()))
				throw new FailResponseToBcException();
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ BC ■ <(요청 법인)수신거부 해지 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"statusCode\": \"{}\",\"errorName\": \"{}\", \"message\": \"{}\", \"errors\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestRejectionToBcException(e);
		}
		catch (NotExistAccessTokenToBcException e)
		{
			log.error("{ ■ BC ■ <(요청 법인)수신거부 해지 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisBcToken.lpush(
							   Q_BC_SESSION.STRUCTURE_NAME
							 , BcAccessTokenResponse.builder()
							 		.error_location(new Object() {}.getClass().getEnclosingMethod().getName())
							 		.error_description(e.getMessage())
							 		.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
							   .build()
							  );

			throw new FailRequestRejectionToBcException(e);
		}
		catch (FailRequestToBcException e)
		{
			log.error("{ ■ BC ■ <(요청 법인)수신거부 해지 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestRejectionToBcException(e);
		}
		catch (FailResponseToBcException e)
		{
			log.error("{ ■ BC ■ <(요청 법인)수신거부 해지 - FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailResponseRejectionToBcException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ BC ■ <(요청 법인)수신거부 해지 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * 수신거부 조회 (G/W -> 비즈센터)
	 * @param StatusRejectionRequest 
	 * @return void
	 * @throws FailRequestStatusRejectionToBcException
	 * @throws FailResponseStatusRejectionToBcException
	 */
	@Override
	public List<StatusRejection> transferStatusRejections(StatusRejectionRequest rejectionRequest) throws FailRequestStatusRejectionToBcException, FailResponseStatusRejectionToBcException
	{
		ResponseEntity<ResponseResults<StatusRejection>> response = null;

		try
		{
			BcAccessTokenResponse accessToken = redisBcToken.hmget(H_BC_SESSION.STRUCTURE_NAME, H_BC_SESSION.STRUCTURE_NAME);

			if (null == accessToken)
				throw new NotExistAccessTokenToBcException();

			bizCenterHeaders.setBearerAuth(accessToken.getAccess_token());

			HttpEntity<StatusRejectionRequest> request = new HttpEntity<>(rejectionRequest, bizCenterHeaders);

			log.info("{ ■ BC ■ <수신거부 조회 요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + BLACKLIST_GET_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), StatusRejectionRequest.class)
			);

			response = restTemplate.exchange(HOSTNAME + BLACKLIST_GET_URL, HttpMethod.POST, request, new ParameterizedTypeReference<ResponseResults<StatusRejection>>(){});

			log.info("{ ■ BC ■ <수신거부 조회 응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"responseResult\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), ResponseResults.class)
			);

			if (true == "01".equals(response.getBody().getResult_cd()))
				throw new FailResponseToBcException();

			return response.getBody().getResults();
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ BC ■ <수신거부 조회 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"statusCode\": \"{}\",\"errorName\": \"{}\", \"message\": \"{}\", \"errors\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestStatusRejectionToBcException(e);
		}
		catch (NotExistAccessTokenToBcException e)
		{
			log.error("{ ■ BC ■ <수신거부 조회 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisBcToken.lpush(
							   Q_BC_SESSION.STRUCTURE_NAME
							 , BcAccessTokenResponse.builder()
							 		.error_location(new Object() {}.getClass().getEnclosingMethod().getName())
							 		.error_description(e.getMessage())
							 		.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
							   .build()
							  );

			throw new FailRequestStatusRejectionToBcException(e);
		}
		catch (FailRequestToBcException e)
		{
			log.error("{ ■ BC ■ <수신거부 조회 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestStatusRejectionToBcException(e);
		}
		catch (FailResponseToBcException e)
		{
			log.error("{ ■ BC ■ <수신거부 조회 - FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailResponseStatusRejectionToBcException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ BC ■ <수신거부 조회 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}
	
	/**
	 * Whitelist(신청/해지) 요청 (G/W -> 비즈센터)
	 * @param Whitelist 
	 * @return void
	 * @throws FailRequestWhitelistToBcException
	 * @throws FailResponseWhitelistToBcException
	 */
	@Override
	public void transferWhitelist(Whitelist whitelistRequest) throws FailRequestWhitelistToBcException, FailResponseWhitelistToBcException
	{
		ResponseEntity<ResponseResult> response = null;

		try
		{
			RegisterWhitelistsRequest requestWhitelistsRequest = RegisterWhitelistsRequest.builder()
																	.mbl_bzowr_dvcd(Const.LGUPLUS.LG_UPLUS_TYPE)
																	.reqs(
																		  whitelistRequest.getWhitelists().stream().map(r->
																		  	RegisterWhitelist.builder()
																		  		.service_cd(r)
																		  		.ci(whitelistRequest.getCi())
																		  		.mbl_bzowr_dvcd(whitelistRequest.getCarrier())
																		  		.apct_acct_cls(whitelistRequest.getApctAcctCls())
																		  		.mdn(whitelistRequest.getPhone())
																		  		.in_cls(whitelistRequest.getInCls())
																		  	.build()
																		  ).collect(Collectors.toList())
																		 )
																 .build();

			BcAccessTokenResponse accessToken = redisBcToken.hmget(H_BC_SESSION.STRUCTURE_NAME, H_BC_SESSION.STRUCTURE_NAME);

			if (null == accessToken)
				throw new NotExistAccessTokenToBcException();

			bizCenterHeaders.setBearerAuth(accessToken.getAccess_token());

			HttpEntity<RegisterWhitelistsRequest> request = new HttpEntity<>(requestWhitelistsRequest, bizCenterHeaders);

			log.info("{ ■ BC ■ <Whitelist 신청/해지 요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + WHITELIST_REG_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), RegisterWhitelistsRequest.class)
			);

			response = restTemplate.exchange(HOSTNAME + WHITELIST_REG_URL, HttpMethod.POST, request, ResponseResult.class);

			log.info("{ ■ BC ■ <Whitelist 신청/해지 응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"responseResult\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), ResponseResult.class)
			);

			if (true == "01".equals(response.getBody().getResult_cd()))
				throw new FailResponseToBcException();
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ BC ■ <Whitelist 신청/해지 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"statusCode\": \"{}\",\"errorName\": \"{}\", \"message\": \"{}\", \"errors\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestWhitelistToBcException(e);
		}
		catch (NotExistAccessTokenToBcException e)
		{
			log.error("{ ■ BC ■ <Whitelist 신청/해지 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisBcToken.lpush(
							   Q_BC_SESSION.STRUCTURE_NAME
							 , BcAccessTokenResponse.builder()
							 		.error_location(new Object() {}.getClass().getEnclosingMethod().getName())
							 		.error_description(e.getMessage())
							 		.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
							   .build()
							  );

			throw new FailRequestWhitelistToBcException(e);
		}
		catch (FailRequestToBcException e)
		{
			log.error("{ ■ BC ■ <Whitelist 신청/해지 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestWhitelistToBcException(e);
		}
		catch (FailResponseToBcException e)
		{
			log.error("{ ■ BC ■ <Whitelist 신청/해지 - FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailResponseWhitelistToBcException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ BC ■ <Whitelist 신청/해지 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}
}
