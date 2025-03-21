package org.example.order.api.common.config.module;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.order.common.auth.AccessUserInfo;
import org.example.order.common.auth.AccessUserManager;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Objects;

public class AccessUserInterceptor implements HandlerInterceptor {
    public static final String HEADER_USER_ID = "x-user-id";
    public static final String HEADER_LOGIN_ID = "x-login-id";
    public static final String HEADER_USER_TYPE = "x-user-type";
    public static final String HEADER_ROLES = "x-roles";
    public static final String HEADER_GROUPS = "x-groups";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userIdHeader = request.getHeader(HEADER_USER_ID);
        String loginIdHeader = request.getHeader(HEADER_LOGIN_ID);
        String userTypeHeader = request.getHeader(HEADER_USER_TYPE);
        String rolesHeader = request.getHeader(HEADER_ROLES);
        String groupsHeader = request.getHeader(HEADER_GROUPS);

        Long userId = Objects.nonNull(userIdHeader) ? Long.parseLong(userIdHeader) : 0L;
        String loginId = Objects.nonNull(loginIdHeader) ? loginIdHeader : "";
        String userType = Objects.nonNull(userTypeHeader) ? userTypeHeader : "UNKNOWN";
        String roles = Objects.nonNull(rolesHeader) ? rolesHeader : "";
        String groups = Objects.nonNull(groupsHeader) ? groupsHeader : "";

        AccessUserManager.setAccessUser(AccessUserInfo.builder()
                .userId(userId)
                .loginId(loginId)
                .userType(userType)
                .roles(roles)
                .groups(groups)
                .build());
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // do nothing to pass
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AccessUserManager.clear();
    }
}
