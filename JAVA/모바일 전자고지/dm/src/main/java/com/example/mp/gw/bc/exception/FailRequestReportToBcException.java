package com.example.mp.gw.bc.exception;


/**
 * @Class Name : FailRequestReportToBcHttpException.java
 * @Description : BizCenter 메시지 결과 전송 오류
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


public class FailRequestReportToBcException extends RuntimeException
{
	private static final long serialVersionUID = 3588102392412728983L;

	public FailRequestReportToBcException()
	{
		super("Biz-center Report Request 전송 오류가 발생했습니다");
	}

	public FailRequestReportToBcException(Throwable cause)
	{
		super(cause);
	}
}
