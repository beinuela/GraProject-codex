package com.campus.material.modules.apply.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.material.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("apply_order")
public class ApplyOrder extends BaseEntity {
    private Long deptId;
    private Long applicantId;
    private Integer urgencyLevel;
    private String status;
    private String reason;
    private String scenario;
    private Integer fastTrack;
    private Long approverId;
    private String approveRemark;
    private LocalDateTime approveTime;
}
