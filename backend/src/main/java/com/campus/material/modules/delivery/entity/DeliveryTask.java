package com.campus.material.modules.delivery.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.material.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("delivery_task")
public class DeliveryTask extends BaseEntity {
    private Long applyOrderId;
    private Long stockOutId;
    private String receiverName;
    private String receiverPhone;
    private String deliveryAddress;
    private Long dispatcherId;
    private String status;
    private String remark;
    private LocalDateTime signedAt;
}
