package com.example.mp.gw.member.exception;


/**
 * @Class Name : NotExistRejectionsException.java
 * @Description : 수신거부목록 중 실제 존재하지않는 아이디가 표함되어 있을 때 발생하는 예외
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
public class NotExistRejectionsException extends RuntimeException
{
	public NotExistRejectionsException()
	{
		super("요청하신 수신거부목록 중 존재하지 않는 아이디가 포함되어있습니다");
	}
}
