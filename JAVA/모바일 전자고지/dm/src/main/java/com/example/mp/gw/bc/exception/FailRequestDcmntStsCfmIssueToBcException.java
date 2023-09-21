package com.example.mp.gw.bc.exception;


/**
 * @Class Name : FailRequestDcmntStsCfmIssueToBcException.java
 * @Description : Biz-center 전자문서 유통정보 수치 확인서 발급 결과 수신 처리 전송 요청 오류
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


public class FailRequestDcmntStsCfmIssueToBcException extends RuntimeException
{
	private static final long serialVersionUID = 1164367236805963986L;

	public FailRequestDcmntStsCfmIssueToBcException()
	{
		super("Biz-center 전자문서 유통정보 수치 확인서 발급 결과 수신 처리 전송 요청 오류입니다");
	}

	public FailRequestDcmntStsCfmIssueToBcException(Throwable cause)
	{
		super(cause);
	}
}
