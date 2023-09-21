package com.example.mp.gw.kisa.exception;

/**
 * @Class Name : NotExistEaddrReqException.java
 * @Description : 공인전자주소 요청(등록/탈퇴/수정) 이력 없음 
 * 
 * @author 조주현
 * @since 2022.03.29
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.03.29	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */

public class NotExistEaddrReqException extends RuntimeException
{
	private static final long serialVersionUID = 7980599062176359L;

	public NotExistEaddrReqException()
	{
		super("공인전자주소 요청(등록/탈퇴/수정) 이력이 없습니다");
	}

	public NotExistEaddrReqException(Throwable cause)
	{
		super(cause);
	}
}
