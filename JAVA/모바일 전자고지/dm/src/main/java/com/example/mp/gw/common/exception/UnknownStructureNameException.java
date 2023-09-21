package com.example.mp.gw.common.exception;


/**
 * @Class Name : UnknownStructureNameException.java
 * @Description : 등록되지 않은 Redis Queue 이름 
 * 
 * @author 조주현
 * @since 2022.01.20
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.01.20	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class UnknownStructureNameException extends RuntimeException
{
	private static final long serialVersionUID = -314680973086188276L;

	public UnknownStructureNameException()
	{
		super("등록되지 않은 Redis Queue 이름입니다");
	}

	public UnknownStructureNameException(Throwable cause)
	{
		super(cause);
	}
}
