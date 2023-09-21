package com.example.mp.gw.bc.exception;


/**
 * @Class Name : FailResponseReportToBcException.java
 * @Description : Biz-Center 메시지(Report) 전송 결과 결과 오류 
 * 
 * @author 조주현
 * @since 2021.12.22
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.12.22	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class FailResponseReportToBcException extends RuntimeException
{
	private static final long serialVersionUID = 5513579470925924735L;

	public FailResponseReportToBcException()
	{
		super("Biz-center Report Response 전송 결과 오류입니다");
	}

	public FailResponseReportToBcException(Throwable cause)
	{
		super(cause);
	}
}
