package com.example.mp.gw.common.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @Class Name : HomeController.java
 * @Description : 기본 컨트롤러
 * 
 * @author 조주현
 * @since 2021.03.26
 * @version 1.0
 * @see
 *
 * <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.03.26	    조주현          최초 생성
 * 
 *  </pre>
 * 
 */


@Controller
public class HomeController
{
	@GetMapping("/")
	public String home()
	{
		return "redirect:/swagger-ui.html";
	}
	
	@GetMapping("/view")
	public String view()
	{
		return "view";
	}
}
