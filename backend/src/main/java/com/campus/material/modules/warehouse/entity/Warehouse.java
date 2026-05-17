package com.campus.material.modules.warehouse.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.material.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("warehouse")
public class Warehouse extends BaseEntity {
    private String warehouseCode;
    private String warehouseName;
    private Long campusId;
    private String campus;
    private String address;
    private String manager;
    private String contactPhone;
    private String status;
    private String remark;
}
