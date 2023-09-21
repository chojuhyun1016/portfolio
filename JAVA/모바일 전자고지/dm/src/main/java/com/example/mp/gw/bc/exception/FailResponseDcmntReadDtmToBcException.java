package com.example.mp.gw.bc.exception;


/**
 * @Class Name : FailResponseDcmntReadDtmToBcException.java
 * @Description : BizCenter 유통증명서 열람일시 전송 결과 오류
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


public class FailResponseDcmntReadDtmToBcException extends RuntimeException
{
	private static final long serialVersionUID = 2888826610265325650L;

	public FailResponseDcmntReadDtmToBcException()
	{
		super("Biz-center 유통증명서 열람일시 전송 결과 오류입니다");
	}

	public FailResponseDcmntReadDtmToBcException(Throwable cause)
	{
		super(cause);
	}
}
