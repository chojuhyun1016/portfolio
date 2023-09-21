package com.example.mp.gw.common.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @Class Name : Logging.java
 * @Description : 로그 유틸
 * 
 * @author 조주현
 * @since 2021. 4. 19.
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자		수정내용
 *  -----------  ----------	------------------------------
 *  2021. 5. 13.	조주현		최초 생성
 * 
 *  </pre>
 * 
 */


@Component
public class Logging
{
	private static final Logger logger = LoggerFactory.getLogger(Logging.class);

	private static StringBuilder totMsg = new StringBuilder();

	private static StringBuilder param = new StringBuilder();

	private static String msg;


	/**
	 * debug			: debug 로깅처리 함수 
	 * @param logNm		: 로깅 업무 명칭
	 * @param reqVal	: 요청정보 내용
	 * @param resVal	: 응답정보 내용
	 */
	public static void debug(
							  String logNm
							, String reqVal
							, String resVal
							)	
	{
		
		setMsg(logNm, null, null, "REQUEST DATA", reqVal, "RESPONSE DATA", resVal);

		logger.debug(msg);
	}
	
	/**
	 * debug			: debug 로깅처리 함수 
	 * @param logNm		: 로깅 업무 명칭
	 * @param reqSysNm	: 요청시스템 명칭
	 * @param resSysNm	: 응답시스템 명칭
	 * @param reqVal	: 요청정보 내용
	 * @param resVal	: 응답정보 내용
	 */
	public static void debug(
							  String logNm
							, String reqSysNm
							, String resSysNm
							, String reqVal
							, String resVal
							)	
	{
		setMsg(logNm, reqSysNm, resSysNm, "REQUEST DATA", reqVal, "RESPONSE DATA", resVal);

		logger.debug(msg);
	}
	
	
	/**
	 * debug			: debug 로깅처리 함수 
	 * @param logNm		: 로깅 업무 명칭
	 * @param reqSysNm	: 요청시스템 명칭
	 * @param resSysNm	: 응답시스템 명칭
	 * @param reqDesc	: 요청정보 타이틀
	 * @param reqVal	: 요청정보 내용
	 * @param resDesc	: 응답정보 타이틀
	 * @param resVal	: 응답정보 내용
	 */
	public static void debug	(
									String logNm
									, String reqSysNm
									, String resSysNm
									, String reqDesc
									, String reqVal
									, String resDesc
									, String resVal
								)
	{
		
		setMsg(logNm, reqSysNm, resSysNm, reqDesc, reqVal, resDesc, resVal);
		logger.debug(msg);
		
	}
	
	/**
	 * info				: info 로깅처리 함수 
	 * @param logNm		: 로깅 업무 명칭
	 * @param reqVal	: 요청정보 내용
	 * @param resVal	: 응답정보 내용
	 */
	public static void info	(
							  String logNm
							, String reqVal
							, String resVal
							)	
	{
		
		setMsg(logNm, null, null, "REQUEST DATA", reqVal, "RESPONSE DATA", resVal);

		logger.info(msg);
		
	}
	
	/**
	 * info				: info 로깅처리 함수 
	 * @param logNm		: 로깅 업무 명칭
	 * @param reqSysNm	: 요청시스템 명칭
	 * @param resSysNm	: 응답시스템 명칭
	 * @param reqVal	: 요청정보 내용
	 * @param resVal	: 응답정보 내용
	 */
	public static void info	(
							  String logNm
							, String reqSysNm
							, String resSysNm
							, String reqVal
							, String resVal
							)
	{
		setMsg(logNm, reqSysNm, resSysNm, "REQUEST DATA", reqVal, "RESPONSE DATA", resVal);

		logger.info(msg);
	}
	
	/**
	 * info				: info 로깅처리 함수 
	 * @param logNm		: 로깅 업무 명칭
	 * @param reqSysNm	: 요청시스템 명칭
	 * @param resSysNm	: 응답시스템 명칭
	 * @param reqDesc	: 요청정보 타이틀
	 * @param reqVal	: 요청정보 내용
	 * @param resDesc	: 응답정보 타이틀
	 * @param resVal	: 응답정보 내용
	 */
	public static void info	(
							  String logNm
							, String reqSysNm
							, String resSysNm
							, String reqDesc
							, String reqVal
							, String resDesc
							, String resVal
							)
	{
		setMsg(logNm, reqSysNm, resSysNm, reqDesc, reqVal, resDesc, resVal);

		logger.info(msg);
	}
	
