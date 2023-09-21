package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : NotExistDocumentStatToKisaException.java
 * @Description : 유통정보 수치가 없음
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


public class NotExistDocumentStatToKisaException extends RuntimeException
{
	private static final long serialVersionUID = -811599597283113526L;

	public NotExistDocumentStatToKisaException()
	{
		super("유통정보 수치가 존재하지 않습니다");
	}

	public NotExistDocumentStatToKisaException(String msg)
	{
		super(msg);
	}
}
