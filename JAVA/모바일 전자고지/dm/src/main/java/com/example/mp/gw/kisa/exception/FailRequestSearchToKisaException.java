package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : FailRequestSearchToKisaException.java
 * @Description : 공인전자주소 조회 요청 에러 
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


public class FailRequestSearchToKisaException extends RuntimeException
{
	private static final long serialVersionUID = 4649632363720528562L;

	public FailRequestSearchToKisaException()
	{
		super("KISA 공인전자주소 조회 요청을 실패하였습니다");
	}

	public FailRequestSearchToKisaException(Throwable cause)
	{
		super(cause);
	}
}
