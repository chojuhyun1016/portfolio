package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : FailResponseUnKnownToKisaException.java
 * @Description : 알 수 없는 오류 
 * 
 * @author 조주현
 * @since 2023.05.09
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2023.05.09	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class FailResponseUnKnownToKisaException extends RuntimeException
{
	private static final long serialVersionUID = -4181329334151006542L;

	public FailResponseUnKnownToKisaException()
	{
		super("알 수 없는 오류가 발생했습니다");
	}
}
