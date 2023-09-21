package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : FailResponseTokenToKisaException.java
 * @Description : Token 발급 요청 결과 오류 
 * 
 * @author 조주현
 * @since 2021.03.27
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.03.27	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class FailResponseTokenToKisaException extends RuntimeException
{
	private static final long serialVersionUID = 2090368388253811634L;

	public FailResponseTokenToKisaException()
	{
		super("KISA Token 발급요청 결과 오류입니다");
	}

	public FailResponseTokenToKisaException(Throwable cause)
	{
		super(cause);
	}	
}
