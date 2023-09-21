package com.example.mp.gw.common.function;


/**
 * @Class Name : MapThrowException.java
 * @Description : 예외를 던지는 Function 인터페이스
 * 
 * @author 조주현
 * @since 2021.04.26
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.26      조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@FunctionalInterface
public interface FunctionThrowsException<T, R, E extends Exception>
{
	R apply(T t) throws E;
}
