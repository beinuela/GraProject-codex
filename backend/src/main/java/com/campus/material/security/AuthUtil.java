package com.campus.material.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

public class AuthUtil {

    private AuthUtil() {}

    public static LoginUser currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
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
