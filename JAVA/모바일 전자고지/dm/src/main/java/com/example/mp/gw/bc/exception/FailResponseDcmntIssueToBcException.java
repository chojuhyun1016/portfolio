package com.example.mp.gw.bc.exception;


/**
 * @Class Name : FailResponseDcmntIssueToBcException.java
 * @Description : BizCenter 유통증명서 전송 결과 오류
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


public class FailResponseDcmntIssueToBcException extends RuntimeException
{
	private static final long serialVersionUID = 2014279832424200711L;

	public FailResponseDcmntIssueToBcException()
	{
		super("Biz-center 유통증명서 전송 결과 오류입니다");
	}

	public FailResponseDcmntIssueToBcException(String msg)
	{
		super(msg);
	}

	public FailResponseDcmntIssueToBcException(Throwable cause)
	{
		super(cause);
	}
}
