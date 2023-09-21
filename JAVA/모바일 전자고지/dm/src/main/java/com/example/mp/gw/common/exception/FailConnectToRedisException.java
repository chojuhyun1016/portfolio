package com.example.mp.gw.common.exception;


/**
 * @Class Name : FailConnectToRedisException.java
 * @Description : 레디스 연결 실패 에러 
 * 
 * @author 조주현
 * @since 2021.04.04
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.04	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class FailConnectToRedisException extends RuntimeException
{
	private static final long serialVersionUID = 2983773223750399079L;

	public FailConnectToRedisException()
	{
		super("레디스 연결에 실패하였습니다");
	}

}
