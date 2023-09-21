package com.example.mp.gw.common.domain;


/**
 * @Class Name : RedisStructure.java
 * @Description : 레디스 구조 객체
 * 
 * @author 조주현
 * @since 2021.03.30
 * @version 1.0
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.03.30	    조주현          최초 생성
 * 
 *      </pre>
 * 
 */


public class RedisStructure
{
	public static interface Q_MB_REG_RQ
	{
		public final static String STRUCTURE_NAME 		= "Q_MB_REG_RQ";
		public final static String STRUCTURE_DESC 		= "";
	}

	public static interface Q_MB_MOD_RQ
	{
		public final static String STRUCTURE_NAME 		= "Q_MB_MOD_RQ";
		public final static String STRUCTURE_DESC 		= "";
	}

	public static interface Q_MB_DEL_RQ
	{
		public final static String STRUCTURE_NAME 		= "Q_MB_DEL_RQ";
		public final static String STRUCTURE_DESC 		= "";
	}

	public static interface Q_REG_AGREE_RQ
	{
		public final static String STRUCTURE_NAME 		= "Q_REG_AGREE_RQ";
		public final static String STRUCTURE_DESC 		= "";
	}

	public static interface Q_DEL_AGREE_RQ
	{
		public final static String STRUCTURE_NAME 		= "Q_DEL_AGREE_RQ";
		public final static String STRUCTURE_DESC 		= "";
	}

	public static interface Q_REG_REJECTION_RQ
	{
		public final static String STRUCTURE_NAME 		= "Q_REG_REJECTION_RQ";
		public final static String STRUCTURE_DESC 		= "";
	}

	public static interface Q_DEL_REJECTION_RQ
	{
		public final static String STRUCTURE_NAME 		= "Q_DEL_REJECTION_RQ";
		public final static String STRUCTURE_DESC 		= "";
	}

	public static interface Q_REG_WHITE_LIST_RQ
	{
		public final static String STRUCTURE_NAME 		= "Q_REG_WHITE_LIST_RQ";
		public final static String STRUCTURE_DESC 		= "";
	}

	public static interface Q_DEL_WHITE_LIST_RQ
	{
		public final static String STRUCTURE_NAME 		= "Q_DEL_WHITE_LIST_RQ";
		public final static String STRUCTURE_DESC 		= "";
	}

	public static interface S_PM_COMMON
	{
		public final static String STRUCTURE_NAME 		= "S_PM_COMMON";
		public final static String STRUCTURE_DESC 		= "";
		public final static Field FILE_BKUP_PERIOD_H	= Field.builder().name("FILE_BKUP_PERIOD_H")	.desc("파일 백업 유지시간").build(); 
		public final static Field TIMEOUT_H 			= Field.builder().name("TIMEOUT_H")				.desc("리포트 만료시간").build(); 
		public final static Field PRIORITY_RATIO 		= Field.builder().name("PRIORITY_RATIO")		.desc("우선순위 분배건수").build(); 
		public final static Field EXPIRE_TS				= Field.builder().name("EXPIRE_TS")				.desc("expire_ts 처리 프로세스ID").build(); 
		public final static Field REALSTAT_TS			= Field.builder().name("REALSTAT_TS")			.desc("실시간 통계 처리 프로세스ID").build(); 
		public final static Field UPLUS_MAPPING_REQ_TS	= Field.builder().name("UPLUS_MAPPING_REQ_TS")	.desc("유플러스 내부용 매핑요청처리 프로세스ID").build(); 
		public final static Field DUP_CHK_SEC			= Field.builder().name("DUP_CHK_SEC")			.desc("수신번호 중복 체크시간").build(); 
		public final static Field RETRY_CNT				= Field.builder().name("RETRY_CNT")				.desc("재처리 건수").build(); 
		public final static Field RETRY_SEC				= Field.builder().name("RETRY_SEC")				.desc("재처리 시간(초)").build(); 
		public final static Field CALLBACK				= Field.builder().name("CALLBACK")				.desc("회신번호").build(); 
		public final static Field ALARM_YN				= Field.builder().name("ALARM_YN")				.desc("알람사용여부").build(); 
		public final static Field ALARM_PHONE			= Field.builder().name("ALARM_PHONE")			.desc("알람 수신번호 목록").build(); 
		public final static Field MAPPING_SUBJECT		= Field.builder().name("MAPPING_SUBJECT")		.desc("동의문자 제목").build(); 
		public final static Field UPLUS_SVC_ORG_CD		= Field.builder().name("UPLUS_SVC_ORG_CD")		.desc("유플러스 기관코드").build(); 
		public final static Field KT_IP_LIST			= Field.builder().name("KT_IP_LIST")			.desc("KT 접근 아이피 목록").build();
	}

