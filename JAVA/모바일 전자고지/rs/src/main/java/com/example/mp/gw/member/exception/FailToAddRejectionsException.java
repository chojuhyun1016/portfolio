package com.example.mp.gw.member.exception;


/**
 * @class name : FailToAddRejectionsException.java
 * @description : 수신거부 처리 실패 에러 
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
public class FailToAddRejectionsException extends RuntimeException
{
	public FailToAddRejectionsException()
	{
		super("수신거부 요청 목록이 이미 있거나 처리에 실패하였습니다");
	}
}
