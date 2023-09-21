package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : FailResponseDcmntIssueToKisaException.java
 * @Description : 유통증명서 발급 실패 
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


public class FailResponseDocumentIssueToKisaException extends RuntimeException
{
	private static final long serialVersionUID = 334132350963393017L;

	public FailResponseDocumentIssueToKisaException()
	{
		super("KISA 유통증명서 발급 요청 응답 결과 오류입니다");
	}

	public FailResponseDocumentIssueToKisaException(Throwable cause)
	{
		super(cause);
	}	
}
