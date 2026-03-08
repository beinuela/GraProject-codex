package com.campus.emergency.modules.transfer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.emergency.common.BaseEntity;
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
