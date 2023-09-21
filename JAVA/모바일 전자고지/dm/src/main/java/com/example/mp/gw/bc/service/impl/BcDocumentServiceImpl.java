package com.example.mp.gw.bc.service.impl;


import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.example.mp.gw.bc.domain.BcAccessTokenResponse;
import com.example.mp.gw.bc.domain.DocumentIssueResultRequest;
import com.example.mp.gw.bc.domain.DocumentStatConfirmationIssueResultRequest;
import com.example.mp.gw.bc.domain.ResponseResult;
import com.example.mp.gw.bc.exception.FailRequestDcmntIssueToBcException;
import com.example.mp.gw.bc.exception.FailRequestDcmntStsCfmIssueToBcException;
import com.example.mp.gw.bc.exception.FailRequestToBcException;
import com.example.mp.gw.bc.exception.FailResponseDcmntIssueToBcException;
import com.example.mp.gw.bc.exception.FailResponseDcmntStsCfmIssueToBcException;
import com.example.mp.gw.bc.exception.FailResponseToBcException;
import com.example.mp.gw.bc.exception.NotExistAccessTokenToBcException;
import com.example.mp.gw.bc.service.BcDocumentService;
import com.example.mp.gw.common.domain.Const;
import com.example.mp.gw.common.domain.RedisQueueDataWrapper;
import com.example.mp.gw.common.domain.Const.BIZCENTER;
import com.example.mp.gw.common.domain.Const.DOCUMENT;
import com.example.mp.gw.common.domain.Const.FORMATTER;
import com.example.mp.gw.common.domain.RedisStructure.H_BC_SESSION;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_DOC_ISS_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_DOC_STS_CFM_ISS_TO_SEND;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_RETRY;
import com.example.mp.gw.common.domain.RedisStructure.Q_BC_SESSION;
import com.example.mp.gw.common.exception.KeepRunningToWorkerException;
import com.example.mp.gw.common.exception.StopRunningToWorkerException;
import com.example.mp.gw.common.service.RedisService;
import com.example.mp.gw.doc.domain.DocumentIssue;
import com.example.mp.gw.doc.domain.DocumentIssueIsd;
import com.example.mp.gw.doc.domain.DocumentIssueStatConfirmation;
import com.example.mp.gw.doc.domain.DocumentIssueStatConfirmationIsd;
import com.example.mp.gw.doc.mappers.altibase.DocumentMapper;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service("BcDocumentService")
public class BcDocumentServiceImpl implements BcDocumentService
{
	@Autowired
	private BcDocumentService bcDocumentService;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<DocumentIssue>> redisDocIssSendToBc;	

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<DocumentIssueStatConfirmation>> redisDocIssStsCfmSendToBc;

	@Autowired
	private RedisService<String, BcAccessTokenResponse> redisBcToken;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<?>> redisRetryToBc;

	@Autowired
	private DocumentMapper documentMapper;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	@Qualifier("bizCenterHeaders")
	private HttpHeaders bizCenterHeaders;
 
	@Value("${spring.bc.hostname}")
	private String HOSTNAME;

	@Value("${spring.bc.url.acmdcerf}")
	private String ACMDCERF_URL;

	@Value("${spring.bc.url.acmdinfocfm}")
	private String ACMDINFOCFM_URL;


