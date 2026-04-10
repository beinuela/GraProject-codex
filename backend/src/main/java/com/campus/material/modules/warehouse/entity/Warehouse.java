package com.campus.material.modules.warehouse.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.material.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("warehouse")
public class Warehouse extends BaseEntity {
    private String warehouseName;
    private String campus;
    private String address;
    private String manager;
}
