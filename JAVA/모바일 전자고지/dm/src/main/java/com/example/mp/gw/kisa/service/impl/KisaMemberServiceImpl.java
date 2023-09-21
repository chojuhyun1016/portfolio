package com.example.mp.gw.kisa.service.impl;


import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.crypto.NoSuchPaddingException;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.domain.FileNameAwareByteArrayResource;
import com.example.mp.gw.common.domain.RedisQueueDataWrapper;
import com.example.mp.gw.common.domain.Const.FORMATTER;
import com.example.mp.gw.common.domain.Const.MEMBER;
import com.example.mp.gw.common.domain.RedisStructure.H_KISA_SESSION;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_EADDR_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_RETRY;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_SESSION;
import com.example.mp.gw.common.exception.KeepRunningToWorkerException;
import com.example.mp.gw.common.exception.StopRunningToWorkerException;
import com.example.mp.gw.common.service.RedisService;
import com.example.mp.gw.common.utils.StringUtil;
import com.example.mp.gw.common.utils.enc.Encryptor;
import com.example.mp.gw.kisa.domain.KisaAccessTokenResponse;
import com.example.mp.gw.kisa.domain.ModifyEaddrRequest;
import com.example.mp.gw.kisa.domain.ModifyEaddrResponse;
import com.example.mp.gw.kisa.domain.RegisterEaddrRequest;
import com.example.mp.gw.kisa.domain.RegisterEaddrResponse;
import com.example.mp.gw.kisa.domain.SearchEaddrOwnerInformationRequest;
import com.example.mp.gw.kisa.domain.SearchEaddrOwnerInformationResponse;
import com.example.mp.gw.kisa.domain.SearchEaddrRequest;
import com.example.mp.gw.kisa.domain.SearchEaddrResponse;
import com.example.mp.gw.kisa.domain.SearchEaddrWithdrawHistRequest;
import com.example.mp.gw.kisa.domain.SearchEaddrWithdrawHistResponse;
import com.example.mp.gw.kisa.domain.WithdrawEaddrRequest;
import com.example.mp.gw.kisa.domain.WithdrawEaddrResponse;
import com.example.mp.gw.kisa.exception.FailReponseRegisterToKisaException;
import com.example.mp.gw.kisa.exception.FailReponseWithdrawToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestAccessTokenToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestEaddrOwnerInformationToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestEaddrSearchWithdrawHistoryToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestModifyToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestRegisterToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestSearchToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestWithdrawToKisaException;
import com.example.mp.gw.kisa.exception.FailResponseEaddrOwnerInformationToKisaException;
import com.example.mp.gw.kisa.exception.FailResponseEaddrSearchWithdrawHistoryToKisaException;
import com.example.mp.gw.kisa.exception.FailResponseModifyToKisaException;
import com.example.mp.gw.kisa.exception.FailResponseSearchToKisaException;
import com.example.mp.gw.kisa.exception.FailResponseToKisaException;
import com.example.mp.gw.kisa.exception.NotExistEaddrReqException;
import com.example.mp.gw.kisa.exception.NotExistTokenToKisaException;
import com.example.mp.gw.kisa.exception.UnknownEaddrReqTypeException;
import com.example.mp.gw.kisa.service.KisaMemberService;
import com.example.mp.gw.member.domain.MemberEaddr;
import com.example.mp.gw.member.mappers.altibase.MemberMapper;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service("KisaMemberService")
public class KisaMemberServiceImpl implements KisaMemberService
{
	@Autowired
	private KisaMemberService kisaMemberService;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<MemberEaddr>> redisSendEaddrToKisa;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<?>> redisRetryEaddrToKisa;

	@Autowired
	private RedisService<String, KisaAccessTokenResponse> redisKisaToken;

	@Autowired
	private MemberMapper memberMapper;

	@Value("${spring.kisa.hostname}")
	private String HOSTNAME;

	@Value("${spring.kisa.url.eaddr-reg}")
	private String EADDR_REG_URL;

	@Value("${spring.kisa.url.eaddr-sch}")
	private String EADDR_SCH_URL;

	@Value("${spring.kisa.url.eaddr-sch-del-hist}")
	private String EADDR_SCH_DEL_HIST_URL;

	@Value("${spring.kisa.url.eaddr-sch-owner-info}")
	private String EADDR_SCH_OWNER_INFO_URL;

	@Value("${spring.kisa.url.eaddr-mod-owner-info-ind}")
	private String EADDR_MOD_IND_URL;

