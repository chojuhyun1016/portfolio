package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : FailRequestDocumentStatsConfirmationIssueToKisaException.java
 * @Description : KISA 전자문서 유통정보 수치 확인서 발급 실패 
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
 *  2021.03.14	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class FailRequestDocumentStatsConfirmationIssueToKisaException extends RuntimeException 
{
	private static final long serialVersionUID = 8841262120620973067L;

	public FailRequestDocumentStatsConfirmationIssueToKisaException()
	{
		super("KISA 전자문서 유통정보 수치 확인서 발급 요청 오류입니다");
	}

	public FailRequestDocumentStatsConfirmationIssueToKisaException(Throwable cause)
	{
		super(cause);
	}
}
