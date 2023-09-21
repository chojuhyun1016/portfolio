package com.example.mp.gw.bc.exception;


/**
 * @Class Name : FailResponseStatusRejectionToBcException.java
 * @Description : BizCenter 수신서부 조회 결과 오류
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


public class FailResponseStatusRejectionToBcException extends RuntimeException
{
	private static final long serialVersionUID = 6179242342662452334L;

	public FailResponseStatusRejectionToBcException()
	{
		super("Biz-center 수신거부 조회 결과 오류입니다");
	}

	public FailResponseStatusRejectionToBcException(Throwable cause)
	{
		super(cause);
	}
}