	public static interface Q_PM_RCS_IMG
	{
		public final static String STRUCTURE_NAME 		= "Q_PM_RCS_IMG";
		public final static String STRUCTURE_DESC 		= "RCS 사전 이미지 큐";
		final static Field RCS_ATTACH_ID 				= Field.builder().name("RCS_ATTACH_ID")			.desc("RCS 첨부파일 아이디").build();
		final static Field RCS_IMG_SIZE					= Field.builder().name("RCS_IMG_SIZE")			.desc("RCS 이미지 크기").build();
		final static Field RCS_IMG_NM					= Field.builder().name("RCS_IMG_NM")			.desc("RCS 이미지 이름").build();
		final static Field RCS_FILE_PATH				= Field.builder().name("RCS_FILE_PATH")			.desc("RCS 이미지 위치").build();
		final static Field RCS_BR_ID					= Field.builder().name("RCS_BR_ID")				.desc("RCS 브랜드 아이디").build();
		final static Field FILE_FORMAT					= Field.builder().name("FILE_FORMAT")			.desc("RCS 파일 포맷").build();
		//본문자 전송시에 사용하는것으로 바뀌어서 소스정리 필요. 20210629
		final static Field SVC_ORG_NM					= Field.builder().name("SVC_ORG_NM")			.desc("RCS 기관명").build();
		final static Field SVC_ORG_CD					= Field.builder().name("SVC_ORG_CD")			.desc("RCS 기관코드").build();
	}

	public static interface H_PM_RCS_IMG
	{
		public final static String STRUCTURE_NAME 		= "H_PM_RCS_IMG";
		public final static String STRUCTURE_DESC 		= "RCS 사전 이미지 해시테이블";
		final static Field RCS_ATTACH_ID 				= Field.builder().name("RCS_ATTACH_ID")			.desc("RCS 첨부파일 아이디").build();
		final static Field RCS_IMG_SIZE					= Field.builder().name("RCS_IMG_SIZE")			.desc("RCS 이미지 크기").build();
		final static Field RCS_IMG_NM					= Field.builder().name("RCS_IMG_NM")			.desc("RCS 이미지 이름").build();
		final static Field RCS_FILE_PATH				= Field.builder().name("RCS_FILE_PATH")			.desc("RCS 이미지 위치").build();
		final static Field RCS_BR_ID					= Field.builder().name("RCS_BR_ID")				.desc("RCS 브랜드 아이디").build();
		final static Field SVC_ORG_NM					= Field.builder().name("SVC_ORG_NM")			.desc("RCS 기관명").build();
		final static Field SVC_ORG_CD					= Field.builder().name("SVC_ORG_CD")			.desc("RCS 기관코드").build();		
	}

	public static interface H_PM_RCS_FILE
	{
		public final static String STRUCTURE_NAME 		= "H_PM_RCS_FILE";
		public final static String STRUCTURE_DESC 		= "RCS 파일_해시";
		final static Field RCS_ATTACH_ID 				= Field.builder().name("RCS_ATTACH_ID")			.desc("RCS 첨부파일 아이디").build();
		final static Field RCS_FILE 					= Field.builder().name("RCS_FILE")				.desc("RCS 첨부파일").build();
	}

	public static interface H_PM_MMS_FILE
	{
		public final static String STRUCTURE_NAME 		= "H_PM_MMS_FILE";
		public final static String STRUCTURE_DESC 		= "MMS 파일_해시";
		final static Field MESSAGE_ID 					= Field.builder().name("MESSAGE_ID")			.desc("MESSAGE_ID").build();
		final static Field MSGKEY 						= Field.builder().name("MSGKEY")				.desc("MSGKEY").build();
		final static Field MMS_FILE						= Field.builder().name("MMS_FILE")				.desc("MMS 첨부파일").build();
	}

