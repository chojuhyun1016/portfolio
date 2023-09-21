package com.example.mp.gw.common.domain;


import lombok.Builder;

/**
 * @Class Name : Filed.java
 * @Description : 필드 객체 
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
public class Field
{
	private String table;

	private String name;

	private String desc;
	
	public String table()
	{
		return table;
	}

	public String name()
	{
		return name;
	}
	
	public String desc()
	{
		return desc;
	}
	
	public String toString()
	{
		return name;
	}
}
