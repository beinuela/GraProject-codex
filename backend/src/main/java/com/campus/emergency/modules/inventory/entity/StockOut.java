package com.campus.emergency.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.emergency.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stock_out")
public class StockOut extends BaseEntity {
    private Long applyOrderId;
    private Long warehouseId;
    private Long operatorId;
    private String remark;
}
