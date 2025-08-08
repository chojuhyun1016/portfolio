package org.example.order.common.web;

import org.example.order.common.core.context.AccessUserInfo;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.*;

/**
 * 게이트웨이 헤더 → AccessUserInfo 주입
 * - 설정(yml) 읽지 않음
 */
public class AccessUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String H_USER_ID = "X-User-Id";
    public static final String H_LOGIN_ID = "X-Login-Id";
    public static final String H_USER_TYPE = "X-User-Type";
    public static final String H_CLIENT_ROLES = "X-Client-Roles";
    public static final String H_CLIENT_GROUPS = "X-Client-Groups";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return AccessUserInfo.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory) {

        String userIdStr = webRequest.getHeader(H_USER_ID);
        String loginId = nvl(webRequest.getHeader(H_LOGIN_ID), "");
        String userType = nvl(webRequest.getHeader(H_USER_TYPE), "UNKNOWN");
        String roles = nvl(webRequest.getHeader(H_CLIENT_ROLES), "");
        String groups = nvl(webRequest.getHeader(H_CLIENT_GROUPS), "");

        Long userId = parseLong(userIdStr);

        if (userId == 0L && loginId.isBlank()) {
            return AccessUserInfo.unknown();
        }

        return AccessUserInfo.builder()
                .userId(userId)
                .loginId(loginId)
                .userType(userType)
                .roles(roles)
                .groups(groups)
                .build();
    }

    private static String nvl(String v, String def) {
        return StringUtils.hasText(v) ? v : def;
    }

    private static Long parseLong(String v) {
        if (!StringUtils.hasText(v)) {
            return 0L;
        }

        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}