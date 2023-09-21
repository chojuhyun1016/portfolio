package com.example.mp.gw.member.exception;


/**
 * @class name : alreadywithdrawnmemberexception.java
 * @description : 이미 탈퇴한 회원 에러
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


@SuppressWarnings("serial")
public class AlreadyExistsMemberException extends RuntimeException
{
	public AlreadyExistsMemberException()
	{
		super("이미 존재하는 회원입니다");
	}
}