	@Value("${spring.kisa.url.eaddr-mod-owner-info-corp}")
	private String EADDR_MOD_CORP_URL;

	@Value("${spring.kisa.url.eaddr-del}")
	private String EADDR_DEL_URL;

	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	@Qualifier(value = "kcesHeaders")
	private HttpHeaders kcesHeaders;

	@Autowired
	@Qualifier(value = "kcesMultipartHeaders")
	private HttpHeaders kcesMutlipartHeaders;


	/**
	 * KISA 공인전자주소(등록/탈퇴/수정) 이력 전송
	 * @return void
	 * @throws KeepRunningToWorkerException
	 * @throws StopRunningToWorkerException
	 */
	@Override
	public void popAndSendEaddrToKisa() throws KeepRunningToWorkerException, StopRunningToWorkerException
	{
		RedisQueueDataWrapper<MemberEaddr> request = null;
		MemberEaddr                        member  = null;
		List<MemberEaddr>                  reqs    = null;

		try
		{
			// 1. 큐에서 KISA 공인전자주소 요청 읽기
			request = redisSendEaddrToKisa.rpop(Q_KISA_EADDR_TO_SEND.STRUCTURE_NAME);

			if (null == request)
				return;

			member = (MemberEaddr) request.getData();

			if (null == member)
				return;

			// 2. 회원 유형("0":일반, "1":법인)에 따른 Trace 설정
			if (true == MEMBER.TYPE.PERSON.val().equals(member.getType()))
				MDC.put(Const.TRACING_ID, member.getPhone() + (StringUtils.hasText(member.getMessageId()) ? ":" + member.getMessageId() : ""));
			else
				MDC.put(Const.TRACING_ID, member.getSvcOrgCd());

			log.info("{ ■ KISA 공인전자주소(등록/탈퇴/수정) 요청 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), member);

			// 3. 공인전자주소 요청 이력 조회
			reqs = kisaMemberService.findEaddrReq(member);

			if (null == reqs || (null != reqs && true == reqs.isEmpty()))
				throw new NotExistEaddrReqException();

			log.debug("{ ■ KISA 공인전자주소(등록/탈퇴/수정) 요청 이력 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(reqs, List.class));

			// 4. 공인전자주소 요청 이력 전송(KISA)
			for (MemberEaddr req : reqs)
				kisaMemberService.sendEaddrToKisa(req);
		}
		catch (NotExistEaddrReqException e)
		{
			// DB 이력 저장보다 Redis 전송이 빠른 경우, 다른 요청으로 이미 처리된 이력인 경우
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), member, e.getClass().getName(), e.getMessage());

			request.setSndDtm();
			redisRetryEaddrToKisa.lpush(Q_KISA_RETRY.STRUCTURE_NAME, request);

			throw new KeepRunningToWorkerException();
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), member, e.getClass().getName(), e.getMessage());

			request.setSndDtm();
			redisRetryEaddrToKisa.lpush(Q_KISA_RETRY.STRUCTURE_NAME, request);

