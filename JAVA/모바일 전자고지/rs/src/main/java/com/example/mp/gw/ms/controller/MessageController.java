package com.example.mp.gw.ms.controller;


import java.util.UUID;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.service.MapValidationErrorService;
import com.example.mp.gw.common.utils.ResponseBuilder;
import com.example.mp.gw.ms.domain.BeforehandRequest;
import com.example.mp.gw.ms.domain.MessageRequest;
import com.example.mp.gw.ms.domain.RemindMessageRequest;
import com.example.mp.gw.ms.domain.ReportRequest;
import com.example.mp.gw.ms.service.MessageService;
import com.google.gson.Gson;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Api(value="메지시 전송", tags = "메시지")
@RestController
@RequiredArgsConstructor
public class MessageController
{
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger("com.uplus.mp.gw.ms");

	@Autowired
	private MapValidationErrorService mapValidationErrorService;

	@Autowired
	private MessageService messageService;


	@PostMapping("/api/v1/rmd")
	@ApiOperation(value="리마인드문자 전송",notes = "리마인드문자 전송")
	public ResponseEntity<?> receiveRemaindMessageRequest(@ApiParam("리마인드문자 전송 요청") @Valid @RequestBody RemindMessageRequest messageRequest, BindingResult result)
	{
		try
		{
			ResponseEntity<?> errorResponse = mapValidationErrorService.mapValidation(result);

			if (null != errorResponse)
				return errorResponse;

			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (log.isDebugEnabled())
				log.debug("{ ■ BC ■ <리마인드문자 전송 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(messageRequest, RemindMessageRequest.class));
			else
				log.info("{ ■ BC ■ <리마인드문자 전송 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), messageRequest);

			messageService.sendRemindMessages(messageRequest);
		}
		catch (Exception e)
		{
			if (log.isDebugEnabled())
				log.error("{ ■ BC ■ <리마인드문자 전송 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(messageRequest, RemindMessageRequest.class), e.getClass().getName(), e.getMessage());
			else
				log.error("{ ■ BC ■ <리마인드문자 전송 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), messageRequest, e.getClass().getName(), e.getMessage());
			
			return ResponseBuilder.error(e);
		}
		finally
		{
			MDC.clear();
		}

		return ResponseBuilder.success(messageRequest.getMills());
	}

	@PostMapping("/api/v1/msg/bfh")
	@ApiOperation(value="사전문자 전송",notes = "사전문자 전송")
	public ResponseEntity<?> receiveBforehandMessageRequest(@ApiParam("사전문자 전송 요청") @Valid @RequestBody BeforehandRequest messageRequest, BindingResult result)
	{
		try
		{
			ResponseEntity<?> errorResponse = mapValidationErrorService.mapValidation(result);

			if (null != errorResponse)
				return errorResponse;

			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (log.isDebugEnabled())
				log.debug("{ ■ BC ■ <사전문자 전송 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(messageRequest, BeforehandRequest.class));
			else
				log.info("{ ■ BC ■ <사전문자 전송 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), messageRequest);

			messageService.sendBeforehandMessages(messageRequest);
		}
		catch (Exception e)
		{
			if (log.isDebugEnabled())
				log.error("{ ■ BC ■ <사전문자 전송 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(messageRequest, BeforehandRequest.class), e.getClass().getName(), e.getMessage());
			else
				log.error("{ ■ BC ■ <사전문자 전송 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), messageRequest, e.getClass().getName(), e.getMessage());

			return ResponseBuilder.error(e);
		}
		finally
		{
			MDC.clear();
		}

		return ResponseBuilder.success(messageRequest.getMills());
	}

	@PostMapping("/api/v1/msg")
	@ApiOperation(value="본문자 전송",notes = "본문자 전송")
	public ResponseEntity<?> receiveMainMessageRequest(@ApiParam("본문자 전송 요청") @Valid @RequestBody MessageRequest messageRequest, BindingResult result)
	{
		try
		{
			ResponseEntity<?> errorResponse = mapValidationErrorService.mapValidation(result);

			if (null != errorResponse)
				return errorResponse;

			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			if (log.isDebugEnabled())
				log.debug("{ ■ BC ■ <본문자 전송 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(messageRequest, MessageRequest.class));
			else
				log.info("{ ■ BC ■ <본문자 전송 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), messageRequest);

			messageService.sendMainMessages(messageRequest);
		}
		catch (Exception e)
		{
			if (log.isDebugEnabled())
				log.error("{ ■ BC ■ <본문자 전송 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(messageRequest, MessageRequest.class), e.getClass().getName(), e.getMessage());
			else
				log.error("{ ■ BC ■ <본문자 전송 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), messageRequest, e.getClass().getName(), e.getMessage());
			
			return ResponseBuilder.error(e);
		}
		finally
		{
			MDC.clear();
		}

		return ResponseBuilder.success(messageRequest.getMills());
	}

	@PostMapping("/api/v1/rpt")
	@ApiOperation(value="리포트 요청", notes = "리포트 요청")
	public ResponseEntity<?> receiveReportRequest(@ApiParam("리포트 재전송 요청") @Valid @RequestBody ReportRequest messageRequest, BindingResult result)
	{
		try
		{
			ResponseEntity<?> errorResponse = mapValidationErrorService.mapValidation(result);

			if (null != errorResponse)
				return errorResponse;

			MDC.put(Const.TRACING_ID, UUID.randomUUID().toString());

			log.info("{ ■ BC ■ <리포트 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(messageRequest, ReportRequest.class));

			messageService.sendReports(messageRequest);
		}
		catch (Exception e)
		{
			log.error("{ ■ BC ■ <리포트 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(messageRequest, ReportRequest.class), e.getClass().getName(), e.getMessage());
			
			return ResponseBuilder.error(e);
		}
		finally
		{
			MDC.clear();
		}

		return ResponseBuilder.success();
	}

	@PostMapping("/api/v1/msg/rcs/image/{attachId}")
	@ApiOperation(value="RCS 사전이미지 등록 요청",notes = "RCS 사전이미지 등록 요청")
	public ResponseEntity<?> receiveRegisterRcsImageRequest(@ApiParam("RCS 이미지") @Valid @NotBlank(message = "이미지는 필수값입니다") @RequestBody MultipartFile file, @PathVariable(required = true) String attachId)
	{
		try
		{
			MDC.put(Const.TRACING_ID, attachId);

			if (log.isDebugEnabled())
				log.debug("{ ■ BC ■ <사전이미지 등록 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"attachId\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), attachId, new Gson().toJson(file, MultipartFile.class));
			else
				log.info("{ ■ BC ■ <사전이미지 등록 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"attachId\": \"{}\", \"fileSize\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), attachId, file.getSize());

			messageService.registerRcsImage(attachId, file);
		}
		catch (Exception e)
		{
			if (log.isDebugEnabled())
				log.error("{ ■ BC ■ <사전이미지 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"attachId\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), attachId, new Gson().toJson(file, MultipartFile.class), e.getClass().getName(), e.getMessage());
			else
				log.error("{ ■ BC ■ <사전이미지 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"attachId\": \"{}\", \"fileSize\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), attachId, file.getSize(), e.getClass().getName(), e.getMessage());

			return ResponseBuilder.error(e);
		}
		finally
		{
			MDC.clear();
		}

		return ResponseBuilder.success();
	}
}
