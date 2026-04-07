package com.campus.emergency.modules.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String token;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Integer expiresIn;
    private Long userId;
    private String username;
    private String realName;
    private String roleCode;
}
