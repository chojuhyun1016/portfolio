package com.example.mp.gw.member.exception;


/**
 * @class name : FailToCancelRejectionsException.java
 * @description : 수신거부 취소 처리 실패 에러 
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
public class FailToCancelRejectionsException extends RuntimeException
{
	public FailToCancelRejectionsException()
	{
		super("수신거부 취소가 이미 완료되었거나 처리에 실패하였습니다");
	}
}
