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
 *  2021.03.30	     조주현           최초 생성
 * 
 *      </pre>
 * 
 */


public class RedisStructure
{
	public static interface H_SINGLE_WORKER
	{
		public final static String STRUCTURE_NAME	= "service:dm:worker";
		public final static String STRUCTURE_DESC	= "";

		final static Code<String> PRIMARY   = Code.<String>builder().val("1").desc("장비-1번").build();
		final static Code<String> SECONDARY = Code.<String>builder().val("2").desc("장비-2번").build();
	}

	public static interface Q_MB_REG_RQ
	{
		public final static String STRUCTURE_NAME	= "Q_MB_REG_RQ";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_MB_DEL_RQ
	{
		public final static String STRUCTURE_NAME	= "Q_MB_DEL_RQ";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_MB_MOD_RQ
	{
		public final static String STRUCTURE_NAME	= "Q_MB_MOD_RQ";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_MB_BFH_RQ
	{
		public final static String STRUCTURE_NAME	= "Q_MB_BFH_RQ";
		public final static String STRUCTURE_DESC	= "";

		final static Field IPIN_CI 		= Field.builder().name("IPIN_CI")	.desc("").build();
		final static Field MESSAGE_ID 	= Field.builder().name("MESSAGE_ID").desc("").build();
		final static Field TRANS_DT		= Field.builder().name("TRANS_DT")	.desc("").build();
	}

	public static interface Q_REG_AGREE_RQ
	{
		public final static String STRUCTURE_NAME	= "Q_REG_AGREE_RQ";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_DEL_AGREE_RQ
	{
		public final static String STRUCTURE_NAME	= "Q_DEL_AGREE_RQ";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_REG_REJECTION_RQ
	{
		public final static String STRUCTURE_NAME	= "Q_REG_REJECTION_RQ";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_DEL_REJECTION_RQ
	{
		public final static String STRUCTURE_NAME	= "Q_DEL_REJECTION_RQ";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_REG_WHITE_LIST_RQ
	{
		public final static String STRUCTURE_NAME	= "Q_REG_WHITE_LIST_RQ";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_DEL_WHITE_LIST_RQ
	{
		public final static String STRUCTURE_NAME	= "Q_DEL_WHITE_LIST_RQ";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_MB_RETRY
	{
		public final static String STRUCTURE_NAME	= "Q_MB_RETRY";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_PM_RPT_RMD
	{
		final static String STRUCTURE_NAME 		= "Q_PM_RPT_RMD";
		final static String STRUCTURE_DESC 		= "";

		final static Field MSGKEY 				= Field.builder().name("MSGKEY")				.desc("메시지키")				.build();
		final static Field RPT_TYPE				= Field.builder().name("RPT_TYPE")				.desc("리포트 유형")			.build();

		final static Field CI 					= Field.builder().name("CI")					.desc("개인식별코드")			.build();
		final static Field PHONE 				= Field.builder().name("PHONE")					.desc("개인휴대폰번호")			.build();
		final static Field MESSAGE_ID			= Field.builder().name("MESSAGE_ID")			.desc("일련번호_수신키")		.build();
		final static Field MSG_TYPE				= Field.builder().name("MSG_TYPE")				.desc("발송 메시지 타입")		.build();

		final static Field BC_SND_RSLT_CD		= Field.builder().name("BC_SND_RSLT_CD")		.desc("메시지 발송 결과 코드")	.build();
		final static Field GW_SND_RSLT_DT		= Field.builder().name("GW_SND_RSLT_DT")		.desc("메시지 발송 완료 시간")	.build();
		final static Field BC_RPT_RSLT_CD		= Field.builder().name("BC_RPT_RSLT_CD")		.desc("메시지 수신 결과 코드")	.build();
		final static Field GW_RPT_RSLT_DT		= Field.builder().name("GW_RPT_RSLT_DT")		.desc("메지지 수신 완료 시간")	.build();

		final static Field MULTI_MBL_PRC_TYPE	= Field.builder().name("MULTI_MBL_PRC_TYPE")	.desc("다회선 사용자 처리여부")	.build();
		final static Field RCS_YN				= Field.builder().name("RCS_YN")				.desc("RCS 발송 여부")			.build();

		final static Field REG_DT				= Field.builder().name("REG_DT")				.desc("메시지 수신 시간")		.build();

		final static Field RETRY_CNT			= Field.builder().name("RETRY_CNT")				.desc("재전송_횟수")			.build();

		public static interface FIELD
		{
			public static interface RPT_TYPE
			{
				final static Code<String> SND_RESULT = Code.<String>builder().val("0").desc("메시지발송결과").build();
				final static Code<String> RSV_RESULT = Code.<String>builder().val("1").desc("메시지수신결과").build();
			}

			public static interface MSG_TYPE
			{
				final static Code<String> SMS = Code.<String>builder().val("S").desc("").build();
				final static Code<String> MMS = Code.<String>builder().val("M").desc("").build();
				final static Code<String> LMS = Code.<String>builder().val("L").desc("").build();
				final static Code<String> RCS = Code.<String>builder().val("R").desc("").build();
			}
		}
	}

	public static interface Q_PM_RPT
	{
		final static String STRUCTURE_NAME 		= "Q_PM_RPT";
		final static String STRUCTURE_DESC 		= "";

		final static Field MSGKEY 				= Field.builder().name("MSGKEY")				.desc("메시지키")				.build();
		final static Field RPT_TYPE				= Field.builder().name("RPT_TYPE")				.desc("리포트 유형")			.build();

		final static Field CI 					= Field.builder().name("CI")					.desc("개인식별코드")			.build();
		final static Field PHONE 				= Field.builder().name("PHONE")					.desc("개인휴대폰번호")			.build();
		final static Field MESSAGE_ID			= Field.builder().name("MESSAGE_ID")			.desc("일련번호_수신키")		.build();
		final static Field MSG_TYPE				= Field.builder().name("MSG_TYPE")				.desc("발송 메시지 타입")		.build();
		final static Field OPT_TYPE				= Field.builder().name("OPT_TYPE")				.desc("발송 발송 구분")			.build();
		final static Field SVC_ORG_CD			= Field.builder().name("SVC_ORG_CD")			.desc("서비스코드(기관코드)")		.build();
		final static Field CN_FORM				= Field.builder().name("CN_FORM")				.desc("문서명")				.build();
		final static Field SND_PLFM_ID			= Field.builder().name("SND_PLFM_ID")			.desc("송신자 플랫폼 ID")		.build();
		final static Field SND_NPOST			= Field.builder().name("SND_NPOST")				.desc("송신 공인전자주소")		.build();
		final static Field DOC_HASH				= Field.builder().name("DOC_HASH")				.desc("문서해시")				.build();

		final static Field BC_SND_RSLT_CD		= Field.builder().name("BC_SND_RSLT_CD")		.desc("메시지 발송 결과 코드")	.build();
		final static Field GW_SND_RSLT_DT		= Field.builder().name("GW_SND_RSLT_DT")		.desc("메시지 발송 완료 시간")	.build();

		final static Field BC_RPT_RSLT_CD		= Field.builder().name("BC_RPT_RSLT_CD")		.desc("메시지 수신 결과 코드")	.build();
		final static Field GW_RPT_RSLT_DT		= Field.builder().name("GW_RPT_RSLT_DT")		.desc("메지지 수신 완료 시간")	.build();

		final static Field TEST_SNDN_YN			= Field.builder().name("TEST_SNDN_YN")			.desc("테스트 발송여부")		.build();
		final static Field MULTI_MBL_PRC_TYPE	= Field.builder().name("MULTI_MBL_PRC_TYPE")	.desc("다회선 사용자 처리여부")	.build();
		final static Field DIST_INFO_CRT_YN		= Field.builder().name("DIST_INFO_CRT_YN")		.desc("유통정보생성여부")		.build();
		final static Field RCS_YN				= Field.builder().name("RCS_YN")				.desc("RCS 발송 여부")			.build();

		final static Field REG_DT				= Field.builder().name("REG_DT")				.desc("메시지 수신 시간")		.build();

		final static Field RETRY_CNT			= Field.builder().name("RETRY_CNT")				.desc("재전송_횟수")			.build();

		public static interface FIELD
		{
			public static interface SND_RSLT_CD
			{
				final static Code<String> SUCCESS = Code.<String>builder().val("40").desc("발송성공").build();
			}

			public static interface RPT_RSLT_CD
			{
				final static Code<String> SUCCESS = Code.<String>builder().val("50").desc("전송성공").build();
			}

			public static interface RPT_TYPE
			{
				final static Code<String> SND_RESULT = Code.<String>builder().val("0").desc("메시지발송결과").build();
				final static Code<String> RSV_RESULT = Code.<String>builder().val("1").desc("메시지수신결과").build();
			}

			public static interface MSG_TYPE
			{
				final static Code<String> SMS = Code.<String>builder().val("S").desc("").build();
				final static Code<String> MMS = Code.<String>builder().val("M").desc("").build();
				final static Code<String> LMS = Code.<String>builder().val("L").desc("").build();
				final static Code<String> RCS = Code.<String>builder().val("R").desc("").build();
			}
		}
	}

	public static interface Q_PM_RPT_RQ
	{
		final static String STRUCTURE_NAME	= "Q_PM_RPT_RQ";
		final static String STRUCTURE_DESC 	= "";

		final static Field MESSAGE_ID 		= Field.builder().name("MESSAGE_ID").desc("").build();
		final static Field PART_MM 			= Field.builder().name("PART_MM")	.desc("").build();

		final static Field RETRY_CNT		= Field.builder().name("RETRY_CNT") .desc("재전송_횟수").build();
	}

	public static interface Q_PM_RPT_RESULT
	{
		final static String STRUCTURE_NAME 		= "Q_PM_RPT_RESULT";
		final static String STRUCTURE_DESC 		= "";

		final static Field RPT_TYPE 			= Field.builder().name("RPT_TYPE")			.desc("리포트 타입").build();
		final static Field RPT_RSLT_CD 			= Field.builder().name("RPT_RSLT_CD")		.desc("BC 리포트 전송 결과").build();
		final static Field RCS_YN				= Field.builder().name("RCS_YN")			.desc("RCS 발송 여부").build();
		final static Field MSGKEY 				= Field.builder().name("MSGKEY")			.desc("메시지키").build();
		final static Field PART_MM 				= Field.builder().name("PART_MM")			.desc("저장시간").build();
		final static Field MESSAGE_ID 			= Field.builder().name("MESSAGE_ID")		.desc("일련번호_수신키").build();
		final static Field SEND_STAT 			= Field.builder().name("SEND_STAT")			.desc("처리결과").build();
		final static Field END_DT 				= Field.builder().name("END_DT")			.desc("완료일시").build();
		final static Field MULTI_MBL_PRC_TYPE	= Field.builder().name("MULTI_MBL_PRC_TYPE").desc("다회선 사용자 처리여부").build();

		public static interface FIELD
		{
			public static interface SEND_STAT_CD
			{
				final static Code<String> SUCCESS = Code.<String>builder().val("30").desc("메시지수신결과(성공)").build();
				final static Code<String> FAIL    = Code.<String>builder().val("31").desc("메시지수신결과(실패)").build();
			}
		}
	}

	public static interface Q_PM_RPT_RESULT_RMD
	{
		final static String STRUCTURE_NAME 		= "Q_PM_RPT_RESULT_RMD";
		final static String STRUCTURE_DESC 		= "";

		final static Field RPT_TYPE 			= Field.builder().name("RPT_TYPE")			.desc("리포트 타입").build();
		final static Field RPT_RSLT_CD 			= Field.builder().name("RPT_RSLT_CD")		.desc("BC 리포트 전송 결과").build();
		final static Field RCS_YN				= Field.builder().name("RCS_YN")			.desc("RCS 발송 여부").build();
		final static Field MSGKEY 				= Field.builder().name("MSGKEY")			.desc("메시지키").build();
		final static Field PART_MM 				= Field.builder().name("PART_MM")			.desc("저장시간").build();
		final static Field MESSAGE_ID 			= Field.builder().name("MESSAGE_ID")		.desc("일련번호_수신키").build();
		final static Field SEND_STAT 			= Field.builder().name("SEND_STAT")			.desc("처리결과").build();
		final static Field END_DT 				= Field.builder().name("END_DT")			.desc("완료일시").build();
		final static Field MULTI_MBL_PRC_TYPE	= Field.builder().name("MULTI_MBL_PRC_TYPE").desc("다회선 사용자 처리여부").build();

		public static interface FIELD
		{
			public static interface SEND_STAT_CD
			{
				final static Code<String> SUCCESS = Code.<String>builder().val("30").desc("메시지수신결과(성공)").build();
				final static Code<String> FAIL    = Code.<String>builder().val("31").desc("메시지수신결과(실패)").build();
			}
		}
	}

	public static interface Q_MS_RETRY
	{
		public final static String STRUCTURE_NAME	= "Q_MS_RETRY";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_KISA_DOC
	{
		public final static String STRUCTURE_NAME	= "Q_KISA_DOC";
		public final static String STRUCTURE_DESC	= "";

		final static Field MESSAGE_ID		= Field.builder().name("MESSAGE_ID")		.desc("일련번호_수신키")			.build();
		final static Field CI 				= Field.builder().name("CI")				.desc("개인식별코드")			.build();
		final static Field MSG_TYPE			= Field.builder().name("MSG_TYPE")			.desc("발송 메시지 타입")			.build();
		final static Field SVC_ORG_CD		= Field.builder().name("SVC_ORG_CD")		.desc("서비스코드(기관코드)")		.build();
		final static Field CN_FORM			= Field.builder().name("CN_FORM")			.desc("문서명")					.build();
		final static Field SND_PLFM_ID		= Field.builder().name("SND_PLFM_ID")		.desc("송신자 플랫폼 ID")		.build();
		final static Field SND_NPOST		= Field.builder().name("SND_NPOST")			.desc("송신 공인전자주소")		.build();
		final static Field DOC_HASH			= Field.builder().name("DOC_HASH")			.desc("문서해시")				.build();
		final static Field REG_DT			= Field.builder().name("REG_DT")			.desc("메시지 수신 시간")			.build();

		final static Field GW_SND_RSLT_DT	= Field.builder().name("GW_SND_RSLT_DT")	.desc("메시지 발송 완료 시간")	.build();
		final static Field GW_RPT_RSLT_DT	= Field.builder().name("GW_RPT_RSLT_DT")	.desc("메지지 수신 완료 시간")	.build();

		final static Field TEST_SNDN_YN		= Field.builder().name("TEST_SNDN_YN")		.desc("테스트 발송여부")			.build();
		final static Field KISA_DOC_TYPE	= Field.builder().name("KISA_DOC_TYPE")		.desc("전자문서 유형")			.build();
		
		public static interface FIELD
		{
			public static interface SEND_STAT_CD
			{
				final static Code<String> SUCCESS = Code.<String>builder().val("30").desc("메시지수신결과(성공)").build();
				final static Code<String> FAIL    = Code.<String>builder().val("31").desc("메시지수신결과(실패)").build();
			}
		}
	}

	public static interface Q_BC_AGREE_TO_SEND
	{
		public final static String STRUCTURE_NAME	= "Q_BC_AGREE_TO_SEND";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_BC_SND_RPT_TO_SEND
	{
		public final static String STRUCTURE_NAME	= "Q_BC_SND_RPT_TO_SEND";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_BC_RSV_RPT_TO_SEND
	{
		public final static String STRUCTURE_NAME	= "Q_BC_RSV_RPT_TO_SEND";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_BC_RMD_SND_RPT_TO_SEND
	{
		public final static String STRUCTURE_NAME	= "Q_BC_RMD_SND_RPT_TO_SEND";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_BC_RMD_RSV_RPT_TO_SEND
	{
		public final static String STRUCTURE_NAME	= "Q_BC_RMD_RSV_RPT_TO_SEND";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_BC_DOC_READ_DTM_TO_SEND
	{
		public final static String STRUCTURE_NAME	= "Q_BC_DOC_READ_DTM_TO_SEND";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_BC_DOC_ISS_TO_SEND
	{
		public final static String STRUCTURE_NAME	= "Q_BC_DOC_ISS_TO_SEND";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_BC_DOC_STS_CFM_ISS_TO_SEND
	{
		public final static String STRUCTURE_NAME	= "Q_BC_DOC_STS_CFM_ISS_TO_SEND";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_BC_RETRY
	{
		public final static String STRUCTURE_NAME	= "Q_BC_RETRY";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_KISA_EADDR_TO_SEND
	{
		public final static String STRUCTURE_NAME	= "Q_KISA_EADDR_TO_SEND";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_KISA_EADDR_RETRY_FROM_DATE
	{
		public final static String STRUCTURE_NAME	= "Q_KISA_EADDR_RETRY_FROM_DATE";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_KISA_EADDR_RETRY_FROM_SEQ
	{
		public final static String STRUCTURE_NAME	= "Q_KISA_EADDR_RETRY_FROM_SEQ";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_KISA_DOC_REG
	{
		public final static String STRUCTURE_NAME	= "Q_KISA_DOC_REG";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_KISA_DOC_REG_TO_SEND
	{
		public final static String STRUCTURE_NAME	= "Q_KISA_DOC_REG_TO_SEND";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_KISA_DOC_RSLT
	{
		public final static String STRUCTURE_NAME	= "Q_KISA_DOC_RSLT";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_KISA_DOC_REG_RD
	{
		public final static String STRUCTURE_NAME	= "Q_KISA_DOC_REG_RD";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_KISA_DOC_REG_RD_TO_SEND
	{
		public final static String STRUCTURE_NAME	= "Q_KISA_DOC_REG_RD_TO_SEND";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_KISA_RETRY
	{
		public final static String STRUCTURE_NAME	= "Q_KISA_DOC_RETRY";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_KISA_DOC_FIX_FROM_DATE
	{
		public final static String STRUCTURE_NAME	= "Q_KISA_DOC_FIX_FROM_DATE";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_KISA_DOC_FIX_FROM_SEQ
	{
		public final static String STRUCTURE_NAME	= "Q_KISA_DOC_FIX_FROM_SEQ";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_KISA_DOC_RETRY_FROM_DATE
	{
		public final static String STRUCTURE_NAME	= "Q_KISA_DOC_RETRY_FROM_DATE";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_KISA_DOC_RETRY_FROM_SEQ
	{
		public final static String STRUCTURE_NAME	= "Q_KISA_DOC_RETRY_FROM_SEQ";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_DOC_ISS
	{
		public final static String STRUCTURE_NAME	= "Q_DOC_ISS";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_DOC_STS_CFM_ISS
	{
		public final static String STRUCTURE_NAME	= "Q_DOC_STS_CFM_ISS";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_DOC_RETRY
	{
		public final static String STRUCTURE_NAME	= "Q_DOC_RETRY";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_BC_SESSION
	{
		public final static String STRUCTURE_NAME	= "Q_BC_SESSION";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface H_BC_SESSION
	{
		public final static String STRUCTURE_NAME	= "H_BC_SESSION";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface Q_KISA_SESSION
	{
		public final static String STRUCTURE_NAME	= "Q_KISA_SESSION";
		public final static String STRUCTURE_DESC	= "";
	}

	public static interface H_KISA_SESSION
	{
		public final static String STRUCTURE_NAME	= "H_KISA_SESSION";
		public final static String STRUCTURE_DESC	= "";
	}
}
