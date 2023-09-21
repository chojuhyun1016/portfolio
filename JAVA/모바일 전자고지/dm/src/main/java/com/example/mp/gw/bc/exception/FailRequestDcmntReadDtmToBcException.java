package com.example.mp.gw.bc.exception;


/**
 * @Class Name : FailRequestDcmntReadDtmToBcHttpException.java
 * @Description : BizCenter 유통증명서 열람일시 전송 요청 오류
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


public class FailRequestDcmntReadDtmToBcException extends RuntimeException
{
	private static final long serialVersionUID = -1563695724807099547L;

	public FailRequestDcmntReadDtmToBcException()
	{
		super("Biz-center 유통증명서 열람일시 전송 요청 오류입니다");
	}
	
	public FailRequestDcmntReadDtmToBcException(Throwable cause)
	{
		super(cause);
	}
}
