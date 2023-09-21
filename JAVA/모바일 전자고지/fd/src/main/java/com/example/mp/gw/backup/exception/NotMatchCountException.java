package com.example.mp.gw.backup.exception;


/**
 * @Class Name : NotMatchCountException.java
 * @Description : DB 처리건수 불일치 
 * 
 * @author 조주현
 * @since 2023.08.08
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2023.08.08	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class NotMatchCountException extends RuntimeException
{
	private static final long serialVersionUID = -7534017080479262328L;

	public NotMatchCountException()
	{
		super("DB 처리건수가 일치하지 않습니다");
	}
}
