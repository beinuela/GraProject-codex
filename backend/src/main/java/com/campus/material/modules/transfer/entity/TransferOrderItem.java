package com.campus.material.modules.transfer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.material.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("transfer_order_item")
public class TransferOrderItem extends BaseEntity {
    private Long transferOrderId;
    private Long materialId;
    private Integer quantity;
}
