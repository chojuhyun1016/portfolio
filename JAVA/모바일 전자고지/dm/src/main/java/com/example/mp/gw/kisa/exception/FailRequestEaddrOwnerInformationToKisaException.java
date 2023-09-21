package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : FailRequestEaddrOwnerInformationToKisaException.java
 * @Description : 공인전자주소 소유자정보 조회 요청 에러 
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


public class FailRequestEaddrOwnerInformationToKisaException extends RuntimeException
{
	private static final long serialVersionUID = 546529604728450585L;

	public FailRequestEaddrOwnerInformationToKisaException()
	{
		super("KISA 공인전자주소 소유자정보 조회 요청을 실패하였습니다");
	}

	public FailRequestEaddrOwnerInformationToKisaException(Throwable cause)
	{
		super(cause);
	}
}
