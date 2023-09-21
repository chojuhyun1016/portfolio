package com.example.mp.gw.common.domain;


import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

/**
 * @Class Name : Const.java
 * @Description : 공통코드 OR 상수값 객체
 * 
 * @author 조주현
 * @since 2021.04.05
 * @version 1.0
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.05	    조주현          최초 생성
 * 
 *      </pre>
 * 
 */


public class Const
{
	public static final String EMPTY_STRING = "";

	public static final boolean TRUE  = true;
	public static final boolean FALSE = false;

	public static final String TRUE_STR  = "true";
	public static final String FALSE_STR = "false";

	public static final String Y = "Y";
	public static final String N = "N";

	public static final String TRACING_ID = "traceId";

	public static interface CONSTRAINTS
	{
		public static final int REQUEST_RPT_SIZE_LIMIT  = 100;
		public static final Long MEMBER_CEA_RESULT_SKIP = 100L;
	}

	public static interface LGUPLUS
	{
		public static final String CEA_SUFFIX    = "LGUPLUS";
		public final static String LG_UPLUS_TYPE = "03";
	}

	public static interface MEMBER
	{
		// 회원 요청 유형
		public static interface REQ_TYPE
		{
			public static final String EADDR_REGISTER = "1";
			public static final String EADDR_WITHDRAW = "0";
			public static final String EADDR_MODIFY   = "2";

			public final static Code<String> REGISTER = Code.<String>builder().val("1").desc("회원 가입")			.build();
			public final static Code<String> WITHDRAW = Code.<String>builder().val("0").desc("회원 탈퇴")			.build();
			public final static Code<String> MODIFY   = Code.<String>builder().val("2").desc("회원 수정(소유자정보)")	.build();
		}

		// 동의 요청 유형
		public static interface AGREE_TYPE
		{
			public static final String AGREE_REGISTER = "1";
			public static final String AGREE_WITHDRAW = "0";
		}

		// 회원식별자(공인전자주소) 구분자
		public interface FORMAT
		{ 
			public final static Code<String> CEA_SEPERATOR = Code.<String>builder().val("|").desc("회원식별자(공인전자주소) 구분자").build();
		}

		// 회원 개인/법인 구분
		public static interface TYPE
		{
			public final static Code<String> PERSON = Code.<String>builder().val("0").desc("회원 개인/법인 구분 - 개인").build();
			public final static Code<String> CORP   = Code.<String>builder().val("1").desc("회원 개인/법인 구분 - 법인").build();
		}

		// 가입 경로
		public static interface ROUTE
		{
			public final static Code<String> WEB     = Code.<String>builder().val("W").desc("회원 가입 - 웹")		.build();
			public final static Code<String> BC      = Code.<String>builder().val("B").desc("회원 가입 - 비즈센터")	.build();
			public final static Code<String> BEF     = Code.<String>builder().val("A").desc("회원 가입 - 자동가입")	.build();
			public final static Code<String> MESSAGE = Code.<String>builder().val("M").desc("회원 가입 - 본문자")		.build();
			public final static Code<String> HYDIRD  = Code.<String>builder().val("H").desc("회원 가입 - 하이브리드").build();
		}

		// 회원 탈퇴 요청
		public static interface WITHDRAW_REQUEST
		{
			public static interface PRCS_RSLT
			{
				public final static Code<String> NOT_YET = Code.<String>builder().val("0").desc("회원 탈퇴 요청 - 미처리").build();
				public final static Code<String> DONE    = Code.<String>builder().val("1").desc("회원 탈퇴 요청 - 완료")  .build();
			}
		}

		public static interface KISA
		{
			public static interface USER_TYPE
			{
				public final static Code<String> PERSON = Code.<String>builder().val("XX").desc("KISA 사용자 구분값 - 개인").build();
			}

			public static interface LGUPLUS
			{
				public final static Code<String> RLY_PLTFM_ID = Code.<String>builder().val("lguplus-01-mms").desc("LG U+ 공인전자문서 중계자 플랫폼 아이디").build();
			}
		}

		public static interface EADDR_RSLT
		{
			public final static Code<String> SUCCESS = Code.<String>builder().val("Y").desc("성공)")	.build();
			public final static Code<String> FAIL    = Code.<String>builder().val("N").desc("실패")	.build();
		}

