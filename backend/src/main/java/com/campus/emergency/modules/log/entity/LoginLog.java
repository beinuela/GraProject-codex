package com.campus.emergency.modules.log.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.emergency.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("login_log")
public class LoginLog extends BaseEntity {
    private Long userId;
    private String username;
    private String loginIp;
    private String loginStatus;
    private LocalDateTime loginTime;
    private String userAgent;
    private String remark;
}
