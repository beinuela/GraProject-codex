package com.campus.emergency.modules.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.emergency.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("auth_refresh_token")
public class AuthRefreshToken extends BaseEntity {
    private Long userId;
    private String tokenId;
    private String tokenHash;
    private LocalDateTime expireAt;
    private Integer revoked;
}
