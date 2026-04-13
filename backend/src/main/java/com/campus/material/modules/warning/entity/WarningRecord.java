package com.campus.material.modules.warning.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.material.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("warning_record")
public class WarningRecord extends BaseEntity {
    private String warningType;
    private Long materialId;
    private Long warehouseId;
    private String content;
    private String handleStatus;
    private Long handlerId;
    private String handleRemark;
}
