package com.example.mp.gw.common.domain;


import java.util.List;
import java.util.Map;

/**
 * @Class Name : ApiResponse.java
 * @Description : 응답객체 (인터페이스)
 * 
 * @author 조주현
 * @since 2021.03.25
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.03.25	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public interface ApiResponse
{
	public void setErrors(List<Map<String, Object>> errors);
}
