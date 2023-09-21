package com.example.mp.gw.token.controller;


import java.util.UUID;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.domain.Const.FORMATTER;
import com.example.mp.gw.common.service.MapValidationErrorService;
import com.example.mp.gw.common.utils.ResponseBuilder;
import com.example.mp.gw.common.utils.ResponseBuilder.API_RESPONSE_CODE;
import com.example.mp.gw.doc.domain.Document;
import com.example.mp.gw.doc.domain.DocumentRd;
import com.example.mp.gw.token.domain.GenerateReadTokenRequest;
import com.example.mp.gw.token.domain.VerifyReadTokenRequest;
import com.example.mp.gw.token.domain.VerifyReadTokenResponse;
import com.example.mp.gw.token.domain.VerifyReplaceTokenRequest;
import com.example.mp.gw.token.domain.VerifyTokenRequest;
import com.example.mp.gw.token.domain.VerifyTokenResponse;
import com.example.mp.gw.token.service.TokenService;
import com.google.gson.Gson;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @Class Name : TokenController.java
 * @Description : 토큰 관련 요청 컨트롤러 
 * 
 * @author 조주현
 * @since 2021.04.22
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.22	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Slf4j
@Api(value="열람 토큰",tags = "토큰")
@RestController
@RequiredArgsConstructor
public class TokenController
{
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger("com.uplus.mp.gw.token");

	@Autowired
	private MapValidationErrorService mapValidationErrorService;

	@Autowired
	private TokenService tokenService;


	/**
	 * 고지 열람 토큰 생성
	 * @param GenerateReadTokenRequest
	 * @param BindingResult
	 * @return ResponseEntity
	 */
	@PostMapping("/api/v1/token")
	@ApiOperation(value="열람토큰 생성")
	public ResponseEntity<?> generateReadToken(@ApiParam("열람토큰 생성 요청") @Valid @RequestBody GenerateReadTokenRequest tokenRequest, BindingResult result)
	{
		try
		{
			ResponseEntity<?> errorResponse = mapValidationErrorService.mapValidation(result);
	
			if (null != errorResponse)
				return errorResponse;

			MDC.put(Const.TRACING_ID, tokenRequest.getMessageId());

			log.debug("{ ■ WEB ■ <열람토큰 생성 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(tokenRequest, GenerateReadTokenRequest.class));

			return new ResponseEntity<String>(tokenService.generateReadToken(tokenRequest), HttpStatus.OK);
		}
		catch (Exception e)
		{
			log.error("{ ■ WEB ■ <열람토큰 생성 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			return ResponseBuilder.error(e);
		}
		finally
		{
			MDC.clear();
		}
	}

	/**
	 * 토큰 인증 확인(인증)
	 * @param VerifyTokenRequest
	 * @param BindingResult
	 * @return ResponseEntity
	 * @throws Exception 
	 */
	@PostMapping(value = "/api/v1/token/status")
	@ApiOperation(value="토큰인증확인 조회")
	public ResponseEntity<?> verifyToken(@ApiParam("토큰인증확인 조회 요청") @Valid @RequestBody VerifyTokenRequest tokenRequest, BindingResult result)
	{
		try
		{
			ResponseEntity<?> errorResponse = mapValidationErrorService.mapValidation(result);

			if (null != errorResponse)
				return errorResponse;

			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			log.debug("{ ■ WEB ■ <토큰인증확인 조회 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(tokenRequest, VerifyTokenRequest.class));

			DocumentRd documentRd = tokenService.verifyToken(tokenRequest);

			return ResponseBuilder.success(
										   VerifyTokenResponse.builder()
										   		.result_cd(API_RESPONSE_CODE.SUCCESS)
										   		.result_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
										   		.sndn_mgnt_seq(Integer.parseInt(documentRd.getMessageId().split("_")[0]))
										   		.sndn_seq_no(Integer.parseInt(documentRd.getMessageId().split("_")[1]))
										   .build()
										  );
		}
		catch (Exception e)
		{
			log.error("{ ■ WEB ■ <토큰인증확인 조회 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(tokenRequest, VerifyTokenRequest.class), e.getClass().getName(), e.getMessage());

			return ResponseBuilder.error(e);
		}
		finally
		{
			MDC.clear();
		}
	}
	
	/**
	 * 토큰 인증 열람 확인 결과(열람)
	 * @param VerifyReadTokenRequest
	 * @param BindingResult
	 * @return ResponseEntity
	 * @throws Exception 
	 */
	@PostMapping(value = "/api/v1/token/status/read")
	@ApiOperation(value="(토큰인증)열람확인결과 처리")
	public ResponseEntity<?> verifyReadToken(@ApiParam("(토큰인증)열람확인결과 처리 요청") @Valid @RequestBody VerifyReadTokenRequest tokenRequest, BindingResult result)
	{
		try
		{
			ResponseEntity<?> errorResponse = mapValidationErrorService.mapValidation(result);

			if (null != errorResponse)
				return errorResponse;

			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			log.debug("{ ■ WEB ■ <(토큰인증)열람확인결과 처리 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(tokenRequest, VerifyReadTokenRequest.class));

			Document document = tokenService.verifyReadToken(tokenRequest);

			return ResponseBuilder.success(
										   VerifyReadTokenResponse.builder()
												.result_cd(API_RESPONSE_CODE.SUCCESS)
			        	   						.result_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
			        	   						.sndn_mgnt_seq(Integer.parseInt(document.getMessageId().split("_")[0]))
			        	   						.sndn_seq_no(Integer.parseInt(document.getMessageId().split("_")[1]))
			        	   						.rcv_npost(document.getRsvCea())
			        	   						.rcv_plfm_id(document.getRsvPlfmId())
			        	   				   .build()
			        	   				  );
		}
		catch (Exception e)
		{
			log.error("{ ■ WEB ■ <(토큰인증)열람확인결과 처리 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID,MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(tokenRequest, VerifyReadTokenRequest.class), e.getClass().getName(), e.getMessage());

			return ResponseBuilder.error(e);
		}
		finally
		{
			MDC.clear();
		}
	}

	/**
	 * 토큰 인증 대체(대체)
	 * @param VerifyTokenReplaceRequest
	 * @param BindingResult
	 * @return ResponseEntity
	 * @throws Exception 
	 */
	@PostMapping(value = "/api/v1/token/replace")
	@ApiOperation(value="토큰인증 대체")
	public ResponseEntity<?> verifyReplaceToken(@ApiParam("토큰인증 대체 요청") @Valid @RequestBody VerifyReplaceTokenRequest tokenRequest, BindingResult result)
	{
		try
		{
			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			ResponseEntity<?> errorResponse = mapValidationErrorService.mapValidation(result);

			if (null != errorResponse)
				return errorResponse;

			MDC.put(Const.TRACING_ID, tokenRequest.getMessageId());

			log.debug("{ ■ WEB ■ <토큰인증 대체 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(tokenRequest, VerifyReplaceTokenRequest.class));

			tokenService.verifyReplaceToken(tokenRequest);

			return ResponseBuilder.success();
		}
		catch (Exception e)
		{
			log.error("{ ■ WEB ■ <토큰인증 대체 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(tokenRequest, VerifyReplaceTokenRequest.class), e.getClass().getName(), e.getMessage());

			return ResponseBuilder.error(e);
		}
		finally
		{
			MDC.clear();
		}
	}
}