		public static interface EADDR_RSLT_CD
		{
			public final static Code<String> SUCCESS = Code.<String>builder().val("01").desc("성공 - KISA 연동")	.build();
			public final static Code<String> FAIL    = Code.<String>builder().val("00").desc("실패 - KISA 연동")	.build();
			public final static Code<String> ETC     = Code.<String>builder().val("99").desc("실패 - 내부")		.build();
		}
	}

	public static interface MESSAGE
	{
		public static interface REPORT_RESPONSE
		{
			public final static Code<String> SND_RESULT = Code.<String>builder().val("0").desc("발송 결과").build();
			public final static Code<String> RSV_RESULT = Code.<String>builder().val("1").desc("수신 결과").build();
		}
	}

	public static interface KISA
	{
		public static interface RESULT_CODE
		{
			public final static Code<Integer> FAIL    = Code.<Integer>builder().val(0).desc("KISA 연동 결과값(실패)").build();
			public final static Code<Integer> SUCCESS = Code.<Integer>builder().val(1).desc("KISA 연동 결과값(성공)").build();
		}

		public static interface TOKEN_YN
		{
			public final static Code<Integer> FAIL    = Code.<Integer>builder().val(0).desc("KISA Token 요청 결과값(실패)").build();
			public final static Code<Integer> SUCCESS = Code.<Integer>builder().val(1).desc("KISA Token 요청 결과값(성공)").build();
		}
	}

	public static interface DOCUMENT
	{
		public static interface ISSUE
		{
			public static interface REQ_GUBUN
			{
				public final static Code<String> PERSON = Code.<String>builder().val("0").desc("발급 요청자 구분 (개인)")		.build();
				public final static Code<String> CORP   = Code.<String>builder().val("1").desc("발급 요청자 구분 (법인)")		.build();
				public final static Code<String> BC     = Code.<String>builder().val("2").desc("발급 요청자 구분 (비즈센터)")	.build();
			}

			public static interface ISD_STATUS
			{
				public final static Code<String> REQ = Code.<String>builder().val("0").desc("요청").build();
				public final static Code<String> ISD = Code.<String>builder().val("1").desc("발급").build();
				public final static Code<String> SND = Code.<String>builder().val("2").desc("전송").build();
			}

			public static interface ISS_RSLT_CD
			{
				public final static Code<String> SUCCESS = Code.<String>builder().val("01").desc("성공").build();
				public final static Code<String> FAIL    = Code.<String>builder().val("00").desc("실패").build();
			}

			public static interface SND_RSLT_CD
			{
				public final static Code<String> SUCCESS = Code.<String>builder().val("01").desc("성공").build();
				public final static Code<String> FAIL    = Code.<String>builder().val("00").desc("실패").build();
			}
		}

		public static interface OPT_TYPE
		{
			public final static Code<List<String>> BUILD_DOC = Code.<List<String>>builder().val(Arrays.asList("1","3","5","7")).desc("유통정보 생성해야하는 OPT_TYPE 목록").build();

			public final static Code<String> MSG_BEFORE       = Code.<String>builder().val("0").desc("사전문자")							  .build();
			public final static Code<String> MSG_BEFORE_AGREE = Code.<String>builder().val("6").desc("사전문자(마케팅 수신동의 고객만 발송)").build();

			public final static Code<String> MSG_TEXT         = Code.<String>builder().val("1").desc("본문자-본문")								   .build();
			public final static Code<String> MSG_HYBRID       = Code.<String>builder().val("3").desc("본문자-하이브리드")							   .build();
			public final static Code<String> MSG_TEXT_AGREE   = Code.<String>builder().val("5").desc("본문자-본문(마케팅 수신동의 고객만 발송)")	   .build();
			public final static Code<String> MSG_HYBRID_AGREE = Code.<String>builder().val("7").desc("본문자-하이브리드(마케팅 수신동의 고객만 발송)").build();
		}

		public static interface READ_CONFIRM
		{
			public final static Code<String> Y = Code.<String>builder().val("Y").desc("Token 열람 확인 여부(Y)").build();
			public final static Code<String> N = Code.<String>builder().val("N").desc("Token 열람 확인 여부(N)").build();
		}