	/**
	 * warn				: warn 로깅처리 함수 
	 * @param logNm		: 로깅 업무 명칭
	 * @param reqVal	: 요청정보 내용
	 * @param resVal	: 응답정보 내용
	 */
	public static void warn	(
								String logNm
								, String reqVal
								, String resVal
							)	
	{
		
		setMsg(logNm, null, null, "REQUEST DATA", reqVal, "RESPONSE DATA", resVal);
		logger.warn(msg);
		
	}
	
	
	/**
	 * warn				: warn 로깅처리 함수 
	 * @param logNm		: 로깅 업무 명칭
	 * @param reqSysNm	: 요청시스템 명칭
	 * @param resSysNm	: 응답시스템 명칭
	 * @param reqVal	: 요청정보 내용
	 * @param resVal	: 응답정보 내용
	 */
	public static void warn	(
							  String logNm
							, String reqSysNm
							, String resSysNm
							, String reqVal
							, String resVal
							)	
	{
		setMsg(logNm, reqSysNm, resSysNm, "REQUEST DATA", reqVal, "RESPONSE DATA", resVal);

		logger.warn(msg);
	}
	
	/**
	 * warn				: warn 로깅처리 함수 
	 * @param logNm		: 로깅 업무 명칭
	 * @param reqSysNm	: 요청시스템 명칭
	 * @param resSysNm	: 응답시스템 명칭
	 * @param reqDesc	: 요청정보 타이틀
	 * @param reqVal	: 요청정보 내용
	 * @param resDesc	: 응답정보 타이틀
	 * @param resVal	: 응답정보 내용
	 */
	public static void warn	(
							  String logNm
							, String reqSysNm
							, String resSysNm
							, String reqDesc
							, String reqVal
							, String resDesc
							, String resVal
							)
	{
		setMsg(logNm, reqSysNm, resSysNm, reqDesc, reqVal, resDesc, resVal);

		logger.warn(msg);
	}
	
	/**
	 * error			: error 로깅처리 함수 
	 * @param logNm		: 로깅 업무 명칭
	 * @param reqVal	: 요청정보 내용
	 * @param resVal	: 응답정보 내용
	 */
	public static void error (
							   String logNm
							 , String reqVal
							 , String resVal
							 )	
	{
		setMsg(logNm, null, null, "REQUEST DATA", reqVal, "RESPONSE DATA", resVal);

		logger.error(msg);
	}
	
	/**
	 * error			: error 로깅처리 함수 
	 * @param logNm		: 로깅 업무 명칭
	 * @param reqSysNm	: 요청시스템 명칭
	 * @param resSysNm	: 응답시스템 명칭
	 * @param reqVal	: 요청정보 내용
	 * @param resVal	: 응답정보 내용
	 */
	public static void error (
							   String logNm
							 , String reqSysNm
							 , String resSysNm
							 , String reqVal
							 , String resVal
							 )
	{
		setMsg(logNm, reqSysNm, resSysNm, "REQUEST DATA", reqVal, "RESPONSE DATA", resVal);

		logger.error(msg);
	}
	
	/**
	 * error			: error 로깅처리 함수 
	 * @param logNm		: 로깅 업무 명칭
	 * @param reqSysNm	: 요청시스템 명칭
	 * @param resSysNm	: 응답시스템 명칭
	 * @param reqDesc	: 요청정보 타이틀
	 * @param reqVal	: 요청정보 내용
	 * @param resDesc	: 응답정보 타이틀
	 * @param resVal	: 응답정보 내용
	 */
	public static void error (
							   String logNm
							 , String reqSysNm
							 , String resSysNm
							 , String reqDesc
							 , String reqVal
							 , String resDesc
							 , String resVal
							 )
	{
		setMsg(logNm, reqSysNm, resSysNm, reqDesc, reqVal, resDesc, resVal);

		logger.error(msg);
	}
	
	/**
	 * trace			: trace 로깅처리 함수 
	 * @param logNm		: 로깅 업무 명칭
	 * @param reqVal	: 요청정보 내용
	 * @param resVal	: 응답정보 내용
	 */
	public static void trace (
							   String logNm
							 , String reqVal
							 , String resVal
							 )	
	{
		setMsg(logNm, null, null, "REQUEST DATA", reqVal, "RESPONSE DATA", resVal);

		logger.trace(msg);
	}
	
