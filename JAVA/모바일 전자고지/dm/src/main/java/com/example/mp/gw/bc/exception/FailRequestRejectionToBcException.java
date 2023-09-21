package com.example.mp.gw.bc.exception;


/**
 * @Class Name : FailRequestRejectionToBcException.java
 * @Description : BizCenter 수신거보 요청 오류
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


public class FailRequestRejectionToBcException extends RuntimeException
{
	private static final long serialVersionUID = -3158424370727604312L;

	public FailRequestRejectionToBcException()
	{
		super("Biz-center 수신거부 요청 오류입니다");
	}

	public FailRequestRejectionToBcException(Throwable cause)
	{
		super(cause);
	}
}
