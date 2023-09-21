package com.example.mp.gw.common.domain;


import lombok.Builder;

/**
 * @Class Name : Code.java
 * @Description : 공통코드 객체
 * 
 * @author 조주현
 * @since 2021.04.05
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.05	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Builder
public class Code<T>
{
	private T      val;

	@SuppressWarnings("unused")
	private String desc;

	public T val()
	{
		return val;
	}

	public String toString()
	{
		return val.toString();
	}
}
