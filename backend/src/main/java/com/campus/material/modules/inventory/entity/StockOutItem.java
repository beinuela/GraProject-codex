package com.campus.material.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.material.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stock_out_item")
public class StockOutItem extends BaseEntity {
    private Long stockOutId;
    private Long materialId;
    private Integer quantity;
}
