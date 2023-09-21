package com.example.mp.gw.kisa.service.impl;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.message.internal.DataSourceProvider.ByteArrayDataSource;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.domain.RedisQueueDataWrapper;
import com.example.mp.gw.common.domain.Const.FORMATTER;
import com.example.mp.gw.common.domain.RedisStructure.H_KISA_SESSION;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_REG;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_REG_RD_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_REG_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_DOC_RSLT;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_RETRY;
import com.example.mp.gw.common.domain.RedisStructure.Q_KISA_SESSION;
import com.example.mp.gw.common.exception.KeepRunningToWorkerException;
import com.example.mp.gw.common.exception.StopRunningToWorkerException;
import com.example.mp.gw.common.service.RedisService;
import com.example.mp.gw.common.utils.StringUtil;
import com.example.mp.gw.doc.domain.Document;
import com.example.mp.gw.doc.domain.DocumentFail;
import com.example.mp.gw.doc.mappers.altibase.DocumentMapper;
import com.example.mp.gw.kisa.domain.IssueDocumentRequest;
import com.example.mp.gw.kisa.domain.IssueDocumentResponse;
import com.example.mp.gw.kisa.domain.IssueDocumentStatConfirmationRequest;
import com.example.mp.gw.kisa.domain.IssueDocumentStatConfirmationResponse;
import com.example.mp.gw.kisa.domain.KisaAccessTokenResponse;
import com.example.mp.gw.kisa.domain.RegisterDocumentCirculationRequest;
import com.example.mp.gw.kisa.domain.RegisterDocumentCirculationResponse;
import com.example.mp.gw.kisa.domain.RegisterDocumentCirculationsRequest;
import com.example.mp.gw.kisa.domain.RegisterDocumentCirculationsResponse;
import com.example.mp.gw.kisa.exception.FailRequestAccessTokenToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestDocumentIssueToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestDocumentRegisterRdToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestDocumentRegisterToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestDocumentStatsConfirmationIssueToKisaException;
import com.example.mp.gw.kisa.exception.FailRequestToKisaException;
import com.example.mp.gw.kisa.exception.FailResponseDocumentIssueToKisaException;
import com.example.mp.gw.kisa.exception.FailResponseDocumentRegisterRdToKisaException;
import com.example.mp.gw.kisa.exception.FailResponseDocumentRegisterToKisaException;
import com.example.mp.gw.kisa.exception.FailResponseDocumentStatsConfirmationIssueToKisaException;
import com.example.mp.gw.kisa.exception.FailResponseToKisaException;
import com.example.mp.gw.kisa.exception.FailResponseUnKnownToKisaException;
import com.example.mp.gw.kisa.exception.NotExistDocumentStatToKisaException;
import com.example.mp.gw.kisa.exception.NotExistDocumentToKisaException;
import com.example.mp.gw.kisa.exception.NotExistTokenToKisaException;
import com.example.mp.gw.kisa.exception.NotMatchResponseCountToKisaException;
import com.example.mp.gw.kisa.service.KisaDocumentService;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service("KisaDocumentService")
public class KisaDocumentServiceImpl implements KisaDocumentService
{
	@Autowired
	private KisaDocumentService kisaDocumentService;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Document>> redisSendDocumentToKisa;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Document>> redisSendDocumentRdToKisa;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Document>> redisUpdateDocumentRslt;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<Document>> redisRetryDocumentToKisa;

	@Autowired
	private RedisService<String, KisaAccessTokenResponse> redisKisaToken;

	@Autowired
	private DocumentMapper documentMapper;

	@Value("${spring.kisa.hostname}")
	private String HOSTNAME;

	@Value("${spring.kisa.url.doc-reg}")
	private String DOC_REG_URL;

	@Value("${spring.kisa.url.doc-iss}")
	private String DOC_ISS_URL;

	@Value("${spring.kisa.url.doc-iss-sts-cfm}")
	private String DOC_ISS_STS_CFM_URL;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	@Qualifier(value = "kcesHeaders")
	private HttpHeaders kcesHeaders;


