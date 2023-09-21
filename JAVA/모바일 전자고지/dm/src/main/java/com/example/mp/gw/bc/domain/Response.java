package com.example.mp.gw.bc.domain;


import java.util.List;
import java.util.Map;

/**
 * @Class Name : Response.java
 * @Description : BC 응답객체 (인터페이스 객체)
 * 
 * @author 조주현
 * @since 2022.03.14
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.03.14	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


public interface Response
{
	public void setErrors(List<Map<String, Object>> errors);
}
