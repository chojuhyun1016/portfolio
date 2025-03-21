package org.example.order.api.master.config.aop;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.example.order.api.common.config.module.CustomHttpServletRequestWrapper;
import org.example.order.common.auth.AccessUserManager;
import org.example.order.common.response.CommonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Enumeration;

@Aspect
@Component
@Slf4j
public class OrderLoggingAop {

    // Pointcut for all controller methods
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerMethods() {}

    @Before("controllerMethods()")
    public void before(JoinPoint joinPoint) throws IOException {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return;
        }

        HttpServletRequest request = new CustomHttpServletRequestWrapper(attributes.getRequest());
        logHttpRequest(request);
    }

    @AfterReturning(pointcut = "controllerMethods()", returning = "result")
    public void afterReturning(Object result) {
        logHttpResponse(result);
    }

    private void logHttpRequest(HttpServletRequest request) throws IOException {
        StringBuilder header = new StringBuilder();
        StringBuilder body = new StringBuilder();

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            header.append(headerName).append("=").append(headerValue).append("; ");
        }

        BufferedReader reader = request.getReader();
        String line;
        while ((line = reader.readLine()) != null) {
            body.append(line);
        }

        log.info("--------------------------------------------------------------------");
        log.info("Http Request");
        log.info("url : {}", request.getRequestURL());
        log.info("ip : {}", request.getRemoteAddr());
        log.info("access user : {}", AccessUserManager.getAccessUser() != null ? AccessUserManager.getAccessUser().toString() : "null");
        log.info("method : {}", request.getMethod());
        log.info("query string : {}", request.getQueryString());
        log.info("headers : {}", header);
        log.info("body : {}", body);
        log.info("--------------------------------------------------------------------");
    }

    private void logHttpResponse(Object result) {
        if (result instanceof ResponseEntity<?> responseEntity) {
            if (responseEntity.getBody() instanceof CommonResponse<?> commonResponse) {
                log.info("--------------------------------------------------------------------");
                log.info("Http Response");
                log.info("status : {}", responseEntity.getStatusCode());
                log.info("body : {}", commonResponse.getData() != null ? commonResponse.getData().toString() : "null");
                log.info("metadata : {}", commonResponse.getMetadata() != null ? commonResponse.getMetadata().toString() : "null");
                log.info("--------------------------------------------------------------------");
            }
        }
    }
}
