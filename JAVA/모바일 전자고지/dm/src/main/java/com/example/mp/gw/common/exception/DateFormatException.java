package com.example.mp.gw.common.exception;


/**
 * @Class Name : DateFormatException.java
 * @Description : 날짜 형식 오류 
 * 
 * @author 조주현
 * @since 2022.02.07
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.02.07	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class DateFormatException extends RuntimeException
{
	private static final long serialVersionUID = -1154519044426588009L;

	public DateFormatException()
	{
		super("요청 형식(날짜)이 올바르지 않습니다");
	}

	public DateFormatException(String date)
	{
		super("요청 형식(날짜)이 올바르지 않습니다" + "[" + date + "]");
	}

	public DateFormatException(Throwable cause)
	{
		super(cause);
	}
}
