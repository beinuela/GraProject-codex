package com.campus.material.modules.campus.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.material.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("campus")
public class Campus extends BaseEntity {
    private String campusName;
    private String address;
    private String manager;
    private String contactPhone;
    private String remark;
}
