package com.campus.emergency.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.emergency.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("stock_in_item")
public class StockInItem extends BaseEntity {
    private Long stockInId;
    private Long materialId;
    private String batchNo;
    private Integer quantity;
    private LocalDate productionDate;
    private LocalDate expireDate;
}