	/**
	 * trace			: trace 로깅처리 함수 
	 * @param logNm		: 로깅 업무 명칭
	 * @param reqSysNm	: 요청시스템 명칭
	 * @param resSysNm	: 응답시스템 명칭
	 * @param reqVal	: 요청정보 내용
	 * @param resVal	: 응답정보 내용
	 */
	public static void trace (
							   String logNm
							 , String reqSysNm
							 , String resSysNm
							 , String reqVal
							 , String resVal
							 )
	{
		setMsg(logNm, reqSysNm, resSysNm, "REQUEST DATA", reqVal, "RESPONSE DATA", resVal);

		logger.trace(msg);
	}
	
	/**
	 * trace			: trace 로깅처리 함수 
	 * @param logNm		: 로깅 업무 명칭
	 * @param reqSysNm	: 요청시스템 명칭
	 * @param resSysNm	: 응답시스템 명칭
	 * @param reqDesc	: 요청정보 타이틀
	 * @param reqVal	: 요청정보 내용
	 * @param resDesc	: 응답정보 타이틀
	 * @param resVal	: 응답정보 내용
	 */
	public static void trace (
							   String logNm
							 , String reqSysNm
							 , String resSysNm
							 , String reqDesc
							 , String reqVal
							 , String resDesc
							 , String resVal
							 )
	{
		setMsg(logNm, reqSysNm, resSysNm, reqDesc, reqVal, resDesc, resVal);

		logger.trace(msg);
	}
	
	/**
	 * setMsg			: 메시지 셋팅 변수 
	 * @param logNm		: 로깅 업무 명칭
	 * @param reqSysNm	: 요청시스템 명칭
	 * @param resSysNm	: 응답시스템 명칭
	 * @param reqDesc	: 요청정보 타이틀
	 * @param reqVal	: 요청정보 내용
	 * @param resDesc	: 응답정보 타이틀
	 * @param resVal	: 응답정보 내용
	 */
	private static void setMsg (
								 String logNm
							   , String reqSysNm
							   , String resSysNm
							   , String reqDesc
							   , String reqVal
							   , String resDesc
							   , String resVal
							   )
	{
		/* 전역변수 초기화 */
		clean();
		
		/* 로그 제목 셋팅 (S) */
		totMsg.append(" >>> ");
		totMsg.append(logNm);
		totMsg.append(" LOG >>> ");
		
		/* 로그 제목 셋팅 (E) */
		
		/* 통신 시스템 이름 셋팅 (S) */
		if (null != reqSysNm)
		{
			param.append("{");
			addParam("REQUEST SYSTEM", reqSysNm);
			param.append("}");
		}
		
		if (null != reqSysNm && null != resSysNm)
		{
			param.append(" -> ");
		}
		
		if (null != resSysNm)
		{
			param.append("{");
			addParam("RESPONSE SYSTEM", resSysNm);
			param.append("}");
		}
		
		if (null != reqSysNm && null != resSysNm)
		{
			param.append(" >>> ");
		}
		
		/* 통신 시스템 이름 셋팅 (E) */
		
		/* 통신 값 셋팅 (S) */
		
		param.append("{");
		
		addParam(reqDesc, reqVal);
		
		param.append("} -> {");
		
		addParam(resDesc, resVal);
		
		param.append("}");
		
		/* 통신 값 셋팅 (E) */
		
		/* 전역 변수 셋팅 (S)*/
		
		totMsg.append(param.toString());
		
		msg = totMsg.toString();
		
		/* 전역 변수 셋팅 (E)*/
	}
	
	/**
	 * clean	: 전역변수 값을 초기화한다. 
	 */
	private static void clean ()
	{
		totMsg = new StringBuilder();
		param = new StringBuilder();
		msg = null;
	}
	
	/**
	 * addParam				: "설명 : 값" 형태의 문자열을 전역변수에 추가한다. 
	 * @param description	: 설명
	 * @param value			: 값
	 */
	private static void addParam (String description, String value)
	{
		param.append(description);
		param.append(" : ");
		param.append(value);
	}
}
