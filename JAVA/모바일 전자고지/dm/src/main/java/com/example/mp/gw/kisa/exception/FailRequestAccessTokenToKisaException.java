package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : ErrorRequestIssueDcmntToKisaHttpException.java
 * @Description : 공인전자주소 발급 요청 오류 
 * 
 * @author 조주현
 * @since 2021.12.18
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.12.18	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class FailRequestAccessTokenToKisaException extends RuntimeException
{
	private static final long serialVersionUID = 7576933025230483323L;

	public FailRequestAccessTokenToKisaException()
	{
		super("Kisa Access Token 오류입니다");
	}

	public FailRequestAccessTokenToKisaException(String msg)
	{
		super(msg);
	}	

	public FailRequestAccessTokenToKisaException(Throwable cause)
	{
		super(cause);
	}	
}
