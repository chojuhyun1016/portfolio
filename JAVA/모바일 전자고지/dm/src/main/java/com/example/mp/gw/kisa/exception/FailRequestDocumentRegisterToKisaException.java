package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : FailRequestDcmntRegisterToKisaException.java
 * @Description : 유통증명서 등록 요청 오류
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


public class FailRequestDocumentRegisterToKisaException extends RuntimeException
{
	private static final long serialVersionUID = -4841953096941748190L;

	public FailRequestDocumentRegisterToKisaException()
	{
		super("KISA 유통증명서 등록 요청 실패입니다");
	}

	public FailRequestDocumentRegisterToKisaException(Throwable cause)
	{
		super(cause);
	}	
}
