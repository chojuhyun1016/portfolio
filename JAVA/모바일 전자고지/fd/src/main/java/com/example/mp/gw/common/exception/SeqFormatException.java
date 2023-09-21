package com.example.mp.gw.common.exception;


/**
 * @Class Name : SeqFormatException.java
 * @Description : 시퀀스 형식 오류 
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


public class SeqFormatException extends RuntimeException
{
	private static final long serialVersionUID = -6944480331493821752L;

	public SeqFormatException()
	{
		super("요청 형식(Sequence)이 올바르지 않습니다");
	}

	public SeqFormatException(String msg)
	{
		super("요청 형식(Sequence)이 올바르지 않습니다" + "[" + msg + "]");
	}

	public SeqFormatException(Throwable cause)
	{
		super(cause);
	}
}
