package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : FailResponseModifyToKisaException.java
 * @Description : 공인전자주소 소유자정보 수정 요청 결과 오류
 * 
 * @author 조주현
 * @since 2021.12.26
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.12.26	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class FailResponseModifyToKisaException extends RuntimeException
{
	private static final long serialVersionUID = -5728687156440796798L;

	public FailResponseModifyToKisaException()
	{
		super("KISA 공인전자주소 소유자정보 수정 요청 결과 오류입니다");
	}

	public FailResponseModifyToKisaException(Throwable cause)
	{
		super(cause);
	}
}
