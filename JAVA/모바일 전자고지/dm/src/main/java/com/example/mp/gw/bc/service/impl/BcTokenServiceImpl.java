package com.example.mp.gw.bc.service.impl;


import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.mp.gw.bc.domain.BcAccessTokenRequest;
import com.example.mp.gw.bc.domain.BcAccessTokenResponse;
import com.example.mp.gw.bc.exception.FailResponseToBcException;
import com.example.mp.gw.bc.service.BcTokenService;
import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.domain.Const.FORMATTER;
import com.example.mp.gw.common.domain.RedisStructure.H_BC_SESSION;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_SESSION;
import com.example.mp.gw.common.service.RedisService;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

/**
 * @Class Name : BcTokenServiceImpl.java
 * @Description : BizCenter 토큰 서비스 구현 객체 
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
@Service("BcTokenService")
public class BcTokenServiceImpl implements BcTokenService
{
	@Autowired
	private BcTokenService bcTokenService;

	@Autowired
	private RedisService<String, BcAccessTokenResponse> redisBcToken;

	@Autowired
	@Qualifier("bizCenterHeaders")
	private HttpHeaders bizCenterHeaders;

	@Autowired
	@Qualifier("bizCenterTokenHeaders")
	private HttpHeaders bizCenterTokenHeaders;

	@Value("${spring.bc.hostname}")
	private String HOSTNAME;

	@Value("${spring.bc.url.oauth-token}")
	private String TOKEN_AUTH_URL;

	@Value("${spring.bc.token.grant_type}")
	private String GRANT_TYPE;

	@Value("${spring.bc.token.client_id}")
	private String CLIENT_ID;

	@Value("${spring.bc.token.client_secret}")
	private String CLIENT_SECRET;

	@Value("${spring.bc.token.scope}")
	private String SCOPE;


	/**
	 * BC ACCESS TOKEN 발급 처리(G/W -> 비즈센터)
	 * @return void
	 */
	@Override
	public void issueBcToken()
	{
		BcAccessTokenRequest bizCenterAccessTokenRequest = BcAccessTokenRequest.builder()
																.grant_type(GRANT_TYPE)
																.client_id(CLIENT_ID)
																.client_secret(CLIENT_SECRET)
																.scope(SCOPE)
														   .build();

		BcAccessTokenResponse bizCenterAccessTokenResponse = null;
		BcAccessTokenResponse bizCenterHashToken           = null;

		log.info("{ ■ BC Access Token 발급 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), "issueBcToken", new Gson().toJson(bizCenterAccessTokenRequest, BcAccessTokenRequest.class));

		try
		{
			bizCenterHashToken = redisBcToken.hmget(H_BC_SESSION.STRUCTURE_NAME, H_BC_SESSION.STRUCTURE_NAME);

			if (bizCenterHashToken != null)
				redisBcToken.hmdel(H_BC_SESSION.STRUCTURE_NAME, H_BC_SESSION.STRUCTURE_NAME);

			bizCenterAccessTokenResponse = bcTokenService.issueBcAccessToken(bizCenterAccessTokenRequest);

			bizCenterAccessTokenResponse.setReg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));

			redisBcToken.hmset(H_BC_SESSION.STRUCTURE_NAME, H_BC_SESSION.STRUCTURE_NAME, bizCenterAccessTokenResponse);
		}
		catch (Exception e)
		{
			redisBcToken.lpush(
							   Q_BC_SESSION.STRUCTURE_NAME
							 , BcAccessTokenResponse.builder()
							 		.error_location(new Object() {}.getClass().getEnclosingMethod().getName())
							 		.error_description(e.getMessage())
							 		.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
							   .build()
							  );
		}
	}

	/**
	 * BC ACCESS TOKEN 발급 처리(G/W -> 비즈센터)
	 * @return void
	 */
	@Override
	public void popAndIssueBcToken()
	{
		BcAccessTokenRequest bizCenterAccessTokenRequest = BcAccessTokenRequest.builder()
					.grant_type(GRANT_TYPE)
					.client_id(CLIENT_ID)
					.client_secret(CLIENT_SECRET)
					.scope(SCOPE)
				.build();

		BcAccessTokenResponse hashToken    = null;
		BcAccessTokenResponse queueToken   = null;
		BcAccessTokenResponse reponseToken = null;

		try
		{
			while (null != (queueToken = redisBcToken.rpop(Q_BC_SESSION.STRUCTURE_NAME)))
			{
				log.info("{ ■ BC Access Token 발급 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), "popAndIssueBcToken", new Gson().toJson(queueToken, BcAccessTokenResponse.class));

				hashToken = redisBcToken.hmget(H_BC_SESSION.STRUCTURE_NAME, H_BC_SESSION.STRUCTURE_NAME);

				if (Long.parseLong(queueToken.getReg_dt()) > Long.parseLong(hashToken!=null?hashToken.getReg_dt():"0"))
				{
					redisBcToken.hmdel(H_BC_SESSION.STRUCTURE_NAME, H_BC_SESSION.STRUCTURE_NAME);

					reponseToken = bcTokenService.issueBcAccessToken(bizCenterAccessTokenRequest);

					reponseToken.setReg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));

					redisBcToken.hmset(H_BC_SESSION.STRUCTURE_NAME, H_BC_SESSION.STRUCTURE_NAME, reponseToken);
				}
			}
		}
		catch (Exception e)
		{
			redisBcToken.lpush(
							   Q_BC_SESSION.STRUCTURE_NAME
							 , BcAccessTokenResponse.builder()
							 		.error_location(new Object() {}.getClass().getEnclosingMethod().getName())
							 		.error_description(e.getMessage())
							 		.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
							   .build()
							  );

			throw e;
		}
	}

	/**
	 * BC Access Token 발급 요청 (G/W -> 비즈센터)
	 * @param member
	 * @return
	 */
	@Override
	public BcAccessTokenResponse issueBcAccessToken(BcAccessTokenRequest bcAccessTokenRequest)
	{
		HttpEntity<MultiValueMap<String, String>> request  = null;
		ResponseEntity<BcAccessTokenResponse>     response = null;

		try
		{
			MultiValueMap<String, String> param = new LinkedMultiValueMap<>();

			param.add("grant_type"   , bcAccessTokenRequest.getGrant_type());
			param.add("client_id"    , bcAccessTokenRequest.getClient_id());
			param.add("client_secret", bcAccessTokenRequest.getClient_secret());
			param.add("scope"        , bcAccessTokenRequest.getScope());

			request = new HttpEntity<>(param, bizCenterTokenHeaders);

			RestTemplate restTemplate = new RestTemplate();
			restTemplate.getMessageConverters().add(0, new FormHttpMessageConverter());

			log.info("{ ■ BC ■ <BC Access Token 요청> \"{}\": \"{}\", \"type\": \"url\": \"{}\", \"request\": \"Header\": \"{}\", \"Body\": \"{}\" }"
				   , Const.TRACING_ID, MDC.get(Const.TRACING_ID), HOSTNAME + TOKEN_AUTH_URL
				   , log.isDebugEnabled() ? request.getHeaders() : ""
				   , request.getBody()
				    );

			response = restTemplate.exchange(HOSTNAME + TOKEN_AUTH_URL, HttpMethod.POST, request, BcAccessTokenResponse.class);

			log.info("{ ■ BC ■ <BC Access Token 응답> \"{}\": \"{}\", \"type\": \"response\", \"statusCode\": \"{}\", \"responseResult\": \"{}\" }"
				   , Const.TRACING_ID, MDC.get(Const.TRACING_ID)
				   , response.getStatusCode()
				   , response.getBody()
				    );

			if (true == StringUtils.hasText(response.getBody().getError()))
				throw new FailResponseToBcException();

			return response.getBody();
		}
		catch (FailResponseToBcException e)
		{
			log.error("{ ■ BC ■ <BC Access Token 요청 - FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"error\": \"{}\", \"error_desctiption\": \"{}\"}", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), request.getHeaders(), request.getBody(),e.getClass().getName(), e.getMessage(), response.getBody().getError(), response.getBody().getError_description());

			throw new RuntimeException(e);
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ BC ■ <BC Access Token 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\", \"errorName\": \"{}\", \"statusCode\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), request.getHeaders(), request.getBody(), e.getClass().getName(), response.getStatusCode(), e.getMessage());

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ BC ■ <BC Access Token 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), request.getHeaders(), request.getBody(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}
}
