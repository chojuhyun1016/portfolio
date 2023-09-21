package com.example.mp.gw.bc.exception;


/**
 * @Class Name : FailRequestDcmntIssueToBcHttpException.java
 * @Description : BizCenter 유통증명서 전송 요청 오류
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


public class FailRequestDcmntIssueToBcException extends RuntimeException
{
	private static final long serialVersionUID = 4763891543718857451L;

	public FailRequestDcmntIssueToBcException()
	{
		super("Biz-center 유통증명서 전송 요청 오류입니다");
	}

	public FailRequestDcmntIssueToBcException(Throwable cause)
	{
		super(cause);
	}
}
