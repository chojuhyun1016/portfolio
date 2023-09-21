package com.example.mp.gw.member.controller;


import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.mp.gw.bc.service.BcMemberService;
import com.example.mp.gw.common.domain.ApiResponseResults;
import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.service.MapValidationErrorService;
import com.example.mp.gw.common.utils.ResponseBuilder;
import com.example.mp.gw.member.domain.AgreeRequest;
import com.example.mp.gw.member.domain.RegisterCorpMemberRequest;
import com.example.mp.gw.member.domain.RegisterCorpMemberRequestPart1;
import com.example.mp.gw.member.domain.RegisterPersonMemberRequest;
import com.example.mp.gw.member.domain.RejectionRequest;
import com.example.mp.gw.member.domain.StatusRejection;
import com.example.mp.gw.member.domain.StatusRejectionRequest;
import com.example.mp.gw.member.domain.WhitelistRequest;
import com.example.mp.gw.member.domain.WithdrawPersonMemberRequest;
import com.example.mp.gw.member.service.MemberService;
import com.google.gson.Gson;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Api(value="1. 회원",tags = "회원")
@Slf4j
@RequiredArgsConstructor
@RestController
public class MemberController
{
	@SuppressWarnings("unused")
	private static final Logger logger = LoggerFactory.getLogger("com.uplus.mp.gw.member");

	@Autowired
	private MapValidationErrorService MapValidationErrorService;

	@Autowired
	private MemberService memberService;

	@Autowired
	private BcMemberService bcMemberService;	


