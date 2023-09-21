package com.example.mp.gw.kisa.service.impl;


import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.crypto.NoSuchPaddingException;

import java.util.stream.Collectors;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.domain.RedisQueueDataWrapper;
import com.example.mp.gw.common.domain.Const.FORMATTER;
import com.example.mp.gw.common.domain.Const.MEMBER;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_FIX_FROM_DATE;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_FIX_FROM_SEQ;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_REG;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_REG_RD_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_REG_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_RETRY_FROM_DATE;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_RETRY_FROM_SEQ;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_RSLT;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_EADDR_RETRY_FROM_DATE;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_EADDR_RETRY_FROM_SEQ;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_EADDR_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_RETRY;
import com.example.mp.gw.common.exception.DateFormatException;
import com.example.mp.gw.common.exception.KeepRunningToWorkerException;
import com.example.mp.gw.common.exception.SeqFormatException;
import com.example.mp.gw.common.exception.StopRunningToWorkerException;
import com.example.mp.gw.common.exception.UnknownStructureNameException;
import com.example.mp.gw.common.service.RedisService;
import com.example.mp.gw.common.utils.enc.Encryptor;
import com.example.mp.gw.doc.domain.Document;
import com.example.mp.gw.doc.domain.DocumentFail;
import com.example.mp.gw.doc.mappers.altibase.DocumentMapper;
import com.example.mp.gw.kisa.service.KisaRetryService;
import com.example.mp.gw.member.domain.MemberEaddr;
import com.example.mp.gw.member.domain.MemberEaddrFail;
import com.example.mp.gw.member.mappers.altibase.MemberMapper;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service("KisaRetryService")
public class KisaRetryServiceImpl implements KisaRetryService
{
	@Autowired
	private KisaRetryService kisaRetryService;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<?>> retryToKisa;

	@Autowired
	private RedisService<String, String> retryEaddrFromDateToKisa;

	@Autowired
	private RedisService<String, String> retryEaddrFromSeqToKisa;

	@Autowired
	private RedisService<String, String> retryDocFromDateToKisa;

	@Autowired
	private RedisService<String, String> retryDocFromSeqToKisa;

	@Autowired
	private MemberMapper memberMapper;

	@Autowired
	private DocumentMapper documentMapper;

	@Value("${spring.kisa.scheduler.retry.send-cnt}")
	private String RETRY_SND_CNT;

	@Value("${spring.kisa.scheduler.retry.send-interval}")
	private String RETRY_SND_INTERVAL;


