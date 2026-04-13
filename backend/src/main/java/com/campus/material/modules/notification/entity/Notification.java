package com.campus.material.modules.notification.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.material.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notification")
public class Notification extends BaseEntity {
    private String title;
    private String content;
    private String msgType;
    private Long targetUserId;
    private Integer isRead;
    private String bizType;
    private Long bizId;
}
