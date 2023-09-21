package com.example.mp.gw.ms.service.impl;


import static com.example.mp.gw.common.domain.RedisStructure.Q_PM_RS;
import static com.example.mp.gw.common.domain.RedisStructure.Q_PM_RS_RMD;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.mp.gw.common.domain.RedisQueueDataWrapper;
import com.example.mp.gw.common.domain.RedisStructure;
import com.example.mp.gw.common.domain.Const.FORMATTER;
import com.example.mp.gw.common.domain.RedisStructure.H_PM_RCS_IMG;
import com.example.mp.gw.common.domain.RedisStructure.Q_MB_BFH_RQ;
import com.example.mp.gw.common.domain.RedisStructure.Q_PM_RCS_IMG;
import com.example.mp.gw.common.domain.RedisStructure.Q_PM_RPT_RQ;
import com.example.mp.gw.common.function.FunctionThrowsException;
import com.example.mp.gw.common.service.RedisService;
import com.example.mp.gw.member.domain.BeforeMember;
import com.example.mp.gw.ms.domain.BeforehandRequest;
import com.example.mp.gw.ms.domain.MessageRequest;
import com.example.mp.gw.ms.domain.RemindMessageRequest;
import com.example.mp.gw.ms.domain.ReportRequest;
import com.example.mp.gw.ms.exception.FailRegisterRcsImageException;
import com.example.mp.gw.ms.exception.NoImageFileException;
import com.example.mp.gw.ms.service.MessageService;

import lombok.extern.slf4j.Slf4j;

