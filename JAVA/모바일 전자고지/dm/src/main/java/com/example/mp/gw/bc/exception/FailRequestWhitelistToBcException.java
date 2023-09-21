package com.example.mp.gw.bc.exception;


/**
 * @Class Name : FailRequestWhitelistToBcException.java
 * @Description : BizCenter whitelist 전송 요청 실패
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


public class FailRequestWhitelistToBcException extends RuntimeException
{
	private static final long serialVersionUID = -8603051146390291294L;

	public FailRequestWhitelistToBcException()
	{
		super("Biz-center Whitelist 전송 오류가 발생했습니다");
	}

	public FailRequestWhitelistToBcException(Throwable cause)
	{
		super(cause);
	}
}
