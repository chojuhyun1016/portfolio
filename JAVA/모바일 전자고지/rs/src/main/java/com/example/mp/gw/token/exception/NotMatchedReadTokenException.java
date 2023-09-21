package com.example.mp.gw.token.exception;


/**
 * @Class Name : NotMatchedReadTokenException.java
 * @Description : 비정상 열람토큰 에러
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
public class NotMatchedReadTokenException extends RuntimeException
{
	public NotMatchedReadTokenException()
	{
		super("열람 토큰 정보가 올바르지 않습니다");
	}
}
