package com.example.mp.gw.bc.exception;


/**
 * @Class Name : FailResponseToBcException.java
 * @Description : 공인전자주소 전송 결과 오류 
 * 
 * @author 조주현
 * @since 2021.12.22
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.12.22	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class FailResponseToBcException extends RuntimeException
{
	private static final long serialVersionUID = 3715723388753601998L;

	public FailResponseToBcException()
	{
		super("Biz-center 전송 결과 오류입니다");
	}

	public FailResponseToBcException(String msg)
	{
		super(msg);
	}

	public FailResponseToBcException(Throwable cause)
	{
		super(cause);
	}
}
