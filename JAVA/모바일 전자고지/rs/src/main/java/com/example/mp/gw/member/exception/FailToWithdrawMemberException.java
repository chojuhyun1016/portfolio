package com.example.mp.gw.member.exception;


/**
 * @Class Name : FailToWithdrawMemberException.java
 * @Description : 회원 탈퇴 실패 에러 
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
public class FailToWithdrawMemberException extends RuntimeException
{
	public FailToWithdrawMemberException()
	{
		super("회원 탈퇴에 실피하였습니다");
	}
}
