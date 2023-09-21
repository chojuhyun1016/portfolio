package com.example.mp.gw.bc.exception;


/**
 * @Class Name : FailResponseWhitelistToBcException.java
 * @Description : BizCenter whitelist 전송 결과 오류
 * 
 * @author 조주현
 * @since 2021.12.21
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.12.21	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class FailResponseWhitelistToBcException extends RuntimeException
{
	private static final long serialVersionUID = -4053519877140128061L;

	public FailResponseWhitelistToBcException()
	{
		super("Biz-center Whitelist 전송 결과 오류입니다");
	}

	public FailResponseWhitelistToBcException(Throwable cause)
	{
		super(cause);
	}
}