	public static interface Q_PM_RS_RMD
	{
		final static String STRUCTURE_NAME 		= "Q_PM_RS_RMD";
		final static String STRUCTURE_DESC 		= "";
		final static Field MESSAGE_ID 			= Field.builder().name("MESSAGE_ID")			.desc("").build();
		final static Field REG_DT 				= Field.builder().name("REG_DT")				.desc("").build();
		final static Field SVC_ORG_CD			= Field.builder().name("SVC_ORG_CD")			.desc("").build();
		final static Field SVC_ORG_NM			= Field.builder().name("SVC_ORG_NM")			.desc("").build();
		final static Field CN_KEY				= Field.builder().name("CN_KEY")				.desc("").build();
		final static Field CN_FORM				= Field.builder().name("CN_FORM")				.desc("").build();
		final static Field TRANS_DT				= Field.builder().name("TRANS_DT")				.desc("").build();
		final static Field SRC_CALL_NO 			= Field.builder().name("SRC_CALL_NO")			.desc("").build();
		final static Field MSG_TYPE 			= Field.builder().name("MSG_TYPE")				.desc("").build();
		final static Field SNDN_MGMT_SEQ 		= Field.builder().name("SNDN_MGMT_SEQ")			.desc("").build();
		final static Field MULTI_MBL_PRC_TYPE	= Field.builder().name("MULTI_MBL_PRC_TYPE")	.desc("").build();
		final static Field SEQ_NO 				= Field.builder().name("SEQ_NO")				.desc("").build();
		final static Field IPIN_CI 				= Field.builder().name("IPIN_CI")				.desc("").build();
		final static Field MSG 					= Field.builder().name("MSG")					.desc("").build();
		final static Field MSG_TITLE 			= Field.builder().name("MSG_TITLE")				.desc("").build();
		final static Field MDN 					= Field.builder().name("MDN")					.desc("").build();

		public static interface FIELD
		{
			public static interface MSG_TYPE
			{
				final static Code<String> SMS = Code.<String>builder().val("S").desc("").build();
				final static Code<String> MMS = Code.<String>builder().val("M").desc("").build();
				final static Code<String> LMS = Code.<String>builder().val("L").desc("").build();
				final static Code<String> RCS = Code.<String>builder().val("R").desc("").build();
			}
		}
	}

