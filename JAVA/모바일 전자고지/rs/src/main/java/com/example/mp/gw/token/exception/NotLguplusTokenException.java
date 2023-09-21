package com.example.mp.gw.token.exception;


/**
 * @Class Name : NotLguplusTokenException.java
 * @Description : LG 유플러스 토큰 에러
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
public class NotLguplusTokenException extends RuntimeException
{
	public NotLguplusTokenException()
	{
		super("LG U+ 토큰이 아닙니다");
	}
}