	/**
	 * KISA 유통정보 등록 전송 (G/W -> KISA)
	 * @param int
	 * @return void
	 */
	@Override
	public void popAndSendDocToKisa(int KISA_DCMNT_SEND_CNT)
	{
		RedisQueueDataWrapper<Document>       request  = null;
		List<RedisQueueDataWrapper<Document>> requests = new ArrayList<RedisQueueDataWrapper<Document>>();

		List<RedisQueueDataWrapper<Document>> rslt  = new ArrayList<RedisQueueDataWrapper<Document>>();
		List<RedisQueueDataWrapper<Document>> retry = new ArrayList<RedisQueueDataWrapper<Document>>();

		Document       document  = null;
		List<Document> documents = new ArrayList<Document>();

		try
		{
			// 1. KISA 유통정보(등록, 등록 + 열람일시) 전송 데이터 생성
			for (int i = 0; i < KISA_DCMNT_SEND_CNT; i++)
			{
				// 1.1 큐에서 KISA 유통정보(등록, 등록 + 열람일시) 전송 요청 데이터 읽기
				request = redisSendDocumentToKisa.rpop(Q_KISA_DOC_REG_TO_SEND.STRUCTURE_NAME);

				if (null == request)
					break;

				document = (Document) request.getData();

				if (null == document)
					break;

				// 1.2 로그 출력
				log.info("{ ■ KISA 유통정보 등록 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, document.getMessageId(), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(document, Document.class));

				// 1.3 테스트 발송 시 KISA 연동 제외
				if (true == Const.Y.equals(document.getTestSndnYn()))
				{
					document.setDcmntRslt(Const.DOCUMENT.DCMNT_RSLT.SUCCESS.val());
					document.setDcmntRsltCd(Const.DOCUMENT.DCMNT_RSLT_CD.SUCCESS.val());
					document.setDcmntRsltDtm(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));
				}

				// 1.4 KISA 전송 결과 업데이트 대상 추가
				requests.add(request);

				// 1.5 KISA 유통정보 연동 성공 이후 예외로 다시 진입하는 경우 전송 대상에서 제외 하고 DB 결과만 업데이트
				if (false == Const.DOCUMENT.DCMNT_RSLT.SUCCESS.val().equals(document.getDcmntRslt()))					
					documents.add(document);
			}

			// 2. KISA 유통정보 등록 전송
			if (false == documents.isEmpty())
				kisaDocumentService.registerDocumentMulti(documents);

			// 3. 유통정보 연동 결과 등록 요청
			for (RedisQueueDataWrapper<Document> req: requests)
			{
				// 3.1 전자문서 수신시점에 수신자 공인전자주소가 존재하지 않는 경우
				if (Const.DOCUMENT.DCMNT_RSLT_CD.RSV_CEA.val().equals(req.getData().getDcmntRsltCd()))
				{
					req.setSndDtm();
					req.setStructure_name(Q_KISA_DOC_REG_TO_SEND.STRUCTURE_NAME);
					req.getData().setDcmntRslt(Const.DOCUMENT.DCMNT_RSLT.FAIL.val());

					retry.add(req);
				}
				else
				{
					req.setStructure_name(Q_KISA_DOC_RSLT.STRUCTURE_NAME);

					rslt.add(req);
				}
			}

			// 4. 재전송 처리
			if (false == retry.isEmpty())
				redisUpdateDocumentRslt.lpushT(Q_KISA_RETRY.STRUCTURE_NAME, retry);

			// 5. 최종 결과 처리
			if (false == rslt.isEmpty())
				redisUpdateDocumentRslt.lpushT(Q_KISA_DOC_RSLT.STRUCTURE_NAME, rslt);
		}
		catch (FailRequestDocumentRegisterToKisaException e)
		{
			log.error("{ ■ KISA 유통정보 등록 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(documents, List.class), e.getClass().getName(), e.getMessage());

			if (null != requests && false == requests.isEmpty())
			{
				for (int i = 0; i < requests.size(); i++)
				{
					requests.get(i).setSndDtm();
					requests.get(i).setStructure_name(Q_KISA_DOC_REG_TO_SEND.STRUCTURE_NAME);
				}

				redisRetryDocumentToKisa.lpushT(Q_KISA_RETRY.STRUCTURE_NAME, requests);
			}
		}
		catch (Exception e)
		{
			log.error("{ ■ KISA 유통정보 등록 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(requests, List.class), e.getClass().getName(), e.getMessage());

			if (null != requests && false == requests.isEmpty())
			{
				for (int i = 0; i < requests.size(); i++)
				{
					requests.get(i).setSndDtm();
					requests.get(i).setStructure_name(Q_KISA_DOC_REG_TO_SEND.STRUCTURE_NAME);
				}

				redisRetryDocumentToKisa.lpushT(Q_KISA_RETRY.STRUCTURE_NAME, requests);
			}

			throw e;
		}
	}

	/**
	 * KISA 유통정보 열람일시 등록 전송 (G/W -> KISA)
	 * @param int
	 * @return void
	 */
	@Override
	public void popAndSendDocRdToKisa(int KISA_DCMNT_RD_SEND_CNT)
	{
		RedisQueueDataWrapper<Document>          request  = null;
		List<RedisQueueDataWrapper<Document>>    requests = new ArrayList<RedisQueueDataWrapper<Document>>();

		Document          document  = null;
		List<Document>    documents = new ArrayList<Document>();

		try
		{
			// 1. KISA 유통정보(열람일시) 전송 데이터 생성
			for (int i = 0; i < KISA_DCMNT_RD_SEND_CNT; i++)
			{
				// 1.1 큐에서 KISA 유통정보(등록, 열람일시) 전송 요청 데이터 읽기
				request = redisSendDocumentRdToKisa.rpop(Q_KISA_DOC_REG_RD_TO_SEND.STRUCTURE_NAME);

				if (null == request)
					break;

				document = (Document) request.getData();

				if (null == document)
					break;

				// 1.2 로그 출력
				log.info("{ ■ KISA 유통정보 열람일시 등록 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, document.getMessageId(), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(document, Document.class));

				// 1.3 KISA 연동 제외
				//	   테스트 발송
				//     알림서비스(열람일시를 생성하지 않는 알림서비스) 
				if (true == Const.Y.equals(document.getTestSndnYn())
				 || Const.DOCUMENT.DOC_TYPE.NOTICE.val() == document.getDocType()
				   )
				{
					document.setDcmntRslt(Const.DOCUMENT.DCMNT_RSLT.SUCCESS.val());
					document.setDcmntRsltCd(Const.DOCUMENT.DCMNT_RSLT_CD.SUCCESS.val());
					document.setDcmntRsltDtm(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));
				}

				// 1.4 KISA 전송 결과 업데이트 대상 추가
				requests.add(request);

				// 1.5 연동 완료 이후 발생한 예외로 다시 진입하는 경우 전송 대상에서 제외
				if (false == Const.DOCUMENT.DCMNT_RSLT.SUCCESS.val().equals(document.getDcmntRslt()))					
					documents.add(document);
			}

			// 2. KISA 유통정보 열람일시 등록 전송
			if (false == documents.isEmpty())
				kisaDocumentService.registerDocumentRdMulti(documents);

			// 3. 유통정보 열람일시 연동 결과 등록 요청(MP_DCMNT_INFO 테이블 Update)
			if (false == requests.isEmpty())
			{
				for(int i = 0; i < requests.size(); i++)
					requests.get(i).setStructure_name(Q_KISA_DOC_RSLT.STRUCTURE_NAME);

				redisUpdateDocumentRslt.lpushT(Q_KISA_DOC_RSLT.STRUCTURE_NAME, requests);
			}
		}
		catch (FailRequestDocumentRegisterRdToKisaException e)
		{
			log.error("{ ■ KISA 유통정보 열람일시 등록 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(documents, List.class), e.getClass().getName(), e.getMessage());

			if (null != requests && false == requests.isEmpty())
			{
				for (int i = 0; i < requests.size(); i++)
				{
					requests.get(i).setSndDtm();
					requests.get(i).setStructure_name(Q_KISA_DOC_REG_RD_TO_SEND.STRUCTURE_NAME);
				}

				redisRetryDocumentToKisa.lpushT(Q_KISA_RETRY.STRUCTURE_NAME, requests);
			}
		}
		catch (Exception e)
		{
			log.error("{ ■ KISA 유통정보 열람일시 등록 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(requests, List.class), e.getClass().getName(), e.getMessage());

			if (null != requests && false == requests.isEmpty())
			{
				for (int i = 0; i < requests.size(); i++)
				{
					requests.get(i).setSndDtm();
					requests.get(i).setStructure_name(Q_KISA_DOC_REG_RD_TO_SEND.STRUCTURE_NAME);
				}

				redisRetryDocumentToKisa.lpushT(Q_KISA_RETRY.STRUCTURE_NAME, requests);
			}

			throw e;
		}
	}

	/**
	 * KISA 유통정보 전송(등록/열람일시) 결과 업데이트
	 * @return void
	 * @throws KeepRunningToWorkerException
	 * @throws StopRunningToWorkerException
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void popAndUpdateDocRslt() throws KeepRunningToWorkerException, StopRunningToWorkerException
	{
		RedisQueueDataWrapper<Document> request = null;
		Document                        doc     = null;

		try
		{
			// 1. 큐에서 KISA 유통정보 전송(등록/열람일시) 결과 업데이트 요청 읽기
			request = redisUpdateDocumentRslt.rpop(Q_KISA_DOC_RSLT.STRUCTURE_NAME);

			if (null == request)
				return;

			doc = (Document) request.getData();

			if (null == doc)
				return;

			MDC.put(Const.TRACING_ID, doc.getMessageId());

			log.info("{ ■ KISA 유통정보 결과 등록 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(doc, Document.class));

			// 2. 유통정보 등록 결과가 중복("02") 등록이면 종료
			if (true == Const.DOCUMENT.DCMNT_RSLT_CD.DUPLICATE.val().equals(doc.getDcmntRsltCd()))
				return;

			// 3. 유통정보 테이블(MP_DCMNT_INFO) 결과 Update
			documentMapper.updateDocumentRslt(doc);

			// 4. KISA 연동 성공("01") 결과 처리
			if (true == Const.DOCUMENT.DCMNT_RSLT_CD.SUCCESS.val().equals(doc.getDcmntRsltCd()))
			{
				// 유통정보 재전송 테이블(MP_DCMNT_RETRY) 대상 삭제
				documentMapper.deleteDocumentReqFail(doc);
			}
			else
			{
				// 유통정보 문서 상태에 따른 목적지 큐(redis) 설정
				String structName = null;

				if (true == Const.DOCUMENT.DCMNT_STAT.HYBRID.val().equals(doc.getDcmntStat()))
				{
					// 하이브리드 요청(등록+열람일시)
					structName = Q_KISA_DOC_REG_TO_SEND.STRUCTURE_NAME;
				}
				else if (true == Const.DOCUMENT.DCMNT_STAT.REGISTER.val().equals(doc.getDcmntStat()))
				{
					// 등록 요청(등록)
					structName = Q_KISA_DOC_REG_TO_SEND.STRUCTURE_NAME;
				}
				else if (true == Const.DOCUMENT.DCMNT_STAT.UPDATE.val().equals(doc.getDcmntStat()))
				{
					// 열람일시 요청(열람일시)
					structName = Q_KISA_DOC_REG_RD_TO_SEND.STRUCTURE_NAME;
				}
				else if (true == Const.DOCUMENT.DCMNT_STAT.MAKE.val().equals(doc.getDcmntStat()))
				{
					// 생성 상태(예외)
					structName = Q_KISA_DOC_REG.STRUCTURE_NAME;
				}
				else
				{
					// 기타(예외)
					structName = doc.getDcmntStat();
				}

				// 유통정보 재전송 테이블(MP_DCMNT_REQ_FAIL) 저장
				documentMapper.insertDocumentReqFail(
													 DocumentFail.builder()
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
								   						.dcmntRslt(doc.getDcmntRslt())
								   						.dcmntRsltCd(doc.getDcmntRsltCd())
								   						.dcmntRsltDtm(doc.getDcmntRsltDtm())
								   						.regDtm(doc.getRegDtm())
								   						.partMm(doc.getPartMm())
								   						.failStructureName(structName)
								   						.failSndCnt(0)
								   						.failSndDtm("")
								   						.failRegDtm(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
								   						.testSndnYn(doc.getTestSndnYn())
								   						.docType(doc.getDocType())
									   				 .build()
													);
			}
		}
		catch (DataAccessException e)
		{
			SQLException se = (SQLException)e.getCause();

			log.error("{ ■ KISA 유통정보 결과 등록 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"se.errorName\": \"{}\", \"se.message\": \"{}\", \"se.code\": \"{}\", \"se.slate\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(doc, Document.class), e.getClass().getName(), e.getMessage(), se.getClass().getName(), se.getMessage(), se.getErrorCode(), se.getSQLState());

			request.setSndDtm();
			redisRetryDocumentToKisa.lpush(Q_KISA_RETRY.STRUCTURE_NAME, request);

			if (e instanceof DataIntegrityViolationException )
				throw new KeepRunningToWorkerException();
			else
				throw new StopRunningToWorkerException();
		}
		catch(Exception e)
		{
			log.error("{ ■ KISA 유통정보 결과 등록 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class), e.getClass().getName(), e.getMessage());

			if (null != request)
			{
				request.setSndDtm();
				redisRetryDocumentToKisa.lpush(Q_KISA_RETRY.STRUCTURE_NAME, request);
			}

			throw new StopRunningToWorkerException();
		}
	}

	// KISA 공인전자문서 유통정보 등록(단건)
	@Override
	public RegisterDocumentCirculationsResponse registerDocument(Document document) throws FailRequestDocumentRegisterToKisaException, FailResponseDocumentRegisterToKisaException
	{
		HttpEntity<RegisterDocumentCirculationsRequest>      request  = null;
		ResponseEntity<RegisterDocumentCirculationsResponse> response = null;

		String REQUEST_TYPE = "";

		// 1. 등록, 열람일시 갱신, 등록 + 열람일시 갱신 여부 확인
		if (true == document.getDcmntStat().equals(Const.DOCUMENT.DCMNT_STAT.REGISTER.val()))
			REQUEST_TYPE = "(등록)";
		else if (true == document.getDcmntStat().equals(Const.DOCUMENT.DCMNT_STAT.HYBRID.val()))
			REQUEST_TYPE = "(등록 + 열람일시)";
		else
			REQUEST_TYPE = "";

		document.setDcmntRslt(Const.DOCUMENT.DCMNT_RSLT.FAIL.val());
		document.setDcmntRsltCd(Const.DOCUMENT.DCMNT_RSLT_CD.FAIL.val());
		document.setDcmntRsltDtm("");

		try
		{
			KisaAccessTokenResponse hashToken = redisKisaToken.hmget(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME);

			if (null == hashToken)
				throw new NotExistTokenToKisaException();

			kcesHeaders.setBearerAuth(hashToken.getAccessToken());

			kcesHeaders.set("req-UUID", UUID.randomUUID().toString());
			kcesHeaders.set("req-date", StringUtil.yyyyMMddHHmmss(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis())));

			RegisterDocumentCirculationRequest registerDocCirculationRequest = new RegisterDocumentCirculationRequest();

			registerDocCirculationRequest.setEdocNum(document.getElctrcDcmntNo());
			registerDocCirculationRequest.setSubject(document.getElctrcDcmntSbjct());
			registerDocCirculationRequest.setSendEaddr(document.getSndCea());
			registerDocCirculationRequest.setSendSubEaddr(document.getSndSubCea());
			registerDocCirculationRequest.setRecvEaddr(document.getRsvCea());
			registerDocCirculationRequest.setRecvSubEaddr(document.getRsvSubCea());
			registerDocCirculationRequest.setSendPlatformId(document.getSndPlfmId());
			registerDocCirculationRequest.setRecvPlatformId(document.getRsvPlfmId());
			registerDocCirculationRequest.setSendDate(StringUtil.yyyyMMddHHmmss(document.getSndDtm()));
			registerDocCirculationRequest.setRecvDate(StringUtil.yyyyMMddHHmmss(document.getRsvDtm()));
			registerDocCirculationRequest.setReadDate(StringUtils.hasText(document.getRdDtm())?StringUtil.yyyyMMddHHmmss(document.getRdDtm()):null);
			registerDocCirculationRequest.setDocType(document.getDocType());
			registerDocCirculationRequest.setContentHash(StringUtil.toStringFrom(document.getCntntHash()));
			registerDocCirculationRequest.setMessageId(document.getMessageId());

			if (true == StringUtils.hasText(document.getFileHash()))
			{
				List<String> list = new ArrayList<String>();
				list.add(document.getFileHash());
				registerDocCirculationRequest.setFileHashes(list);
			}

			RegisterDocumentCirculationsRequest registerDocCirculationsRequest = new RegisterDocumentCirculationsRequest();
			List<RegisterDocumentCirculationRequest> list = new ArrayList<RegisterDocumentCirculationRequest>();
			list.add(registerDocCirculationRequest);
			registerDocCirculationsRequest.setCirculations(list);

			request = new HttpEntity<>(registerDocCirculationsRequest, kcesHeaders);

			log.info("{ ■ KISA ■ <전자문서 유통정보{} 등록 요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"header\": \"{}\", \"body\": \"{}\" }"
					, REQUEST_TYPE, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + DOC_REG_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), RegisterDocumentCirculationsRequest.class)
					);

			response = restTemplate.exchange(HOSTNAME + DOC_REG_URL, HttpMethod.POST, request, RegisterDocumentCirculationsResponse.class);

			if (0 == response.getBody().getResultCode())
			{
				if (true == response.getBody().getErrCode().contains("ERR-01-103")
				 || true == response.getBody().getErrCode().contains("ERR-01-104")
				   )
				{
					throw new FailRequestAccessTokenToKisaException();
				}

				throw new FailRequestToKisaException();
			}

			response.getBody().getCirculations().get(0).setMessageId(document.getMessageId());

			log.info("{ ■ KISA ■ <전자문서 유통정보{} 등록 응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"body\": \"{}\" }"
					, REQUEST_TYPE, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), RegisterDocumentCirculationsResponse.class)
					);

			if (1 == response.getBody().getResultCode())
			{
				document.setDcmntRslt(Const.DOCUMENT.DCMNT_RSLT.SUCCESS.val());
				document.setDcmntRsltDtm(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));

				RegisterDocumentCirculationResponse registerDocCirculationResponse = response.getBody().getCirculations().get(0);

				String resultCd = Integer.toString(registerDocCirculationResponse.getResult());

				document.setDcmntRsltCd(resultCd.length() == 1 ? "0" + resultCd : resultCd);

				if (1 != registerDocCirculationResponse.getResult())
				{
					/* 유통정보 등록
					* result는 각 유통정보에 등록결과 값을 나타낸다
					- 1: 등록성공
					- 2: 이미등록된 유통정보 
					- 3: 전자문서유통 시각정보가 역전된 경우
					- 4: 전자문서 송신시점에 송신자 공인전자주소가 존재하지 않는 경우
					- 5: 전자문서 수신시점에 수신자 공인전자주소가 존재하지 않는 경우
					- 6: 등록되지 않은 송신중계자 플랫폼ID
					- 7: 등록되지 않은 수신중계자 플랫폼ID
					- 9: 기타오류
					*/

					log.info("{ ■ KISA ■ <전자문서 유통정보{} 등록 응답 - FAIL> \"{}\": \"{}\", \"type\": \"response\", \"body\": \"{}\" }", REQUEST_TYPE, Const.TRACING_ID,MDC.get(Const.TRACING_ID), new Gson().toJson(registerDocCirculationResponse, RegisterDocumentCirculationResponse.class));

					throw new FailResponseToKisaException();
				}
			}

			return response.getBody();
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보{} 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"statusCode\": \"{}\", \"message\": \"{}\" }", REQUEST_TYPE, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestDocumentRegisterToKisaException(e);
		}
		catch (NotExistTokenToKisaException | FailRequestAccessTokenToKisaException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보{} 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", REQUEST_TYPE, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisKisaToken.lpush(
								 Q_KISA_SESSION.STRUCTURE_NAME, KisaAccessTokenResponse.builder()
									.errLocation(new Object() {}.getClass().getEnclosingMethod().getName())
									.errMsg(e.getMessage())
									.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
								 .build()
								);

			throw new FailRequestDocumentRegisterToKisaException(e);
		}
		catch (FailRequestToKisaException e)
		{
			log.debug("{ ■ KISA ■ <전자문서 유통정보{} 등록 요청> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"body\": \"{}\" }"
					, REQUEST_TYPE, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), RegisterDocumentCirculationsResponse.class)
					);

			log.error("{ ■ KISA ■ <전자문서 유통정보{} 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"body\": \"{}\" }", REQUEST_TYPE, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), new Gson().toJson(response.getBody(), RegisterDocumentCirculationsResponse.class));

			throw new FailRequestDocumentRegisterToKisaException(e);
		}
		catch (FailResponseToKisaException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보{} 등록 응답 - FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"resultCode\": \"{}\" ,\"errCode\": \"{}\", \"errMsg\": \"{}\" }", REQUEST_TYPE, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), response.getBody().getResultCode(), response.getBody().getErrCode(), response.getBody().getErrMsg());

			throw new FailResponseDocumentRegisterToKisaException(e);
		}
		catch(Exception e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보{} 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", REQUEST_TYPE, Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	// KISA 공인전자문서 유통정보 등록(다건)
	@Override
	public List<RegisterDocumentCirculationResponse> registerDocumentMulti(List<Document> document) throws FailRequestDocumentRegisterToKisaException
	{
		HttpEntity<RegisterDocumentCirculationsRequest>      request  = null;
		ResponseEntity<RegisterDocumentCirculationsResponse> response = null;

		try
		{
			KisaAccessTokenResponse hashToken = redisKisaToken.hmget(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME);

			if (null == hashToken)
				throw new NotExistTokenToKisaException();

			kcesHeaders.setBearerAuth(hashToken.getAccessToken());

			kcesHeaders.set("req-UUID", UUID.randomUUID().toString());
			kcesHeaders.set("req-date", StringUtil.yyyyMMddHHmmss(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis())));

			RegisterDocumentCirculationsRequest registerDocCirculationsRequest = new RegisterDocumentCirculationsRequest();

			List<RegisterDocumentCirculationRequest> circulations = new ArrayList<RegisterDocumentCirculationRequest>();

			for (Document doc : document)
			{
				String readDate = null;

				doc.setDcmntRslt(Const.DOCUMENT.DCMNT_RSLT.FAIL.val());
				doc.setDcmntRsltCd(Const.DOCUMENT.DCMNT_RSLT_CD.FAIL.val());
				doc.setDcmntRsltDtm("");

				List<String> cntnHash = new ArrayList<String>();

				if (true == StringUtils.hasText(doc.getRdDtm()))
				
				if (true == StringUtils.hasText(doc.getFileHash()))
					cntnHash.add(doc.getFileHash());

				// 열람일시 생성 케이스
				// 1. Hybrid
				// 2. 알림서비스(2:열람일시 미생성)가 아닌 경우(0:일반문서, 알림서비스(1:열람일시 생성))
				if (true == Const.DOCUMENT.DCMNT_STAT.HYBRID.val().equals(doc.getDcmntStat())
				 && Const.DOCUMENT.DOC_TYPE.NOTICE.val() != doc.getDocType()
				   )
				{
					readDate = StringUtil.yyyyMMddHHmmss(doc.getRdDtm());
				}

				circulations.add(
								 RegisterDocumentCirculationRequest.builder()
								  		.edocNum(doc.getElctrcDcmntNo())
								  		.subject(doc.getElctrcDcmntSbjct())
								  		.sendEaddr(doc.getSndCea())
								  		.sendSubEaddr(doc.getSndSubCea())
								  		.recvEaddr(doc.getRsvCea())
								  		.recvSubEaddr(doc.getRsvSubCea())
								  		.sendPlatformId(doc.getSndPlfmId())
								  		.recvPlatformId(doc.getRsvPlfmId())
								  		.sendDate(StringUtil.yyyyMMddHHmmss(doc.getSndDtm()))
								  		.recvDate(StringUtil.yyyyMMddHHmmss(doc.getRsvDtm()))
								  		.readDate(readDate)
								  		.docType(doc.getDocType())
								  		.contentHash(StringUtil.toStringFrom(doc.getCntntHash()))
								  		.fileHashes(cntnHash)
								  		.messageId(doc.getMessageId())
								 .build()
								);
			}

			registerDocCirculationsRequest.setCirculations(circulations);

			request = new HttpEntity<>(registerDocCirculationsRequest, kcesHeaders);

			log.info("{ ■ KISA ■ <전자문서 유통정보 등록 요청> \"{}\": \"{}\", \"type\": \"request\", \"url\": \"{}\", \"header\": \"{}\", \"body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + DOC_REG_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), RegisterDocumentCirculationsRequest.class)
					);

			response = restTemplate.exchange(HOSTNAME + DOC_REG_URL, HttpMethod.POST, request, RegisterDocumentCirculationsResponse.class);

			if (0 == response.getBody().getResultCode())
			{
				if (true == response.getBody().getErrCode().contains("ERR-01-103")
				 || true == response.getBody().getErrCode().contains("ERR-01-104")
				   )
				{
					throw new FailRequestAccessTokenToKisaException();
				}

				throw new FailRequestToKisaException();
			}

			if (circulations.size() != response.getBody().getCirculations().size())
				throw new NotMatchResponseCountToKisaException();

			for (int i = 0; i < document.size(); i++ )
				response.getBody().getCirculations().get(i).setMessageId(document.get(i).getMessageId());

			log.info("{ ■ KISA ■ <전자문서 유통정보 등록 응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), RegisterDocumentCirculationsResponse.class)
					);

			String nowDtm = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());

			for (int i = 0; i < document.size(); i++ )
			{
				RegisterDocumentCirculationResponse registerDocCirculationResponse = response.getBody().getCirculations().get(i);

				String resultCd = Integer.toString(registerDocCirculationResponse.getResult());

				document.get(i).setDcmntRslt(Const.DOCUMENT.DCMNT_RSLT.SUCCESS.val());
				document.get(i).setDcmntRsltCd(resultCd.length() == 1 ? "0" + resultCd : resultCd);
				document.get(i).setDcmntRsltDtm(nowDtm);

				if (1 != registerDocCirculationResponse.getResult())
				{
					/* 유통 열람일시 등록
					* result는 각 유통정보에 등록결과 값을 나타낸다
					- 1: 등록성공
					- 2: 이미등록된 유통정보 
					- 3: 전자문서유통 시각정보가 역전된 경우
					- 4: 전자문서 송신시점에 송신자 공인전자주소가 존재하지 않는 경우
					- 5: 전자문서 수신시점에 수신자 공인전자주소가 존재하지 않는 경우
					- 6: 등록되지 않은 송신중계자 플랫폼ID
					- 7: 등록되지 않은 수신중계자 플랫폼ID
					- 9: 기타오류
					*/

					log.info("{ ■ KISA ■ <전자문서 유통정보 등록 응답 - FAIL> \"{}\": \"{}\", \"type\": \"response\", \"body\": \"{}\" }"
							, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
							, new Gson().toJson(registerDocCirculationResponse, RegisterDocumentCirculationResponse.class)
							);
				}
			}

			return response.getBody().getCirculations();
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"statusCode\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestDocumentRegisterToKisaException(e);
		}
		catch (NotExistTokenToKisaException | FailRequestAccessTokenToKisaException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisKisaToken.lpush(
								 Q_KISA_SESSION.STRUCTURE_NAME, KisaAccessTokenResponse.builder()
								 	.errLocation(new Object() {}.getClass().getEnclosingMethod().getName())
								 	.errMsg(e.getMessage())
								 	.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
								 .build()
								);

			throw new FailRequestDocumentRegisterToKisaException(e);
		}
		catch (NotMatchResponseCountToKisaException e)
		{
			log.debug("{ ■ KISA ■ <전자문서 유통정보 등록 응답 - ERROR> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), RegisterDocumentCirculationsResponse.class)
					);

			log.error("{ ■ KISA ■ <전자문서 유통정보 등록 응답 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"body\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), new Gson().toJson(response.getBody(), RegisterDocumentCirculationsResponse.class));

			throw new FailRequestDocumentRegisterToKisaException(e);
		}
		catch (FailRequestToKisaException e)
		{
			log.debug("{ ■ KISA ■ <전자문서 유통정보 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), RegisterDocumentCirculationsResponse.class)
					);

			log.error("{ ■ KISA ■ <전자문서 유통정보 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"body\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), new Gson().toJson(response.getBody(), RegisterDocumentCirculationsResponse.class));

			throw new FailRequestDocumentRegisterToKisaException(e);
		}
		catch(Exception e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), new Gson().toJson(document, List.class));

			throw e;
		}
	}

	// KISA 공인전자문서 유통정보 열람일시 등록(단건)
	@Override
	public RegisterDocumentCirculationsResponse registerDocumentRd(Document document) throws FailRequestDocumentRegisterRdToKisaException, FailResponseDocumentRegisterRdToKisaException
	{
		HttpEntity<RegisterDocumentCirculationsRequest>      request  = null;
		ResponseEntity<RegisterDocumentCirculationsResponse> response = null;

		document.setDcmntRslt(Const.DOCUMENT.DCMNT_RSLT.FAIL.val());
		document.setDcmntRsltCd(Const.DOCUMENT.DCMNT_RSLT_CD.FAIL.val());
		document.setDcmntRsltDtm("");

		try
		{
			KisaAccessTokenResponse hashToken = redisKisaToken.hmget(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME);

			if (null == hashToken)
				throw new NotExistTokenToKisaException();

			kcesHeaders.setBearerAuth(hashToken.getAccessToken());
			kcesHeaders.set("req-UUID", UUID.randomUUID().toString());
			kcesHeaders.set("req-date", StringUtil.yyyyMMddHHmmss(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis())));
			
			RegisterDocumentCirculationRequest registerDocCirculationRequest = new RegisterDocumentCirculationRequest();

			registerDocCirculationRequest.setEdocNum(document.getElctrcDcmntNo());
			registerDocCirculationRequest.setSubject(null);
			registerDocCirculationRequest.setSendEaddr(null);
			registerDocCirculationRequest.setSendSubEaddr(null);
			registerDocCirculationRequest.setRecvEaddr(null);
			registerDocCirculationRequest.setRecvSubEaddr(null);
			registerDocCirculationRequest.setSendPlatformId(null);
			registerDocCirculationRequest.setRecvPlatformId(null);
			registerDocCirculationRequest.setSendDate(null);
			registerDocCirculationRequest.setRecvDate(null);
			registerDocCirculationRequest.setReadDate(StringUtil.yyyyMMddHHmmss(document.getRdDtm()));
			registerDocCirculationRequest.setDocType(null);
			registerDocCirculationRequest.setContentHash(null);
			registerDocCirculationRequest.setFileHashes(null);
			registerDocCirculationRequest.setMessageId(document.getMessageId());

			RegisterDocumentCirculationsRequest registerDocCirculationsRequest = new RegisterDocumentCirculationsRequest();
			List<RegisterDocumentCirculationRequest> list = new ArrayList<RegisterDocumentCirculationRequest>();
			list.add(registerDocCirculationRequest);
			registerDocCirculationsRequest.setCirculations(list);

			request = new HttpEntity<>(registerDocCirculationsRequest,kcesHeaders);

			log.info("{ ■ KISA ■ <전자문서 유통정보 열람일시 등록 요청> \"{}\": \"{}\", \"type\": , \"request\", \"url\": \"{}\", \"header\": \"{}\", \"body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + DOC_REG_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), RegisterDocumentCirculationsRequest.class)
					);

			response = restTemplate.exchange(HOSTNAME + DOC_REG_URL, HttpMethod.PATCH, request, RegisterDocumentCirculationsResponse.class);

			if (0 == response.getBody().getResultCode())
			{
				if (true == response.getBody().getErrCode().contains("ERR-01-103")
				 || true == response.getBody().getErrCode().contains("ERR-01-104")
				   )
				{
					throw new FailRequestAccessTokenToKisaException();
				}

				throw new FailRequestToKisaException();
			}

			response.getBody().getCirculations().get(0).setMessageId(document.getMessageId());

			log.info("{ ■ KISA ■ <전자문서 유통정보 열람일시 등록 응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), RegisterDocumentCirculationsResponse.class)
					);

			if (1== response.getBody().getResultCode())
			{
				document.setDcmntRslt(Const.DOCUMENT.DCMNT_RSLT.SUCCESS.val());
				document.setDcmntRsltDtm(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()));

				RegisterDocumentCirculationResponse registerDocCirculationResponse = response.getBody().getCirculations().get(0);

				String resultCd = Integer.toString(registerDocCirculationResponse.getResult());

				document.setDcmntRsltCd(resultCd.length() == 1 ? "0" + resultCd : resultCd);

				if (1 != registerDocCirculationResponse.getResult())
				{
					/* 유통 열람일시 등록
					* result는 각 유통정보에 등록결과 값을 나타낸다
					- 1: 등록성공
					- 2: 열람일시가 이미 등록된 경우
					- 3: 열람일시가 수신일시보다 빠른 경우(시각역전)
					- 4: 전자문서번호가 등록되지 않은 경우
					- 9: 기타오류
					*/
					
					log.info("{ ■ KISA ■ <전자문서 유통정보 열람일시 등록 응답 - FAIL> \"{}\": \"{}\", \"type\": \"response\", \"body\": \"{}\" }"
							, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
							, new Gson().toJson(registerDocCirculationResponse, RegisterDocumentCirculationResponse.class)
							);
					
					throw new FailResponseToKisaException();
				}
			}

			return response.getBody();
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보 열람일시 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"statusCode\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestDocumentRegisterRdToKisaException(e);
		}
		catch (NotExistTokenToKisaException | FailRequestAccessTokenToKisaException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보 열람일시 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisKisaToken.lpush(
								 Q_KISA_SESSION.STRUCTURE_NAME, KisaAccessTokenResponse.builder()
								 	.errLocation(new Object() {}.getClass().getEnclosingMethod().getName())
								 	.errMsg(e.getMessage())
								 	.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
								 .build()
								);

			throw new FailRequestDocumentRegisterRdToKisaException(e);
		}
		catch (FailRequestToKisaException e)
		{
			log.debug("{ ■ KISA ■ <전자문서 유통정보 열람일시 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), RegisterDocumentCirculationsResponse.class)
					 );

			log.error("{ ■ KISA ■ <전자문서 유통정보 열람일시 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"body\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), new Gson().toJson(response.getBody(), RegisterDocumentCirculationsResponse.class));

			throw new FailRequestDocumentRegisterRdToKisaException(e);
		}
		catch (FailResponseToKisaException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보 열람일시 등록 응답 - FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"resultCode\": \"{}\" ,\"errCode\": \"{}\", \"errMsg\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), response.getBody().getResultCode(), response.getBody().getErrCode(), response.getBody().getErrMsg());

			throw new FailResponseDocumentRegisterRdToKisaException(e);
		}
		catch(Exception e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보 열람일시 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	// KISA 공인전자문서 유통정보 열람일시 등록(다건)
	@Override
	public List<RegisterDocumentCirculationResponse> registerDocumentRdMulti(List<Document> document) throws FailRequestDocumentRegisterRdToKisaException
	{
		HttpEntity<RegisterDocumentCirculationsRequest>      request  = null;
		ResponseEntity<RegisterDocumentCirculationsResponse> response = null;

		try
		{
			KisaAccessTokenResponse hashToken = redisKisaToken.hmget(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME);

			if (null == hashToken)
				throw new NotExistTokenToKisaException();

			kcesHeaders.setBearerAuth(hashToken.getAccessToken());
			kcesHeaders.set("req-UUID", UUID.randomUUID().toString());
			kcesHeaders.set("req-date", StringUtil.yyyyMMddHHmmss(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis())));

			RegisterDocumentCirculationsRequest registerDocCirculationsRequest = new RegisterDocumentCirculationsRequest();

			List<RegisterDocumentCirculationRequest> circulations = new ArrayList<RegisterDocumentCirculationRequest>();

			for (Document doc : document)
			{
				doc.setDcmntRslt(Const.DOCUMENT.DCMNT_RSLT.FAIL.val());
				doc.setDcmntRsltCd(Const.DOCUMENT.DCMNT_RSLT_CD.FAIL.val());
				doc.setDcmntRsltDtm("");

				circulations.add(
								 RegisterDocumentCirculationRequest.builder()
								  		.edocNum(doc.getElctrcDcmntNo())
								  		.subject(null)
								  		.sendEaddr(null)
								  		.sendSubEaddr(null)
								  		.recvEaddr(null)
								  		.recvSubEaddr(null)
								  		.sendPlatformId(null)
								  		.recvPlatformId(null)
								  		.sendDate(null)
								  		.recvDate(null)
								  		.readDate(StringUtil.yyyyMMddHHmmss(doc.getRdDtm()))
								  		.docType(null)
								  		.contentHash(null)
								  		.fileHashes(null)
								  		.messageId(doc.getMessageId())
								 .build()
								);
			}

			registerDocCirculationsRequest.setCirculations(circulations);

			request = new HttpEntity<>(registerDocCirculationsRequest, kcesHeaders);

			log.info("{ ■ KISA ■ <전자문서 유통정보 열람일시 등록 요청> \"{}\": \"{}\", \"type\": , \"request\", \"url\": \"{}\", \"header\": \"{}\", \"body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + DOC_REG_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), RegisterDocumentCirculationsRequest.class)
					);

			response = restTemplate.exchange(HOSTNAME + DOC_REG_URL, HttpMethod.PATCH, request, RegisterDocumentCirculationsResponse.class);

			if (0 == response.getBody().getResultCode())
			{
				if (true == response.getBody().getErrCode().contains("ERR-01-103")
				 || true == response.getBody().getErrCode().contains("ERR-01-104")
				   )
				{
					throw new FailRequestAccessTokenToKisaException();
				}

				throw new FailRequestToKisaException();
			}

			if (circulations.size() != response.getBody().getCirculations().size())
				throw new NotMatchResponseCountToKisaException();

			for (int i = 0; i < document.size(); i++ )
				response.getBody().getCirculations().get(i).setMessageId(document.get(i).getMessageId());

			log.info("{ ■ KISA ■ <전자문서 유통정보 열람일시 등록 응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), RegisterDocumentCirculationsResponse.class)
					);

			String now = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());

			for (int i = 0; i < document.size(); i++ )
			{
				RegisterDocumentCirculationResponse registerDocCirculationResponse = response.getBody().getCirculations().get(i);

				String resultCd = Integer.toString(registerDocCirculationResponse.getResult());

				document.get(i).setDcmntRslt(Const.DOCUMENT.DCMNT_RSLT.SUCCESS.val());
				document.get(i).setDcmntRsltCd(resultCd.length() == 1 ? "0" + resultCd : resultCd);
				document.get(i).setDcmntRsltDtm(now);

				if (1 != registerDocCirculationResponse.getResult())
				{
					/* 유통 열람일시 등록
					* result는 각 유통정보에 등록결과 값을 나타낸다
					- 1: 등록성공
					- 2: 열람일시가 이미 등록된 경우
					- 3: 열람일시가 수신일시보다 빠른 경우(시각역전)
					- 4: 전자문서번호가 등록되지 않은 경우
					- 9: 기타오류
					*/

					log.info("{ ■ KISA ■ <전자문서 유통정보(열람일시) 등록 응답 - FAIL> \"{}\": \"{}\", \"type\": \"response\", \"body\": \"{}\" }"
							, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
							, new Gson().toJson(registerDocCirculationResponse, RegisterDocumentCirculationResponse.class)
							);
				}
			}

			return response.getBody().getCirculations();
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보 열람일시 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"statusCode\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestDocumentRegisterRdToKisaException(e);
		}
		catch (NotExistTokenToKisaException | FailRequestAccessTokenToKisaException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보 열람일시 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisKisaToken.lpush(
								 Q_KISA_SESSION.STRUCTURE_NAME
							   , KisaAccessTokenResponse.builder()
							   		.errLocation(new Object() {}.getClass().getEnclosingMethod().getName())
							   		.errMsg(e.getMessage())
							   		.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
							   	 .build()
							   	);

			throw new FailRequestDocumentRegisterRdToKisaException(e);
		}
		catch (NotMatchResponseCountToKisaException e)
		{
			log.debug("{ ■ KISA ■ <전자문서 유통정보 열람일시 등록 등록 응답 - ERROR> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), RegisterDocumentCirculationsResponse.class)
					);

			log.error("{ ■ KISA ■ <전자문서 유통정보 열람일시 등록 응답 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"body\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), new Gson().toJson(response.getBody(), RegisterDocumentCirculationsResponse.class));

			throw new FailRequestDocumentRegisterRdToKisaException(e);
		}
		catch (FailRequestToKisaException e)
		{
			log.info("{ ■ KISA ■ <전자문서 유통정보 열람일시 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), RegisterDocumentCirculationsResponse.class)
					);

			log.error("{ ■ KISA ■ <전자문서 유통정보 열람일시 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"body\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), new Gson().toJson(response.getBody(), RegisterDocumentCirculationsResponse.class));

			throw new FailRequestDocumentRegisterRdToKisaException(e);
		}
		catch(Exception e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보 열람일시 등록 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage(), new Gson().toJson(document, List.class));

			throw e;
		}
	}

	// KISA 전자문서 유통증명서 발급
	@Override
	public IssueDocumentResponse issueDocument(IssueDocumentRequest issueRequest) throws FailRequestDocumentIssueToKisaException, FailResponseDocumentIssueToKisaException
	{
		HttpEntity<IssueDocumentRequest> request          = null;
		ResponseEntity<byte[]>           response         = null;
		IssueDocumentResponse            issueDocResponse = null;

		try
		{
			KisaAccessTokenResponse hashToken = redisKisaToken.hmget(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME);

			if (null == hashToken)
				throw new NotExistTokenToKisaException();

			kcesHeaders.setBearerAuth(hashToken.getAccessToken());
			kcesHeaders.set("req-UUID", UUID.randomUUID().toString());
			kcesHeaders.set("req-date", StringUtil.yyyyMMddHHmmss(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis())));

			request = new HttpEntity<>(issueRequest, kcesHeaders);

			log.info("{ ■ KISA ■ <전자문서 유통증명서 발급 요청> \"{}\": \"{}\", \"type\": \"request\",  \"url\": \"{}\", \"header\": \"{}\", \"body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + DOC_ISS_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), IssueDocumentRequest.class)
					);

			response = restTemplate.exchange( HOSTNAME + DOC_ISS_URL, HttpMethod.POST, request, byte[].class);

			log.info("{ ■ KISA ■ <전자문서 유통증명서 발급 응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"body\": \"{}\" }"
				   , Const.TRACING_ID, MDC.get(Const.TRACING_ID)
				   , response.getStatusCode()
				   , "생략" //new Gson().toJson(response.getBody(), Byte[].class)
					);

			if (response.getHeaders().getContentType().toString().contains(MediaType.TEXT_PLAIN.toString()))
			{
				issueDocResponse = new Gson().fromJson(new String(response.getBody(), "UTF8"), IssueDocumentResponse.class);

				log.info("{ ■ KISA ■ <전자문서 유통증명서 발급 결과> \"{}\": \"{}\", \"type\": \"response\", \"ContentType\": \"{}\", \"issueDocResponse\": \"{}\" }"
					    , Const.TRACING_ID
					    , MDC.get(Const.TRACING_ID)
					    , response.getHeaders().getContentType()
					    , new Gson().toJson(issueDocResponse, IssueDocumentResponse.class)
						);

	        	if (true == issueDocResponse.getErrCode().contains("ERR-01-103")
	        	 || true == issueDocResponse.getErrCode().contains("ERR-01-104")
	        	   )
	        	{
					throw new FailRequestAccessTokenToKisaException(issueDocResponse.getErrMsg());
	        	}
	        	else if (true == issueDocResponse.getErrCode().contains("ERR-01-501"))
	        	{
	        		throw new NotExistDocumentToKisaException(issueDocResponse.getErrMsg());
	        	}
	        	else
	        	{
	        		throw new FailResponseToKisaException(issueDocResponse.getErrMsg());
	        	}
			}

			ByteArrayResource   resource   = new ByteArrayResource(response.getBody());
			ByteArrayDataSource datasource = new ByteArrayDataSource(resource.getInputStream(), "multipart/mixed");

			MimeMultipart multipart = new MimeMultipart(datasource);

			int count = multipart.getCount();

			for (int i = 0; i < count; i++)
			{
				BodyPart bodyPart = multipart.getBodyPart(i);

				if (bodyPart.isMimeType("text/plain"))
				{
					log.debug("{ ■ KISA ■ <전자문서 유통증명서 발급 응답 - 상세1> \"{}\": \"{}\", \"type\": \"response\", \"ContentType\": \"{}\", \"Object\": \"{}\" }"
							, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
							, bodyPart.getContentType()
							, new Gson().toJson(bodyPart.getContent(), Object.class)
							 );
				}
				else if (bodyPart.isMimeType("application/octet-stream"))
				{
					log.debug("{ ■ KISA ■ <전자문서 유통증명서 발급 응답 - 상세2> \"{}\": \"{}\", \"type\": \"response\", \"ContentType\": \"{}\", \"InputStream\": \"{}\" }"
							, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
							, bodyPart.getContentType()
							, new Gson().toJson(bodyPart.getInputStream(), InputStream.class)
							 );
				}
				else if (bodyPart.isMimeType("application/json"))
				{
					StringBuilder  sb = new StringBuilder();
					BufferedReader br = new BufferedReader(new InputStreamReader(bodyPart.getInputStream(), "UTF-8"));

					while (br.ready())
					{
						sb.append(br.readLine());
					}

					log.debug("{ ■ KISA ■ <전자문서 유통증명서 발급 응답 - 상세3> \"{}\": \"{}\", \"type\": \"response\", \"ContentType\": \"{}\", \"IssueDocResponse\": \"{}\" }"
							, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
							, bodyPart.getContentType()
							, sb.toString()
							 );

					issueDocResponse = new Gson().fromJson(sb.toString(), IssueDocumentResponse.class);

					log.debug("{ ■ KISA ■ <전자문서 유통증명서 발급 결과> \"{}\": \"{}\", \"type\": \"response\", \"ContentType\": \"{}\", \"IssueDocResponse\": \"{}\" }"
							, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
							, bodyPart.getContentType()
							, new Gson().toJson(issueDocResponse, IssueDocumentResponse.class)
							 );

					if (0 == issueDocResponse.getResultCode())
					{
			        	if (true == issueDocResponse.getErrCode().contains("ERR-01-103")
			        	 || true == issueDocResponse.getErrCode().contains("ERR-01-104")
			        	   )
			        	{
							throw new FailRequestAccessTokenToKisaException(issueDocResponse.getErrMsg());
			        	}
			        	else if (true == issueDocResponse.getErrCode().contains("ERR-01-501"))
			        	{
			        		throw new NotExistDocumentToKisaException(issueDocResponse.getErrMsg());
			        	}
			        	else
			        	{
			        		throw new FailResponseToKisaException(issueDocResponse.getErrMsg());
			        	}
					}
				}
				else if (bodyPart.isMimeType("application/pdf"))
				{
					log.debug("{ ■ KISA ■ <전자문서 유통증명서 발급 응답 - 상세4> \"{}\": \"{}\", \"type\": \"response\", \"ContentType\": \"{}\", \"InputStream\": \"{}\" }"
							, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
							, bodyPart.getContentType()
							, "생략" //new Gson().toJson(bodyPart.getInputStream(), InputStream.class)
							 );

					issueDocResponse.setFileBinary(IOUtils.toByteArray(bodyPart.getInputStream()));

					return issueDocResponse;
				}
				else
				{
					log.debug("{ ■ KISA ■ <전자문서 유통증명서 발급 응답 - 상세5> \"{}\": \"{}\", \"type\": \"response\", \"ContentType\": \"{}\", \"Object\": \"{}\" }"
							, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
							, bodyPart.getContentType()
							, new Gson().toJson(bodyPart.getContent(), Object.class)
							 );
				}
			}

			throw new FailResponseToKisaException(new FailResponseUnKnownToKisaException());
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통증명서 발급 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"statusCode\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestDocumentIssueToKisaException(e);
		}
		catch (IOException | MessagingException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통증명서 발급 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestDocumentIssueToKisaException(e);
		}
		catch (NotExistDocumentToKisaException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통증명서 발급 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
		catch (NotExistTokenToKisaException | FailRequestAccessTokenToKisaException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통증명서 발급 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisKisaToken.lpush(
								 Q_KISA_SESSION.STRUCTURE_NAME
							   , KisaAccessTokenResponse.builder()
									.errLocation(new Object() {}.getClass().getEnclosingMethod().getName())
									.errMsg(e.getMessage())
									.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
								 .build()
								);

			throw new FailRequestDocumentIssueToKisaException(e);
		}
		catch (FailRequestToKisaException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통증명서 발급 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestDocumentIssueToKisaException(e);
		}
		catch (FailResponseToKisaException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통증명서 발급 응답 - FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailResponseDocumentIssueToKisaException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통증명서 발급 요청- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	// KISA 전자문서 유통정보 수치 확인서 발급
	@Override
	public IssueDocumentStatConfirmationResponse issueDocumentStatsConfirmation(IssueDocumentStatConfirmationRequest issueRequest) throws FailRequestDocumentStatsConfirmationIssueToKisaException, FailResponseDocumentStatsConfirmationIssueToKisaException
	{
		HttpEntity<IssueDocumentStatConfirmationRequest> request       = null;
		ResponseEntity<byte[]>                           response      = null;
		IssueDocumentStatConfirmationResponse            issueResponse = null;

		try
		{
			KisaAccessTokenResponse hashToken = redisKisaToken.hmget(H_KISA_SESSION.STRUCTURE_NAME, H_KISA_SESSION.STRUCTURE_NAME);

			if (null == hashToken)
				throw new NotExistTokenToKisaException();

			kcesHeaders.setBearerAuth(hashToken.getAccessToken());
			kcesHeaders.set("req-UUID", UUID.randomUUID().toString());
			kcesHeaders.set("req-date", StringUtil.yyyyMMddHHmmss(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis())));

			request = new HttpEntity<>(issueRequest, kcesHeaders);

			log.info("{ ■ KISA ■ <전자문서 유통정보 수치 확인서 발급 요청> \"{}\": \"{}\", \"type\": \"request\",  \"url\": \"{}\", \"header\": \"{}\", \"body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + DOC_ISS_STS_CFM_URL
					, log.isDebugEnabled() ? request.getHeaders() : ""
					, new Gson().toJson(request.getBody(), IssueDocumentStatConfirmationRequest.class)
					);

			response = restTemplate.exchange( HOSTNAME + DOC_ISS_STS_CFM_URL, HttpMethod.POST, request, byte[].class);

			log.info("{ ■ KISA ■ <전자문서 유통정보 수치 확인서 발급 응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, "생략" //new Gson().toJson(response.getBody(), Byte[].class)
					);

			if (response.getHeaders().getContentType().toString().contains(MediaType.TEXT_PLAIN.toString()))
			{
				issueResponse = new Gson().fromJson(new String(response.getBody(), "UTF8"), IssueDocumentStatConfirmationResponse.class);

				log.info("{ ■ KISA ■ <전자문서 유통정보 수치 확인서 발급 결과> \"{}\": \"{}\", \"type\": \"response\", \"ContentType\": \"{}\", \"issueDocResponse\": \"{}\" }"
					    , Const.TRACING_ID
					    , MDC.get(Const.TRACING_ID)
					    , response.getHeaders().getContentType()
					    , new Gson().toJson(issueResponse, IssueDocumentStatConfirmationResponse.class)
						);

	        	if (true == issueResponse.getErrCode().contains("ERR-01-103")
	        	 || true == issueResponse.getErrCode().contains("ERR-01-104")
	        	   )
	        	{
					throw new FailRequestAccessTokenToKisaException(issueResponse.getErrMsg());
	        	}
	        	else if (true == issueResponse.getErrCode().contains("ERR-01-501"))
	        	{
	        		throw new NotExistDocumentStatToKisaException(issueResponse.getErrMsg());
	        	}
	        	else
	        	{
	        		throw new FailResponseToKisaException(issueResponse.getErrMsg());
	        	}
			}

			ByteArrayResource   resource   = new ByteArrayResource(response.getBody());
			ByteArrayDataSource datasource = new ByteArrayDataSource(resource.getInputStream(), "multipart/mixed");

			MimeMultipart multipart = new MimeMultipart(datasource);

			int count = multipart.getCount();

			for (int i = 0; i < count; i++)
			{
				BodyPart bodyPart = multipart.getBodyPart(i);

				if (bodyPart.isMimeType("text/plain"))
				{
					log.debug("{ ■ KISA ■ <전자문서 유통정보 수치 확인서 발급 응답 - 상세1> \"{}\": \"{}\", \"type\": \"response\", \"ContentType\": \"{}\", \"Object\": \"{}\" }"
							, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
							, bodyPart.getContentType()
							, new Gson().toJson(bodyPart.getContent(), Object.class)
							);
				}
				else if (bodyPart.isMimeType("application/octet-stream"))
				{
					log.debug("{ ■ KISA ■ <전자문서 유통정보 수치 확인서 발급 - 상세2> \"{}\": \"{}\", \"type\": \"response\", \"ContentType\": \"{}\", \"InputStream\": \"{}\" }"
							, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
							, bodyPart.getContentType()
							, new Gson().toJson(bodyPart.getInputStream(), InputStream.class)
							);
				}
				else if (bodyPart.isMimeType("application/json"))
				{
					StringBuilder  sb = new StringBuilder();
					BufferedReader br = new BufferedReader(new InputStreamReader(bodyPart.getInputStream(), "UTF-8"));

					while (br.ready())
					{
						sb.append(br.readLine());
					}

					log.debug("{ ■ KISA ■ <전자문서 유통정보 수치 확인서 발급 응답 - 상세3> \"{}\": \"{}\", \"type\": \"response\", \"ContentType\": \"{}\", \"IssueDocResponse\": \"{}\" }"
							, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
							, bodyPart.getContentType()
							, sb.toString()
							 );

					issueResponse = new Gson().fromJson(sb.toString() , IssueDocumentStatConfirmationResponse.class);

					log.info("{ ■ KISA ■ <전자문서 유통정보 수치 확인서 발급 결과> \"{}\": \"{}\", \"type\": \"response\", \"ContentType\": \"{}\", \"IssueDocResponse\": \"{}\" }"
						   , Const.TRACING_ID, MDC.get(Const.TRACING_ID)
						   , bodyPart.getContentType()
						   , new Gson().toJson(issueResponse, IssueDocumentStatConfirmationResponse.class)
							);

					if (0 == issueResponse.getResultCode())
					{
			        	if (true == issueResponse.getErrCode().contains("ERR-01-103")
		   	        	 || true == issueResponse.getErrCode().contains("ERR-01-104")
		   	        	   )
		   	        	{
		   					throw new FailRequestAccessTokenToKisaException(issueResponse.getErrMsg());
		   	        	}
		   	        	else if (true == issueResponse.getErrCode().contains("ERR-01-501"))
		   	        	{
		   	        		throw new NotExistDocumentStatToKisaException(issueResponse.getErrMsg());
		   	        	}
		   	        	else
		   	        	{
		   	        		throw new FailResponseToKisaException(issueResponse.getErrMsg());
		   	        	}
					}
				}
				else if (bodyPart.isMimeType("application/pdf"))
				{
					log.debug("{ ■ KISA ■ <전자문서 유통정보 수치 확인서 발급 - 상세4> \"{}\": \"{}\", \"type\": \"response\", \"ContentType\": \"{}\", \"InputStream\": \"{}\" }"
							, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
							, bodyPart.getContentType()
							, "생략" //new Gson().toJson(bodyPart.getInputStream(), InputStream.class)
							 );

					issueResponse.setFileBinary(IOUtils.toByteArray(bodyPart.getInputStream()));

					return issueResponse;
				}
				else
				{
					log.debug("{ ■ KISA ■ <전자문서 유통정보 수치 확인서 발급 응답 - 상세5> \"{}\": \"{}\", \"type\": \"response\", \"ContentType\": \"{}\", \"Object\": \"{}\" }"
							, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
							, bodyPart.getContentType()
							, new Gson().toJson(bodyPart.getContent(), Object.class)
							 );
				}
			}

			throw new FailResponseToKisaException(new FailResponseUnKnownToKisaException());
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보 수치 확인서 발급 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"statusCode\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestDocumentIssueToKisaException(e);
		}
		catch (IOException | MessagingException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보 수치 확인서 발급 요청 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestDocumentIssueToKisaException(e);
		}
		catch (NotExistDocumentStatToKisaException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보 수치 확인서 발급 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
		catch (NotExistTokenToKisaException | FailRequestAccessTokenToKisaException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보 수치 확인서 발급 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisKisaToken.lpush(
								 Q_KISA_SESSION.STRUCTURE_NAME
							   , KisaAccessTokenResponse.builder()
									.errLocation(new Object() {}.getClass().getEnclosingMethod().getName())
									.errMsg(e.getMessage())
									.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
								 .build()
								);

			throw new FailRequestDocumentIssueToKisaException(e);
		}
		catch (FailRequestToKisaException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보 수치 확인서 발급 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestDocumentIssueToKisaException(e);
		}
		catch (FailResponseToKisaException e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보 수치 확인서 발급 - FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailResponseDocumentStatsConfirmationIssueToKisaException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ KISA ■ <전자문서 유통정보 수치 확인서 발급 요청- ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}
}