			throw new StopRunningToWorkerException();
		}
	}

	/**
	 * KISA 공인전자주소(등록/탈퇴/수정) 요청 이력 조회
	 * @return void
	 */
	@Override
	@Transactional(readOnly = true)
	public List<MemberEaddr> findEaddrReq(MemberEaddr member)
	{
		try
		{
			// 공인전자주소(등록/탈퇴/수정) 요청 이력 테이블 조회
			return memberMapper.findMemberEaddrReq(member);
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), member, e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), member, e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * KISA 공인전자주소(등록/탈퇴/수정) 요청 전송
	 * @return void
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public void sendEaddrToKisa(MemberEaddr member)
	{
		try
		{
			// 1. 요청 이력 데이터 삭제
			memberMapper.removeMemberEaddrReq(member);

			// 2. 실패 요청 이력 데이터 삭제
			memberMapper.removeMemberEaddrReqFail(member);

			// 3. KISA 공인전자주소 요청 처리
			switch(member.getReqType())
			{
				// KISA 공인전자주소 등록
				case Const.MEMBER.REQ_TYPE.EADDR_REGISTER :

					@SuppressWarnings("unchecked")
					ResponseEntity<RegisterEaddrResponse> registerResponse = (ResponseEntity<RegisterEaddrResponse>) kisaMemberService.registerEaddr(member);

					log.debug("===============================resultCode===============================");
					log.debug("code:     " + registerResponse.getBody().getResultCode());
					log.debug("===============================resultCode===============================");

					break;

				// 공인전자주소 탈퇴
				case Const.MEMBER.REQ_TYPE.EADDR_WITHDRAW :

					// KISA 공인전자주소 탈퇴 요청
					@SuppressWarnings("unchecked")
					ResponseEntity<WithdrawEaddrResponse> withdrawResponse = (ResponseEntity<WithdrawEaddrResponse>) kisaMemberService.withdrawEaddr(member);

					log.debug("===============================resultCode===============================");
					log.debug("code:     " + withdrawResponse.getBody().getResultCode());
					log.debug("===============================resultCode===============================");

					break;

				// 공인전자주소 수정(소유자정보)
				case Const.MEMBER.REQ_TYPE.EADDR_MODIFY :

					// KISA 공인전자주소 변경(소유자정보) 요청
					@SuppressWarnings("unchecked")
					ResponseEntity<ModifyEaddrResponse> modifyResponse = (ResponseEntity<ModifyEaddrResponse>) kisaMemberService.modifyEaddr(member);

					log.debug("===============================resultCode===============================");
					log.debug("code:     " + modifyResponse.getBody().getResultCode());
					log.debug("===============================resultCode===============================");

					break;

				default :
					throw new UnknownEaddrReqTypeException();
			}
		}
		catch (FailRequestRegisterToKisaException | FailRequestWithdrawToKisaException | FailRequestModifyToKisaException e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), member, e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
		catch (FailReponseRegisterToKisaException | FailReponseWithdrawToKisaException | FailResponseModifyToKisaException e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), member, e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), member, e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), member, e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * 공인전자주소 등록
	 * @param member
	 * @return ResponseEntity
	 * @throws FailRequestRegisterToKisaException 
	 * @throws FailReponseRegisterToKisaException
	 * @throws Exception
	 */	
	@Override
	public ResponseEntity<?> registerEaddr(MemberEaddr member) throws FailRequestRegisterToKisaException, FailReponseRegisterToKisaException
	{
		HttpEntity<?>                         request  = null;
		ResponseEntity<RegisterEaddrResponse> response = null;

		RegisterEaddrRequest eaddrRequest = null;

		String STATEMENT   = " 등록";
		String MEMBER_TYPE = " ";
		
		try
		{
			final boolean IS_CORP = MEMBER.TYPE.CORP.val().equals(member.getType());	

			if (!StringUtils.hasText(member.getCreatedDtm()))
			{
				member.setCreatedDtm(FORMATTER.yyyyMMddHHmmssForKisa.val().format(System.currentTimeMillis()));
			}

			if (IS_CORP)
			{
				MEMBER_TYPE = "(법인) ";

				eaddrRequest = RegisterEaddrRequest.builder()	
															 .idn(StringUtils.replace(member.getBusiNum(), "-", ""))
															 .eaddr(member.getCea())
															 .name(member.getName())
															 .type(Integer.parseInt(member.getSvcOrgType()))
															 .regDate(StringUtil.yyyyMMddHHmmss(member.getCreatedDtm()))
												   .build();
			}
			else
			{
				MEMBER_TYPE = "(일반) ";

				eaddrRequest = RegisterEaddrRequest.builder()	
															 .idn(Encryptor.decryptCi(member.getCi()))
															 .eaddr(member.getCea())
															 .name(member.getName())
															 .type(0)
															 .regDate(StringUtil.yyyyMMddHHmmss(member.getCreatedDtm()))
													.build();
			}

			KisaAccessTokenResponse hashToken = redisKisaToken.hmget(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME);

			if (null == hashToken)
				throw new NotExistTokenToKisaException();

			if (member.getType().equals("1"))
			{
				kcesMutlipartHeaders.setBearerAuth(hashToken.getAccessToken());
			}
			else
			{
				kcesHeaders.setBearerAuth(hashToken.getAccessToken());
			}

			if (IS_CORP)
			{
				kcesMutlipartHeaders.set("req-UUID", UUID.randomUUID().toString());
				kcesMutlipartHeaders.set("req-date", StringUtil.yyyyMMddHHmmss(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis())));
				kcesMutlipartHeaders.set("Content-Disposition", "attachment");
				LinkedMultiValueMap<String, Object> multipartValue = new LinkedMultiValueMap<>();
				multipartValue.add("msg", eaddrRequest);

				multipartValue.add("file", new FileNameAwareByteArrayResource("reg-doc.jpg", member.getFile1().getFileData(), ""));
				multipartValue.add("file", new FileNameAwareByteArrayResource("biz-doc.jpg", member.getFile2().getFileData(), ""));

				request = new HttpEntity<LinkedMultiValueMap<String, Object>>(multipartValue, kcesMutlipartHeaders);

				log.info("{ ■ KISA ■ <공인전자주소" + STATEMENT + MEMBER_TYPE + "요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\" }"
						, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
						, HOSTNAME + EADDR_REG_URL
						, log.isDebugEnabled() ? request.getHeaders() : ""
						, new Gson().toJson(eaddrRequest, RegisterEaddrRequest.class)
						);
			}
			else
			{
				kcesHeaders.set("req-UUID", UUID.randomUUID().toString());
				kcesHeaders.set("req-date", StringUtil.yyyyMMddHHmmss(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis())));
				request = new HttpEntity<RegisterEaddrRequest>(eaddrRequest, kcesHeaders);

				log.info("{ ■ KISA ■ <공인전자주소" + STATEMENT + MEMBER_TYPE + "요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\" }"
						, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
						, HOSTNAME + EADDR_REG_URL
						, log.isDebugEnabled() ? request.getHeaders() : ""
						, new Gson().toJson(request.getBody(), RegisterEaddrRequest.class)
						);
			}

			response = restTemplate.exchange(HOSTNAME + EADDR_REG_URL, HttpMethod.POST, request, RegisterEaddrResponse.class);

			log.info("{ ■ KISA ■ <공인전자주소" + STATEMENT + MEMBER_TYPE + "응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"responseResult\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), RegisterEaddrResponse.class)
					);			

			if (response.getBody().getResultCode() == 0)
			{
				if (true == response.getBody().getErrCode().contains("ERR-01-103")
				 || true == response.getBody().getErrCode().contains("ERR-01-104")
				   )
				{
					throw new FailRequestAccessTokenToKisaException();
				}

				throw new FailResponseToKisaException();
			}

			return response;
		}
		catch (NotExistTokenToKisaException | FailRequestAccessTokenToKisaException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + MEMBER_TYPE + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisKisaToken.lpush(
								 Q_KISA_SESSION.STRUCTURE_NAME
							   , KisaAccessTokenResponse.builder()
							   		.errLocation(new Object() {}.getClass().getEnclosingMethod().getName())
							   		.errMsg(e.getMessage())
							   		.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
							   	 .build()
							   	);

			throw new FailRequestRegisterToKisaException(e);
		}
		catch (NoSuchPaddingException | NoSuchAlgorithmException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + MEMBER_TYPE + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestRegisterToKisaException(e);
		}
		catch (FailRequestToKisaException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + MEMBER_TYPE + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestRegisterToKisaException(e); 
		}
		catch (FailResponseToKisaException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + MEMBER_TYPE + "- FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"resultCode\": \"{}\" ,\"errCode\": \"{}\", \"errMsg\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), response.getBody().getResultCode(), response.getBody().getErrCode(), response.getBody().getErrMsg());

			throw new FailReponseRegisterToKisaException(e); 
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + MEMBER_TYPE + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"statusCode\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestRegisterToKisaException();
		}
		catch (Exception e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + MEMBER_TYPE + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * 공인전자주소 조회
	 * @param member
	 * @return ResponseEntity
	 * @throws FailRequestSearchToKisaException
	 * @throws FailReponseWithdrawToKisaException
	 */
	@Override
	public ResponseEntity<?> searchEaddr(MemberEaddr member) throws FailRequestSearchToKisaException, FailResponseSearchToKisaException
	{
		HttpEntity<SearchEaddrRequest>      request  = null;
		ResponseEntity<SearchEaddrResponse> response = null;

		String STATEMENT = " 조회 ";

		try
		{
			SearchEaddrRequest eaddrRequest =  SearchEaddrRequest.builder()
																		   .idn(member.getCi())
																		   .platformId(MEMBER.KISA.LGUPLUS.RLY_PLTFM_ID.val())
																 .build();

			KisaAccessTokenResponse hashToken = redisKisaToken.hmget(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME);

			if (null == hashToken)
				throw new NotExistTokenToKisaException();

			kcesHeaders.setBearerAuth(hashToken.getAccessToken());
			kcesHeaders.set("req-UUID", UUID.randomUUID().toString());
			kcesHeaders.set("req-date", StringUtil.yyyyMMddHHmmss(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis())));

			request = new HttpEntity<SearchEaddrRequest>(eaddrRequest, kcesHeaders);
	
			log.info("{ ■ KISA ■ <공인전자주소" + STATEMENT + "요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + EADDR_SCH_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), SearchEaddrRequest.class)
					);

			response = restTemplate.exchange(HOSTNAME + EADDR_SCH_URL + "?idn=" + member.getCi(), HttpMethod.GET, request, SearchEaddrResponse.class);

			log.info("{ ■ KISA ■ <공인전자주소" + STATEMENT + "응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"responseResult\": \"{}\" }"
					, Const.TRACING_ID,MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), SearchEaddrResponse.class)
					);

			if ( 0 == response.getBody().getResultCode())
			{
				if (true == response.getBody().getErrCode().contains("ERR-01-103")
				 || true == response.getBody().getErrCode().contains("ERR-01-104")
				   )
				{
					throw new FailRequestAccessTokenToKisaException();
				}

				throw new FailResponseToKisaException();
			}

			return response;
		}
		catch (NotExistTokenToKisaException | FailRequestAccessTokenToKisaException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisKisaToken.lpush(
								 Q_KISA_SESSION.STRUCTURE_NAME
							   , KisaAccessTokenResponse.builder()
							   		.errLocation(new Object() {}.getClass().getEnclosingMethod().getName())
							   		.errMsg(e.getMessage())
							   		.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
							   	 .build()
							   	);

			throw new FailRequestSearchToKisaException(e);
		}
		catch (FailRequestToKisaException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestSearchToKisaException(e);
		}
		catch (FailResponseToKisaException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"resultCode\": \"{}\" ,\"errCode\": \"{}\", \"errMsg\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), response.getBody().getResultCode(), response.getBody().getErrCode(), response.getBody().getErrMsg());

			throw new FailResponseSearchToKisaException(e); 
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"statusCode\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestSearchToKisaException();
		}
		catch (Exception e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\"}", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * 공인전자주소 탈퇴이력 조회
	 * @param member
	 * @return ResponseEntity
	 * @throws FailRequestEaddrSearchWithdrawHistoryToKisaException
	 * @throws FailResponseEaddrSearchWithdrawHistoryToKisaException
	 */
	@Override
	public ResponseEntity<?> searchEaddrWithdrawHist(MemberEaddr member) throws FailRequestEaddrSearchWithdrawHistoryToKisaException, FailResponseEaddrSearchWithdrawHistoryToKisaException
	{
		HttpEntity<SearchEaddrWithdrawHistRequest>      request  = null;
		ResponseEntity<SearchEaddrWithdrawHistResponse> response = null;

		String STATEMENT = " 탈퇴이력 조회 ";

		try
		{
			SearchEaddrWithdrawHistRequest eaddrRequest =  SearchEaddrWithdrawHistRequest.builder()
																.idn(member.getCi())
														   .build();

			KisaAccessTokenResponse hashToken = redisKisaToken.hmget(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME);

			if (null == hashToken)
				throw new NotExistTokenToKisaException();

			kcesHeaders.setBearerAuth(hashToken.getAccessToken());
			kcesHeaders.set("req-UUID", UUID.randomUUID().toString());
			kcesHeaders.set("req-date", StringUtil.yyyyMMddHHmmss(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis())));

			request = new HttpEntity<SearchEaddrWithdrawHistRequest>(eaddrRequest, kcesHeaders);
	
			log.info("{ ■ KISA ■ <공인전자주소" + STATEMENT + "요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + EADDR_SCH_DEL_HIST_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), SearchEaddrWithdrawHistRequest.class)
					);

			response = restTemplate.exchange(HOSTNAME + EADDR_SCH_DEL_HIST_URL + "?idn=" + member.getCi(), HttpMethod.GET, request, SearchEaddrWithdrawHistResponse.class);

			log.info("{ ■ KISA ■ <공인전자주소" + STATEMENT + "응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"responseResult\": \"{}\" }"
					, Const.TRACING_ID,MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), SearchEaddrWithdrawHistResponse.class)
					);

			if ( 0 == response.getBody().getResultCode())
			{
				if (true == response.getBody().getErrCode().contains("ERR-01-103")
				 || true == response.getBody().getErrCode().contains("ERR-01-104")
				   )
				{
					throw new FailRequestAccessTokenToKisaException();
				}

				throw new FailResponseToKisaException();
			}

			return response;
		}
		catch (NotExistTokenToKisaException | FailRequestAccessTokenToKisaException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisKisaToken.lpush(
								 Q_KISA_SESSION.STRUCTURE_NAME
							   , KisaAccessTokenResponse.builder()
							   		.errLocation(new Object() {}.getClass().getEnclosingMethod().getName())
							   		.errMsg(e.getMessage())
							   		.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
							   	 .build()
							   	);

			throw new FailRequestEaddrSearchWithdrawHistoryToKisaException(e);
		}
		catch (FailRequestToKisaException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestEaddrSearchWithdrawHistoryToKisaException(e);
		}
		catch (FailResponseToKisaException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"resultCode\": \"{}\" ,\"errCode\": \"{}\", \"errMsg\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), response.getBody().getResultCode(), response.getBody().getErrCode(), response.getBody().getErrMsg());

			throw new FailResponseEaddrSearchWithdrawHistoryToKisaException(e); 
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"statusCode\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestEaddrSearchWithdrawHistoryToKisaException();
		}
		catch (Exception e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\"}", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * 공인전자주소 소유자정보 조회
	 * @param member
	 * @return ResponseEntity
	 * @throws FailRequestEaddrOwnerInformationToKisaException
	 * @throws FailResponseEaddrOwnerInformationToKisaException
	 */
	@Override
	public ResponseEntity<?> searchEaddrOwnerInfo(MemberEaddr member) throws FailRequestEaddrOwnerInformationToKisaException, FailResponseEaddrOwnerInformationToKisaException
	{
		HttpEntity<SearchEaddrOwnerInformationRequest>      request  = null;
		ResponseEntity<SearchEaddrOwnerInformationResponse> response = null;

		String STATEMENT = " 소유자정보 조회 ";

		try
		{
			SearchEaddrOwnerInformationRequest eaddrRequest = SearchEaddrOwnerInformationRequest.builder()
																.eaddr(member.getCea())
															  .build();

			KisaAccessTokenResponse hashToken = redisKisaToken.hmget(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME);

			if (null == hashToken)
				throw new NotExistTokenToKisaException();

			kcesHeaders.setBearerAuth(hashToken.getAccessToken());
			kcesHeaders.set("req-UUID", UUID.randomUUID().toString());
			kcesHeaders.set("req-date", StringUtil.yyyyMMddHHmmss(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis())));

			request = new HttpEntity<SearchEaddrOwnerInformationRequest>(eaddrRequest, kcesHeaders);
	
			log.info("{ ■ KISA ■ <공인전자주소" + STATEMENT + "요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + EADDR_SCH_OWNER_INFO_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), SearchEaddrOwnerInformationRequest.class)
					);

			response = restTemplate.exchange(HOSTNAME + EADDR_SCH_OWNER_INFO_URL + "?eaddr=" + member.getCea(), HttpMethod.GET, request, SearchEaddrOwnerInformationResponse.class);

			log.info("{ ■ KISA ■ <공인전자주소" + STATEMENT + "응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"responseResult\": \"{}\" }"
					, Const.TRACING_ID,MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), SearchEaddrOwnerInformationResponse.class)
					);

			if ( 0 == response.getBody().getResultCode())
			{
				if (true == response.getBody().getErrCode().contains("ERR-01-103")
				 || true == response.getBody().getErrCode().contains("ERR-01-104")
				   )
				{
					throw new FailRequestAccessTokenToKisaException();
				}

				throw new FailResponseToKisaException();
			}

			return response;
		}
		catch (NotExistTokenToKisaException | FailRequestAccessTokenToKisaException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisKisaToken.lpush(
								 Q_KISA_SESSION.STRUCTURE_NAME
							   , KisaAccessTokenResponse.builder()
							   		.errLocation(new Object() {}.getClass().getEnclosingMethod().getName())
							   		.errMsg(e.getMessage())
							   		.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
							   	 .build()
							   	);

			throw new FailRequestEaddrOwnerInformationToKisaException(e);
		}
		catch (FailRequestToKisaException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestEaddrOwnerInformationToKisaException(e);
		}
		catch (FailResponseToKisaException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"resultCode\": \"{}\" ,\"errCode\": \"{}\", \"errMsg\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), response.getBody().getResultCode(), response.getBody().getErrCode(), response.getBody().getErrMsg());

			throw new FailResponseEaddrOwnerInformationToKisaException(e); 
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"statusCode\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestEaddrOwnerInformationToKisaException();
		}
		catch (Exception e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\"}", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * 공인전자주소 소유자정보 수정(개인, 법인)
	 * @param member
	 * @return ResponseEntity
	 * @throws FailRequestUpdateToKisaException
	 * @throws FailResponseUpdateToKisaException
	 */
	@Override
	public ResponseEntity<?> modifyEaddr(MemberEaddr member) throws FailRequestModifyToKisaException, FailResponseModifyToKisaException
	{
		HttpEntity<?>                       request  = null;
		ResponseEntity<ModifyEaddrResponse> response = null;

		String EADDR_MOD_URL = null;
		String STATEMENT     = null;

		ModifyEaddrRequest modifyRequest = null;

		try
		{
			final boolean IS_CORP = MEMBER.TYPE.CORP.val().equals(member.getType());	

			if (IS_CORP)
			{
				EADDR_MOD_URL = EADDR_MOD_CORP_URL;
				STATEMENT     = " 소유자정보 수정(법인) ";
			}
			else
			{
				EADDR_MOD_URL = EADDR_MOD_IND_URL;
				STATEMENT     = " 소유자정보 수정(개인) ";
			}

			if (false == StringUtils.hasText(member.getCreatedDtm()))
			{
				member.setCreatedDtm(FORMATTER.yyyyMMddHHmmssForKisa.val().format(System.currentTimeMillis()));
			}

			modifyRequest = ModifyEaddrRequest.builder()	
														.eaddr(member.getCea())
														.name(member.getName())
														.type(true == StringUtils.hasText(member.getSvcOrgType()) ? Integer.valueOf(member.getSvcOrgType()) : null)
														.updDate(StringUtil.yyyyMMddHHmmss(member.getCreatedDtm()))
											  .build();

			KisaAccessTokenResponse hashToken = redisKisaToken.hmget(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME);

			if (null == hashToken)
				throw new NotExistTokenToKisaException();

			if (IS_CORP)
			{
				if (StringUtils.hasText(member.getSvcOrgType()))
				{
					kcesHeaders.setBearerAuth(hashToken.getAccessToken());

					kcesHeaders.set("req-UUID", UUID.randomUUID().toString());
					kcesHeaders.set("req-date", StringUtil.yyyyMMddHHmmss(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis())));

					request = new HttpEntity<ModifyEaddrRequest>(modifyRequest, kcesHeaders);
				}
				else
				{
					kcesMutlipartHeaders.setBearerAuth(hashToken.getAccessToken());

					kcesMutlipartHeaders.set("req-UUID", UUID.randomUUID().toString());
					kcesMutlipartHeaders.set("req-date", StringUtil.yyyyMMddHHmmss(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis())));
					kcesMutlipartHeaders.set("Content-Disposition", "attachment");

					LinkedMultiValueMap<String, Object> multipartValue = new LinkedMultiValueMap<>();
					multipartValue.add("msg", modifyRequest);

					if (true == StringUtils.hasText(member.getName()))
					{
						multipartValue.add("file", new FileNameAwareByteArrayResource("reg-doc.jpg", member.getFile1().getFileData(), ""));
						multipartValue.add("file", new FileNameAwareByteArrayResource("biz-doc.jpg", member.getFile2().getFileData(), ""));
					}

					request = new HttpEntity<LinkedMultiValueMap<String, Object>>(multipartValue, kcesMutlipartHeaders);
				}

				log.info("{ ■ KISA ■ <공인전자주소" + STATEMENT + "요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\" }"
						, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
						, HOSTNAME + EADDR_MOD_URL
						, log.isDebugEnabled() ? request.getHeaders() : ""
						, new Gson().toJson(modifyRequest, ModifyEaddrRequest.class)
						);
			}
			else
			{
				kcesHeaders.setBearerAuth(hashToken.getAccessToken());

				kcesHeaders.set("req-UUID", UUID.randomUUID().toString());
				kcesHeaders.set("req-date", StringUtil.yyyyMMddHHmmss(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis())));

				request = new HttpEntity<ModifyEaddrRequest>(modifyRequest, kcesHeaders);

				log.info("{ ■ KISA ■ <공인전자주소" + STATEMENT + "요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\" }"
						, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
						, HOSTNAME + EADDR_MOD_URL
						, log.isDebugEnabled() ? request.getHeaders() : ""
						, new Gson().toJson(request.getBody(), ModifyEaddrRequest.class)
						);
			}

			if (IS_CORP)
				response = restTemplate.exchange(HOSTNAME + EADDR_MOD_URL, HttpMethod.POST, request, ModifyEaddrResponse.class);
			else
				response = restTemplate.exchange(HOSTNAME + EADDR_MOD_URL, HttpMethod.PATCH, request, ModifyEaddrResponse.class);

			log.info("{ ■ KISA ■ <공인전자주소" + STATEMENT + "응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"responseResult\": \"{}\" }"
					, Const.TRACING_ID,MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), ModifyEaddrResponse.class)
					);

			if(response.getBody().getResultCode() == 0)
			{
				if (true == response.getBody().getErrCode().contains("ERR-01-103")
				 || true == response.getBody().getErrCode().contains("ERR-01-104")
				   )
				{
					throw new FailRequestAccessTokenToKisaException();
				}

				throw new FailResponseToKisaException();
			}

			return response;
		}
		catch (NotExistTokenToKisaException | FailRequestAccessTokenToKisaException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisKisaToken.lpush(
								 Q_KISA_SESSION.STRUCTURE_NAME
							   , KisaAccessTokenResponse.builder()
							   		.errLocation(new Object() {}.getClass().getEnclosingMethod().getName())
							   		.errMsg(e.getMessage())
							   		.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
							   	 .build()
							   	);

			throw new FailRequestModifyToKisaException(e);
		}
		catch (FailRequestToKisaException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestModifyToKisaException(e);
		}
		catch (FailResponseToKisaException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"resultCode\": \"{}\" ,\"errCode\": \"{}\", \"errMsg\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), response.getBody().getResultCode(), response.getBody().getErrCode(), response.getBody().getErrMsg());

			throw new FailResponseModifyToKisaException(e); 
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"statusCode\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestModifyToKisaException();
		}
		catch (Exception e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * 공인전자주소 탈퇴
	 * @param member
	 * @return
	 * @throws Exception
	 */
	@Override
	public ResponseEntity<?> withdrawEaddr(MemberEaddr member) throws FailRequestWithdrawToKisaException, FailReponseWithdrawToKisaException
	{
		HttpEntity<WithdrawEaddrRequest>      request  = null;
		ResponseEntity<WithdrawEaddrResponse> response = null;

		String STATEMENT = " 탈퇴 ";

		try
		{
			if(!StringUtils.hasText(member.getCreatedDtm()))
			{
				member.setCreatedDtm(FORMATTER.yyyyMMddHHmmssForKisa.val().format(System.currentTimeMillis()));
			}

			WithdrawEaddrRequest eaddrRequest = WithdrawEaddrRequest.builder()	
																			  .eaddr(member.getCea())
																			  .delDate(StringUtil.yyyyMMddHHmmss(member.getCreatedDtm()))
																	.build();

			KisaAccessTokenResponse hashToken = redisKisaToken.hmget(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME);

			if (null == hashToken)
				throw new NotExistTokenToKisaException();

			kcesHeaders.setBearerAuth(hashToken.getAccessToken());

			kcesHeaders.set("req-UUID", UUID.randomUUID().toString());
			kcesHeaders.set("req-date", StringUtil.yyyyMMddHHmmss(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis())));

			request = new HttpEntity<>(eaddrRequest, kcesHeaders);

			log.info("{ ■ KISA ■ <공인전자주소" + STATEMENT + "요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"Header\": \"{}\", \"Body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + EADDR_DEL_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), WithdrawEaddrRequest.class)
					);

			response = restTemplate.exchange(HOSTNAME + EADDR_DEL_URL, HttpMethod.PATCH, request, WithdrawEaddrResponse.class);

			log.info("{ ■ KISA ■ <공인전자주소" + STATEMENT + "응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"responseResult\": \"{}\" }"
					, Const.TRACING_ID,MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), WithdrawEaddrResponse.class)
					);	

			if(response.getBody().getResultCode() == 0)
			{
				if (true == response.getBody().getErrCode().contains("ERR-01-103")
				 || true == response.getBody().getErrCode().contains("ERR-01-104")
				   )
				{
					throw new FailRequestAccessTokenToKisaException();
				}

				if (false == response.getBody().getErrCode().contains("ERR-01-304"))
					throw new FailResponseToKisaException();
			}

			return response;
		}
		catch (NotExistTokenToKisaException | FailRequestAccessTokenToKisaException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisKisaToken.lpush(
								 Q_KISA_SESSION.STRUCTURE_NAME
							   , KisaAccessTokenResponse.builder()
							   		.errLocation(new Object() {}.getClass().getEnclosingMethod().getName())
							   		.errMsg(e.getMessage())
							   		.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
							   	 .build()
							   	);

			throw new FailRequestWithdrawToKisaException(e);
		}
		catch (FailRequestWithdrawToKisaException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestWithdrawToKisaException(e);
		}
		catch (FailResponseToKisaException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"resultCode\": \"{}\" ,\"errCode\": \"{}\", \"errMsg\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), response.getBody().getResultCode(), response.getBody().getErrCode(), response.getBody().getErrMsg());

			throw new FailReponseWithdrawToKisaException(e); 
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"statusCode\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestWithdrawToKisaException();
		}
		catch (Exception e)
		{
			log.error("{ ■ KISA ■ <공인전자주소" + STATEMENT + "- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}
}
