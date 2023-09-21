package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : FailResponseToKisaException.java
 * @Description : 공인전자주소 전송 결과 오류 
 * 
 * @author 조주현
 * @since 2021.12.21
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


public class FailResponseToKisaException extends RuntimeException
{
	private static final long serialVersionUID = -7147819591322407391L;

	public FailResponseToKisaException()
	{
		super("KISA 전송 결과 오류입니다");
	}

	public FailResponseToKisaException(String msg)
	{
		super(msg);
	}

	public FailResponseToKisaException(Throwable cause)
	{
		super(cause);
	}
}
