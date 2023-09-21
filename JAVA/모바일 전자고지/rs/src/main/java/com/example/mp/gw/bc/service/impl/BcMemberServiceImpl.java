package com.example.mp.gw.bc.service.impl;


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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.mp.gw.bc.domain.BcAccessTokenResponse;
import com.example.mp.gw.bc.exception.FailRequestStatusRejectionToBcException;
import com.example.mp.gw.bc.exception.FailRequestToBcException;
import com.example.mp.gw.bc.exception.FailResponseStatusRejectionToBcException;
import com.example.mp.gw.bc.exception.FailResponseToBcException;
import com.example.mp.gw.bc.exception.NotExistAccessTokenToBcException;
import com.example.mp.gw.bc.service.BcMemberService;
import com.example.mp.gw.common.domain.ApiResponseResults;
import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.domain.Const.FORMATTER;
import com.example.mp.gw.common.domain.RedisStructure.H_BC_SESSION;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_SESSION;
import com.example.mp.gw.common.service.RedisService;
import com.example.mp.gw.member.domain.StatusRejection;
import com.example.mp.gw.member.domain.StatusRejectionRequest;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

/**
 * @Class Name : BcMemberServiceImpl.java
 * @Description : BizCenter 회원 서비스 구현체
 * 
 * @author 조주현
 * @since 2021.12.31
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.12.31	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Slf4j
@Service("BcMemberService")
public class BcMemberServiceImpl implements BcMemberService
{
	@Autowired
	private RedisService<String, BcAccessTokenResponse> redisBcToken;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	@Qualifier("bizCenterHeaders")
	private HttpHeaders bizCenterHeaders;

	@Value("${spring.bc.hostname}")
	private String HOSTNAME;

	@Value("${spring.bc.url.blacklist-get}")
	private String BLACKLIST_GET_URL;


	/**
	 * 수신거부조회 (G/W -> 비즈센터)
	 * @param StatusRejectionRequest
	 * @return RejectionStatusResponse
	 */
	@Override
	public ApiResponseResults<StatusRejection> statusRejections(StatusRejectionRequest rejectionStatusRequest)
	{
		ResponseEntity<ApiResponseResults<StatusRejection>> response = null;

		try
		{
			BcAccessTokenResponse accessToken = redisBcToken.hmget(H_BC_SESSION.STRUCTURE_NAME, H_BC_SESSION.STRUCTURE_NAME);

			if (null == accessToken)
				throw new NotExistAccessTokenToBcException();

			bizCenterHeaders.setBearerAuth(accessToken.getAccess_token());

			HttpEntity<StatusRejectionRequest> request = new HttpEntity<>(rejectionStatusRequest, bizCenterHeaders);

			log.info("{ ■ BC ■ <수신거부 조회 요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + BLACKLIST_GET_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), StatusRejectionRequest.class)
					);

			response = restTemplate.exchange(HOSTNAME + BLACKLIST_GET_URL, HttpMethod.POST, request, new ParameterizedTypeReference<ApiResponseResults<StatusRejection>>(){});

			log.info("{ ■ BC ■ <수신거부 조회 응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"responseResult\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), ApiResponseResults.class)
					);

			if (true == "01".equals(response.getBody().getResult_cd()))
				throw new FailResponseToBcException();

			return response.getBody();
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
			log.error("{ ■ BC ■ <수신거부 조회 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailResponseStatusRejectionToBcException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ BC ■ <수신거부 조회 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}		
}
