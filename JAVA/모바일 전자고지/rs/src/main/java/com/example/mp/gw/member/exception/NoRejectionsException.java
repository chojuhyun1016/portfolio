package com.example.mp.gw.member.exception;


/**
 * @Class Name : NoRejectionsException.java
 * @Description : 수신거부목록이 없을 경우 발생하는 예외
 * 
 * 
 * @author 조주현
 * @since 2021.03.26
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.03.26	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@SuppressWarnings("serial")
public class NoRejectionsException extends RuntimeException
{
	public NoRejectionsException()
	{
		super("수신거부목록이 존재하지 않습니다");
	}
}
