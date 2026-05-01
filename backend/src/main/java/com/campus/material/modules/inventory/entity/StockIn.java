package com.campus.material.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.material.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stock_in")
public class StockIn extends BaseEntity {
    private Long warehouseId;
    private String sourceType;
    private Long operatorId;
    private String remark;
}
