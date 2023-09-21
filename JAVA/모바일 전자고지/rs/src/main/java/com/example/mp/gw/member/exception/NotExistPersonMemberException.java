package com.example.mp.gw.member.exception;


/**
 * @Class Name : NotExistPersonMemberException.java
 * @Description : 존재하지 않는 개인 회원 에러 
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
public class NotExistPersonMemberException extends RuntimeException
{
	public NotExistPersonMemberException()
	{
		super("요청하신 개인회원은 존재하지 않습니다");
	}
}
