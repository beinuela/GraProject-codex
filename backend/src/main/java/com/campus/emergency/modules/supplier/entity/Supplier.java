package com.campus.emergency.modules.supplier.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.emergency.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("supplier")
public class Supplier extends BaseEntity {
    private String supplierName;
    private String contactPerson;
    private String contactPhone;
    private String email;
    private String address;
    private String supplyScope;
    private String remark;
}
