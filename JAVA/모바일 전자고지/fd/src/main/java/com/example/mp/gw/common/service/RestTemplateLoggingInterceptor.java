package com.example.mp.gw.common.service;
//package com.uplus.mp.gw.common.service;
//
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//
//import org.slf4j.MDC;
//import org.springframework.http.HttpRequest;
//import org.springframework.http.client.ClientHttpRequestExecution;
//import org.springframework.http.client.ClientHttpRequestInterceptor;
//import org.springframework.http.client.ClientHttpResponse;
//import org.springframework.stereotype.Component;
//
//import com.uplus.mp.gw.common.domain.Const;
//
//import lombok.extern.slf4j.Slf4j;
//
///**
// * @Class Name : RestTemplateLoggingInterceptor.java
// * @Description : RestTemplate 사용시 로깅을 위한 인터셉터
// *
// * @author 조주현
// * @since 2021.05.18
// * @version 1.0
// * @see
// *
// * <pre>
// * << 개정이력(Modification Information) >>
// *
// *   수정일			수정자          수정내용
// *  -----------  -------------    ---------------------------
// *  2021.05.18	    조주현          최초 생성
// *
// *  </pre>
// *
// */
//
//
//@Slf4j
//@Component
//public class RestTemplateLoggingInterceptor implements ClientHttpRequestInterceptor
//{
//	/**
//	 * @param request
//	 * @param body
//	 * @param execution
//	 * @return
//	 * @throws IOException
//	 */
//	@Override
//	public ClientHttpResponse intercept(HttpRequest request, byte[] reqBody, ClientHttpRequestExecution execution) throws IOException
//	{
//		log.debug("{ \"{}\": \"{}\", \"type\": \"request\", \"httpMethod\": \"{}\", \"requestUrl\": \"{}\", \"body\": \"{}\" }",Const.TRACING_ID,MDC.get(Const.TRACING_ID),request.getMethod(),request.getURI(),new String(reqBody, StandardCharsets.UTF_8));
//
//	    return execution.execute(request, reqBody);
//	}
//}
