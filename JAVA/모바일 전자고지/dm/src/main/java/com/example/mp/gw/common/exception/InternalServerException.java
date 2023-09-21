package com.example.mp.gw.common.exception;


/**
 * @Class Name : InternalServerException.java
 * @Description : 내부 서버 오류 
 * 
 * @author 조주현
 * @since 2023.02.06
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2023.02.06	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class InternalServerException extends RuntimeException
{
	private static final long serialVersionUID = 8165131050196805045L;

	public InternalServerException()
	{
		super("내부 서버 오류가 발생했습니다");
	}

	public InternalServerException(Throwable cause)
	{
		super(cause);
	}
}
