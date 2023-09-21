package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : FailRequestDcmntRegisterRdToKisaException.java.java
 * @Description : 유통증명서 열람일시 등록 요청 오류 
 * 
 * @author 조주현
 * @since 2021.12.18
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.12.18	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class FailRequestDocumentRegisterRdToKisaException extends RuntimeException
{
	private static final long serialVersionUID = 5513379773133470115L;

	public FailRequestDocumentRegisterRdToKisaException()
	{
		super("KISA 유통증명서 열람일시 등록 요청 오류입니다");
	}

	public FailRequestDocumentRegisterRdToKisaException(Throwable cause)
	{
		super(cause);
	}	
}