	@PostMapping("/api/v1/member/person")
	@ApiOperation(value="개인 회원 가입/수정 요청", notes = "요청한 데이터를 기준으로 개인 회원가입을 진행한다")
	public ResponseEntity<?> registerPersonMember(@ApiParam("회원 정보 요청") @Valid  @RequestBody RegisterPersonMemberRequest memberRequest, BindingResult result)
	{
		try
		{
			ResponseEntity<?> errorResponse = MapValidationErrorService.mapValidation(result);

			if (null != errorResponse)
				return errorResponse;

			MDC.put(Const.TRACING_ID, memberRequest.getPhone() + (StringUtils.hasText(memberRequest.getMessageId()) ? ":" + memberRequest.getMessageId() : ""));

			if (2 == memberRequest.getReq_dvcd())
			{
				log.info("{ ■ WEB ■ <회원(개인) 공인전자주소 소유자정보 수정 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(memberRequest, RegisterPersonMemberRequest.class));

				memberService.modify(memberRequest);
			}
			else
			{
				log.info("{ ■ WEB ■ <회원(개인) 가입 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(memberRequest, RegisterPersonMemberRequest.class));

				memberService.register(memberRequest);
			}
		}
		catch (Exception e)
		{
			if (2 == memberRequest.getReq_dvcd())
				log.error("{ ■ WEB ■ <회원(개인) 공인전자주소 소유자정보 수정 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(memberRequest, RegisterPersonMemberRequest.class), e.getClass().getName(), e.getMessage());
			else
				log.error("{ ■ WEB ■ <회원(개인) 가입 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(memberRequest, RegisterPersonMemberRequest.class), e.getClass().getName(), e.getMessage());

			return ResponseBuilder.error(e);
		}
		finally
		{
			MDC.clear();
		}

		return ResponseBuilder.success();
	}

	/**
	 * @param RegisterCorpMemberRequestPart1
	 * @param MultipartFile
	 * @param MultipartFile
	 * @return ResponseEntity
	 */
	@PostMapping(value="/api/v1/member/corp")
	@ApiOperation(value="법인 회원 가입/수정 요청", notes = "요청한 데이터를 기준으로 법인 회원 가입/수정을 진행한다 (from.비즈센터)")
	public ResponseEntity<?> registerCorpMember( @ApiParam("법인 회원 가입/수정 - part1") @Valid @RequestPart("part1") RegisterCorpMemberRequestPart1 part1
		                                       , @ApiParam("법인 회원 가입/수정 - part2") @Valid @RequestPart("part2") MultipartFile part2
		                                       , @ApiParam("법인 회원 가입/수정 - part3") @Valid @RequestPart("part3") MultipartFile part3
			                                   , BindingResult result
			                                   )
	{
		try
		{
			ResponseEntity<?> errorResponse = MapValidationErrorService.mapValidation(result);

			if (null != errorResponse)
				return errorResponse;

			MDC.put(Const.TRACING_ID, part1.getService_cd());

			if (2 == part1.getReq_dvcd())
			{
				if (log.isDebugEnabled())
					log.debug("{ ■ BC ■ <회원(법인) 공인전자주소 소유자정보 수정 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"part1\": \"{}\", \"part2\": \"{}\", \"part3\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(part1, RegisterCorpMemberRequestPart1.class), new Gson().toJson(part2, MultipartFile.class), new Gson().toJson(part3, MultipartFile.class));
				else
					log.info("{ ■ BC ■ <회원(법인) 공인전자주소 소유자정보 수정 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"part1\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(part1, RegisterCorpMemberRequestPart1.class));
			}
			else
			{
				if (log.isDebugEnabled())
					log.debug("{ ■ BC ■ <회원(법인) 가입 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"part1\": \"{}\", \"part2\": \"{}\", \"part3\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(part1, RegisterCorpMemberRequestPart1.class), new Gson().toJson(part2, MultipartFile.class), new Gson().toJson(part3, MultipartFile.class));
				else
					log.info("{ ■ BC ■ <회원(법인) 가입 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"part1\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(part1, RegisterCorpMemberRequestPart1.class));
			}

			RegisterCorpMemberRequest memberRequest = RegisterCorpMemberRequest.builder()
																						 .part1(part1)
																						 .part2(part2)
																						 .part3(part3)
																			   .build();

			if (2 == memberRequest.getPart1().getReq_dvcd())
				memberService.modify(memberRequest);
			else
				memberService.register(memberRequest);
		}
		catch (Exception e)
		{
			if (2 == part1.getReq_dvcd())
			{
				if (log.isDebugEnabled())
					log.error("{ ■ BC ■ <회원(법인) 공인전자주소 소유자정보 수정 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"part1\": \"{}\", \"part2\": \"{}\", \"part3\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(part1, RegisterCorpMemberRequestPart1.class), new Gson().toJson(part2, MultipartFile.class), new Gson().toJson(part3, MultipartFile.class), e.getClass().getName(), e.getMessage());
				else
					log.error("{ ■ BC ■ <회원(법인) 공인전자주소 소유자정보 수정 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"part1\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(part1, RegisterCorpMemberRequestPart1.class), e.getClass().getName(), e.getMessage());
			}
			else
			{
				if (log.isDebugEnabled())
					log.error("{ ■ BC ■ <회원(법인) 가입 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"part1\": \"{}\", \"part2\": \"{}\", \"part3\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(part1, RegisterCorpMemberRequestPart1.class), new Gson().toJson(part2, MultipartFile.class), new Gson().toJson(part3, MultipartFile.class), e.getClass().getName(), e.getMessage());
				else
					log.error("{ ■ BC ■ <회원(법인) 가입 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"part1\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(part1, RegisterCorpMemberRequestPart1.class), e.getClass().getName(), e.getMessage());
			}

			return ResponseBuilder.error(e);
		}
		finally
		{
			MDC.clear();
		}

		return ResponseBuilder.success();
	}

	@DeleteMapping("/api/v1/member/person")
	@ApiOperation(value="개인 회원 탈퇴 요청", notes = "요청한 데이터를 기준으로 개인회원탈퇴를 진행한다")
	public ResponseEntity<?> withdrawPersonMember(@ApiParam("회원 탈퇴 요청") @Valid @RequestBody WithdrawPersonMemberRequest memberRequest, BindingResult result)
	{
		try
		{
			ResponseEntity<?> errorResponse = MapValidationErrorService.mapValidation(result);

			if (null != errorResponse)
				return errorResponse;

			MDC.put(Const.TRACING_ID, memberRequest.getPhone());

			log.info("{ ■ WEB ■ <회원(개인) 탈퇴 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(memberRequest, WithdrawPersonMemberRequest.class));

			memberService.withdraw(memberRequest);
		}
		catch (Exception e)
		{
			log.error("{ ■ WEB ■ <회원(개인) 탈퇴 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(memberRequest, WithdrawPersonMemberRequest.class), e.getClass().getName(), e.getMessage());

			return ResponseBuilder.error(e);
		}
		finally
		{
			MDC.clear();
		}

		return ResponseBuilder.success();
	}

	@PostMapping("/api/v1/member/agree")
	@ApiOperation(value="수신동의 요청", notes = "수신동의 요청을 진행한다")
	public ResponseEntity<?> addAgree(@ApiParam("수신동의 요청") @Valid  @RequestBody AgreeRequest agreeRequest, BindingResult result)
	{
		try
		{
			ResponseEntity<?> errorResponse = MapValidationErrorService.mapValidation(result);

			if (null != errorResponse)
				return errorResponse;

			MDC.put(Const.TRACING_ID, agreeRequest.getPhone() + (StringUtils.hasText(agreeRequest.getMessageId()) ? ":" + agreeRequest.getMessageId() : ""));

			log.info("{ ■ WEB ■ <수신동의 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(agreeRequest, AgreeRequest.class));

			memberService.addAgree(agreeRequest);
		}
		catch (Exception e)
		{
			log.error("{ ■ WEB ■ <수신동의 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(agreeRequest, AgreeRequest.class), e.getClass().getName(), e.getMessage());

			return ResponseBuilder.error(e);
		}
		finally
		{
			MDC.clear();
		}

		return ResponseBuilder.success();
	}

	@DeleteMapping("/api/v1/member/agree")
	@ApiOperation(value="수신동의 해제 요청", notes = "수신동의 해제 요청을 진행한다")
	public ResponseEntity<?> cancelAgree(@ApiParam("수신동의 해제 요청") @Valid  @RequestBody AgreeRequest agreeRequest, BindingResult result)
	{
		try
		{
			ResponseEntity<?> errorResponse = MapValidationErrorService.mapValidation(result);

			if (null != errorResponse)
				return errorResponse;

			MDC.put(Const.TRACING_ID, agreeRequest.getPhone() + (StringUtils.hasText(agreeRequest.getMessageId()) ? ":" + agreeRequest.getMessageId() : ""));

			log.info("{ ■ WEB ■ <수신동의 해제 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(agreeRequest, AgreeRequest.class));

			memberService.cancelAgree(agreeRequest);
		}
		catch (Exception e)
		{
			log.error("{ ■ WEB ■ <수신동의 해제 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(agreeRequest, AgreeRequest.class), e.getClass().getName(), e.getMessage());

			return ResponseBuilder.error(e);
		}
		finally
		{
			MDC.clear();
		}

		return ResponseBuilder.success();
	}

	@PostMapping("/api/v1/member/rejections")
	@ApiOperation(value="수신거부 요청",notes = "요청한 데이터를 기준으로 수신거부를 진행한다")
	public ResponseEntity<?> addRejections(@ApiParam("수신거부 요청") @Valid @RequestBody RejectionRequest rejectionRequest, BindingResult result)
	{
		try
		{
			ResponseEntity<?> errorResponse = MapValidationErrorService.mapValidation(result);

			if (null != errorResponse)
				return errorResponse;

			MDC.put(Const.TRACING_ID, rejectionRequest.getPhone() + (StringUtils.hasText(rejectionRequest.getMessageId()) ? ":" + rejectionRequest.getMessageId() : ""));
			
			log.info("{ ■ WEB ■ <수신거부 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(rejectionRequest, RejectionRequest.class));

			memberService.addRejections(rejectionRequest);
		}
		catch (Exception e)
		{
			log.error("{ ■ WEB ■ <수신거부 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(rejectionRequest, RejectionRequest.class), e.getClass().getName(), e.getMessage());
			return ResponseBuilder.error(e);
		}
		finally
		{
			MDC.clear();
		}

		return ResponseBuilder.success();
	}
	
	@DeleteMapping("/api/v1/member/rejections")
	@ApiOperation(value="수신거부 취소 요청",notes = "요청한 데이터를 기준으로 수신거부 취소를 진행한다")
	public ResponseEntity<?> cancelRejections(@ApiParam("수신거부 요청") @Valid @RequestBody RejectionRequest rejectionRequest, BindingResult result)
	{
		try
		{
			ResponseEntity<?> errorResponse = MapValidationErrorService.mapValidation(result);

			if (null != errorResponse)
				return errorResponse;

			MDC.put(Const.TRACING_ID, rejectionRequest.getPhone() + (StringUtils.hasText(rejectionRequest.getMessageId()) ? ":" + rejectionRequest.getMessageId() : ""));

			log.info("{ ■ WEB ■ <수신거부 취소 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(rejectionRequest, RejectionRequest.class));

			memberService.cancelRejections(rejectionRequest);
		}
		catch (Exception e)
		{
			log.error("{ ■ WEB ■ <수신거부 취소 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(rejectionRequest, RejectionRequest.class), e.getClass().getName(), e.getMessage());

			return ResponseBuilder.error(e);
		}
		finally
		{
			MDC.clear();
		}

		return ResponseBuilder.success();
	}

	@PostMapping("/api/v1/member/statusRejections")
	@ApiOperation(value="수신거부 조회",notes = "요청한 데이터를 기준으로 수신거부조회를 진행한다")
	public ResponseEntity<?> statusRejections(@ApiParam("수신거부조회 요청") @Valid @RequestBody StatusRejectionRequest rejectionRequest, BindingResult result)
	{
		try
		{
			ResponseEntity<?> errorResponse = MapValidationErrorService.mapValidation(result);

			if (null != errorResponse)
				return errorResponse;

			MDC.put(Const.TRACING_ID, rejectionRequest.getCi());

			log.info("{ ■ WEB ■ <수신거부 조회 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(rejectionRequest, StatusRejectionRequest.class));

			ApiResponseResults<StatusRejection> rejectionStatusResponse = bcMemberService.statusRejections(rejectionRequest);

			return ResponseBuilder.success(rejectionStatusResponse);
		}
		catch (Exception e)
		{
			log.error("{ ■ WEB ■ <수신거부 조회 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(rejectionRequest, StatusRejectionRequest.class), e.getClass().getName(), e.getMessage());

			return ResponseBuilder.error(e);
		}
		finally
		{
			MDC.clear();
		}
	}

	@PostMapping("/api/v1/member/addWhitelist")
	@ApiOperation(value="화이트리스트 등록",notes = "요청한 데이터를 기준으로 화이트 리스트 등록을 진행한다")
	public ResponseEntity<?> addWhiteList(@ApiParam("화이트리스트등록 요청") @Valid @RequestBody WhitelistRequest whitelistRequest, BindingResult result)
	{
		try
		{
			ResponseEntity<?> errorResponse = MapValidationErrorService.mapValidation(result);

			if (null != errorResponse)
				return errorResponse;

			MDC.put(Const.TRACING_ID, whitelistRequest.getPhone() + (StringUtils.hasText(whitelistRequest.getMessageId()) ? ":" + whitelistRequest.getMessageId() : ""));

			log.info("{ ■ WEB ■ <화이트리스트 등록 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(whitelistRequest, WhitelistRequest.class));

			memberService.addWhiteList(whitelistRequest);
		}
		catch (Exception e)
		{
			log.error("{ ■ WEB ■ <화이트리스트 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(whitelistRequest, WhitelistRequest.class), e.getClass().getName(), e.getMessage());

			return ResponseBuilder.error(e);
		}
		finally
		{
			MDC.clear();
		}

		return ResponseBuilder.success();
	}	

	@DeleteMapping("/api/v1/member/addWhitelist")
	@ApiOperation(value="화이트리스트등록 취소",notes = "요청한 데이터를 기준으로 화이트 리스트 등록취소를 진행한다")
	public ResponseEntity<?> cancelWhiteList(@ApiParam("화이트리스트등록 취소 요청") @Valid @RequestBody WhitelistRequest whitelistRequest, BindingResult result)
	{
		try
		{
			ResponseEntity<?> errorResponse = MapValidationErrorService.mapValidation(result);

			if (null != errorResponse)
				return errorResponse;

			MDC.put(Const.TRACING_ID, whitelistRequest.getPhone() + (StringUtils.hasText(whitelistRequest.getMessageId()) ? ":" + whitelistRequest.getMessageId() : ""));

			log.info("{ ■ WEB ■ <화이트리스트 등록 취소 요청> \"{}\": \"{}\", \"type\": \"controller\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(whitelistRequest, WhitelistRequest.class));

			memberService.cancelWhiteList(whitelistRequest);
		}
		catch (Exception e)
		{
			log.error("{ ■ WEB ■ <화이트리스트 등록 취소 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\" \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(whitelistRequest, WhitelistRequest.class), e.getClass().getName(), e.getMessage());

			return ResponseBuilder.error(e);
		}
		finally
		{
			MDC.clear();
		}

		return ResponseBuilder.success();
	}	
}
