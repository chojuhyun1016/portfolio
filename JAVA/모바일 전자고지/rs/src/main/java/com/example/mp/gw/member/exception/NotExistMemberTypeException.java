package com.example.mp.gw.member.exception;


/**
 * @Class Name : NotExistMemberTypeException.java
 * @Description : 회원 유형 예외
 * 
 * @author 조주현
 * @since 2022.10.04
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2022.10.04	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class NotExistMemberTypeException extends RuntimeException
{
	private static final long serialVersionUID = 4267130137408361874L;

	public NotExistMemberTypeException()
	{
		super("존재하지 않는 회원 유형입니다.");
	}
}
