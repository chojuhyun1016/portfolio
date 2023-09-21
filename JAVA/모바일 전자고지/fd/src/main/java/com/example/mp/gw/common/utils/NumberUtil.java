package com.example.mp.gw.common.utils;


/**
 * @Class Name : NumberUtil.java
 * @Description : 숫자 유틸
 * 
 * @author 조주현
 * @since 2021.04.02
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.04.02	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public class NumberUtil
{
	final static Integer DEFAULT_INTEGER= 0;
	
	public static Integer toIntegerFrom(Object value)
	{
		if (null == value)
			return DEFAULT_INTEGER;
		
		try
		{
			return Integer.valueOf(StringUtil.toStringFrom(value));
		}
		catch (NumberFormatException e)
		{
			return DEFAULT_INTEGER;
		}
	}
	
	public static Integer toIntegerFrom(Object value,Integer defaultIntger)
	{
		if (null == value)
			return defaultIntger;
		
		try
		{
			return Integer.valueOf(StringUtil.toStringFrom(value));
		}
		catch (NumberFormatException e)
		{
			return defaultIntger;
		}
	}
}
