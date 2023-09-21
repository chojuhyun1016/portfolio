package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : FailRequestModifyToKisaException.java
 * @Description : 공인전자주소 소유자정보 수정 요청 에러 
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


public class FailRequestModifyToKisaException extends RuntimeException
{
	private static final long serialVersionUID = 3635103783343695531L;

	public FailRequestModifyToKisaException()
	{
		super("KISA 공인전자주소 소유자정보 수정 요청을 실패하였습니다");
	}

	public FailRequestModifyToKisaException(Throwable cause)
	{
		super(cause);
	}
}
