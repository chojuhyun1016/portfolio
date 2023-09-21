package com.example.mp.gw.member.exception;


/**
 * @Class Name : AlreadyWithdrawnMemberException.java
 * @Description : 이미 탈퇴한 회원 에러
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
public class AlreadyWithdrawnMemberException extends RuntimeException
{
	public AlreadyWithdrawnMemberException()
	{
		super("회원정보가 존재하지 않습니다");
	}
}
