package com.example.mp.gw.bc.exception;


/**
 * @Class Name : FailResponseRejectionToBcException.java
 * @Description : BizCenter 수신거보 요청 결과 오류
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


public class FailResponseRejectionToBcException extends RuntimeException
{
	private static final long serialVersionUID = 1953797885102449449L;

	public FailResponseRejectionToBcException()
	{
		super("Biz-center 수신거부 요청 결과 오류입니다");
	}

	public FailResponseRejectionToBcException(Throwable cause)
	{
		super(cause);
	}
}