		public static interface VERIFY_GUBUN
		{
			public final static Code<String> REPLACE = Code.<String>builder().val("R").desc("Token 인증 대체")	  .build();
			public final static Code<String> VERIFY  = Code.<String>builder().val("V").desc("Token 인증 확인")	  .build();
			public final static Code<String> CONFIRM = Code.<String>builder().val("C").desc("Token 인증 열람 확인").build();
		}

		public static interface DCMNT_STAT
		{
			public final static Code<String> MAKE     = Code.<String>builder().val("M").desc("유통정보 생성")				  .build();
			public final static Code<String> REGISTER = Code.<String>builder().val("R").desc("KISA 유통정보 등록")			  .build();
			public final static Code<String> UPDATE   = Code.<String>builder().val("U").desc("KISA 유통정보 열람일시 등록")	  .build();
			public final static Code<String> HYBRID   = Code.<String>builder().val("H").desc("KISA 유통정보 열람일시 동시 등록").build();
		}

		public static interface DOC_TYPE
		{
			public final static Code<Integer> NORMAL    = Code.<Integer>builder().val(0).desc("일반문서").build();
			public final static Code<Integer> NOTICE_RD = Code.<Integer>builder().val(1).desc("알림서비스(열람일시 생성)")  .build();
			public final static Code<Integer> NOTICE    = Code.<Integer>builder().val(2).desc("알림서비스(열람일시 미생성)").build();
		}

		public static interface DCMNT_RSLT
		{
			public final static Code<String> SUCCESS = Code.<String>builder().val("Y").desc("성공)")	.build();
			public final static Code<String> FAIL    = Code.<String>builder().val("N").desc("실패")	.build();
		}

		public static interface DCMNT_RSLT_CD
		{
			public final static Code<String> SUCCESS   = Code.<String>builder().val("01").desc("성공 - KISA 연동")					 .build();
			public final static Code<String> FAIL      = Code.<String>builder().val("00").desc("실패 - KISA 연동")					 .build();
			public final static Code<String> DUPLICATE = Code.<String>builder().val("02").desc("중복 - KISA 연동")					 .build();
			public final static Code<String> RSV_CEA   = Code.<String>builder().val("05").desc("공인전자주소 없음(수신자) - KISA 연동").build();
			public final static Code<String> ETC       = Code.<String>builder().val("99").desc("실패 - 내부")						 .build();
		}
	}

	public static interface BIZCENTER
	{
		public static interface RESULT_CD
		{
			public final static Code<String> SUCCESS = Code.<String>builder().val("00").desc("성공 - 비즈센터 연동").build();
			public final static Code<String> FAIL    = Code.<String>builder().val("01").desc("실패 - 비즈센터 연동").build();
		}

		public static interface DOC_ISSUE_REQUEST
		{
			public final static Code<String> ISSUE_REQ = Code.<String>builder().val("1").desc("유통증명서 발급 요청").build();
			public final static Code<String> ISSUE_RES = Code.<String>builder().val("2").desc("유통증명서 발급 결과").build();
		}
	}

	public static interface FORMATTER
	{
		public final static Code<SimpleDateFormat> yyyyMMddHHmmss = Code.<SimpleDateFormat>builder()
				.val(new SimpleDateFormat("yyyyMMddHHmmss")).desc("날짜 포맷터 yyyyMMddHHmmss").build();

		public final static Code<SimpleDateFormat> yyyyMMdd_HHmmss = Code.<SimpleDateFormat>builder()
				.val(new SimpleDateFormat("yyyyMMdd_HHmmss")).desc("날짜 포맷터 yyyyMMdd_HHmmss").build();

		public final static Code<SimpleDateFormat> yyyyMMdd = Code.<SimpleDateFormat>builder()
				.val(new SimpleDateFormat("yyyyMMdd")).desc("날짜 포맷터 yyyyMMdd").build();
		public final static Code<SimpleDateFormat> yyyyMMddHHmmssForKisa = Code.<SimpleDateFormat>builder()
				.val(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).desc("날짜 포맷터 yyyy-MM-dd HH:mm:ss").build();				
	}
}
