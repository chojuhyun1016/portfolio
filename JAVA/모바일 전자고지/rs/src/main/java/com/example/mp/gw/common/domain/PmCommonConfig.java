package com.example.mp.gw.common.domain;


import static com.example.mp.gw.common.domain.RedisStructure.S_PM_COMMON.*;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.mp.gw.common.domain.RedisStructure.S_PM_COMMON;
import com.example.mp.gw.common.service.RedisService;
import com.example.mp.gw.common.utils.NumberUtil;
import com.example.mp.gw.common.utils.StringUtil;

import lombok.Getter;
import lombok.ToString;

/**
 * @Class Name : PmCommonConfig.java
 * @Description : PM_COMMON 해시테이블 정보를 가지고 있는 객체
 * 
 * @author 조주현
 * @since 2021.04.02
 * @version 1.0
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.02	    조주현          최초 생성
 * 
 *      </pre>
 * 
 */


@Getter
@ToString
@Component
public class PmCommonConfig
{
	// 파일 백업 유지시간
	private Integer fileBkupPeriodH;

	// 리포트 만료시간
	private Integer timeoutH;

	// 우선순위 분배건수
	private String priorityRatio;

	// expire_ts 처리 프로세스ID
	private String expireTs;

	// 실시간 통계 처리 프로세스ID
	private String realstatTs;

	// 유플러스 내부용 매핑요청처리 프로세스ID
	private String uplusMappingReqTs;

	// 수신번호 중복 체크시간
	private Integer dupChkSec;

	// 재처리 건수
	private Integer retryCnt;

	// 재처리 시간(초)
	private Integer retrySec;

	// 동의요청 만료일
	private Integer mappingExpDay;

	// 회신번호
	private String callback;

	// 알람사용여부
	private String alarmYn;

	// 알람 수신번호 목록
	private String alarmPhone;

	// 동의문자 제목
	private String mappingSubject;

	// 동의문자 내용(SMS)
	private String mappingSmsMsg;

	// 동의문자 내용(MMS)
	private String mappingMmsMsg;

	// 모바일웹 약관1
	private String mappingContents1;

	// 모바일웹 약관2
	private String mappingContents2;

	// 모바일웹 약관3
	private String mappingContents3;

	// 모바일웹 약관4
	private String mappingContents4;

	// 모바일웹 도메인
	private String mwebDomain;

	// 유플러스 기관코드
	private String uplusSvcOrgCd;

	// KT 접근 아이피 목록
	private String[] ktIpList;


	public PmCommonConfig(RedisService<String, Map<String, Object>> redisService)
	{
		Map<String, Object> sPmCommon = redisService.get(S_PM_COMMON.STRUCTURE_NAME);

		if (null == sPmCommon)
			return;
		
		fileBkupPeriodH 	= NumberUtil.toIntegerFrom(	sPmCommon.get(FILE_BKUP_PERIOD_H.name())	);
		timeoutH 			= NumberUtil.toIntegerFrom(	sPmCommon.get(TIMEOUT_H.name())				);
		priorityRatio 		= StringUtil.toStringFrom(	sPmCommon.get(PRIORITY_RATIO.name())		);
		expireTs 			= StringUtil.toStringFrom(	sPmCommon.get(EXPIRE_TS.name())				);
		realstatTs 			= StringUtil.toStringFrom(	sPmCommon.get(REALSTAT_TS.name())			);
		uplusMappingReqTs 	= StringUtil.toStringFrom(	sPmCommon.get(UPLUS_MAPPING_REQ_TS.name())	);
		dupChkSec 			= NumberUtil.toIntegerFrom(	sPmCommon.get(DUP_CHK_SEC.name())			);
		retryCnt 			= NumberUtil.toIntegerFrom(	sPmCommon.get(RETRY_CNT.name())				);
		retrySec 			= NumberUtil.toIntegerFrom(	sPmCommon.get(RETRY_SEC.name())				);
//		mappingExpDay 		= NumberUtil.toIntegerFrom(	sPmCommon.get(MAPPING_EXP_DAY.name())		);
		callback 			= StringUtil.toStringFrom(	sPmCommon.get(CALLBACK.name())				);
		alarmYn 			= StringUtil.toStringFrom(	sPmCommon.get(ALARM_YN.name())				);
		alarmPhone 			= StringUtil.toStringFrom(	sPmCommon.get(ALARM_PHONE.name())			);
		mappingSubject 		= StringUtil.toStringFrom(	sPmCommon.get(MAPPING_SUBJECT.name())		);
//		mappingSmsMsg 		= StringUtil.toStringFrom(	sPmCommon.get(MAPPING_SMS_MSG.name())		);
//		mappingMmsMsg 		= StringUtil.toStringFrom(	sPmCommon.get(MAPPING_MMS_MSG.name())		);
//		mappingContents1 	= StringUtil.toStringFrom(	sPmCommon.get(MAPPING_CONTENTS1.name())		);
//		mappingContents2 	= StringUtil.toStringFrom(	sPmCommon.get(MAPPING_CONTENTS2.name())		);
//		mappingContents3 	= StringUtil.toStringFrom(	sPmCommon.get(MAPPING_CONTENTS3.name())		);
//		mappingContents4 	= StringUtil.toStringFrom(	sPmCommon.get(MAPPING_CONTENTS4.name())		);
//		mwebDomain 			= StringUtil.toStringFrom(	sPmCommon.get(MWEB_DOMAIN.name())			);
		uplusSvcOrgCd 		= StringUtil.toStringFrom(	sPmCommon.get(UPLUS_SVC_ORG_CD.name())		);
		ktIpList 			= StringUtil.toStringFrom(	sPmCommon.get(KT_IP_LIST.name())			).split(",");
	}
}
