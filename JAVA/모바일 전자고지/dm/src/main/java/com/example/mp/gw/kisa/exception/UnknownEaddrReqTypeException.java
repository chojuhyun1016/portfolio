package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : UnknownEaddrReqTypeException.java
 * @Description : 등록되지 않은 공인전자주소 요청 유형 
 * 
 * @author 조주현
 * @since 2022.02.17
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.02.17	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class UnknownEaddrReqTypeException extends RuntimeException
{
	private static final long serialVersionUID = 8545471071803411545L;

	public UnknownEaddrReqTypeException()
	{
		super("등록되지 않은 공인전자주소 요청 유형입니다");
	}

	public UnknownEaddrReqTypeException(Throwable cause)
	{
		super(cause);
	}
}
