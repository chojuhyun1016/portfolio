package com.example.mp.gw.common.exception;


import lombok.Getter;
import lombok.Setter;

/**
 * @Class Name : StopRunningToWorkerException.java
 * @Description : Worker 지속 여부 결정 예외 
 * 
 * @author 조주현
 * @since 2021.12.20
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.12.20	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Getter
@Setter
public class StopRunningToWorkerException extends RuntimeException
{
	private static final long serialVersionUID = -8186551974824928202L;

	public StopRunningToWorkerException()
	{
		super("Worker 정지 예외가 발생했습니다");
	}

	public StopRunningToWorkerException(Throwable cause)
	{
		super(cause);
	}
}