	/**
	 * KISA 연동 실패건 재처리(G/W -> KISA)
	 * @return void
	 * @throws KeepRunningToWorkerException
	 * @throws StopRunningToWorkerException   
	 */
	@Override
	public void popAndRetryToKisa() throws KeepRunningToWorkerException, StopRunningToWorkerException
	{
		RedisQueueDataWrapper<?> request = null;

		try
		{
			boolean IS_RETRY_DTM = false;
			long    nowTime      = System.currentTimeMillis() - Long.parseLong(RETRY_SND_INTERVAL); 
			String	nowDtm       = FORMATTER.yyyyMMddHHmmss.val().format(nowTime);

			// 1. 요청 데이터 읽기
			request = retryToKisa.rpop(Q_KISA_RETRY.STRUCTURE_NAME);

			if (null == request)
				return;

			log.debug("{ ■ KISA 재처리 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class));

			// 2. 재처리 대상 여부 판별(재처리 시간)
			IS_RETRY_DTM = Long.parseLong(request.getSend_dtm()) <= Long.parseLong(nowDtm);

			// 3. 재처리 기준 시간 미만이면 리푸쉬
			if (!IS_RETRY_DTM)
			{
				retryToKisa.lpush(Q_KISA_RETRY.STRUCTURE_NAME, request);

				return;
			}

			if (false == Q_KISA_EADDR_TO_SEND.STRUCTURE_NAME.equals(request.getStructure_name()))
				log.info("{ ■ KISA 재처리 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class));
			else
				log.debug("{ ■ KISA 재처리 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class));

			// 4. 요청 Queue 에 따른 분기 처리
			switch(request.getStructure_name())
			{
				// KISA 공인전자주소(등록/탈퇴/수정) 전송 요청
				case Q_KISA_EADDR_TO_SEND.STRUCTURE_NAME :

					kisaRetryService.caseEaddrToSend(request);

					break;

				// KISA 유통정보 등록(등록/등록 + 열람일시) 전송 요청
				case Q_KISA_DOC_REG_TO_SEND.STRUCTURE_NAME :

					kisaRetryService.caseDocRegToSend(request);

					break;

				// KISA 유통정보 등록(열람일시) 전송 요청
				case Q_KISA_DOC_REG_RD_TO_SEND.STRUCTURE_NAME :

					kisaRetryService.caseDocRegRdToSend(request);

					break;

				// KISA 유통정보 등록(등록/등록 + 열람일시/열람일시) 결과 업데이트 요청
				case Q_KISA_DOC_RSLT.STRUCTURE_NAME :

					kisaRetryService.caseDocRsltUpdate(request);

					break;

				default :

					throw new UnknownStructureNameException();
			}
		}
		catch (UnknownStructureNameException e)
		{
			log.error("{ ■ KISA 재처리 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class), e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ KISA 재처리 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class), e.getClass().getName(), e.getMessage());

			if (null != request)
				retryToKisa.lpush(Q_KISA_RETRY.STRUCTURE_NAME, request);

			throw new StopRunningToWorkerException(e);
		}
	}
	
	/**
	 * KISA 공인전자주소(등록/탈퇴/수정) 실패건 재전송(G/W -> KISA)
	 * * @param RedisQueueDataWrapper<MemberEaddr>
	 * @return void
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void caseEaddrToSend(RedisQueueDataWrapper<?> request)
	{
		MemberEaddr     member = null;
		MemberEaddrFail fail   = null;

		String reqType = "";

		try
		{
			member = (MemberEaddr) request.getData();

			boolean IS_RETRY_CNT = request.getSend_count() < Integer.parseInt(RETRY_SND_CNT);

			final String type = member.getType();

			if (MEMBER.TYPE.PERSON.val().equals(type))
				MDC.put(Const.TRACING_ID, member.getPhone() + (StringUtils.hasText(member.getMessageId()) ? ":" + member.getMessageId() : ""));
			else
				MDC.put(Const.TRACING_ID, member.getSvcOrgCd());

			if (true == Const.MEMBER.REQ_TYPE.REGISTER.val().equals(member.getReqType()))
				reqType = " 등록 ";
			else if(true == Const.MEMBER.REQ_TYPE.WITHDRAW.val().equals(member.getReqType()))
				reqType = " 틸퇴 ";
			else if(true == Const.MEMBER.REQ_TYPE.MODIFY.val().equals(member.getReqType()))
				reqType = " 수정 ";
			else
				reqType = " ";

			log.info("{ ■ KISA 공인전자주소{}- 재요청 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", reqType, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), member);

			// 재처리 횟수 초과 시 폐기
			if (!IS_RETRY_CNT)
			{
				log.error("{ ■ KISA 공인전자주소{}- EXPIRED ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\" }", reqType,  Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(member, MemberEaddr.class));

				fail = MemberEaddrFail.builder()
												.eaddrSeq(member.getEaddrSeq())
												.phone(member.getEncPhone())
												.createdDtm(member.getCreatedDtm())
												.svcOrgCd(member.getSvcOrgCd())
												.messageId(member.getMessageId())
												.failStructureName(request.getStructure_name())
												.failSndCnt(0)
												.failSndDtm("")
												.failRegDtm(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
									  .build();

				memberMapper.addMemberEaddrReqFail(fail);

				log.debug("{ ■ KISA 공인전자주소{}- EXPIRED ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\" }", reqType, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(fail, MemberEaddrFail.class));

				return;
			}

			log.debug("{ ■ KISA 공인전자주소{}- 재요청 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", reqType, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(member, MemberEaddr.class));

			// 재전송 카운트 증가
			request.incSndCnt();

			// 재전송
			retryToKisa.lpush(request.getStructure_name(), request);
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ ■ KISA 공인전자주소{}- ERROR \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", reqType, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(fail, MemberEaddrFail.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ KISA 공인전자주소{}- ERROR \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", reqType, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class), e.getClass().getName(), e.getMessage());

			throw e;
		}

		return;
	}

	/**
	 * KISA 요청(날짜) 공인전자주소(등록/탈퇴/수정) 실패건 재처리(G/W -> G/W, G/W -> KISA)
	 * @param int
	 * @return void
	 */
	@Override
	public void popAndRetryEaddrFromDateToKisa(int KISA_EADDR_RETRY_DATE_CNT)
	{
		String request = null;

		Map<String, Object> searchKey = new HashMap<String, Object>();

		try
		{
			String nowDtm = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());

			// 1. 요청 데이터 읽기
			 request = retryEaddrFromDateToKisa.rpop(Q_KISA_EADDR_RETRY_FROM_DATE.STRUCTURE_NAME);

			if (null == request)
				return;

			if (8 != request.length())
				throw new DateFormatException(request);

			searchKey.put("start"     , request + "000000");
			searchKey.put("end"       , request + "235959");
			searchKey.put("failSndDtm", nowDtm);
			searchKey.put("limit"     , KISA_EADDR_RETRY_DATE_CNT);

			log.info("{ ■ KISA 요청날짜({}) 공인전자주소(등록, 탈퇴, 수정) 실패건 재처리 - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", request, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class));

			// 2. KISA 재전송
			int i = 0;
			int j = 0;

			for (i = 0, j = 0; ; i = i + j, j = 0)
			{
				// 2.1 DB 조회 및 업데이트 및 KISA 재전송 요청
				j = kisaRetryService.sendRetryEaddrToKisaT(searchKey, nowDtm);

				if (0 == j)
					break;
			}

			log.info("{ ■ KISA 요청날짜({}) 공인전자주소(등록, 탈퇴, 수정) 실패건 재처리 - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\", \"cnt\": \"{}\" }", request, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), i);
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), request, e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * KISA 요청(SEQ) 공인전자주소(등록/탈퇴/수정) 실패건 재처리(G/W -> G/W, G/W -> KISA)
	 * @param int
	 * @return void
	 */
	@Override
	@Transactional(readOnly = true)
	public void popAndRetryEaddrFromSeqToKisa(int KISA_EADDR_RETRY_SEQ_CNT)
	{
		String request = null;

		Map<String, Object> searchKey = new HashMap<String, Object>();

		List<MemberEaddrFail> reqs = new ArrayList<MemberEaddrFail>();

		try
		{
			// 1. 요청 데이터 읽기
			request = retryEaddrFromSeqToKisa.rpop(Q_KISA_EADDR_RETRY_FROM_SEQ.STRUCTURE_NAME);

			if (null == request)
				return;

			List<Long> failSeq = Stream.of(request.split(",")).map(Long::valueOf).collect(Collectors.toList());

			if (true == failSeq.isEmpty() || failSeq.size() > KISA_EADDR_RETRY_SEQ_CNT )
				throw new SeqFormatException(Integer.toString(failSeq.size()));

			searchKey.put("limit"  , KISA_EADDR_RETRY_SEQ_CNT);
			searchKey.put("failSeq", failSeq);

			log.info("{ ■ KISA 요청 SEQ({}) 공인전자주소(등록, 탈퇴, 수정) 실패건 재처리 - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", request, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class));

			// 2. DB 재전송 대상 조회
			reqs = memberMapper.findMemberEaddrReqFailFromSeq(searchKey);

			// 3. DB 조회 및 업데이트 및 KISA 재전송 요청
			for (MemberEaddrFail req : reqs)
				kisaRetryService.sendRetryEaddrToKisa(req);

			log.info("{ ■ KISA 요청 SEQ({}) 공인전자주소(등록, 탈퇴, 수정) 실패건 재처리 - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", request, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class));
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"search\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), request, e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * KISA 하루전 공인전자주소(등록/탈퇴/수정) 실패건 재전송(G/W -> KISA)
	 * @param int 
	 * @return void
	 */
	@Override
	public void selectAndSendEaddrToKisa(int KISA_EADDR_RETRY_CNT)
	{
		Map<String, Object> searchKey = new HashMap<String, Object>();

		try
		{
			long now = System.currentTimeMillis();

			// 1. 재전송 대상 설정
			String yesDtm = FORMATTER.yyyyMMdd.val().format(now - (1000 * 60 * 60 * 24));
			String nowDtm = FORMATTER.yyyyMMddHHmmss.val().format(now);

			searchKey.put("start"     , yesDtm + "000000");
			searchKey.put("end"       , yesDtm + "235959");
			searchKey.put("failSndDtm", nowDtm);
			searchKey.put("limit"     , KISA_EADDR_RETRY_CNT);

			log.info("{ ■ KISA 이전({}) 공인전자주소(등록, 탈퇴, 수정) 실패건 재처리 - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", yesDtm, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class));

			// 2. KISA 재전송
			int i = 0;
			int j = 0;

			for (i = 0, j = 0; ; i = i + j, j = 0)
			{
				// 2.1 DB 조회 및 업데이트 및 KISA 재전송 요청
				j = kisaRetryService.sendRetryEaddrToKisaT(searchKey, nowDtm);

				if (0 == j)
					break;
			}

			log.info("{ ■ KISA 이전({}) 공인전자주소(등록, 탈퇴, 수정) 실패건 재처리 - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\", \"cnt\": \"{}\" }", yesDtm, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), i);
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * KISA 공인전자주소(등록/탈퇴/수정) 실패건 DB 업데이트 및 단건 전송(G/W -> KISA)
	 * @param MemberEaddrFail
	 * @return void
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public void sendRetryEaddrToKisa(MemberEaddrFail req)
	{
		try
		{
			log.info("{ ■ KISA 공인전자주소(등록, 탈퇴, 수정) 전송 - 재요청 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, MemberEaddrFail.class));

			// 1 재전송 테이블(MP_MB_EADDR_REQ_FAIL) 상태값(FAIL_SND_CNT, FAIL_SND_DTM) 변경
			req.incFailSndCnt();
			req.setFailSndDtm(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));

			memberMapper.updateMemberEaddrReqFail(req);

			// 2.2 KISA 공인전자주소(등록, 탈퇴, 수정) 재전송 요청 데이터 생성
			retryToKisa.lpush(
							  req.getFailStructureName()
							, RedisQueueDataWrapper.<MemberEaddr>builder()
								.structure_name(req.getFailStructureName())
								.data(
									  MemberEaddr.builder()
														   .eaddrSeq(req.getEaddrSeq())
														   .mbId(req.getMbId())
														   .cea(req.getCea())
														   .ci(StringUtils.hasText(req.getCi())?Encryptor.decryptCi(req.getCi()):"")
														   .phone(StringUtils.hasText(req.getPhone())?Encryptor.decryptPhone(req.getPhone()):"")
														   .birthday(req.getBirthday())
														   .name(req.getName())
														   .gender(req.getGender())
														   .createdDtm(req.getCreatedDtm())
														   .type(req.getType())
														   .busiNum(req.getBusiNum())
														   .svcOrgCd(req.getSvcOrgCd())
														   .svcOrgName(req.getSvcOrgName())
														   .svcOrgType(req.getSvcOrgType())
														   .reqType(req.getReqType())
														   .reqRoute(req.getReqRoute())
														   .messageId(req.getMessageId())
														   .file1(req.getFile1())
														   .file2(req.getFile2())
														   .regDtm(req.getRegDtm())
												 .build()
									 )
							  .build()
							 );
		}
		catch (NoSuchPaddingException | NoSuchAlgorithmException e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, MemberEaddrFail.class), e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, MemberEaddrFail.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw e;
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, MemberEaddrFail.class), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * KISA 공인전자주소(등록/탈퇴/수정) 실패건 DB 조회 및 업데이트 및 다건 전송(G/W -> KISA)
	 * @param Map<String, Object>
	 * @param String
	 * @return int
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public int sendRetryEaddrToKisaT(Map<String, Object> searchKey, String now)
	{
		int i = 0;

		List<MemberEaddrFail>          reqs  = new ArrayList<MemberEaddrFail>();
		List<RedisQueueDataWrapper<?>> fails = new ArrayList<RedisQueueDataWrapper<?>>();

		try
		{
			// 1. 재전송 대상 조회
			reqs = memberMapper.findMemberEaddrReqFailFromDtm(searchKey);

			if (true == reqs.isEmpty())
				return 0;

			// 2. KISA 재전송 요청
			for (MemberEaddrFail req : reqs)
			{
				try
				{
					log.info("{ ■ KISA 공인전자주소(등록, 탈퇴, 수정) 전송 - 재요청 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, MemberEaddrFail.class));
	
					// 2.1 재전송 테이블(MP_MB_EADDR_REQ_FAIL) 상태값(FAIL_SND_CNT, FAIL_SND_DTM) 변경
					req.incFailSndCnt();
					req.setFailSndDtm(now);

					memberMapper.updateMemberEaddrReqFail(req);

					// 2.2 KISA 공인전자주소(등록, 탈퇴, 수정) 재전송 요청 데이터 생성
					fails.add(
							  RedisQueueDataWrapper.<MemberEaddr>builder()
								.structure_name(req.getFailStructureName())
								.data(
									  MemberEaddr.builder()
														   .eaddrSeq(req.getEaddrSeq())
														   .mbId(req.getMbId())
														   .cea(req.getCea())
														   .ci(StringUtils.hasText(req.getCi())?Encryptor.decryptCi(req.getCi()):"")
														   .phone(StringUtils.hasText(req.getPhone())?Encryptor.decryptPhone(req.getPhone()):"")
														   .birthday(req.getBirthday())
														   .name(req.getName())
														   .gender(req.getGender())
														   .createdDtm(req.getCreatedDtm())
														   .type(req.getType())
														   .busiNum(req.getBusiNum())
														   .svcOrgCd(req.getSvcOrgCd())
														   .svcOrgName(req.getSvcOrgName())
														   .svcOrgType(req.getSvcOrgType())
														   .reqType(req.getReqType())
														   .reqRoute(req.getReqRoute())
														   .messageId(req.getMessageId())
														   .file1(req.getFile1())
														   .file2(req.getFile2())
														   .regDtm(req.getRegDtm())
				   					  			 .build()
									 )
							  .build()
							 );
				}
				catch (NoSuchPaddingException | NoSuchAlgorithmException e)
				{
					log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, MemberEaddrFail.class), e.getClass().getName(), e.getMessage());

					throw new RuntimeException(e);
				}
				catch (DataAccessException e)
				{
					SQLException se = (SQLException)e.getCause();
	
					log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, MemberEaddrFail.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());
	
					throw e;
				}
				catch (Exception e)
				{
					log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, MemberEaddrFail.class), e.getClass().getName(), e.getMessage());
	
					throw e;
				}
	
				i++;
			}

