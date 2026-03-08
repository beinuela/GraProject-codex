package com.campus.emergency.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.emergency.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inventory_batch")
public class InventoryBatch extends BaseEntity {
    private Long materialId;
    private Long warehouseId;
    private String batchNo;
    private Integer inQty;
    private Integer remainQty;
    private LocalDate productionDate;
    private LocalDate expireDate;
}
