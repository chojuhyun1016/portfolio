package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : FailResponseDcmntIssueToKisaException.java
 * @Description : KISA 전자문서 유통정보 수치 확인서 발급 응답 오류 
 * 
 * @author 조주현
 * @since 2022.03.14
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.03.14	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class FailResponseDocumentStatsConfirmationIssueToKisaException extends RuntimeException
{
	private static final long serialVersionUID = 3877183085347905166L;

	public FailResponseDocumentStatsConfirmationIssueToKisaException()
	{
		super("KISA 전자문서 유통정보 수치 확인서 발급 응답 결과 오류입니다");
	}

	public FailResponseDocumentStatsConfirmationIssueToKisaException(Throwable cause)
	{
		super(cause);
	}
}
