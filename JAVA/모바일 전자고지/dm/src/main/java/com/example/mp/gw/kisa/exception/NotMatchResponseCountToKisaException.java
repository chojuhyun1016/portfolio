package com.example.mp.gw.kisa.exception;


/**
 * @Class Name : NotMatchResponseCountToKisaException.java
 * @Description : KISA 결과 건 수 오류 
 * 
 * @author 조주현
 * @since 2022.01.28
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.01.28	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class NotMatchResponseCountToKisaException extends RuntimeException
{
	private static final long serialVersionUID = -753657798311058255L;

	public NotMatchResponseCountToKisaException()
	{
		super("KISA 결과 건 수 오류입니다");
	}
}
