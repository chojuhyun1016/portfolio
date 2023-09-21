package com.example.mp.gw.token.exception;


/**
 * @Class Name : ExpiredTokenException.java
 * @Description : 만료 토큰 에러
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
public class ExpiredTokenException extends RuntimeException
{
	public ExpiredTokenException()
	{
		super("만료된 토큰입니다");
	}
}
