package com.example.mp.gw.common.exception;


/**
 * @class name : MalformedException.java
 * @description : 형식 불일치 예외
 * 
 * @author 조주현
 * @since 2021.03.27
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(modification information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.03.27	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class MalformedException extends RuntimeException
{
	private static final long serialVersionUID = 1815609792230542154L;

	public MalformedException()
	{
		super("형식이 올바르지 않습니다");
	}
}
