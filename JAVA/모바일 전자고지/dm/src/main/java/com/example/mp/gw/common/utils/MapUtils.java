package com.example.mp.gw.common.utils;


import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @Class Name : MapUtil.java
 * @Description :  Map 유틸리티
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


public class MapUtils
{
	public static Map<?, ?> objToMap(Object obj)
	{
		return new ObjectMapper().convertValue(obj, Map.class);
	}
}