	/**
	 * BC 유통증명서발급 결과 전송 스케줄러 (G/W -> 비즈센터)
	 * @return void 
	 * @throws KeepRunningToWorkerException 
	 * @throws StopRunningToWorkerException
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void popAndSendIssDoc() throws KeepRunningToWorkerException, StopRunningToWorkerException
	{
		RedisQueueDataWrapper<DocumentIssue> request       = null;
		DocumentIssue                        documentIssue = null;

		try
		{
			// 1. 유통증명서발급 결과 전송 요청 데이터 읽기
			request = redisDocIssSendToBc.rpop(Q_BC_DOC_ISS_TO_SEND.STRUCTURE_NAME);
			
			if (request == null)
				return;

			// 2. 요청(유통증명서) 데이터 추출
			documentIssue = (DocumentIssue) request.getData();

			if (null == documentIssue)
				return;

			MDC.put(Const.TRACING_ID, documentIssue.getSndn_mgnt_seq() + "_" + documentIssue.getSndn_seq_no());

			log.info("{ ■ BC 유통증명서발급 결과 전송 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), documentIssue);

			String strDate = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());

			// 3. 요청(유통증명서) 전송 데이터 설정
			DocumentIssueResultRequest requestIssuedDocument = DocumentIssueResultRequest.builder()
																	.sndn_mgnt_seq(documentIssue.getSndn_mgnt_seq())
																	.sndn_seq_no(documentIssue.getSndn_seq_no())
																	.iss_seq(documentIssue.getIss_seq())
																	.file_name(BIZCENTER.RESULT_CD.SUCCESS.val().equals(documentIssue.getIss_result_cd()) ?
																					strDate.substring(0, 8)
																				  + "_"
																				  + strDate.substring(0, 14)
																				  + "_UPLUS_"
																				  + String.format("%013d", System.currentTimeMillis())
																				  + ".pdf"
																			   : null
																			  )
		 															.file_binary(documentIssue.getFile_binary())
																	.iss_result_cd(documentIssue.getIss_result_cd())
																	.iss_result_msg(documentIssue.getIss_result_msg())
															   .build();

			// 4. Biz-center 유통증명서 결과 전송
			bcDocumentService.sendIssueDocument(requestIssuedDocument);

			// 5. 유통증명서(법인/비즈) 발급 내역 갱신
			DocumentIssueIsd documentIssueIsd = DocumentIssueIsd.builder()
																		  .isdStatus(DOCUMENT.ISSUE.ISD_STATUS.SND.val())
																		  .sndRsltCd(DOCUMENT.ISSUE.SND_RSLT_CD.SUCCESS.val())
																		  .sndRsltDtm(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
																		  .dcmntIsdId(documentIssue.getDcmnt_isd_id())
																.build();

			documentMapper.updateDocumentIssueIsd(documentIssueIsd);
		}
		catch (FailRequestDcmntIssueToBcException e)
		{
			log.error("{ ■ BC 유통증명서발급 결과 전송 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), documentIssue, e.getClass().getName(), e.getMessage());

			request.setSndDtm();
			request.getData().setSnd_result_msg(e.getMessage());

			redisRetryToBc.lpush(Q_BC_RETRY.STRUCTURE_NAME, request);

			throw new KeepRunningToWorkerException();
		}
		catch (FailResponseDcmntIssueToBcException e)
		{
			log.error("{ ■ BC 유통증명서발급 결과 전송 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), documentIssue, e.getClass().getName(), e.getMessage());

			request.setSndDtm();
			request.getData().setSnd_result_msg(e.getMessage());

			redisRetryToBc.lpush(Q_BC_RETRY.STRUCTURE_NAME, request);

			throw new KeepRunningToWorkerException();
		}
		catch (Exception e)
		{
			log.error("{ ■ BC 유통증명서발급 결과 전송 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class), e.getClass().getName(), e.getMessage());

			if (null != request)
			{
				request.setSndDtm();
				request.getData().setSnd_result_msg(e.getMessage());

				redisRetryToBc.lpush(Q_BC_RETRY.STRUCTURE_NAME, request);
			}

			throw new KeepRunningToWorkerException();
		}
	}

	/**
	 * BC 유통정보 수치 확인서 발급 결과 전송 스케줄러 (G/W -> 비즈센터)
	 * @return void 
	 * @throws KeepRunningToWorkerException 
	 * @throws StopRunningToWorkerException
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void popAndSendIssDocStsCfm() throws KeepRunningToWorkerException, StopRunningToWorkerException
	{
		RedisQueueDataWrapper<DocumentIssueStatConfirmation> request             = null;
		DocumentIssueStatConfirmation                        documentIssueStsCfm = null;

		try
		{
			// 1. 유통정보 수치 확인서 발급 결과 전송 요청 데이터 읽기
			request = redisDocIssStsCfmSendToBc.rpop(Q_BC_DOC_STS_CFM_ISS_TO_SEND.STRUCTURE_NAME);
			
			if (request == null)
				return;

			// 2. 유통정보 수치 확인서 발급 결과 데이터 추출
			documentIssueStsCfm = (DocumentIssueStatConfirmation) request.getData();

			if (null == documentIssueStsCfm)
				return;

			MDC.put(Const.TRACING_ID, documentIssueStsCfm.getEaddr());

			log.info("{ ■ BC 유통정보 수치 확인서 발급 결과 전송 ■ \"{}\": \"{}\", \"type\": \"service\", \"methodName\": \"{}\", \"target\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), documentIssueStsCfm);

			String strDate = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());

			// 3. 요청(유통정보 수치 확인서 발급 결과) 전송 데이터 설정
			DocumentStatConfirmationIssueResultRequest requestIssuedDocument = DocumentStatConfirmationIssueResultRequest.builder()
																					.eaddr(documentIssueStsCfm.getEaddr())
																					.period(documentIssueStsCfm.getPeriod())
																					.iss_seq(documentIssueStsCfm.getIss_seq())
																					.file_name(BIZCENTER.RESULT_CD.SUCCESS.val().equals(documentIssueStsCfm.getIss_result_cd()) ?
																									strDate.substring(0, 8)
																								  + "_"
																								  + strDate.substring(0, 14)
																								  + "_UPLUS_"
																								  + String.format("%013d", System.currentTimeMillis())
																								  + ".pdf"
																							   : null
																							  )
																					.file_binary(documentIssueStsCfm.getFileBytes())
																					.iss_result_cd(documentIssueStsCfm.getIss_result_cd())
																					.iss_result_msg(documentIssueStsCfm.getIss_result_msg())
																			   .build();

			// 4. Biz-center 유통정보 수치 확인서 발급 결과 전송
			bcDocumentService.sendIssueDocumentStatsConfirmation(requestIssuedDocument);

			// 5. 유통정보 수치 확인서(법인/비즈) 발급 내역 갱신
			DocumentIssueStatConfirmationIsd documentStatIssueIsd = DocumentIssueStatConfirmationIsd.builder()
																		.isdStatus(DOCUMENT.ISSUE.ISD_STATUS.SND.val())
																		.sndRsltCd(DOCUMENT.ISSUE.SND_RSLT_CD.SUCCESS.val())
																		.sndRsltDtm(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
																		.dcmntStsIsdSeq(documentIssueStsCfm.getDcmnt_sts_isd_seq())
																	.build();

			documentMapper.updatetDocumentStatConfirmationIssueIsd(documentStatIssueIsd);
		}
		catch (FailRequestDcmntStsCfmIssueToBcException e)
		{
			log.error("{ ■ BC 유통정보 수치 확인서 발급 결과 전송 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), documentIssueStsCfm, e.getClass().getName(), e.getMessage());

			request.setSndDtm();
			request.getData().setSnd_result_msg(e.getMessage());

			redisRetryToBc.lpush(Q_BC_RETRY.STRUCTURE_NAME, request);

			throw new KeepRunningToWorkerException();
		}
		catch (FailResponseDcmntStsCfmIssueToBcException e)
		{
			log.error("{ ■ BC 유통정보 수치 확인서 발급 결과 전송 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), documentIssueStsCfm, e.getClass().getName(), e.getMessage());

			request.setSndDtm();
			request.getData().setSnd_result_msg(e.getMessage());

			redisRetryToBc.lpush(Q_BC_RETRY.STRUCTURE_NAME, request);

			throw new KeepRunningToWorkerException();
		}
		catch (Exception e)
		{
			log.error("{ ■ BC 유통정보 수치 확인서 발급 결과 전송 - ERROR ■ \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"target\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), new Gson().toJson(request, RedisQueueDataWrapper.class), e.getClass().getName(), e.getMessage());

			if (null != request)
			{
				request.setSndDtm();
				request.getData().setSnd_result_msg(e.getMessage());

				redisRetryToBc.lpush(Q_BC_RETRY.STRUCTURE_NAME, request);
			}

			throw new KeepRunningToWorkerException();
		}
	}

	/**
	 * 유통증명서발급 결과 전송(G/W -> 비즈센터)
	 * @param DocumentIssueResultRequest
	 * @return void
	 * @throws FailRequestDcmntIssueToBcException
	 * @throws FailResponseDcmntIssueToBcException
	 */
	@Override
	public void sendIssueDocument(DocumentIssueResultRequest issueResultRequest) throws FailRequestDcmntIssueToBcException, FailResponseDcmntIssueToBcException
	{
		HttpEntity<DocumentIssueResultRequest> request  = null;
		ResponseEntity<ResponseResult>         response = null;

		try
		{
			BcAccessTokenResponse accessToken = redisBcToken.hmget(H_BC_SESSION.STRUCTURE_NAME, H_BC_SESSION.STRUCTURE_NAME);

			if (null == accessToken)
				throw new NotExistAccessTokenToBcException();

			bizCenterHeaders.setBearerAuth(accessToken.getAccess_token());

			request = new HttpEntity<>(issueResultRequest, bizCenterHeaders);

			log.info("{ ■ BC ■ <유통증명서발급 결과 전송 요청> \"{}\": \"{}\", \"type\": \"url\": \"{}\", \"request\": \"Header\": \"{}\", \"Body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + ACMDCERF_URL
					, request.getHeaders()
					, log.isDebugEnabled() ? new Gson().toJson(request.getBody(), DocumentIssueResultRequest.class) : issueResultRequest
					);

			response = restTemplate.exchange(HOSTNAME + ACMDCERF_URL, HttpMethod.POST, request, ResponseResult.class);

			log.info("{ ■ BC ■ <유통증명서발급 결과 전송 응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"responseResult\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), ResponseResult.class)
					);

