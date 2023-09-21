package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : FailRequestWithdrawToKisaException.java
 * @Description : 공인전자주소 탈퇴 요청 HTTP 오류 
 * 
 * @author 조주현
 * @since 2021.03.27
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.03.27	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class FailRequestWithdrawToKisaException extends RuntimeException
{
	private static final long serialVersionUID = 3853134199104950462L;

	public FailRequestWithdrawToKisaException()
	{
		super("KISA 공인전자주소 탈퇴 요청을 실패하였습니다");
	}

	public FailRequestWithdrawToKisaException(Throwable cause)
	{
		super(cause);
	}	
}

