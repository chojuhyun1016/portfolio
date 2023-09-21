package com.example.mp.gw.kisa.service.impl;


import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.domain.Const.FORMATTER;
import com.example.mp.gw.common.domain.RedisStructure.H_KISA_SESSION;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_SESSION;
import com.example.mp.gw.common.service.RedisService;
import com.example.mp.gw.common.utils.StringUtil;
import com.example.mp.gw.kisa.domain.KisaAccessTokenRequest;
import com.example.mp.gw.kisa.domain.KisaAccessTokenResponse;
import com.example.mp.gw.kisa.exception.FailResponseTokenToKisaException;
import com.example.mp.gw.kisa.service.KisaTokenService;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service("KisaTokenService")
public class KisaTokenServiceImpl implements KisaTokenService
{
	@Autowired
	private KisaTokenService kisaTokenService;

	@Autowired
	private RedisService<String, KisaAccessTokenResponse> redisKisaToken;

	@Value("${spring.kisa.hostname}")
	private String HOSTNAME;

	@Value("${spring.kisa.url.token-auth}")
	private String TOKEN_AUTH_URL;

	@Value("${spring.kisa.token.client_id}")
	private String CLIENT_ID;

	@Value("${spring.kisa.token.client_secret}")
	private String CLIENT_SECRET;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	@Qualifier(value = "kcesHeaders")
	private HttpHeaders kcesHeaders;


	/**
	 * KISA ACCESS TOKEN 발급처리(G/W -> KISA)
	 */
	@Override
	public void popAndIssueKisaToken()
	{
		KisaAccessTokenResponse hashToken           = null;
		KisaAccessTokenResponse queueToken          = null;

		KisaAccessTokenRequest  accessTokenRequest  = null;
		KisaAccessTokenResponse accessTokenResponse = null;

		try
		{
			while (null != (queueToken = redisKisaToken.rpop(Q_KISA_SESSION.STRUCTURE_NAME)))
			{
				log.debug("{ ■ KISA Access Token 발급 요청 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(queueToken, KisaAccessTokenResponse.class));

				try
				{
					hashToken = redisKisaToken.hmget(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME);

					accessTokenRequest = KisaAccessTokenRequest.builder()
																		 .grantType(1) 
																		 .clientId(CLIENT_ID)
																		 .clientSecret(CLIENT_SECRET)
																		 .refreshToken(hashToken.getRefreshToken())
															   .build();
				}
				catch (Exception e)
				{
					accessTokenRequest = KisaAccessTokenRequest.builder()
																		 .grantType(0) 
																		 .clientId(CLIENT_ID)
																		 .clientSecret(CLIENT_SECRET)
															   .build();
				}

				if (Long.parseLong(queueToken.getReg_dt()) > Long.parseLong(hashToken!=null?hashToken.getReg_dt():"0"))
				{
					if (null != hashToken)
						redisKisaToken.hmdel(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME);

					accessTokenResponse = kisaTokenService.issueKisaAccessToken(accessTokenRequest);

					accessTokenResponse.setReg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));

					redisKisaToken.hmset(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME, accessTokenResponse);
				}
			}
		}
		catch (Exception e)
		{
			redisKisaToken.lpush(
								 Q_KISA_SESSION.STRUCTURE_NAME
							   , KisaAccessTokenResponse.builder()
									.errLocation(new Object() {}.getClass().getEnclosingMethod().getName())
									.errMsg(e.getMessage())
									.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
								 .build()
								);

			throw e;
		}
	}

	/**
	 * KISA ACCESS TOKEN 발급처리(G/W -> KISA)
	 * @throws Exception
	 */
	@Override
	public void issueKisaToken()
	{
		KisaAccessTokenRequest  accessTokenRequest  = null;
		KisaAccessTokenResponse accessTokenResponse = null;

		try
		{
			KisaAccessTokenResponse hashToken = redisKisaToken.hmget(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME);

			if (null != hashToken)
				redisKisaToken.hmdel(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME);
			
			accessTokenRequest = KisaAccessTokenRequest.builder()
																 .grantType(1) 
																 .clientId(CLIENT_ID)
																 .clientSecret(CLIENT_SECRET)
																 .refreshToken(hashToken.getRefreshToken())
													   .build();
		}
		catch (Exception e)
		{
			accessTokenRequest = KisaAccessTokenRequest.builder()
																 .grantType(0) 
																 .clientId(CLIENT_ID)
																 .clientSecret(CLIENT_SECRET)
													   .build();
		}

		log.info("{ ■ KISA Access Token 발급 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(accessTokenRequest, KisaAccessTokenRequest.class));

		try
		{
			accessTokenResponse = kisaTokenService.issueKisaAccessToken(accessTokenRequest);

			accessTokenResponse.setReg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));

			redisKisaToken.hmset(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME, accessTokenResponse);
		}
		catch (Exception e)
		{
			redisKisaToken.lpush(
								 Q_KISA_SESSION.STRUCTURE_NAME
							   , KisaAccessTokenResponse.builder()
									.errLocation(new Object() {}.getClass().getEnclosingMethod().getName())
									.errMsg(e.getMessage())
									.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
								 .build()
								);
		}
	}

	/**
	 * KISA ACCESS TOKEN 발급처리(G/W -> KISA)
	 */
	@Override
	public KisaAccessTokenResponse issueKisaAccessToken(KisaAccessTokenRequest accessTokenRequest)
	{
		kcesHeaders.set("req-UUID", UUID.randomUUID().toString());
		kcesHeaders.set("req-date", StringUtil.yyyyMMddHHmmss(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis())));

		HttpEntity<KisaAccessTokenRequest>      request  = null;
		ResponseEntity<KisaAccessTokenResponse> response = null;

		try
		{
			request  = new HttpEntity<>(accessTokenRequest, kcesHeaders);

			log.info("{ ■ KISA ■ <KISA Access 토큰 요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + TOKEN_AUTH_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), KisaAccessTokenRequest.class)
					);

			response = restTemplate.exchange(HOSTNAME + TOKEN_AUTH_URL, HttpMethod.POST, request, KisaAccessTokenResponse.class);

			log.info("{ ■ KISA ■ <KISA Access 토큰 응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"responseResult\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), KisaAccessTokenResponse.class)
					);

			if (Const.KISA.TOKEN_YN.SUCCESS.val() != response.getBody().getResultCode())
				throw new FailResponseTokenToKisaException();

			return response.getBody();
		}
		catch (FailResponseTokenToKisaException e)
		{
			log.error("{ ■ KISA ■ <KISA Access 토큰 - FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"resultCode\": \"{}\", \"errCode\": \"{}\", \"errMsg\": \"{}\"}", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), request.getHeaders(), request.getBody(), e.getClass().getName(), e.getMessage(), response.getBody().getResultCode(), response.getBody().getErrCode(), response.getBody().getErrMsg());

			throw new RuntimeException(e);
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ KISA ■ <KISA Access 토큰 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\", \"errorName\": \"{}\", \"statusCode\": \"{}\", \"message\": \"{}\"}", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), request.getHeaders(), request.getBody(), e.getClass().getName(), response.getStatusCode(), e.getMessage());

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ KISA ■ <KISA Access 토큰 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\"}", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), request.getHeaders(), request.getBody(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}
}