/**
 * @Class Name : MessageServiceImpl.java
 * @Description : 메시지 서비스 구현 객체 
 * 
 * @author 조주현
 * @since 2021.03.29
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.03.29	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Slf4j
@Service("MessageService")
public class MessageServiceImpl implements MessageService
{
	@Autowired
	private RedisService<String, Map<String, Object>> redisBeforeMessage;

	@Autowired
	private RedisService<String, RedisQueueDataWrapper<BeforeMember>> redisBeforeMember;

	@Autowired
	private RedisService<String, Map<String, Object>> redisMessage;

	@Autowired
	private RedisService<String, Map<String, Object>> redisRcsImage;

	@Value("${spring.file.upload.rcs-image.path}")
	private String RCS_IMAGE_UPLOAD_PATH;

	@Value("${spring.beforehand.enabled}")
	private String BEFOREHAND_ENABLED;


	/**
	 * 리마인드문자 전송
	 * @param RemindMessageRequest
	 * @return
	 * @throws IOException 
	 */
	@Override
	public void sendRemindMessages(RemindMessageRequest messageRequest) throws IOException
	{
		long         startTime = System.nanoTime();
		final String now       = FORMATTER.yyyyMMddHHmmss.val().format(messageRequest.getMills());		

		// 레디스 'Q_PM_RS_RMD' 키에 PUSH 할 목록
		redisMessage.lpushT(Q_PM_RS_RMD.STRUCTURE_NAME, mappingRemindMessageRequest(messageRequest, now));

		log.debug("messages size : {}, time : {} ms", messageRequest.getReqs().size(), (System.nanoTime() - startTime) / 1000000.0);
	}

	/**
	 * 사전문자 전송
	 * @param messageRequest
	 * @return
	 * @throws 
	 */
	@Override
	public void sendBeforehandMessages(BeforehandRequest messageRequest)
	{
		final String now = FORMATTER.yyyyMMddHHmmss.val().format(messageRequest.getMills());

		// 레디스 'QP_PM_RS' 키에 PUSH 할 목록
		List<Map<String, Object>> Q_PM_RS_REQS = mappingBeforeheadMessageRequest(messageRequest);

		if (BEFOREHAND_ENABLED.equals("true"))
		{
			// 사전문자 자동가입 대상 목록 
			List<RedisQueueDataWrapper<BeforeMember>> Q_MB_BFH_REQS = messageRequest.getReqs()
																	   .stream()
																	   		.map(wrapper(m->RedisQueueDataWrapper
																	   						.<BeforeMember>builder()
																	   							.structure_name(Q_MB_BFH_RQ.STRUCTURE_NAME)
																	   								.data(
																	   										BeforeMember.builder()
																	   											.createdDtm(now)
																	   											.messageId(messageRequest.getSndn_mgnt_seq() + "_" + m.getSndn_seq_no())
																	   											.svcOrgCd(messageRequest.getService_cd())
																	   											.ci(m.getCi())
																	   											.phone(m.getMdn())
																	   										.build()
																	   									 )
																							.build()
																	   					)
																	   			).collect(Collectors.toList());

			// LPUSH (Q_MB_BFH_RQ 사전문자 자동가입 큐)
			redisBeforeMember.lpushT(Q_MB_BFH_RQ.STRUCTURE_NAME, Q_MB_BFH_REQS);
		}

		// QP_PM_RS 에 추가
		redisBeforeMessage.lpushT(Q_PM_RS.STRUCTURE_NAME, Q_PM_RS_REQS);
	}
	
	/**
	 * 본문자 전송
	 * @param MessageRequest
	 * @return
	 * @throws IOException 
	 */
	@Override
	public void sendMainMessages(MessageRequest messageRequest) throws IOException
	{
		long	startTime             = System.nanoTime();
		final	String now            = FORMATTER.yyyyMMddHHmmss.val().format(messageRequest.getMills());
		final	String nowForAttachId = FORMATTER.yyyyMMddHHmmssS.val().format(messageRequest.getMills());

		// 이미지 저장
		putMessageImages(messageRequest, nowForAttachId);

		// 레디스 'Q_PM_RS' 키에 PUSH 할 목록
		redisMessage.lpushT(Q_PM_RS.STRUCTURE_NAME, mappingMessageRequest(messageRequest, now, nowForAttachId));

		log.debug("messages size : {}, time : {} ms", messageRequest.getReqs().size(), (System.nanoTime() - startTime) / 1000000.0);
	}

	/**
	 * 리포트 재전송
	 * @param ReportRequest
	 * @return
	 * @throws IOException 
	 */
	@Override
	public void sendReports(ReportRequest messageRequest) throws IOException
	{
		long         startTime = System.nanoTime();
		final String now       = FORMATTER.yyyyMMddHHmmss.val().format(System.currentTimeMillis());		

		// 레디스 'Q_PM_RPT_RQ' 키에 PUSH 할 목록
		redisMessage.lpushT(Q_PM_RPT_RQ.STRUCTURE_NAME, mappingReportRequest(messageRequest, now));

		log.debug("messages size : {}, time : {} ms", messageRequest.getReqs().size(), (System.nanoTime() - startTime) / 1000000.0);
	}

	/**
	 * RCS 사전이미지 등록 
	 * @param String
	 * @param MultipartFile
	 * @return void
	 * @throws IOException 
	 * @throws IllegalStateException 
	 */
	@Override
	public void registerRcsImage(String attachId, MultipartFile image) throws IllegalStateException, IOException, FailRegisterRcsImageException
	{
		if (null == image)
			throw new NoImageFileException();

		HashMap<String, Object> imageData = new HashMap<>();

		// <FIXME:파일 이름 정하기>
		final String IMAGE_FILE_PATH = RCS_IMAGE_UPLOAD_PATH + File.separator +  FORMATTER.yyyyMMdd.val().format(System.currentTimeMillis());
		final String FILE_NAME       = attachId + "_" + FORMATTER.yyyyMMdd.val().format(System.currentTimeMillis()) + image.getOriginalFilename().substring(image.getOriginalFilename().lastIndexOf("."));
		final String FULL_FILE_PATH  = IMAGE_FILE_PATH + File.separator + FILE_NAME;

		if (!new File(IMAGE_FILE_PATH).exists())
			new File(IMAGE_FILE_PATH).mkdirs();

		IOUtils.copy(image.getInputStream(), new FileOutputStream(new File(FULL_FILE_PATH)));

		imageData.put(Q_PM_RCS_IMG.RCS_ATTACH_ID.name(), attachId);
		imageData.put(Q_PM_RCS_IMG.RCS_IMG_SIZE.name() , image.getSize());
		imageData.put(Q_PM_RCS_IMG.RCS_IMG_NM.name()   , FILE_NAME); 		// <FIXME:이미지이름 받아야함>
		imageData.put(Q_PM_RCS_IMG.RCS_FILE_PATH.name(), FULL_FILE_PATH);	// <FIXME:업로드 모듈 만들어야함>
		imageData.put(Q_PM_RCS_IMG.RCS_BR_ID.name()    , "br_id"); 			// <FIXME:attachid를 통해 brandid를 가져오거나 별도로 brandid 도 함께 받아야함>

		redisRcsImage.hmset(H_PM_RCS_IMG.STRUCTURE_NAME, attachId, imageData);

		Map<String,Object> result =  redisRcsImage.hmget(H_PM_RCS_IMG.STRUCTURE_NAME, attachId);

		Long index = redisRcsImage.lpush(Q_PM_RCS_IMG.STRUCTURE_NAME, imageData);

		if (null == result || 0 > index)
		{
			throw new FailRegisterRcsImageException();
		}
	}

	/**
	 * 리마인드문자 매핑
	 * @param RemindMessageRequest
	 * @return List<HashMap<String, Object>>
	 */
	private List<Map<String, Object>> mappingRemindMessageRequest(RemindMessageRequest messageRequest, String now)
	{
		// 레디스 'Q_PM_RS_RMD' 형태에 맞게 변환
		return messageRequest.getReqs()
								.parallelStream()
									.map(req->{
												Map<String, Object> Q_PM_RS_RMD_REQ = new HashMap<>();

												Q_PM_RS_RMD_REQ.put(Q_PM_RS_RMD.SVC_ORG_CD.name()        , messageRequest.getService_cd());									// 기관코드	: 수요기관 관리 구분코드
												Q_PM_RS_RMD_REQ.put(Q_PM_RS_RMD.SVC_ORG_NM.name()        , messageRequest.getBiz_nm());										// 기관명		: 수요기관 관리 구분 이름
												Q_PM_RS_RMD_REQ.put(Q_PM_RS_RMD.CN_KEY.name()            , messageRequest.getMsg_cd());										// 문서구분코드
												Q_PM_RS_RMD_REQ.put(Q_PM_RS_RMD.CN_FORM.name()           , messageRequest.getMsg_nm());										// 문서명
												Q_PM_RS_RMD_REQ.put(Q_PM_RS_RMD.TRANS_DT.name()          , messageRequest.getMake_dt());									// 요청일자: 요청일자(YYYYMMDD)
												Q_PM_RS_RMD_REQ.put(Q_PM_RS_RMD.SRC_CALL_NO.name()       , messageRequest.getSnd_tel_no());									// 발송번호(서비스기관 발송전화번호)
												Q_PM_RS_RMD_REQ.put(Q_PM_RS_RMD.MSG_TYPE.name()          , RedisStructure.Q_PM_RS_RMD.FIELD.MSG_TYPE.SMS.val());			// 메시지 타입 (SMS 고정)
												Q_PM_RS_RMD_REQ.put(Q_PM_RS_RMD.SNDN_MGMT_SEQ.name()     , messageRequest.getSndn_mgnt_seq());								// 요청그룹코드	: 유니크한 그룹코드값 (통계 요청/응답 메시지의 key로 사용)
												Q_PM_RS_RMD_REQ.put(Q_PM_RS_RMD.MULTI_MBL_PRC_TYPE.name(), messageRequest.getMulti_mbl_prc_type());							// 다회선 사용자 처리여부("2":다회선 발송 제외(default), "3":다회선 중 임의 1회선 발송)
												Q_PM_RS_RMD_REQ.put(Q_PM_RS_RMD.MESSAGE_ID.name()        , messageRequest.getSndn_mgnt_seq() + "_" + req.getSndn_seq_no());	// 메시지 일련번호 : "발송요청관리번호"_"발송요청일련번호
												Q_PM_RS_RMD_REQ.put(Q_PM_RS_RMD.REG_DT.name()            , now);															// 등록 일시
												Q_PM_RS_RMD_REQ.put(Q_PM_RS_RMD.SEQ_NO.name()            , req.getSndn_seq_no());											// 일련번호		
												Q_PM_RS_RMD_REQ.put(Q_PM_RS_RMD.IPIN_CI.name()           , req.getCi());													// 아이핀 CI 값
												Q_PM_RS_RMD_REQ.put(Q_PM_RS_RMD.MSG.name()               , req.getMms_dtl_cnts());											// MMS 메시지 상세내용
												Q_PM_RS_RMD_REQ.put(Q_PM_RS_RMD.MSG_TITLE.name()         , req.getMms_title());												// 메시지 제목
												Q_PM_RS_RMD_REQ.put(Q_PM_RS_RMD.MDN.name()               , req.getMdn());													// 개인휴대전화번호

												return Q_PM_RS_RMD_REQ;
											  }
										).collect(toList());
	}

	/**
	 * 사전문자 매핑
	 * @param messageRequest
	 * @return List<HashMap<String, Object>>
	 */
	private List<Map<String, Object>> mappingBeforeheadMessageRequest(BeforehandRequest messageRequest)
	{
		// 레디스 'Q_PM_RS' 형태에 맞게 변환
		final String now = FORMATTER.yyyyMMddHHmmss.val().format(messageRequest.getMills());

		return messageRequest.getReqs()
								.stream()
									.map(req->{
												Map<String, Object> Q_PM_RS_REQ = new HashMap<>();

												Q_PM_RS_REQ.put(Q_PM_RS.SVC_ORG_CD.name()        , messageRequest.getService_cd());								// 기관코드 		: 수요기관 관리 구분코드
												Q_PM_RS_REQ.put(Q_PM_RS.SVC_ORG_NM.name()        , messageRequest.getBiz_nm());									// 기관명			: 수요기관 관리 구분 이름
												Q_PM_RS_REQ.put(Q_PM_RS.CN_KEY.name()            , messageRequest.getMsg_cd());									// 문서구분코드
												Q_PM_RS_REQ.put(Q_PM_RS.CN_FORM.name()           , messageRequest.getMsg_nm());									// 문서명	
												Q_PM_RS_REQ.put(Q_PM_RS.MSG_TYPE.name()          , messageRequest.getMsgTypeCode());							// 메시지 타입     LMS,RCS로만 구분되어짐.
												Q_PM_RS_REQ.put(Q_PM_RS.TRANS_DT.name()          , messageRequest.getMake_dt());								// 요청일자		: 요청일자 (YYYYMMDD)
												Q_PM_RS_REQ.put(Q_PM_RS.SNDN_EX_TIME.name()      , messageRequest.getSndn_ex_time());							// 발송마감시간		: 메시지 발송에 대한 마감시간 
												Q_PM_RS_REQ.put(Q_PM_RS.SRC_CALL_NO.name()       , messageRequest.getSnd_tel_no());								// 발송번호(서비스기관 발송전화번호)
												Q_PM_RS_REQ.put(Q_PM_RS.RCS_BR_ID.name()         , messageRequest.getBrand_id());								// RCS 브랜드 아이디
												Q_PM_RS_REQ.put(Q_PM_RS.OPT_TYPE.name()          , messageRequest.getOpt_type());								// 고지발송 구분 	: 0(OPT-IN 사전,본문),1(OPT-OUT 본문),2(OPT-OUT 사전),3(OPT-OUT 하이브리드)
												Q_PM_RS_REQ.put(Q_PM_RS.SNDN_MGMT_SEQ.name()     , messageRequest.getSndn_mgnt_seq());							// 요청그룹코드		: 유니크한 그룹코드값 (통계 요청/응답 메시지의 key로 사용)
												Q_PM_RS_REQ.put(Q_PM_RS.SEQ_NO.name()            , req.getSndn_seq_no());										// 일련번호		
												Q_PM_RS_REQ.put(Q_PM_RS.IPIN_CI.name()           , req.getCi());
												Q_PM_RS_REQ.put(Q_PM_RS.MDN.name()               , req.getMdn());												// 개인 휴대전화번호
												Q_PM_RS_REQ.put(Q_PM_RS.URL.name()               , req.getUrl());												// 사전문자 본인인증 URL
												Q_PM_RS_REQ.put(Q_PM_RS.RCVE_RF_STR.name()       , req.getRcve_rf_str());										// 수신거부 및 수신 휴대폰 지정하기 치환문구
												Q_PM_RS_REQ.put(Q_PM_RS.INFO_CFRM_STR.name()     , req.getInfo_cfrm_str());										// 안내문 확인하기 치환문
												Q_PM_RS_REQ.put(Q_PM_RS.MSG.name()               , messageRequest.getBfh_ltr_cnts());							// 메시지 상세내용
												Q_PM_RS_REQ.put(Q_PM_RS.RCS_MSG.name()           , messageRequest.getBfh_ltr_cnts());							// RCS 메시지 상세내용
												Q_PM_RS_REQ.put(Q_PM_RS.MULTI_MBL_PRC_TYPE.name(), messageRequest.getMulti_mbl_prc_type());						// 다회선 사용자 처리여부("1":다회선 모두 발송, "2":다회선 발송 제외(default), "3":다회선 중 임의 1회선 발송)
												Q_PM_RS_REQ.put(Q_PM_RS.TEST_SNDN_YN.name()      , messageRequest.getTest_sndn_yn());							// 테스트 발송여부("Y":테스트 발송(KISA 연동, 통계, 이력 제외), "N":실발송)
												Q_PM_RS_REQ.put(Q_PM_RS.MSG_TITLE.name()         , messageRequest.getBfh_ltr_ttl());							// 메시지 제목
												Q_PM_RS_REQ.put(Q_PM_RS.MESSAGE_ID.name()        , messageRequest.getSndn_mgnt_seq()+"_"+req.getSndn_seq_no());	// 메시지 일련번호 	: ("발송요청관리번호"_"발송요청일련번호) 
												Q_PM_RS_REQ.put(Q_PM_RS.MMS_IMG_SIZE.name()      , 0); 															// 이미지 정보 받는게 없어 0으로 추가
												Q_PM_RS_REQ.put(Q_PM_RS.REG_DT.name()            , now);														// 등록 일시

												if(messageRequest.getMsgTypeCode().equals("R"))
												{
													Q_PM_RS_REQ.put(Q_PM_RS.RCS_AGENCY_ID.name() , messageRequest.getAgency_id());								// 대행사 ID
												}

												Q_PM_RS_REQ.put(Q_PM_RS.MSG_SIZE.name()          , messageRequest.getBfh_ltr_cnts().getBytes().length);			// 메시지 사이즈		: <FIXME: RCS 일 경우 사이즈 필요>
												Q_PM_RS_REQ.put(Q_PM_RS.MMS_IMG_NM.name()        , 0); 															// 이미지 정보 받는게 없어 0으로 추가

												return Q_PM_RS_REQ;
											  }
										).collect(toList());
	}

	/**
	 * 본문자 매핑
	 * @param messageRequest
	 * @return List<HashMap<String, Object>>
	 */
	private List<Map<String, Object>> mappingMessageRequest(MessageRequest messageRequest, String now, String nowForAttachId)
	{
        class Counter {
            private long count;

            Counter() {
                this.count = 0;
            }

            private long inc() {
            	return this.count++;
            }
        }

        Counter counter = new Counter();

		// 레디스 'Q_PM_RS' 형태에 맞게 변환
		return messageRequest.getReqs()
								.stream()
									.map(req->{
												Map<String, Object> Q_PM_RS_REQ = new HashMap<>();

												Q_PM_RS_REQ.put(Q_PM_RS.SVC_ORG_CD.name(), messageRequest.getService_cd());	// 기관코드	: 수요기관 관리 구분코드
												Q_PM_RS_REQ.put(Q_PM_RS.SVC_ORG_NM.name(), messageRequest.getBiz_nm());		// 기관명		: 수요기관 관리 구분 이름
												Q_PM_RS_REQ.put(Q_PM_RS.CN_KEY.name()    , messageRequest.getMsg_cd());		// 문서구분코드
												Q_PM_RS_REQ.put(Q_PM_RS.CN_FORM.name()   , messageRequest.getMsg_nm());		// 문서명

												// 메시지 타입 : S/L/M/R 판단 msg_type 1: RCS 2:Xms
												if(1 == messageRequest.getMsg_type())
												{
													Q_PM_RS_REQ.put(Q_PM_RS.RCS_AGENCY_ID.name(), messageRequest.getAgency_id());
													Q_PM_RS_REQ.put(Q_PM_RS.MSG_TYPE.name()     , "R");

													String attachId = "LGU_MPS_" + messageRequest.getMsg_cd() + "_" + messageRequest.getBrand_id() + "_" + messageRequest.getSndn_mgnt_seq() + "_" + nowForAttachId + '_' + String.format("%03d", counter.inc());

													Q_PM_RS_REQ.put(Q_PM_RS.RCS_ATTACH_ID.name(), attachId);

													if (StringUtils.hasText(messageRequest.getMms_binary()))	
													{
														Q_PM_RS_REQ.put(Q_PM_RS.MMS_IMG_SIZE.name(), messageRequest.getMms_binary().getBytes().length);
													}
													else
													{
														Q_PM_RS_REQ.put(Q_PM_RS.MMS_IMG_SIZE.name(), 0);
													}
												}
												// getMsg_type == 2 인경우 L/M을 판단
												else
												{
													if (StringUtils.hasText(messageRequest.getMms_binary()) || StringUtils.hasText(req.getMms_binary()))
													{
														Q_PM_RS_REQ.put(Q_PM_RS.MSG_TYPE.name(), "M");
														
														//개별이미지 존재시 개별이미지의 크기를 셋팅 개별이미지 미존재시 공통이미지 크기를 셋팅
														if (StringUtils.hasText(req.getMms_binary()))	
														{ 
															Q_PM_RS_REQ.put(Q_PM_RS.MMS_IMG_SIZE.name(), req.getMms_binary().getBytes().length); 						
														}
														else
														{
															Q_PM_RS_REQ.put(Q_PM_RS.MMS_IMG_SIZE.name(), messageRequest.getMms_binary().getBytes().length); 				
														}
													}
													else
													{
														Q_PM_RS_REQ.put(Q_PM_RS.MSG_TYPE.name()    , "L");
														Q_PM_RS_REQ.put(Q_PM_RS.MMS_IMG_SIZE.name(), 0);	// XMS 둘다 mms_binary가 없는경우는  LMS로 판단되어 mms 미존재 사이즈 0
													}
												}

												Q_PM_RS_REQ.put(Q_PM_RS.TRANS_DT.name()          , messageRequest.getMake_dt());									// 요청일자: 요청일자(YYYYMMDD)
												Q_PM_RS_REQ.put(Q_PM_RS.SNDN_EX_TIME.name()      , messageRequest.getSndn_ex_time());								// 발송마감시간
												Q_PM_RS_REQ.put(Q_PM_RS.EXPIRE_DT.name()         , messageRequest.getEx_time());									// 열람마감시간
												Q_PM_RS_REQ.put(Q_PM_RS.SRC_CALL_NO.name()       , messageRequest.getSnd_tel_no());									// 발송번호(서비스기관 발송전화번호)
												Q_PM_RS_REQ.put(Q_PM_RS.RCS_BR_ID.name()         , messageRequest.getBrand_id());									// RCS 브랜드 아이디			
												Q_PM_RS_REQ.put(Q_PM_RS.OPT_TYPE.name()          , messageRequest.getOpt_type());									// 고지발송 구분 : 0(OPT_IN 사전,본문),1(OPT-OUT 본문),2(OPT-OUT 사전),3(OPT-OUT 하이브리드)
												Q_PM_RS_REQ.put(Q_PM_RS.SNDN_MGMT_SEQ.name()     , messageRequest.getSndn_mgnt_seq());								// 요청그룹코드	: 유니크한 그룹코드값 (통계 요청/응답 메시지의 key로 사용)
												Q_PM_RS_REQ.put(Q_PM_RS.SND_PLFM_ID.name()       , messageRequest.getSnd_plfm_id());								// 송신자 플랫폼 ID(포스토피아)
												Q_PM_RS_REQ.put(Q_PM_RS.SND_NPOST.name()         , messageRequest.getSnd_npost());									// 송신 공인전자주소(포스토피아)
												Q_PM_RS_REQ.put(Q_PM_RS.SND_DATE.name()          , messageRequest.getSnd_date());									// 송신일시(포스토피아)
												Q_PM_RS_REQ.put(Q_PM_RS.MESSAGE_ID.name()        , messageRequest.getSndn_mgnt_seq() + "_" + req.getSndn_seq_no());	// 메시지 일련번호 : "발송요청관리번호"_"발송요청일련번호
												Q_PM_RS_REQ.put(Q_PM_RS.REG_DT.name()            , now);															// 등록 일시
												Q_PM_RS_REQ.put(Q_PM_RS.TKN_RPMT_YN.name()       , messageRequest.getTkn_rpmt_yn());								// 토큰확인대체여부
												Q_PM_RS_REQ.put(Q_PM_RS.RDNG_RPMT_YN.name()      , messageRequest.getRdng_rpmt_yn());								// 열람확인대체여부
												Q_PM_RS_REQ.put(Q_PM_RS.TEST_SNDN_YN.name()      , messageRequest.getTest_sndn_yn());								// 테스트 발송여부("Y":테스트 발송(KISA 연동, 통계, 이력 제외), "N":실발송)												
												Q_PM_RS_REQ.put(Q_PM_RS.MULTI_MBL_PRC_TYPE.name(), messageRequest.getMulti_mbl_prc_type());							// 다회선 사용자 처리여부("2":다회선 발송 제외(default), "3":다회선 중 임의 1회선 발송)
												Q_PM_RS_REQ.put(Q_PM_RS.REOPEN_DAY.name()        , messageRequest.getReopen_day());									// 재열람 일수
												Q_PM_RS_REQ.put(Q_PM_RS.KISA_DOC_TYPE.name()     , messageRequest.getKisa_doc_type());								// 전자문서 유형(null:일반문서, "0":일반문서,"1":알림서비스, "2":알림서비스(열람일시 미생성))
												Q_PM_RS_REQ.put(Q_PM_RS.SEQ_NO.name()            , req.getSndn_seq_no());											// 일련번호		
												Q_PM_RS_REQ.put(Q_PM_RS.IPIN_CI.name()           , req.getCi());													// 아이핀 CI 값
												Q_PM_RS_REQ.put(Q_PM_RS.MSG.name()               , req.getMms_dtl_cnts());											// MMS 메시지 상세내용
												Q_PM_RS_REQ.put(Q_PM_RS.MSG_TITLE.name()         , req.getMms_title());												// 메시지 제목
												Q_PM_RS_REQ.put(Q_PM_RS.RCS_MSG.name()           , req.getRcs_dtl_cnts());											// RCS 메시지 상세내용
												Q_PM_RS_REQ.put(Q_PM_RS.REDIRECT_URL.name()      , req.getUrl());													// 리다이렉트 URL
												Q_PM_RS_REQ.put(Q_PM_RS.DOC_HASH.name()          , req.getDoc_hash());												// 문서해시
												Q_PM_RS_REQ.put(Q_PM_RS.FILE_FMAT.name()         , req.getFile_fmat());												// 파일포맷(확장자)
												Q_PM_RS_REQ.put(Q_PM_RS.MDN.name()               , req.getMdn());													// 개인휴대전화번호
												Q_PM_RS_REQ.put(Q_PM_RS.DIST_INFO_CRT_YN.name()  , req.getDist_info_crt_yn());										// 유통정보생성여부
												Q_PM_RS_REQ.put(Q_PM_RS.INFO_CFRM_STR.name()     , req.getInfo_cfrm_str());											// 안내문 확인하기 치환문구
												Q_PM_RS_REQ.put(Q_PM_RS.RCVE_RF_STR.name()       , req.getRcve_rf_str());											// 수신거부 및 수신 휴대폰 지정하기 치환문구

												return Q_PM_RS_REQ;
											  }
										).collect(toList());
	}

	/**
	 * 리포트 요청 매핑
	 * @param ReportRequest
	 * @return List<HashMap<String, Object>>
	 */
	private List<Map<String, Object>> mappingReportRequest(ReportRequest messageRequest, String now)
	{
		// 레디스 'Q_PM_RPT_RQ' 형태에 맞게 변환
		return messageRequest.getReqs()
								.parallelStream()
									.map(req->{
												Map<String, Object> REQ = new HashMap<>();

												REQ.put(Q_PM_RPT_RQ.MESSAGE_ID.name(), req.getSndn_mgnt_seq() + "_" + req.getSndn_seq_no());	// 메시지 일련번호 : "발송요청관리번호"_"발송요청일련번호
												REQ.put(Q_PM_RPT_RQ.PART_MM.name()   , req.getPart_mm());										// 전송 요청 파티션 정보		

												return REQ;
											  }
										).collect(toList());
	}

	/**
	 * 메시지 이미지 매핑 및 Redis 적재
	 * @param messageRequest
	 */
	private void putMessageImages(MessageRequest messageRequest, String NOWForAttachId) throws IOException
	{
		//msg_type(1:RCS)이며 mms_binary에 데이터 존재시 RCS 사전이미지 등록 요청 큐 적재 추가    
		if (1 == messageRequest.getMsg_type() && !"".equals(messageRequest.getMms_binary()))
		{
			registerRcsImageForMsg(messageRequest, NOWForAttachId);
		}

		//공통이미지에만 mms_binary가 들어오는 경우도 있으므로 .filter(req-StringUtils.hasText(req.getMms_binary())) 주석처리
		messageRequest.getReqs()
						.stream()
							//.filter(req->StringUtils.hasText(req.getMms_binary()))
							.forEach(req->{
											String messageId = messageRequest.getSndn_mgnt_seq() + "_" + req.getSndn_seq_no();
											
											String slmrType = "";
											// 메시지 타입 : S/L/M/R 판단
											if(1 == messageRequest.getMsg_type())
											{
												slmrType = "R";																
											}
											// getMsg_type == 2 인경우 L,M을 판단
											else
											{
												if (StringUtils.hasText(messageRequest.getMms_binary()) || StringUtils.hasText(req.getMms_binary()))
												{
													slmrType = "M";
												}
												else
												{
													slmrType = "L";
												}
											}
										
											// MMS 이거나, rcs_fallback_yn 이 'Y' 일 경우
											if (slmrType.equals("M"))
											{
												byte[] getMmsbinary = null;
												
												//공통이미지와 개별이미지 필드의 우선순위 적용. 개별이미지값이 존재시 개별이미지 값을 Redis 적재 
												if (StringUtils.hasText(req.getMms_binary()))
												{
													getMmsbinary = Base64.getDecoder().decode(req.getMms_binary());
												}
												//개별이미지값이 미존재시 공통이미지값을 Redis 적재
												else
												{
													getMmsbinary = Base64.getDecoder().decode(messageRequest.getMms_binary());
												}
												
												redisRcsImage.hmsetObj(RedisStructure.H_PM_MMS_FILE.STRUCTURE_NAME, messageId, getMmsbinary);
											}
											//RCS이미지인경우에는 공통이미지값을 Redis 적재
											else if (slmrType.equals("R"))
											{
												byte[] getMmsbinary = Base64.getDecoder().decode(messageRequest.getMms_binary());
												
												redisRcsImage.hmsetObj(RedisStructure.H_PM_MMS_FILE.STRUCTURE_NAME, messageId, getMmsbinary);
											}
										}
									);
	}

	/**
	 * RCS 이미지 업로드
	 * @param attachId
	 * @param image
	 * @return
	 * @throws IOException 
	 * @throws IllegalStateException 
	 */
	@Override
	public void registerRcsImageForMsg(MessageRequest messageRequest, String NOWForAttachId) throws IllegalStateException, IOException, FailRegisterRcsImageException
	{
		HashMap<String, Object> imageData = new HashMap<>();

		String attachId = "LGU_MPS_" + messageRequest.getMsg_cd() + "_" + messageRequest.getBrand_id() + "_" + messageRequest.getSndn_mgnt_seq() + "_" + NOWForAttachId;

		imageData.put(Q_PM_RCS_IMG.RCS_ATTACH_ID.name(), attachId);
		imageData.put(Q_PM_RCS_IMG.SVC_ORG_NM.name()   , messageRequest.getBiz_nm());		// 기관명 
		imageData.put(Q_PM_RCS_IMG.SVC_ORG_CD.name()   , messageRequest.getService_cd());	// 기관코드
		imageData.put(Q_PM_RCS_IMG.FILE_FORMAT.name()  , messageRequest.getFile_fmat());	// 파일포맷

		byte[] getMmsbinary = Base64.getDecoder().decode(messageRequest.getMms_binary());

		redisRcsImage.hmsetObj(RedisStructure.H_PM_RCS_FILE.STRUCTURE_NAME, attachId, getMmsbinary);

		Long index = redisRcsImage.lpush(Q_PM_RCS_IMG.STRUCTURE_NAME, imageData);

		if (0 > index)
		{
			throw new FailRegisterRcsImageException();
		}
	}

	private <T, R, E extends Exception> Function<T, R> wrapper(FunctionThrowsException<T, R, E> fe)
	{
        return arg -> {
            try
            {
                return fe.apply(arg);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        };
    }
}
