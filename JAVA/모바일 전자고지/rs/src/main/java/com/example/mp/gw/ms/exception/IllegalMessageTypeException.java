package com.example.mp.gw.ms.exception;


/**
 * @Class Name : IllegalMessageTypeException.java
 * @Description : 미존재 메시징 타입 요청 예외
 * 
 * @author 조주현
 * @since 2021.04.04
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.04	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@SuppressWarnings("serial")
public class IllegalMessageTypeException extends RuntimeException
{
	public IllegalMessageTypeException()
	{
		super("존재하지 않는 메시징 타입을 요청하였습니다");
	}
}
