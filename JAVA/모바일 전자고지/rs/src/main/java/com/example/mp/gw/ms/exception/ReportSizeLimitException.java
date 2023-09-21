package com.example.mp.gw.ms.exception;


import com.example.mp.gw.common.domain.Const;

/**
 * @Class Name : RpoertSizeLimitException.java
 * @Description : 발송결과 요청 갯수 제한 
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
public class ReportSizeLimitException extends RuntimeException
{
	public ReportSizeLimitException()
	{
		super("발송결과 요청 갯수 제한을 초과하였습니다 ("+Const.CONSTRAINTS.REQUEST_RPT_SIZE_LIMIT+"개)");
	}
}