	public static interface Q_PM_RS
	{
		final static String STRUCTURE_NAME 		= "Q_PM_RS";
		final static String STRUCTURE_DESC 		= "";
		final static Field MESSAGE_ID 			= Field.builder().name("MESSAGE_ID")			.desc("").build();
		final static Field SVC_ORG_CD			= Field.builder().name("SVC_ORG_CD")			.desc("").build();
		final static Field SVC_ORG_NM			= Field.builder().name("SVC_ORG_NM")			.desc("").build();
		final static Field TRANS_DT				= Field.builder().name("TRANS_DT")				.desc("").build();
		final static Field SNDN_EX_TIME			= Field.builder().name("SNDN_EX_TIME")			.desc("").build();
		final static Field SEQ_NO 				= Field.builder().name("SEQ_NO")				.desc("").build();
		final static Field SVC_GRP_CD 			= Field.builder().name("SVC_GRP_CD")			.desc("").build();
		final static Field SNDN_MGMT_SEQ 		= Field.builder().name("SNDN_MGMT_SEQ")			.desc("").build();
		final static Field IPIN_CI 				= Field.builder().name("IPIN_CI")				.desc("").build();			
		final static Field MSG_TYPE 			= Field.builder().name("MSG_TYPE")				.desc("").build();
		final static Field MSG_INFO 			= Field.builder().name("MSG_INFO")				.desc("").build();
		final static Field MSG_SIZE 			= Field.builder().name("MSG_SIZE")				.desc("").build();
		final static Field MSG 					= Field.builder().name("MSG")					.desc("").build();
		final static Field MSG_TITLE 			= Field.builder().name("MSG_TITLE")				.desc("").build();
		final static Field RCS_MSG 				= Field.builder().name("RCS_MSG")				.desc("").build();
		final static Field TEST_SNDN_YN			= Field.builder().name("TEST_SNDN_YN")			.desc("").build();
		final static Field MULTI_MBL_PRC_TYPE	= Field.builder().name("MULTI_MBL_PRC_TYPE")	.desc("").build();
		final static Field REOPEN_DAY			= Field.builder().name("REOPEN_DAY")			.desc("").build();
		final static Field KISA_DOC_TYPE		= Field.builder().name("KISA_DOC_TYPE")			.desc("").build();
		final static Field MMS_IMG_SIZE 		= Field.builder().name("MMS_IMG_SIZE")			.desc("").build();
		final static Field MMS_IMG_NM 			= Field.builder().name("MMS_IMG_NM")			.desc("").build();
		final static Field PRIORITY 			= Field.builder().name("PRIORITY")				.desc("").build();
		final static Field REG_DT 				= Field.builder().name("REG_DT")				.desc("").build();
		final static Field CN_KEY 				= Field.builder().name("CN_KEY")				.desc("").build();
		final static Field CN_FORM 				= Field.builder().name("CN_FORM")				.desc("").build();
		final static Field OPT_TYPE 			= Field.builder().name("OPT_TYPE")				.desc("").build();
		final static Field SRC_CALL_NO 			= Field.builder().name("SRC_CALL_NO")			.desc("").build();
		final static Field RCS_BR_ID 			= Field.builder().name("RCS_BR_ID")				.desc("").build();		
		final static Field EXPIRE_DT			= Field.builder().name("EXPIRE_DT")				.desc("").build();
		final static Field RCS_AGENCY_ID		= Field.builder().name("RCS_AGENCY_ID")			.desc("").build();
		final static Field RCS_ATTACH_ID 		= Field.builder().name("RCS_ATTACH_ID")			.desc("").build();
		final static Field REDIRECT_URL			= Field.builder().name("REDIRECT_URL")			.desc("").build();
		final static Field DOC_HASH				= Field.builder().name("DOC_HASH")				.desc("").build();
		final static Field FILE_FMAT			= Field.builder().name("FILE_FMAT")				.desc("").build();
		final static Field TKN_RPMT_YN			= Field.builder().name("TKN_RPMT_YN")			.desc("").build();
		final static Field RDNG_RPMT_YN			= Field.builder().name("RDNG_RPMT_YN")			.desc("").build();
		final static Field MDN 					= Field.builder().name("MDN")					.desc("").build();
		final static Field URL           		= Field.builder().name("URL")				    .desc("").build();
		final static Field DIST_INFO_CRT_YN		= Field.builder().name("DIST_INFO_CRT_YN")	    .desc("").build();
		final static Field INFO_CFRM_STR   		= Field.builder().name("INFO_CFRM_STR")		    .desc("").build();
		final static Field RCVE_RF_STR      	= Field.builder().name("RCVE_RF_STR")			.desc("").build();
		final static Field SND_PLFM_ID   		= Field.builder().name("SND_PLFM_ID")			.desc("").build();
		final static Field SND_NPOST     		= Field.builder().name("SND_NPOST") 			.desc("").build();
		final static Field SND_DATE     		= Field.builder().name("SND_DATE")   			.desc("").build();

		public static interface FIELD
		{
			public static interface OPT_TYPE
			{
				final static Code<Integer> OPT_IN_MMS 			= Code.<Integer>builder().val(0)		.desc("OPT_IN 본문").build();
				final static Code<Integer> OPT_OUT_MMS 			= Code.<Integer>builder().val(1)		.desc("OPT_OUT 본문").build();
				final static Code<Integer> OPT_OUT_BEFOREHEAD 	= Code.<Integer>builder().val(2)		.desc("OPT_OUT 사전문자").build();
				final static Code<Integer> OPT_OUT_HYBRID 		= Code.<Integer>builder().val(3)		.desc("OPT_OUT 하이브리드").build();
			}

			public static interface SEND_TYPE
			{
				final static Code<String> AGREE = Code.<String>builder().val("0")		.desc("동의처리").build();
				final static Code<String> MSG 	= Code.<String>builder().val("1")		.desc("고지발송").build();
				final static Code<String> STAT 	= Code.<String>builder().val("2")		.desc("통게").build();
			}