			if (true == "01".equals(response.getBody().getResult_cd()))
				throw new FailResponseToBcException(response.getBody().getErrors().toString());
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ BC ■ <유통증명서발급 결과 전송 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"statusCode\": \"{}\",\"errorName\": \"{}\", \"message\": \"{}\", \"errors\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestDcmntIssueToBcException(e);
		}
		catch (NotExistAccessTokenToBcException e)
		{
			log.error("{ ■ BC ■ <유통증명서발급 결과 전송 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisBcToken.lpush(
							   Q_BC_SESSION.STRUCTURE_NAME
							 , BcAccessTokenResponse.builder()
							 		.error_location(new Object() {}.getClass().getEnclosingMethod().getName())
							 		.error_description(e.getMessage())
							 		.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
							   .build()
							  );

			throw new FailRequestDcmntIssueToBcException(e);
		}
		catch (FailRequestToBcException e)
		{
			log.error("{ ■ BC ■ <유통증명서발급 결과 전송 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestDcmntIssueToBcException(e);
		}
		catch (FailResponseToBcException e)
		{
			log.error("{ ■ BC ■ <유통증명서발급 결과 전송 - FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailResponseDcmntIssueToBcException(e.getMessage());
		}
		catch (Exception e)
		{
			log.error("{ ■ BC ■ <유통증명서발급 결과 전송 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	/**
	 * 전자문서 유통정보 수치 확인서 발급 결과 전송(G/W -> 비즈센터)
	 * @param DocumentStatConfirmationIssueResultRequest
	 * @return void
	 * @throws FailRequestDcmntStsCfmIssueToBcException
	 * @throws FailResponseDcmntStsCfmIssueToBcException
	 */
	@Override
	public void sendIssueDocumentStatsConfirmation(DocumentStatConfirmationIssueResultRequest issueResultRequest) throws FailRequestDcmntStsCfmIssueToBcException, FailResponseDcmntStsCfmIssueToBcException
	{
		HttpEntity<DocumentStatConfirmationIssueResultRequest> request  = null;
		ResponseEntity<ResponseResult>                         response = null;

		try
		{
			BcAccessTokenResponse accessToken = redisBcToken.hmget(H_BC_SESSION.STRUCTURE_NAME, H_BC_SESSION.STRUCTURE_NAME);

			if (null == accessToken)
				throw new NotExistAccessTokenToBcException();

			bizCenterHeaders.setBearerAuth(accessToken.getAccess_token());

			request = new HttpEntity<>(issueResultRequest, bizCenterHeaders);

			log.info("{ ■ BC ■ <전자문서 유통정보 수치 확인서 발급 결과 전송 요청> \"{}\": \"{}\", \"type\": \"url\": \"{}\", \"request\": \"Header\": \"{}\", \"Body\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, HOSTNAME + ACMDINFOCFM_URL
					, request.getHeaders()
					, log.isDebugEnabled() ? new Gson().toJson(request.getBody(), DocumentStatConfirmationIssueResultRequest.class) : issueResultRequest
					);

			response = restTemplate.exchange(HOSTNAME + ACMDINFOCFM_URL, HttpMethod.POST, request, ResponseResult.class);

			log.info("{ ■ BC ■ <전자문서 유통정보 수치 확인서 발급 결과 전송 응답> \"{}\": \"{}\", \"type\": \"response\", \"httpCode\": \"{}\", \"responseResult\": \"{}\" }"
					, Const.TRACING_ID, MDC.get(Const.TRACING_ID)
					, response.getStatusCode()
					, new Gson().toJson(response.getBody(), ResponseResult.class)
					);

			if (true == "01".equals(response.getBody().getResult_cd()))
				throw new FailResponseToBcException(response.getBody().getErrors().toString());
		}
		catch (HttpClientErrorException e)
		{
			log.error("{ ■ BC ■ <전자문서 유통정보 수치 확인서 발급 결과 전송 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"statusCode\": \"{}\",\"errorName\": \"{}\", \"message\": \"{}\", \"errors\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getStatusCode(), e.getMessage());

			throw new FailRequestDcmntStsCfmIssueToBcException(e);
		}
		catch (NotExistAccessTokenToBcException e)
		{
			log.error("{ ■ BC ■ <전자문서 유통정보 수치 확인서 발급 결과 전송 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			redisBcToken.lpush(
							   Q_BC_SESSION.STRUCTURE_NAME
							 , BcAccessTokenResponse.builder()
							   		.error_location(new Object() {}.getClass().getEnclosingMethod().getName())
							   		.error_description(e.getMessage())
							   		.reg_dt(FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis()))
							   .build()
							  );

			throw new FailRequestDcmntStsCfmIssueToBcException(e);
		}
		catch (FailRequestToBcException e)
		{
			log.error("{ ■ BC ■ <전자문서 유통정보 수치 확인서 발급 결과 전송 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailRequestDcmntStsCfmIssueToBcException(e);
		}
		catch (FailResponseToBcException e)
		{
			log.error("{ ■ BC ■ <전자문서 유통정보 수치 확인서 발급 결과 전송 - FAIL> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw new FailResponseDcmntStsCfmIssueToBcException(e);
		}
		catch (Exception e)
		{
			log.error("{ ■ BC ■ <전자문서 유통정보 수치 확인서 발급 결과 전송 - ERROR> \"{}\": \"{}\", \"type\": \"error\", \"methodName\": \"{}\", \"errorName\": \"{}\", \"message\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), new Object() {}.getClass().getEnclosingMethod(), e.getClass().getName(), e.getMessage());

			throw e;
		}
	}
}
