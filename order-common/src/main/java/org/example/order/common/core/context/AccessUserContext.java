package org.example.order.common.core.context;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AccessUserContext {

    private static final ThreadLocal<AccessUserInfo> accessUserInfoThreadLocal = new ThreadLocal<>();

    public static AccessUserInfo getAccessUser() {
        AccessUserInfo accessUserInfo =  accessUserInfoThreadLocal.get();
        return accessUserInfo == null ? AccessUserInfo.unknown() : accessUserInfo;
    }

    public static void setAccessUser(AccessUserInfo accessUserInfo) {
        accessUserInfoThreadLocal.set(accessUserInfo);
    }

    public static AccessUserInfo clear() {
        AccessUserInfo accessSystemInfo = accessUserInfoThreadLocal.get();
        accessUserInfoThreadLocal.remove();
        return accessSystemInfo;
    }
}