			public static interface MSG_TYPE
			{
				final static Code<String> SMS = Code.<String>builder().val("S")		.desc("").build();
				final static Code<String> MMS = Code.<String>builder().val("M")		.desc("").build();
				final static Code<String> LMS = Code.<String>builder().val("L")		.desc("").build();
				final static Code<String> RCS = Code.<String>builder().val("R")		.desc("").build();
			}

			public static interface MSG
			{
				final static Code<String> header 		= Code.<String>builder().val("header")		.desc("").build();
				final static Code<String> footer 		= Code.<String>builder().val("footer")		.desc("").build();
				final static Code<String> copyAllowed 	= Code.<String>builder().val("copyAllowed")	.desc("").build();
				final static Code<String> buttons 		= Code.<String>builder().val("buttons")		.desc("").build();
				final static Code<String> suggeststions = Code.<String>builder().val("suggestions")	.desc("").build();
				final static Code<String> action 		= Code.<String>builder().val("action")		.desc("").build();
				final static Code<String> urlAction		= Code.<String>builder().val("urlAction")	.desc("").build();
				final static Code<String> openUrl 		= Code.<String>builder().val("openUrl")		.desc("").build();
				final static Code<String> url 			= Code.<String>builder().val("url")			.desc("").build();
				final static Code<String> displayText	= Code.<String>builder().val("displayText")	.desc("").build();
				final static Code<String> postback 		= Code.<String>builder().val("postback")	.desc("").build();
				final static Code<String> data 			= Code.<String>builder().val("data")		.desc("").build();
				final static Code<String> body 			= Code.<String>builder().val("body")		.desc("").build();
				final static Code<String> media 		= Code.<String>builder().val("media")		.desc("").build();
				final static Code<String> description 	= Code.<String>builder().val("description")	.desc("").build();
				final static Code<String> title			= Code.<String>builder().val("title")		.desc("").build();
			}

			public static interface header
			{
				final static Code<String> info 		= Code.<String>builder().val("0")	.desc("").build();
				final static Code<String> ad		= Code.<String>builder().val("1")	.desc("").build();
				
			}
		}
	}

	public static interface Q_PM_RPT_RQ
	{
		final static String STRUCTURE_NAME	= "Q_PM_RPT_RQ";
		final static String STRUCTURE_DESC 	= "";
		final static Field MESSAGE_ID 		= Field.builder().name("MESSAGE_ID").desc("").build();
		final static Field PART_MM 			= Field.builder().name("PART_MM")	.desc("").build();
	}

	public static interface Q_MB_BFH_RQ
	{
		public final static String STRUCTURE_NAME 		= "Q_MB_BFH_RQ";
		public final static String STRUCTURE_DESC 		= "";

		final static Field IPIN_CI 		= Field.builder().name("IPIN_CI")		.desc("").build();
		final static Field MESSAGE_ID 	= Field.builder().name("MESSAGE_ID")	.desc("").build();
		final static Field TRANS_DT		= Field.builder().name("TRANS_DT")		.desc("").build();
	}

	public static interface Q_KISA_DOC_REG_RD
	{
		public final static String STRUCTURE_NAME 		= "Q_KISA_DOC_REG_RD";
		public final static String STRUCTURE_DESC 		= "";
	}

	public static interface Q_DOC_ISS
	{
		public final static String STRUCTURE_NAME 		= "Q_DOC_ISS";
		public final static String STRUCTURE_DESC 		= "";
	}

	public static interface Q_DOC_STS_CFM_ISS
	{
		public final static String STRUCTURE_NAME 		= "Q_DOC_STS_CFM_ISS";
		public final static String STRUCTURE_DESC 		= "";
	}

	public static interface Q_BC_SESSION
	{
		public final static String STRUCTURE_NAME 		= "Q_BC_SESSION";
		public final static String STRUCTURE_DESC 		= "";
	}

	public static interface H_BC_SESSION
	{
		public final static String STRUCTURE_NAME 		= "H_BC_SESSION";
		public final static String STRUCTURE_DESC 		= "";
	}

	public static interface Q_KISA_SESSION
	{
		public final static String STRUCTURE_NAME 		= "Q_KISA_SESSION";
		public final static String STRUCTURE_DESC 		= "";
	}

	public static interface H_KISA_SESSION
	{
		public final static String STRUCTURE_NAME 		= "H_KISA_SESSION";
		public final static String STRUCTURE_DESC 		= "";
	}
}
