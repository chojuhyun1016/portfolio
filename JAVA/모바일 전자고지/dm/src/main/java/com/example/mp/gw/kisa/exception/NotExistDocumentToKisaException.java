package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : NotExistDocumentToKisaException.java
 * @Description : 유통정보 없음 오류 
 * 
 * @author 조주현
 * @since 2023.05.09
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2023.05.09	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class NotExistDocumentToKisaException extends RuntimeException
{
	private static final long serialVersionUID = 7646559717348173768L;

	public NotExistDocumentToKisaException()
	{
		super("유통정보가 존재하지 않습니다");
	}

	public NotExistDocumentToKisaException(String msg)
	{
		super(msg);
	}
}
