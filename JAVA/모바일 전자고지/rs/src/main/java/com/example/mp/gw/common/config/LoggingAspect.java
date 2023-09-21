package com.example.mp.gw.common.config;


import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.mp.gw.common.domain.Const;

import lombok.extern.slf4j.Slf4j;

/**
 * @Class Name : LoggingAspect.java
 * @Description :
 * 
 * @author 조주현
 * @since 2021.05.16
 * @version 1.0
 * @see
 *
 *      <pre>
 * << 개정이력(Modification Information) >>
 * 
 *   수정일			수정자          수정내용
 *  -----------  -------------    ---------------------------
 *  2021.05.16	    조주현          최초 생성
 * 
 *      </pre>
 * 
 */


@Slf4j
@Aspect
@Component
public class LoggingAspect
{
	@Pointcut("execution(* com.uplus.mp.gw.*.controller.*.*(..) )")
	public void controllerAdvice()
	{
	}

	@Before("controllerAdvice()")
	public void requestLogging(JoinPoint joinPoint)
	{
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

		String traceId = request.getHeader(Const.TRACING_ID);

		MDC.put(Const.TRACING_ID, traceId == null ? UUID.randomUUID().toString() : traceId);

		log.debug("{ \"{}\": \"{}\", \"type\": \"request\", \"httpMethod\": \"{}\", \"requestUrl\": \"{}\", \"requestArgs\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), request.getMethod(), request.getRequestURL(), joinPoint.getArgs());
	}

	@AfterReturning(pointcut="controllerAdvice()", returning="returnValue")
	public void requestLogging(JoinPoint joinPoint, Object returnValue)
	{
		HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();

		log.debug("{ \"{}\": \"{}\", \"type\": \"response\", \"httpMethod\": \"{}\", \"requestUrl\": \"{}\", \"responseResult\": \"{}\" }", Const.TRACING_ID, MDC.get(Const.TRACING_ID), request.getMethod(), request.getRequestURL(), returnValue);

		MDC.clear();
	}
}
