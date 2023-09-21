package com.example.mp.gw.ms.exception;


/**
 * @Class Name : FailRegisterRcsImageException.java
 * @Description : RCS 사전이미지 등록 실패 예외  
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


@SuppressWarnings("serial")
public class FailRegisterRcsImageException extends RuntimeException
{
	public FailRegisterRcsImageException()
	{
		super("RCS 사전이미지 등록이 실패하였습니다");
	}
}
