package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : FailRequestToKisaException.java
 * @Description : KISA 전송 요청 실패 
 * 
 * @author 조주현
 * @since 2021.12.22
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.12.22	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class FailRequestToKisaException extends RuntimeException
{
	private static final long serialVersionUID = -5908314801685040780L;

	public FailRequestToKisaException()
	{
		super("KISA 전송 요청을 실패했습니다");
	}

	public FailRequestToKisaException(Throwable cause)
	{
		super(cause);
	}	
}
