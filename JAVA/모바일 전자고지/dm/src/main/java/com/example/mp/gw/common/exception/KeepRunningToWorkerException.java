package com.example.mp.gw.common.exception;


import lombok.Getter;
import lombok.Setter;

/**
 * @Class Name : KeepRunningToWorkerException.java
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
public class KeepRunningToWorkerException extends RuntimeException
{
	private static final long serialVersionUID = -8994711549801307459L;

	public KeepRunningToWorkerException()
	{
		super("Worker 지속 예외가 발생했습니다");
	}

	public KeepRunningToWorkerException(Throwable cause)
	{
		super(cause);
	}
}
