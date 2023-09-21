package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : FailResponseSearchToKisaException.java
 * @Description : 공인전자주소 조회 결과 오류 
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
 *  2021.12.21	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class FailResponseSearchToKisaException extends RuntimeException
{
	private static final long serialVersionUID = -5446854025271269560L;

	public FailResponseSearchToKisaException()
	{
		super("KISA 공인전자주소 조회 결과 오류입니다");
	}

	public FailResponseSearchToKisaException(Throwable cause)
	{
		super(cause);
	}
}
