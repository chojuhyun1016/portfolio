package com.example.mp.gw.member.exception;


/**
 * @Class Name : NoCorpMemberException.java
 * @Description : 
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
public class NoPersonMemberException extends RuntimeException
{
	public NoPersonMemberException()
	{
		super("개인 회원만 수신거부가 가능합니다");
	}
}
