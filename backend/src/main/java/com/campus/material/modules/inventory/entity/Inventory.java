package com.campus.material.modules.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.material.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("inventory")
public class Inventory extends BaseEntity {
    private Long materialId;
    private Long warehouseId;
    private Integer currentQty;
    private Integer lockedQty;
}
