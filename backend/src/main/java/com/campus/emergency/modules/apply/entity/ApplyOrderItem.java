package com.campus.emergency.modules.apply.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.emergency.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("apply_order_item")
public class ApplyOrderItem extends BaseEntity {
    private Long applyOrderId;
    private Long materialId;
    private Integer applyQty;
    private Integer actualQty;
}