			// 3 KISA 공인전자주소(등록, 탈퇴, 수정) 재전송 요청(레디스)
			if (false == fails.isEmpty())
				retryToKisa.lpushT(Q_KISA_EADDR_TO_SEND.STRUCTURE_NAME, fails);
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(reqs, List.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), e.getClass().getName(), e.getMessage());

			throw e;
		}

		return i;
	}

	/**
	 * KISA 유통정보 등록(등록/등록 + 열람일시) 실패건 재전송(G/W -> KISA)
	 * @param RedisQueueDataWrapper<Document> 
	 * @return void
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void caseDocRegToSend(RedisQueueDataWrapper<?> request)
	{
		DocumentFail fail = null;

		try
		{
			Document doc = (Document) request.getData();

			boolean IS_RETRY_CNT = request.getSend_count() < Integer.parseInt(RETRY_SND_CNT);

			MDC.put(Const.TRACING_ID, doc.getMessageId());

			log.info("{ ■ KISA 유통정보 등록(등록, 등록 + 열람일시) - 재요청 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(doc, Document.class));

			// 재처리 횟수 초과 시 폐기
			if (!IS_RETRY_CNT)
			{
				log.error("{ ■ KISA 유통정보 등록(등록, 등록 + 열람일시) - EXPIRED ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class));

				fail = DocumentFail.builder()
											 .dcmntInfoId(doc.getDcmntInfoId())
				   						     .messageId(doc.getMessageId())
				   						     .elctrcDcmntNo(doc.getElctrcDcmntNo())
				   						     .elctrcDcmntSbjct(doc.getElctrcDcmntSbjct())
				   						     .sndCea(doc.getSndCea())
				   						     .rsvCea(doc.getRsvCea())
				   						     .sndDtm(doc.getSndDtm())
				   						     .rsvDtm(doc.getRsvDtm())
				   						     .rdDtm(StringUtils.hasText(doc.getRdDtm())?doc.getRdDtm():"")
				   						     .cntntHash(doc.getCntntHash())
				   						     .fileHash(StringUtils.hasText(doc.getFileHash())?doc.getFileHash():"")
				   						     .sndName(doc.getSndName())
				   						     .rsvName(doc.getRsvName())
				   						     .sndPlfmId(doc.getSndPlfmId())
				   						     .rsvPlfmId(doc.getRsvPlfmId())
				   						     .dcmntStat(doc.getDcmntStat())
				   						     .dcmntRslt(StringUtils.hasText(doc.getDcmntRslt())?doc.getDcmntRslt():"")
				   						     .dcmntRsltCd(StringUtils.hasText(doc.getDcmntRsltCd())?doc.getDcmntRsltCd():"")
				   						     .dcmntRsltDtm(StringUtils.hasText(doc.getDcmntRsltDtm())?doc.getDcmntRsltDtm():"")
				   						     .regDtm(doc.getRegDtm())
				   						     .partMm(doc.getPartMm())
				   						     .failStructureName(request.getStructure_name())
				   						     .failSndCnt(0)
				   						     .failSndDtm("")
				   						     .failRegDtm(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
				   						     .testSndnYn(doc.getTestSndnYn())
				   						     .docType(doc.getDocType())
				   				   .build();

				log.debug("{ ■ KISA 유통정보 등록(등록, 등록 + 열람일시) - EXPIRED ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(fail, DocumentFail.class));

				documentMapper.insertDocumentReqFail(fail);

				return;
			}

			// 재전송 카운트 증가
			request.incSndCnt();

			// 재전송
			retryToKisa.lpush(request.getStructure_name(), request);
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ ■ KISA 유통정보 등록(등록, 등록 + 열람일시) - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(fail, DocumentFail.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ KISA 유통정보 등록(등록, 등록 + 열람일시) - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class), e.getClass().getName(), e.getMessage());

			throw e;
		}

		return;
	}

	/**
	 * KISA 유통정보 등록(열람일시) 실패건 재전송(G/W -> KISA)
	 * @param RedisQueueDataWrapper<Document> 
	 * @return void
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void caseDocRegRdToSend(RedisQueueDataWrapper<?> request)
	{
		DocumentFail fail = null;

		try
		{
			Document doc = (Document) request.getData();

			boolean IS_RETRY_CNT = request.getSend_count() < Integer.parseInt(RETRY_SND_CNT);

			MDC.put(Const.TRACING_ID, doc.getMessageId());

			log.info("{ ■ KISA 유통정보 등록(열람일시) - 재요청 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(doc, Document.class));

			// 재처리 횟수 초과 시 폐기
			if (!IS_RETRY_CNT)
			{
				log.error("{ ■ KISA 유통정보 등록(열람일시) - EXPIRED ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class));

				fail = DocumentFail.builder()
											 .dcmntInfoId(doc.getDcmntInfoId())
				   						     .messageId(doc.getMessageId())
				   						     .elctrcDcmntNo(doc.getElctrcDcmntNo())
				   						     .elctrcDcmntSbjct(doc.getElctrcDcmntSbjct())
				   						     .sndCea(doc.getSndCea())
				   						     .rsvCea(doc.getRsvCea())
				   						     .sndDtm(doc.getSndDtm())
				   						     .rsvDtm(doc.getRsvDtm())
				   						     .rdDtm(StringUtils.hasText(doc.getRdDtm())?doc.getRdDtm():"")
				   						     .cntntHash(doc.getCntntHash())
				   						     .fileHash(StringUtils.hasText(doc.getFileHash())?doc.getFileHash():"")
				   						     .sndName(doc.getSndName())
				   						     .rsvName(doc.getRsvName())
				   						     .sndPlfmId(doc.getSndPlfmId())
				   						     .rsvPlfmId(doc.getRsvPlfmId())
				   						     .dcmntStat(StringUtils.hasText(doc.getDcmntStat())?doc.getDcmntStat():"")
				   						     .dcmntRslt(StringUtils.hasText(doc.getDcmntRslt())?doc.getDcmntRslt():"")
				   						     .dcmntRsltCd(StringUtils.hasText(doc.getDcmntRsltCd())?doc.getDcmntRsltCd():"")
				   						     .dcmntRsltDtm(doc.getDcmntRsltDtm())
				   						     .regDtm(doc.getRegDtm())
				   						     .partMm(doc.getPartMm())
				   						     .failStructureName(request.getStructure_name())
				   						     .failSndCnt(0)
				   						     .failSndDtm("")
				   						     .failRegDtm(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
				   						     .testSndnYn(doc.getTestSndnYn())
				   						     .docType(doc.getDocType())
				   				   .build();

				log.debug("{ ■ KISA 유통정보 등록(열람일시) - EXPIRED ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(fail, DocumentFail.class));

				documentMapper.insertDocumentReqFail(fail);

				return;
			}

			// 전자문서 수신시점에 수신자 공인전자주소가 존재하지 않는 경우 -> KISA 유통정보 등록 결과 초기화  
			if (Const.DOCUMENT.DCMNT_RSLT_CD.RSV_CEA.val().equals(doc.getDcmntRsltCd()))
				doc.setDcmntRslt("");

			// 재전송 카운트 증가
			request.incSndCnt();

			// 재전송
			retryToKisa.lpush(request.getStructure_name(), request);
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ ■ KISA 유통정보 등록(열람일시) - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(fail, DocumentFail.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ KISA 유통정보 등록(열람일시) - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class), e.getClass().getName(), e.getMessage());

			throw e;
		}

		return;
	}

	/**
	 * KISA 유통정보 전송(등록/등록 + 열람일시/열람일시) 결과 업데이트(G/W)
	 * @param RedisQueueDataWrapper<Document>
	 * @return void
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void caseDocRsltUpdate(RedisQueueDataWrapper<?> request)
	{
		DocumentFail fail = null;

		try
		{
			Document doc = (Document) request.getData();

			boolean IS_RETRY_CNT = request.getSend_count() < Integer.parseInt(RETRY_SND_CNT);

			MDC.put(Const.TRACING_ID, doc.getMessageId());

			log.info("{ ■ KISA 유통정보 전송(등록/등록 + 열람일시/열람일시) 결과 업데이트 - 재요청 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(doc, Document.class));

			// 재처리 횟수 초과 시 폐기
			if (!IS_RETRY_CNT)
			{
				log.error("{ ■ KISA 유통정보 전송(등록/등록 + 열람일시/열람일시) 결과 업데이트 - EXPIRED ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class));

				return;
			}

			// 재전송 카운트 증가
			request.incSndCnt();

			// 재전송
			retryToKisa.lpush(request.getStructure_name(), request);
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ ■ KISA 유통정보 전송(등록/등록 + 열람일시/열람일시) 결과 업데이트 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(fail, DocumentFail.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ KISA 유통정보 전송(등록/등록 + 열람일시/열람일시) 결과 업데이트 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class), e.getClass().getName(), e.getMessage());

			throw e;
		}

		return;
	}

	/**
	 * KISA 요청(날짜) 유통정보(등록/등록 + 열람일시/열람일시) 수신시간 보정(G/W)
	 * @param int
	 * @return void
	 */
	@Override
	public void popAndFixDocFromDate(int KISA_DCMNT_FIX_DATE_CNT)
	{
		String request = null;

		Map<String, Object> searchKey = new HashMap<String, Object>();

		try
		{
			// 1. 보정 요청 읽기
			 request = retryDocFromDateToKisa.rpop(Q_KISA_DOC_FIX_FROM_DATE.STRUCTURE_NAME);

			if (null == request)
				return;

			if (8 != request.length())
				throw new DateFormatException(request);

			// 2. 보정 대상 설정(수신시간)
			searchKey.put("start"      , request + "000000");
			searchKey.put("end"        , request + "235959");
			searchKey.put("limit"      , KISA_DCMNT_FIX_DATE_CNT);

			log.info("{ ■ KISA 요청날짜({}) 유통정보 수신시간 보정 - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", request, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class));

			// 3. 수신시간 보정
			int i = 0;
			int j = 0;

			for (i = 0, j = 0; ; i = i + j, j = 0)
			{
				// 2.1 대상 조회 및 업데이트
				j = kisaRetryService.fixRetryDocRsvDtm(searchKey);

				if (0 == j)
					break;
			}

			log.info("{ ■ KISA 요청날짜({}) 유통정보 수신시간 보정 - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\", \"cnt\": \"{}\" }", request, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), i);
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), request, e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * KISA 요청(SEQ) 유통정보(등록/등록 + 열람일시/열람일시) 수신시간 보정(G/W)
	 * @param int
	 * @return void
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void popAndFixDocFromSeq(int KISA_DCMNT_FIX_SEQ_CNT)
	{
		String request = null;

		Map<String, Object> searchKey = new HashMap<String, Object>();

		List<DocumentFail> reqs = new ArrayList<DocumentFail>();

		try
		{
			// 1. 보정(수신 시간) 요청 읽기
			request = retryDocFromSeqToKisa.rpop(Q_KISA_DOC_FIX_FROM_SEQ.STRUCTURE_NAME);

			if (null == request)
				return;

			List<Long> failSeq = Stream.of(request.split(",")).map(Long::valueOf).collect(Collectors.toList());

			if (true == failSeq.isEmpty() || failSeq.size() > KISA_DCMNT_FIX_SEQ_CNT )
				throw new SeqFormatException(Integer.toString(failSeq.size()));

			searchKey.put("limit"  , KISA_DCMNT_FIX_SEQ_CNT);
			searchKey.put("failSeq", failSeq);

			log.info("{ ■ KISA 요청 SEQ({}) 유통정보 수신시간 보정 - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", request, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class));

			// 2. 보정 대상 조회
			reqs = documentMapper.selectDocumentReqFailForFixRsvDtmFromSeq(searchKey);

			// 3. 보정
			for (DocumentFail req : reqs)
			{
				try
				{
					log.info("{ ■ KISA 유통정보 수신시간 - 보정 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, DocumentFail.class));

					// 2.1 시간보정(수신시간=열람시간)
					req.setRsvDtm(req.getRdDtm());
	
					// 2.2 유통증명정보 테이블(MP_DCMNT_INFO) 수신시간 변경
					documentMapper.updateDocumentRsvDtm(
														Document.builder()
																		  .rsvDtm(req.getRsvDtm())
																		  .partMm(req.getPartMm())
																		  .dcmntInfoId(req.getDcmntInfoId())
																		  .messageId(req.getMessageId())
																.build()
													   );
	
					// 2.3 재전송 테이블(MP_DCMNT_REQ_FAIL) 수신시간 변경
					documentMapper.updateDocumentReqfailRsvDtm(req);
				}
				catch (DataAccessException e)
				{
					SQLException se = (SQLException)e.getCause();

					log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, DocumentFail.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

					throw new RuntimeException(e);
				}
				catch (Exception e)
				{
					log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, DocumentFail.class), e.getClass().getName(), e.getMessage());

					throw e;
				}
			}

			log.info("{ ■ KISA 요청 SEQ({}) 유통정보 수신시간 보정 - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", request, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class));
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), request, e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * KISA 요청(날짜) 유통정보(생성/등록/등록 + 열람일시/열람일시) 실패건 재처리(G/W -> G/W, G/W -> KISA)
	 * @param int
	 * @return void
	 */
	@Override
	public void popAndRetryDocFromDateToKisa(int KISA_DCMNT_RETRY_DATE_CNT)
	{
		String request = null;

		Map<String, Object> searchKey = new HashMap<String, Object>();

		try
		{
			String nowDtm = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());

			// 1. 재전송 요청 읽기
			 request = retryDocFromDateToKisa.rpop(Q_KISA_DOC_RETRY_FROM_DATE.STRUCTURE_NAME);

			if (null == request)
				return;

			if (8 != request.length())
				throw new DateFormatException(request);

			// 2. 조회 대상 설정(내부 연동 실패)
			searchKey.put("start"      , request + "000000");
			searchKey.put("end"        , request + "235959");
			searchKey.put("dcmntRsltCd", Const.DOCUMENT.DCMNT_RSLT_CD.ETC.val());
			searchKey.put("failSndDtm" , nowDtm);
			searchKey.put("limit"      , KISA_DCMNT_RETRY_DATE_CNT);

			log.info("{ ■ KISA 요청날짜({}) 유통정보 [내부] 연동 실패건 재처리 - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", request, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class));

			// 3. KISA 재전송 요청
			int i = 0;
			int j = 0;

			for (i = 0, j = 0; ; i = i + j, j = 0)
			{
				// 2.1 DB 조회 및 업데이트 및 KISA 재전송 요청
				j = kisaRetryService.sendRetryDocToKisaT(searchKey, nowDtm, Q_KISA_DOC_REG.STRUCTURE_NAME);

				if (0 == j)
					break;
			}

			log.info("{ ■ KISA 요청날짜({}) 유통정보 [내부] 연동 실패건 재처리 - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\", \"cnt\": \"{}\" }", request, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), i);

			// 3. 조회 대상 설정(외부 연동 실패)
			searchKey.remove("dcmntRsltCd");

			log.info("{ ■ KISA 요청날짜({}) 유통정보 [외부] 연동 실패건 재처리 - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", request, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class));

			// 4. KISA 재전송 요청
			for (i = 0, j = 0; ; i = i + j, j = 0)
			{
				// 4.1 DB 조회 및 업데이트 및 KISA 재전송 요청
				j = kisaRetryService.sendRetryDocToKisaT(searchKey, nowDtm, Q_KISA_DOC_REG_TO_SEND.STRUCTURE_NAME);

				if (j == 0)
					break;
			}

			log.info("{ ■ KISA 요청날짜({}) 유통정보 [외부] 연동 실패건 재처리 - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\", \"cnt\": \"{}\" }", request, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), i);
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), request, e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * KISA 요청(SEQ) 유통정보(생성/등록/등록 + 열람일시/열람일시) 실패건 재처리(G/W -> G/W, G/W -> KISA)
	 * @param int
	 * @return void
	 */
	@Override
	@Transactional(readOnly = true)
	public void popAndRetryDocFromSeqToKisa(int KISA_DCMNT_RETRY_SEQ_CNT)
	{
		String request = null;

		Map<String, Object> searchKey = new HashMap<String, Object>();

		List<DocumentFail> reqs = new ArrayList<DocumentFail>();

		try
		{
			// 1. 재전송 요청 읽기
			request = retryDocFromSeqToKisa.rpop(Q_KISA_DOC_RETRY_FROM_SEQ.STRUCTURE_NAME);

			if (null == request)
				return;

			List<Long> failSeq = Stream.of(request.split(",")).map(Long::valueOf).collect(Collectors.toList());

			if (true == failSeq.isEmpty() || failSeq.size() > KISA_DCMNT_RETRY_SEQ_CNT )
				throw new SeqFormatException(Integer.toString(failSeq.size()));

			searchKey.put("limit"  , KISA_DCMNT_RETRY_SEQ_CNT);
			searchKey.put("failSeq", failSeq);

			log.info("{ ■ KISA 요청 SEQ({}) 유통정보 실패건 재처리 - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", request, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class));

			// 2. DB 재전송 대상 조회
			reqs = documentMapper.selectDocumentReqFailFromSeq(searchKey);

			// 3. DB 조회 및 업데이트 및 KISA 재전송 요청
			for (DocumentFail req : reqs)
				kisaRetryService.sendRetryDocToKisa(req);

			log.info("{ ■ KISA 요청 SEQ({}) 유통정보 실패건 재처리 - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", request, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class));
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), request, e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * KISA 이틀전 유통정보(등록/등록 + 열람일시) 수신시간 보정(G/W)
	 * @param int
	 * @return void
	 */
	@Override
	public void selectAndFixDoc(int KISA_DCMNT_FIX_CNT)
	{
		Map<String, Object> searchKey = new HashMap<String, Object>();

		try
		{
			long now = System.currentTimeMillis();

			// 1. 보정(수신시간) 대상 설정(-2일)
			String beYesDtm = FORMATTER.yyyyMMdd.val().format(now - (1000 * 60 * 60 * 24 * 2));

			searchKey.put("start", beYesDtm + "000000");
			searchKey.put("end"  , beYesDtm + "235959");
			searchKey.put("limit", KISA_DCMNT_FIX_CNT);

			log.info("{ ■ KISA 이전({}) 유통정보(등록/등록 + 열람일시) 수신시간 보정 - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", beYesDtm, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class));

			// 2. 보정
			int i = 0;
			int j = 0;

			for (i = 0, j = 0; ; i = i + j, j = 0)
			{
				// 2.1 보정 대상 조회 및 보정(수신시간)
				j = kisaRetryService.fixRetryDocRsvDtm(searchKey);

				if (0 == j)
					break;
			}

			log.info("{ ■ KISA 이전({}) 유통정보(등록/등록 + 열람일시) 수신시간 보정 - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\", \"cnt\": \"{}\" }", beYesDtm, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), i);
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * KISA 이전(이틀전, 하루전) 유통정보(등록/등록 + 열람일시) 연동 실패건 재전송(G/W -> KISA)
	 * @param int
	 * @return void
	 */
	@Override
	public void selectAndSendDocToKisa(int KISA_DCMNT_RETRY_CNT)
	{
		Map<String, Object> searchKey = new HashMap<String, Object>();

		try
		{
			long now = System.currentTimeMillis();

			for (int loop = 2; loop > 0; loop--)
			{
				// 1. 조회 대상 설정(-2일, -1일)
				String yesDtm = FORMATTER.yyyyMMdd.val().format(now - (1000 * 60 * 60 * 24 * loop));
				String nowDtm = FORMATTER.yyyyMMddHHmmss.val().format(now);
	
				List<String> stat = new ArrayList<String>();
	
				stat.add(Const.DOCUMENT.DCMNT_STAT.REGISTER.val());
				stat.add(Const.DOCUMENT.DCMNT_STAT.HYBRID.val());
	
				searchKey.put("start"     , yesDtm + "000000");
				searchKey.put("end"       , yesDtm + "235959");
				searchKey.put("stat"      , stat);
				searchKey.put("failSndDtm", nowDtm);
				searchKey.put("limit"     , KISA_DCMNT_RETRY_CNT);
	
				log.info("{ ■ KISA 이전({}) 유통정보(등록, 등록 + 열람일시) 실패건 재처리 - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", yesDtm, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class));
	
				// 2. KISA 재전송 요청
				int i = 0;
				int j = 0;
	
				for (i = 0, j = 0; ; i = i + j, j = 0)
				{
					// 2.1 DB 조회 및 업데이트 및 KISA 재전송 요청
					j = kisaRetryService.sendRetryDocToKisaT(searchKey, nowDtm, Q_KISA_DOC_REG_TO_SEND.STRUCTURE_NAME);
	
					if (0 == j)
						break;
				}
	
				log.info("{ ■ KISA 이전({}) 유통정보(등록, 등록 + 열람일시) 실패건 재처리 - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\", \"cnt\": \"{}\" }", yesDtm, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), i);
			}
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * KISA 이전(이틀전, 하루전) 유통정보(열람일시) 연동 실패건 재전송(G/W -> KISA)
	 * @param int
	 * @return void
	 */
	@Override
	public void selectAndSendDocRdToKisa(int KISA_DCMNT_RD_RETRY_CNT)
	{
		Map<String, Object> searchKey = new HashMap<String, Object>();

		try
		{
			long now = System.currentTimeMillis();

			for (int loop = 2; loop > 0; loop--)
			{
				// 1. 재전송 대상 설정(-2일, -1일)
				String yesDtm = FORMATTER.yyyyMMdd.val().format(now - (1000 * 60 * 60 * 24 * loop));
				String nowDtm = FORMATTER.yyyyMMddHHmmss.val().format(now);
	
				List<String> stat = new ArrayList<String>();
	
				stat.add(Const.DOCUMENT.DCMNT_STAT.UPDATE.val());
	
				searchKey.put("start"     , yesDtm + "000000");
				searchKey.put("end"       , yesDtm + "235959");
				searchKey.put("stat"      , stat);
				searchKey.put("failSndDtm", nowDtm);
				searchKey.put("limit"     , KISA_DCMNT_RD_RETRY_CNT);
	
				log.info("{ ■ KISA {} 유통정보(열람일시) 실패건 재처리 - 시작 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", yesDtm, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class));
	
				// 2. KISA 재전송 요청
				int i = 0;
				int j = 0;
	
				for (i = 0, j = 0; ; i = i + j, j = 0)
				{
					// 2.1 DB 조회 및 업데이트 및 KISA 재전송 요청
					j = kisaRetryService.sendRetryDocToKisaT(searchKey, nowDtm, Q_KISA_DOC_REG_RD_TO_SEND.STRUCTURE_NAME);
	
					if (0 == j)
						break;
				}
	
				log.info("{ ■ KISA {} 유통정보(열람일시) 실패건 재처리 - 종료 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\", \"cnt\": \"{}\" }", yesDtm, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), i);
			}
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * KISA 유통정보(등록/등록 + 열람일시/열람일시) 실패건 DB업데이트 및 단건 전송(G/W -> KISA)
	 * @param DocumentFail
	 * @return void
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public void sendRetryDocToKisa(DocumentFail req)
	{
		try
		{
			log.info("{ ■ KISA 유통정보 - 재요청 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, DocumentFail.class));

			// 1 재전송 테이블(MP_MB_EADDR_REQ_FAIL) 상태값(FAIL_SND_CNT, FAIL_SND_DTM) 변경
			req.incFailSndCnt();
			req.setFailSndDtm(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));

			documentMapper.updateDocumentReqFail(req);

			// 2 KISA 유통정보(등록, 열람일시, 등록 + 열람일시) 재전송 요청(레디스)
			retryToKisa.lpush(
							  req.getFailStructureName()
							, RedisQueueDataWrapper.<Document>builder()
								.structure_name(req.getFailStructureName())
								.data(
									  Document.builder()
								   						.dcmntInfoId(req.getDcmntInfoId())
								   						.messageId(req.getMessageId())
								   						.elctrcDcmntNo(req.getElctrcDcmntNo())
								   						.elctrcDcmntSbjct(req.getElctrcDcmntSbjct())
								   						.sndCea(req.getSndCea())
								   						.rsvCea(req.getRsvCea())
								   						.sndDtm(req.getSndDtm())
								   						.rsvDtm(req.getRsvDtm())
								   						.rdDtm(req.getRdDtm())
								   						.cntntHash(req.getCntntHash())
								   						.fileHash(req.getFileHash())
								   						.sndName(req.getSndName())
								   						.rsvName(req.getRsvName())
								   						.sndPlfmId(req.getSndPlfmId())
								   						.rsvPlfmId(req.getRsvPlfmId())
								   						.dcmntStat(req.getDcmntStat())
								   						.dcmntRslt("")
								   						.dcmntRsltCd(Const.DOCUMENT.DCMNT_RSLT_CD.ETC.val().equals(req.getDcmntRsltCd())?Const.DOCUMENT.DCMNT_RSLT_CD.ETC.val():"")
								   						.dcmntRsltDtm("")
								   						.regDtm(req.getRegDtm())
								   						.partMm(req.getPartMm())
								   						.testSndnYn(req.getTestSndnYn())
								   						.docType(req.getDocType())
								   			  .build()
									 )
							  .build()
							 );
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, DocumentFail.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, DocumentFail.class), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * KISA 유통정보(등록/등록 + 열람일시/열람일시) 수신시간 보정 대상 DB 조회 및 업데이트(G/W)
	 * @param Map<String, Object>
	 * @param String
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public int fixRetryDocRsvDtm(Map<String, Object> searchKey)
	{
		int i = 0;

		List<DocumentFail> reqs = new ArrayList<DocumentFail>();

		try
		{
			// 1. 대상 조회
			reqs = documentMapper.selectDocumentReqFailForFixRsvDtmFromDtm(searchKey);

			if (true == reqs.isEmpty())
				return 0;

			// 2. 보정
			for (DocumentFail req : reqs)
			{
				try
				{
					log.info("{ ■ KISA 유통정보 수신시간 - 보정 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, DocumentFail.class));

					// 2.1 시간보정(수신시간=열람시간)
					req.setRsvDtm(req.getRdDtm());

					// 2.2 유통증명정보 테이블(MP_DCMNT_INFO) 수신시간 변경
					documentMapper.updateDocumentRsvDtm(
														Document.builder()
																		  .rsvDtm(req.getRsvDtm())
																		  .partMm(req.getPartMm())
																		  .dcmntInfoId(req.getDcmntInfoId())
																		  .messageId(req.getMessageId())
																.build()
													   );

					// 2.3 재전송 테이블(MP_DCMNT_REQ_FAIL) 수신시간 변경
					documentMapper.updateDocumentReqfailRsvDtm(req);
				}
				catch (DataAccessException e)
				{
					SQLException se = (SQLException)e.getCause();

					log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, DocumentFail.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

					throw e;
				}
				catch (Exception e)
				{
					log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, DocumentFail.class), e.getClass().getName(), e.getMessage());

					throw e;
				}

				i++;
			}
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), e.getClass().getName(), e.getMessage());

			throw e;
		}

		return i;
	}

	/**
	 * KISA 유통정보(등록/등록 + 열람일시/열람일시) 실패건 DB 조회 및 업데이트 및 다건 전송(G/W -> KISA)
	 * @param Map<String, Object>
	 * @param String
	 * @param String
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
	public int sendRetryDocToKisaT(Map<String, Object> searchKey, String now, String structure)
	{
		int i = 0;

		List<DocumentFail>             reqs  = new ArrayList<DocumentFail>();
		List<RedisQueueDataWrapper<?>> fails = new ArrayList<RedisQueueDataWrapper<?>>();

		try
		{
			// 1. KISA 재전송 대상 조회
			reqs = documentMapper.selectDocumentReqFailFromDtm(searchKey);

			if (true == reqs.isEmpty())
				return 0;

			// 2. KISA 재전송 요청
			for (DocumentFail req : reqs)
			{
				try
				{
					log.info("{ ■ KISA 유통정보 - 재요청 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, DocumentFail.class));

					// 2.1 재전송 테이블(MP_MB_EADDR_REQ_FAIL) 상태값(FAIL_SND_CNT, FAIL_SND_DTM) 변경
					req.incFailSndCnt();
					req.setFailSndDtm(now);

					documentMapper.updateDocumentReqFail(req);

					// 2.2 재전송 데이터 생성
					fails.add(
							  RedisQueueDataWrapper.<Document>builder()
								.structure_name(req.getFailStructureName())
								.data(
									  Document.builder()
								   						.dcmntInfoId(req.getDcmntInfoId())
								   						.messageId(req.getMessageId())
								   						.elctrcDcmntNo(req.getElctrcDcmntNo())
								   						.elctrcDcmntSbjct(req.getElctrcDcmntSbjct())
								   						.sndCea(req.getSndCea())
								   						.rsvCea(req.getRsvCea())
								   						.sndDtm(req.getSndDtm())
								   						.rsvDtm(req.getRsvDtm())
								   						.rdDtm(req.getRdDtm())
								   						.cntntHash(req.getCntntHash())
								   						.fileHash(req.getFileHash())
								   						.sndName(req.getSndName())
								   						.rsvName(req.getRsvName())
								   						.sndPlfmId(req.getSndPlfmId())
								   						.rsvPlfmId(req.getRsvPlfmId())
								   						.dcmntStat(req.getDcmntStat())
								   						.dcmntRslt("")
								   						.dcmntRsltCd(Const.DOCUMENT.DCMNT_RSLT_CD.ETC.val().equals(req.getDcmntRsltCd())?Const.DOCUMENT.DCMNT_RSLT_CD.ETC.val():"")
								   						.dcmntRsltDtm("")
								   						.regDtm(req.getRegDtm())
								   						.partMm(req.getPartMm())
								   						.testSndnYn(req.getTestSndnYn())
								   						.docType(req.getDocType())
								   			  .build()
									 )
							  .build()
							 );
				}
				catch (DataAccessException e)
				{
					SQLException se = (SQLException)e.getCause();

					log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, DocumentFail.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

					throw e;
				}
				catch (Exception e)
				{
					log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(req, DocumentFail.class), e.getClass().getName(), e.getMessage());

					throw e;
				}

				i++;
			}

			// 3. KISA 유통정보(등록, 열람일시, 등록 + 열람일시) 재전송 요청(레디스)
			if (false == fails.isEmpty())
				retryToKisa.lpushT(structure, fails);
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			log.error("{ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(searchKey, Map.class), e.getClass().getName(), e.getMessage());

			throw e;
		}

		return i;
	}
}
