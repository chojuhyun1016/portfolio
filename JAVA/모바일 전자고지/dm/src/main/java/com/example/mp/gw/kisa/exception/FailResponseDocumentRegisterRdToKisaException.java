package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : FailResponseDcmntRegisterRdToKisaException.java.java
 * @Description : 유통증명서 열람일시 등록 요청 결과 오류 
 * 
 * @author 조주현
 * @since 2021.12.23
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.12.23	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class FailResponseDocumentRegisterRdToKisaException extends RuntimeException
{
	private static final long serialVersionUID = -6316669343372327416L;

	public FailResponseDocumentRegisterRdToKisaException()
	{
		super("KISA 유통증명서 열람일시 등록 요청 결과 오류입니다");
	}

	public FailResponseDocumentRegisterRdToKisaException(Throwable cause)
	{
		super(cause);
	}	
}
