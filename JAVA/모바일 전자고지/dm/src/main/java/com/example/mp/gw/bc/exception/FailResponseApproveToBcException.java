package com.example.mp.gw.bc.exception;


/**
 * @Class Name : FailResponseApproveToBcException.java
 * @Description : BizCenter 수신동의(등록/해제) 정보 요청 결과 오류
 * 
 * @author 조주현
 * @since 2022.05.13
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.05.13	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class FailResponseApproveToBcException extends RuntimeException
{
	private static final long serialVersionUID = -6218598708154417696L;

	public FailResponseApproveToBcException()
	{
		super("Biz-center 수신동의 정보 요청 결과 오류입니다");
	}

	public FailResponseApproveToBcException(Throwable cause)
	{
		super(cause);
	}
}
