package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : FailRequestRegisterToKisaException.java
 * @Description : 공인전자주소 등록 요청 오류 
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


public class FailRequestRegisterToKisaException extends RuntimeException
{
	private static final long serialVersionUID = -5803445244817942165L;

	public FailRequestRegisterToKisaException()
	{
		super("KISA 공인전자주소 등록 요청 오류입니다");
	}

	public FailRequestRegisterToKisaException(Throwable cause)
	{
		super(cause);
	}
}

