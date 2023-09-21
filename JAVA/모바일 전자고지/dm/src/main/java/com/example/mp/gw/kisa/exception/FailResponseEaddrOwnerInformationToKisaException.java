package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : FailResponseEaddrOwnerInformationToKisaException.java
 * @Description : 공인전자주소 소유자정보 조회 결과 오류 
 * 
 * @author 조주현
 * @since 2022.04.04
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.04.04	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class FailResponseEaddrOwnerInformationToKisaException extends RuntimeException
{
	private static final long serialVersionUID = -4080863710732864578L;

	public FailResponseEaddrOwnerInformationToKisaException()
	{
		super("KISA 공인전자주소 탈퇴이력 조회 결과 오류입니다");
	}

	public FailResponseEaddrOwnerInformationToKisaException(Throwable cause)
	{
		super(cause);
	}
}
