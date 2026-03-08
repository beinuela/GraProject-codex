package com.campus.emergency.security;

import org.springframework.security.core.context.SecurityContextHolder;

public class AuthUtil {

    private AuthUtil() {}

    public static LoginUser currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    public static Long currentUserId() {
        LoginUser user = currentUser();
        return user == null ? null : user.getUserId();
    }

    public static String currentRole() {
        LoginUser user = currentUser();
        return user == null ? null : user.getRoleCode();
    }
}
