package com.example.mp.gw.member.exception;


/**
 * @Class Name : NotExistsMemberOrNotPersonMemberException.java
 * @Description : 비회원 또는 비개인회원 예외 
 * 
 * @author 조주현
 * @since 2021.03.27
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.03.27	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@SuppressWarnings("serial")
public class NotExistsMemberOrNotPersonMemberException extends RuntimeException
{
	public NotExistsMemberOrNotPersonMemberException()
	{
		super("회원 정보가 존재하지 않거나 개인 회원이 아닙니다");
	}
}
