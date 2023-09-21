package com.example.mp.gw.token.service.impl;


import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Calendar;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.domain.RedisQueueDataWrapper;
import com.example.mp.gw.common.domain.Const.FORMATTER;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_REG_RD;
import com.example.mp.gw.common.service.RedisService;
import com.example.mp.gw.doc.domain.Document;
import com.example.mp.gw.doc.domain.DocumentRd;
import com.example.mp.gw.doc.mappers.altibase.DocumentMapper;
import com.example.mp.gw.token.domain.GenerateReadTokenRequest;
import com.example.mp.gw.token.domain.VerifyReadTokenRequest;
import com.example.mp.gw.token.domain.VerifyReplaceTokenRequest;
import com.example.mp.gw.token.domain.VerifyTokenRequest;
import com.example.mp.gw.token.exception.ExpiredTokenException;
import com.example.mp.gw.token.service.TokenService;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

/**
 * @Class Name : TokenServiceImpl.java
 * @Description : 토큰 서비스 구현체
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
@Service("TokenService")
public class TokenServiceImpl implements TokenService
{
	@Autowired
	private RedisService<String, RedisQueueDataWrapper<DocumentRd>> redisDocumentRd;

	@Autowired
	private DocumentMapper documentMapper;

	// 열람시 OTT 마감시간(밀리세컨드)
	@Value("${spring.msg.read-ott-expire-ms}")
	private int readOttExpireMs;

	private final String TOKEN_DELIMITER = "|";


	/**
	 * 열람 토큰 생성
	 * @param GenerateReadTokenRequest
	 * @return String
	 */
	@Override
	public String generateReadToken(GenerateReadTokenRequest request)
	{
		/* OTT(One Time Token)생성 */
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MILLISECOND, readOttExpireMs);

		/* OTT(One Time Token) 암호화 (meddageId|regDt|밀리세컨드(현재시간 + OTT만료시간)) */
		String ott = Const.LGUPLUS.LG_UPLUS_TYPE + new String(
															  Base64.getEncoder().encode((new StringBuffer()
																	.append(request.getMessageId())
																	.append(TOKEN_DELIMITER)
																	.append(request.getRegDt())
																	.append(TOKEN_DELIMITER)
																	.append(cal.getTimeInMillis())
																.toString().trim().getBytes()))
															 );

		log.info("{ ■ WEB ■ <열람 토큰 생성 요청> \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(ott, String.class));

		return ott;
	}

	/**
	 * 토큰인증확인 조회(인증)
	 * @param VerifyTokenRequest
	 * @return void
	 * @throws Exception 
	 */
	@Override
	public DocumentRd verifyToken(VerifyTokenRequest request)
	{
		String token = request.getToken();

		long curTimeMs        = 0;		// 현재 시각(밀리세컨드)
		long tokenTimeMs      = 0;		// 토큰에서 추출한 시각(밀리세컨드)

		String tokenMessageId = null;	// 토큰에서 추출한 메시지ID
		String tokenRegDt     = null;	// 토큰에서 추출한 메시지 수신 시간
		String[] tmpArr       = null;

		Decoder decoder = Base64.getDecoder();

		curTimeMs = Calendar.getInstance().getTimeInMillis();

		try
		{
			token = new String(decoder.decode(token.getBytes()));
			tmpArr = token.split("\\" + TOKEN_DELIMITER);

			tokenMessageId = tmpArr[0];
			tokenRegDt     = tmpArr[1];
			tokenTimeMs    = Long.parseLong(tmpArr[2]);

			MDC.put(Const.TRACING_ID, tokenMessageId);

			if (curTimeMs > tokenTimeMs)
			{
				throw new ExpiredTokenException();
			}

			String readDtm = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());

			DocumentRd updateDocumentRequest = DocumentRd.builder()
															       .messageId(tokenMessageId)
															       .regDt(tokenRegDt)
															       .rdDtm(readDtm)
															       .verifyType(Const.DOCUMENT.VERIFY_GUBUN.VERIFY.val())
														 .build();

			log.info("{ ■ WEB ■ <토큰 인증 요청> \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(updateDocumentRequest, DocumentRd.class));

			redisDocumentRd.lpush(
								  Q_KISA_DOC_REG_RD.STRUCTURE_NAME
								, RedisQueueDataWrapper.<DocumentRd>builder()
										.structure_name(Q_KISA_DOC_REG_RD.STRUCTURE_NAME)
										.data(updateDocumentRequest)
								  .build()
								 );

			return updateDocumentRequest;
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	/**
	 * 토큰 인증 열람 확인 결과(열람)
	 * @param VerifyReadTokenRequest
	 * @return Document
	 * @throws Exception 
	 */
	@Override
	@Transactional(readOnly = true)
	public Document verifyReadToken(VerifyReadTokenRequest request) 
	{
		String token = request.getToken();

		String tokenMessageId = null;	// 토큰에서 추출한 메시지ID
		String tokenRegDt     = null;	// 토큰에서 추출한 메시지 수신 시간

		/* OTT 유효성 검증 (S) */
		Decoder decoder = Base64.getDecoder();
		String[] tmpArr = null;

		try
		{
			token = new String(decoder.decode(token.getBytes()));
			tmpArr = token.split("\\" + TOKEN_DELIMITER);

			tokenMessageId = tmpArr[0];
			tokenRegDt     = tmpArr[1];

			MDC.put(Const.TRACING_ID, tokenMessageId);

			Document document = Document.builder()
												  .regDtm(tokenRegDt)
												  .messageId(tokenMessageId)
										.build();

			document = documentMapper.selectDocumentRsvInfoByDocument(document);

			DocumentRd updateDocumentRequest = DocumentRd.builder()
															       .messageId(tokenMessageId)
															       .regDt(tokenRegDt)
															       .rdDtm(request.getMms_rdg_tmst())
															       .verifyType(Const.DOCUMENT.VERIFY_GUBUN.CONFIRM.val())
														 .build();

			log.info("{ ■ WEB ■ <토큰 인증 열람 확인 요청> \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(updateDocumentRequest, DocumentRd.class));

			redisDocumentRd.lpush(
								  Q_KISA_DOC_REG_RD.STRUCTURE_NAME
								, RedisQueueDataWrapper.<DocumentRd>builder()
										.structure_name(Q_KISA_DOC_REG_RD.STRUCTURE_NAME)
										.data(updateDocumentRequest)
								  .build()
								 );

			return document;
		}
		catch (DataAccessException e)
		{
			throw new RuntimeException(e);
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	/**
	 * 토큰 인증 대체(대체)
	 * @param VerifyReplaceTokenRequest
	 * @return void
	 * @throws Exception 
	 */
	public void verifyReplaceToken(VerifyReplaceTokenRequest request)
	{
		try
		{
			DocumentRd updateDocumentRequest = DocumentRd.builder()
															       .messageId(request.getMessageId())
															       .regDt(request.getRegDt())
															       .rdDtm(request.getMms_rdg_tmst())
															       .verifyType(Const.DOCUMENT.VERIFY_GUBUN.REPLACE.val())
														 .build();

			log.info("{ ■ WEB ■ <토큰 인증 대체 요청> \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(updateDocumentRequest, DocumentRd.class));

			redisDocumentRd.lpush(
								  Q_KISA_DOC_REG_RD.STRUCTURE_NAME
								, RedisQueueDataWrapper.<DocumentRd>builder()
										.structure_name(Q_KISA_DOC_REG_RD.STRUCTURE_NAME)
										.data(updateDocumentRequest)
								  .build()
								 );
		}
		catch (Exception e)
		{
			throw e;
		}
	}
}
